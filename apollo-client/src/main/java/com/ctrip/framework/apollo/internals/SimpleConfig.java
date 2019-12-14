package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.enums.ConfigSourceType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class SimpleConfig extends AbstractConfig implements RepositoryChangeListener {
  private static final Logger logger = LoggerFactory.getLogger(SimpleConfig.class);

  /**
   * namespace
   */
  private final String m_namespace;

  /**
   * 配置仓库
   */
  private final ConfigRepository m_configRepository;

  /**
   * Properties
   */
  private volatile Properties m_configProperties;

  /**
   * 配置来源，即仓库类型
   */
  private volatile ConfigSourceType m_sourceType = ConfigSourceType.NONE;

  /**
   * Constructor.
   *
   * @param namespace        the namespace for this config instance
   * @param configRepository the config repository for this config instance
   */
  public SimpleConfig(String namespace, ConfigRepository configRepository) {
    m_namespace = namespace;
    m_configRepository = configRepository;
    this.initialize();
  }

  private void initialize() {
    try {
      updateConfig(m_configRepository.getConfig(), m_configRepository.getSourceType());
    } catch (Throwable ex) {
      Tracer.logError(ex);
      logger.warn("Init Apollo Simple Config failed - namespace: {}, reason: {}", m_namespace,
          ExceptionUtil.getDetailMessage(ex));
    } finally {
      //register the change listener no matter config repository is working or not
      //so that whenever config repository is recovered, config could get changed
      m_configRepository.addChangeListener(this);
    }
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    if (m_configProperties == null) {
      logger.warn("Could not load config from Apollo, always return default value!");
      return defaultValue;
    }
    return this.m_configProperties.getProperty(key, defaultValue);
  }

  @Override
  public Set<String> getPropertyNames() {
    if (m_configProperties == null) {
      return Collections.emptySet();
    }

    return m_configProperties.stringPropertyNames();
  }

  @Override
  public ConfigSourceType getSourceType() {
    return m_sourceType;
  }

  @Override
  public synchronized void onRepositoryChange(String namespace, Properties newProperties) {
    // 配置未变更
    if (newProperties.equals(m_configProperties)) {
      return;
    }

    // 复制一份
    Properties newConfigProperties = new Properties();
    newConfigProperties.putAll(newProperties);

    // 计算变更
    List<ConfigChange> changes = calcPropertyChanges(namespace, m_configProperties, newConfigProperties);

    // 转换为map，key为propertyName
    Map<String, ConfigChange> changeMap = Maps.uniqueIndex(changes,
        new Function<ConfigChange, String>() {
          @Override
          public String apply(ConfigChange input) {
            return input.getPropertyName();
          }
        });

    // 更新配置
    updateConfig(newConfigProperties, m_configRepository.getSourceType());

    // 清空配置缓存
    clearConfigCache();

    // 通知变更事件
    this.fireConfigChange(new ConfigChangeEvent(m_namespace, changeMap));

    Tracer.logEvent("Apollo.Client.ConfigChanges", m_namespace);
  }

  private void updateConfig(Properties newConfigProperties, ConfigSourceType sourceType) {
    m_configProperties = newConfigProperties;
    m_sourceType = sourceType;
  }
}
