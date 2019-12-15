package com.ctrip.framework.apollo.spring.config;

import java.util.List;

import com.ctrip.framework.apollo.Config;
import com.google.common.collect.Lists;

/**
 * ConfigPropertySource factory
 */
public class ConfigPropertySourceFactory {

  private final List<ConfigPropertySource> configPropertySources = Lists.newLinkedList();

  /**
   * 获取 ConfigPropertySource
   *
   * @param name
   * @param source
   * @return
   */
  public ConfigPropertySource getConfigPropertySource(String name, Config source) {
    // 创建 ConfigPropertySource
    ConfigPropertySource configPropertySource = new ConfigPropertySource(name, source);

    // 保存下来
    configPropertySources.add(configPropertySource);

    return configPropertySource;
  }

  /**
   * 获取所有的 ConfigPropertySource
   *
   * @return
   */
  public List<ConfigPropertySource> getAllConfigPropertySources() {
    return Lists.newLinkedList(configPropertySources);
  }
}
