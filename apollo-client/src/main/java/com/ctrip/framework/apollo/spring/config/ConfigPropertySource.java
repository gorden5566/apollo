package com.ctrip.framework.apollo.spring.config;

import com.ctrip.framework.apollo.ConfigChangeListener;
import java.util.Set;

import org.springframework.core.env.EnumerablePropertySource;

import com.ctrip.framework.apollo.Config;

/**
 * Property source wrapper for Config
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigPropertySource extends EnumerablePropertySource<Config> {
  private static final String[] EMPTY_ARRAY = new String[0];

  ConfigPropertySource(String name, Config source) {
    super(name, source);
  }

  @Override
  public String[] getPropertyNames() {
    // 获取 property name
    Set<String> propertyNames = this.source.getPropertyNames();
    if (propertyNames.isEmpty()) {
      return EMPTY_ARRAY;
    }
    return propertyNames.toArray(new String[propertyNames.size()]);
  }

  @Override
  public Object getProperty(String name) {
    // 获取属性值
    return this.source.getProperty(name, null);
  }

  /**
   * 添加 config 变更监听器
   *
   * @param listener
   */
  public void addChangeListener(ConfigChangeListener listener) {
    this.source.addChangeListener(listener);
  }
}
