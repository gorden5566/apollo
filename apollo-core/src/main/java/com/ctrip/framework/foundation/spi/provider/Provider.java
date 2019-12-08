package com.ctrip.framework.foundation.spi.provider;

/**
 * 对一类属性进行封装，对外提供查询
 */
public interface Provider {
  /**
   * 获取当前 provider 的类型
   *
   * @return the current provider's type
   */
  Class<? extends Provider> getType();

  /**
   * 获取属性值
   *
   * Return the property value with the given name, or {@code defaultValue} if the name doesn't exist.
   *
   * @param name the property name
   * @param defaultValue the default value when name is not found or any error occurred
   * @return the property value
   */
  String getProperty(String name, String defaultValue);

  /**
   * 初始化 provider
   *
   * Initialize the provider
   */
  void initialize();
}
