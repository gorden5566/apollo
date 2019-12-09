package com.ctrip.framework.apollo.spi;

import java.util.Map;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.google.common.collect.Maps;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigFactoryManager implements ConfigFactoryManager {
  /**
   * ConfigFactory 注册中心，用于管理 namespace 对应的 ConfigFactory
   */
  private ConfigRegistry m_registry;

  /**
   * key: namespace
   * value: ConfigFactory 实例
   */
  private Map<String, ConfigFactory> m_factories = Maps.newConcurrentMap();

  public DefaultConfigFactoryManager() {
    m_registry = ApolloInjector.getInstance(ConfigRegistry.class);
  }

  @Override
  public ConfigFactory getFactory(String namespace) {
    // step 1: check hacked factory
    // 先到注册中心取
    ConfigFactory factory = m_registry.getFactory(namespace);

    if (factory != null) {
      return factory;
    }

    // step 2: check cache
    // 再到缓存取
    factory = m_factories.get(namespace);

    if (factory != null) {
      return factory;
    }

    // step 3: check declared config factory
    // 到guice容器取，目前未实现
    factory = ApolloInjector.getInstance(ConfigFactory.class, namespace);

    if (factory != null) {
      return factory;
    }

    // step 4: check default config factory
    // 到guice容器取
    factory = ApolloInjector.getInstance(ConfigFactory.class);

    m_factories.put(namespace, factory);

    // factory should not be null
    return factory;
  }
}
