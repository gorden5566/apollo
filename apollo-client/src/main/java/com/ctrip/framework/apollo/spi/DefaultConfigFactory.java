package com.ctrip.framework.apollo.spi;

import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.PropertiesCompatibleConfigFile;
import com.ctrip.framework.apollo.internals.PropertiesCompatibleFileConfigRepository;
import com.ctrip.framework.apollo.internals.TxtConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.internals.ConfigRepository;
import com.ctrip.framework.apollo.internals.DefaultConfig;
import com.ctrip.framework.apollo.internals.JsonConfigFile;
import com.ctrip.framework.apollo.internals.LocalFileConfigRepository;
import com.ctrip.framework.apollo.internals.PropertiesConfigFile;
import com.ctrip.framework.apollo.internals.RemoteConfigRepository;
import com.ctrip.framework.apollo.internals.XmlConfigFile;
import com.ctrip.framework.apollo.internals.YamlConfigFile;
import com.ctrip.framework.apollo.internals.YmlConfigFile;
import com.ctrip.framework.apollo.util.ConfigUtil;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigFactory implements ConfigFactory {

  private static final Logger logger = LoggerFactory.getLogger(DefaultConfigFactory.class);

  /**
   * ConfigUtil
   */
  private ConfigUtil m_configUtil;

  public DefaultConfigFactory() {
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public Config create(String namespace) {
    // 检测 namespace 对应的文件格式
    ConfigFileFormat format = determineFileFormat(namespace);

    // 与 properties 兼容的格式（yaml）
    if (ConfigFileFormat.isPropertiesCompatible(format)) {
      return new DefaultConfig(namespace, createPropertiesCompatibleFileConfigRepository(namespace, format));
    }

    return new DefaultConfig(namespace, createLocalConfigRepository(namespace));
  }

  @Override
  public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    ConfigRepository configRepository = createLocalConfigRepository(namespace);
    switch (configFileFormat) {
      case Properties:
        return new PropertiesConfigFile(namespace, configRepository);
      case XML:
        return new XmlConfigFile(namespace, configRepository);
      case JSON:
        return new JsonConfigFile(namespace, configRepository);
      case YAML:
        return new YamlConfigFile(namespace, configRepository);
      case YML:
        return new YmlConfigFile(namespace, configRepository);
      case TXT:
        return new TxtConfigFile(namespace, configRepository);
    }

    return null;
  }

  /**
   * 创建本地缓存文件仓库
   *
   * @param namespace
   * @return
   */
  LocalFileConfigRepository createLocalConfigRepository(String namespace) {
    // 本地模式
    if (m_configUtil.isInLocalMode()) {
      logger.warn(
          "==== Apollo is in local mode! Won't pull configs from remote server for namespace {} ! ====",
          namespace);
      // 仅从本地获取配置
      return new LocalFileConfigRepository(namespace);
    }

    // 远程仓库作为本地仓库同步的上游仓库
    return new LocalFileConfigRepository(namespace, createRemoteConfigRepository(namespace));
  }

  /**
   * 创建远程仓库
   *
   * @param namespace
   * @return
   */
  RemoteConfigRepository createRemoteConfigRepository(String namespace) {
    return new RemoteConfigRepository(namespace);
  }

  /**
   * 创建配置仓库
   *
   * @param namespace
   * @param format
   * @return
   */
  PropertiesCompatibleFileConfigRepository createPropertiesCompatibleFileConfigRepository(String namespace,
      ConfigFileFormat format) {
    // 去掉扩展名，获取到 namespace
    String actualNamespaceName = trimNamespaceFormat(namespace, format);

    // 获取配置文件
    PropertiesCompatibleConfigFile configFile = (PropertiesCompatibleConfigFile) ConfigService
        .getConfigFile(actualNamespaceName, format);

    return new PropertiesCompatibleFileConfigRepository(configFile);
  }

  // for namespaces whose format are not properties, the file extension must be present, e.g. application.yaml
  ConfigFileFormat determineFileFormat(String namespaceName) {
    // 转换为小写
    String lowerCase = namespaceName.toLowerCase();

    // 若扩展名是否为已知格式，则直接返回
    for (ConfigFileFormat format : ConfigFileFormat.values()) {
      if (lowerCase.endsWith("." + format.getValue())) {
        return format;
      }
    }

    return ConfigFileFormat.Properties;
  }

  /**
   * 去掉扩展名
   *
   * @param namespaceName
   * @param format
   * @return
   */
  String trimNamespaceFormat(String namespaceName, ConfigFileFormat format) {
    String extension = "." + format.getValue();
    if (!namespaceName.toLowerCase().endsWith(extension)) {
      return namespaceName;
    }

    return namespaceName.substring(0, namespaceName.length() - extension.length());
  }

}
