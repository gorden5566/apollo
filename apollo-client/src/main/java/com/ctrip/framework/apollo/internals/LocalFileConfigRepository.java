package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.enums.ConfigSourceType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class LocalFileConfigRepository extends AbstractConfigRepository
    implements RepositoryChangeListener {
  private static final Logger logger = LoggerFactory.getLogger(LocalFileConfigRepository.class);

  /**
   * 本地缓存子目录
   */
  private static final String CONFIG_DIR = "/config-cache";

  /**
   * namespace
   */
  private final String m_namespace;

  /**
   * 本地缓存目录
   */
  private File m_baseDir;

  /**
   * apollo 配置
   */
  private final ConfigUtil m_configUtil;

  /**
   * 属性值
   */
  private volatile Properties m_fileProperties;

  /**
   * 上游仓库
   */
  private volatile ConfigRepository m_upstream;

  /**
   * 配置来源类型
   */
  private volatile ConfigSourceType m_sourceType = ConfigSourceType.LOCAL;

  /**
   * Constructor.
   *
   * @param namespace the namespace
   */
  public LocalFileConfigRepository(String namespace) {
    this(namespace, null);
  }

  public LocalFileConfigRepository(String namespace, ConfigRepository upstream) {
    m_namespace = namespace;

    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);

    // 设置本地缓存目录
    this.setLocalCacheDir(findLocalCacheDir(), false);

    // 设置上游仓库
    this.setUpstreamRepository(upstream);

    // 从上游仓库同步数据
    this.trySync();
  }

  /**
   * 设置本地缓存目录
   *
   * @param baseDir 缓存目录
   * @param syncImmediately 是否立即同步
   */
  void setLocalCacheDir(File baseDir, boolean syncImmediately) {
    m_baseDir = baseDir;

    // 检查本地缓存目录
    this.checkLocalConfigCacheDir(m_baseDir);

    // 立即同步
    if (syncImmediately) {
      this.trySync();
    }
  }

  /**
   * 获取本地缓存目录
   *
   * @return
   */
  private File findLocalCacheDir() {
    try {
      String defaultCacheDir = m_configUtil.getDefaultLocalCacheDir();
      Path path = Paths.get(defaultCacheDir);
      if (!Files.exists(path)) {
        // 创建缓存目录
        Files.createDirectories(path);
      }
      if (Files.exists(path) && Files.isWritable(path)) {
        // 默认缓存目录下的 config-cache 目录
        return new File(defaultCacheDir, CONFIG_DIR);
      }
    } catch (Throwable ex) {
      //ignore
    }

    // 使用 class path 下的 config-cache 目录
    return new File(ClassLoaderUtil.getClassPath(), CONFIG_DIR);
  }

  @Override
  public Properties getConfig() {
    if (m_fileProperties == null) {
      sync();
    }
    Properties result = new Properties();
    result.putAll(m_fileProperties);
    return result;
  }

  @Override
  public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
    if (upstreamConfigRepository == null) {
      return;
    }
    //clear previous listener
    if (m_upstream != null) {
      // 清除 listener
      m_upstream.removeChangeListener(this);
    }
    m_upstream = upstreamConfigRepository;

    // 从 upstream 同步
    trySyncFromUpstream();

    // 添加 listener
    upstreamConfigRepository.addChangeListener(this);
  }

  @Override
  public ConfigSourceType getSourceType() {
    return m_sourceType;
  }

  @Override
  public void onRepositoryChange(String namespace, Properties newProperties) {
    if (newProperties.equals(m_fileProperties)) {
      return;
    }

    // 所有属性配置
    Properties newFileProperties = new Properties();
    newFileProperties.putAll(newProperties);

    // 更新文件缓存
    updateFileProperties(newFileProperties, m_upstream.getSourceType());

    // 通知仓库变更事件
    this.fireRepositoryChange(namespace, newProperties);
  }

  @Override
  protected void sync() {
    //sync with upstream immediately
    boolean syncFromUpstreamResultSuccess = trySyncFromUpstream();

    // 同步成功
    if (syncFromUpstreamResultSuccess) {
      return;
    }

    Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "syncLocalConfig");
    Throwable exception = null;
    try {
      transaction.addData("Basedir", m_baseDir.getAbsolutePath());

      // 从本地读取文件
      m_fileProperties = this.loadFromLocalCacheFile(m_baseDir, m_namespace);
      m_sourceType = ConfigSourceType.LOCAL;

      transaction.setStatus(Transaction.SUCCESS);
    } catch (Throwable ex) {
      Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
      transaction.setStatus(ex);
      exception = ex;
      //ignore
    } finally {
      transaction.complete();
    }

    if (m_fileProperties == null) {
      m_sourceType = ConfigSourceType.NONE;
      throw new ApolloConfigException(
          "Load config from local config failed!", exception);
    }
  }

  /**
   * 从上游仓库同步
   *
   * @return
   */
  private boolean trySyncFromUpstream() {
    if (m_upstream == null) {
      return false;
    }
    try {
      updateFileProperties(m_upstream.getConfig(), m_upstream.getSourceType());
      return true;
    } catch (Throwable ex) {
      Tracer.logError(ex);
      logger
          .warn("Sync config from upstream repository {} failed, reason: {}", m_upstream.getClass(),
              ExceptionUtil.getDetailMessage(ex));
    }
    return false;
  }

  /**
   * 更新文件中的属性值
   *
   * @param newProperties
   * @param sourceType
   */
  private synchronized void updateFileProperties(Properties newProperties, ConfigSourceType sourceType) {
    this.m_sourceType = sourceType;
    if (newProperties.equals(m_fileProperties)) {
      return;
    }
    this.m_fileProperties = newProperties;

    // 持久化到本地文件
    persistLocalCacheFile(m_baseDir, m_namespace);
  }

  /**
   * 从本地缓存读取属性值
   *
   * @param baseDir
   * @param namespace
   * @return
   * @throws IOException
   */
  private Properties loadFromLocalCacheFile(File baseDir, String namespace) throws IOException {
    Preconditions.checkNotNull(baseDir, "Basedir cannot be null");

    File file = assembleLocalCacheFile(baseDir, namespace);
    Properties properties = null;

    if (file.isFile() && file.canRead()) {
      InputStream in = null;

      try {
        in = new FileInputStream(file);

        properties = new Properties();
        properties.load(in);
        logger.debug("Loading local config file {} successfully!", file.getAbsolutePath());
      } catch (IOException ex) {
        Tracer.logError(ex);
        throw new ApolloConfigException(String
            .format("Loading config from local cache file %s failed", file.getAbsolutePath()), ex);
      } finally {
        try {
          if (in != null) {
            in.close();
          }
        } catch (IOException ex) {
          // ignore
        }
      }
    } else {
      throw new ApolloConfigException(
          String.format("Cannot read from local cache file %s", file.getAbsolutePath()));
    }

    return properties;
  }

  /**
   * 持久化本地缓存文件
   *
   * @param baseDir
   * @param namespace
   */
  void persistLocalCacheFile(File baseDir, String namespace) {
    if (baseDir == null) {
      return;
    }
    File file = assembleLocalCacheFile(baseDir, namespace);

    OutputStream out = null;

    // 事务监控
    Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "persistLocalConfigFile");
    transaction.addData("LocalConfigFile", file.getAbsolutePath());
    try {
      out = new FileOutputStream(file);
      m_fileProperties.store(out, "Persisted by DefaultConfig");

      // 事务状态为成功
      transaction.setStatus(Transaction.SUCCESS);
    } catch (IOException ex) {
      ApolloConfigException exception =
          new ApolloConfigException(
              String.format("Persist local cache file %s failed", file.getAbsolutePath()), ex);

      // error 埋点
      Tracer.logError(exception);

      // 事务状态为异常
      transaction.setStatus(exception);
      logger.warn("Persist local cache file {} failed, reason: {}.", file.getAbsolutePath(),
          ExceptionUtil.getDetailMessage(ex));
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ex) {
          //ignore
        }
      }
      // 事务结束
      transaction.complete();
    }
  }

  private void checkLocalConfigCacheDir(File baseDir) {
    // 目录已存在
    if (baseDir.exists()) {
      return;
    }
    Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "createLocalConfigDir");
    transaction.addData("BaseDir", baseDir.getAbsolutePath());
    try {
      // 创建本地缓存目录
      Files.createDirectory(baseDir.toPath());

      transaction.setStatus(Transaction.SUCCESS);
    } catch (IOException ex) {
      ApolloConfigException exception =
          new ApolloConfigException(
              String.format("Create local config directory %s failed", baseDir.getAbsolutePath()),
              ex);
      Tracer.logError(exception);
      transaction.setStatus(exception);
      logger.warn(
          "Unable to create local config cache directory {}, reason: {}. Will not able to cache config file.",
          baseDir.getAbsolutePath(), ExceptionUtil.getDetailMessage(ex));
    } finally {
      transaction.complete();
    }
  }

  /**
   * 组装缓存文件名
   *
   * @param baseDir
   * @param namespace
   * @return
   */
  File assembleLocalCacheFile(File baseDir, String namespace) {
    String fileName =
        String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
            .join(m_configUtil.getAppId(), m_configUtil.getCluster(), namespace));
    return new File(baseDir, fileName);
  }
}
