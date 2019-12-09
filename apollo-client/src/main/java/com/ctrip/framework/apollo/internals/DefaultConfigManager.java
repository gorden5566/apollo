package com.ctrip.framework.apollo.internals;

import java.util.Map;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.google.common.collect.Maps;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigManager implements ConfigManager {

  /**
   * ConfigFactory 管理工厂
   */
  private ConfigFactoryManager m_factoryManager;

  /**
   * key: namespace
   * value: 对应的配置实例
   */
  private Map<String, Config> m_configs = Maps.newConcurrentMap();

  /**
   * key: namespace 文件名
   * value: 配置文件
   */
  private Map<String, ConfigFile> m_configFiles = Maps.newConcurrentMap();

  public DefaultConfigManager() {
    m_factoryManager = ApolloInjector.getInstance(ConfigFactoryManager.class);
  }

  @Override
  public Config getConfig(String namespace) {
    // 到缓存中获取配置实例
    Config config = m_configs.get(namespace);

    if (config == null) {
      synchronized (this) {
        config = m_configs.get(namespace);

        if (config == null) {
          // 获取 namespace 对应的 ConfigFactory 实例
          ConfigFactory factory = m_factoryManager.getFactory(namespace);

          // 创建 config 实例
          config = factory.create(namespace);

          // 保存到缓存中
          m_configs.put(namespace, config);
        }
      }
    }

    return config;
  }

  @Override
  public ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    // namespace 文件名，例如 application.json
    String namespaceFileName = String.format("%s.%s", namespace, configFileFormat.getValue());

    // 到缓存中获取配置文件
    ConfigFile configFile = m_configFiles.get(namespaceFileName);

    if (configFile == null) {
      synchronized (this) {
        configFile = m_configFiles.get(namespaceFileName);

        if (configFile == null) {
          // 获取 namespace 对应的 ConfigFactory 实例
          ConfigFactory factory = m_factoryManager.getFactory(namespaceFileName);

          // 创建 configFile 实例
          configFile = factory.createConfigFile(namespaceFileName, configFileFormat);

          // 保存到缓存中
          m_configFiles.put(namespaceFileName, configFile);
        }
      }
    }

    return configFile;
  }
}
