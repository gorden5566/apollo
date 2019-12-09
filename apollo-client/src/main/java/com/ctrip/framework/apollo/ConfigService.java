package com.ctrip.framework.apollo;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigRegistry;

/**
 * 客户端获取配置的入口
 *
 * Entry point for client config use
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigService {
  /**
   * ConfigService 单例
   */
  private static final ConfigService s_instance = new ConfigService();

  /**
   * Config 管理工厂
   */
  private volatile ConfigManager m_configManager;

  /**
   * ConfigFactory 注册中心
   */
  private volatile ConfigRegistry m_configRegistry;

  /**
   * 获取 Config 管理工厂
   *
   * @return
   */
  private ConfigManager getManager() {
    if (m_configManager == null) {
      synchronized (this) {
        if (m_configManager == null) {
          m_configManager = ApolloInjector.getInstance(ConfigManager.class);
        }
      }
    }

    return m_configManager;
  }

  /**
   * 获取 ConfigFactory 注册中心
   *
   * @return
   */
  private ConfigRegistry getRegistry() {
    if (m_configRegistry == null) {
      synchronized (this) {
        if (m_configRegistry == null) {
          m_configRegistry = ApolloInjector.getInstance(ConfigRegistry.class);
        }
      }
    }

    return m_configRegistry;
  }

  /**
   * 获取 application namespace 的配置
   *
   * Get Application's config instance.
   *
   * @return config instance
   */
  public static Config getAppConfig() {
    return getConfig(ConfigConsts.NAMESPACE_APPLICATION);
  }

  /**
   * 根据 namespace 获取配置
   *
   * Get the config instance for the namespace.
   *
   * @param namespace the namespace of the config
   * @return config instance
   */
  public static Config getConfig(String namespace) {
    return s_instance.getManager().getConfig(namespace);
  }

  /**
   * 根据 namespace 和文件格式获取配置文件
   *
   * @param namespace
   * @param configFileFormat
   * @return
   */
  public static ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
    return s_instance.getManager().getConfigFile(namespace, configFileFormat);
  }

  /**
   * 设置 application 的配置实例
   *
   * @param config
   */
  static void setConfig(Config config) {
    setConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
  }

  /**
   * 根据 namespace 设置配置实例
   *
   * Manually set the config for the namespace specified, use with caution.
   *
   * @param namespace the namespace
   * @param config    the config instance
   */
  static void setConfig(String namespace, final Config config) {
    s_instance.getRegistry().register(namespace, new ConfigFactory() {
      @Override
      public Config create(String namespace) {
        return config;
      }

      @Override
      public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
        return null;
      }

    });
  }

  /**
   * 注册 application 的 ConfigFactory
   *
   * @param factory
   */
  static void setConfigFactory(ConfigFactory factory) {
    setConfigFactory(ConfigConsts.NAMESPACE_APPLICATION, factory);
  }

  /**
   * 往 ConfigRegistry 中注册 namespace 对应的 ConfigFactory
   *
   * Manually set the config factory for the namespace specified, use with caution.
   *
   * @param namespace the namespace
   * @param factory   the factory instance
   */
  static void setConfigFactory(String namespace, ConfigFactory factory) {
    s_instance.getRegistry().register(namespace, factory);
  }

  // for test only
  static void reset() {
    synchronized (s_instance) {
      s_instance.m_configManager = null;
      s_instance.m_configRegistry = null;
    }
  }
}
