package com.ctrip.framework.foundation.spi;

import com.ctrip.framework.foundation.spi.provider.Provider;

/**
 * provider工厂
 */
public interface ProviderManager {
  /**
   * 遍历所有 provider，获取属性值
   *
   * @param name
   * @param defaultValue
   * @return
   */
  String getProperty(String name, String defaultValue);

  /**
   * 根据 provider 类型获取对应实例
   * @param clazz
   * @param <T>
   * @return
   */
  <T extends Provider> T provider(Class<T> clazz);
}
