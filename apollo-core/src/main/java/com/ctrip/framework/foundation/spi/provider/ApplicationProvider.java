package com.ctrip.framework.foundation.spi.provider;

import java.io.InputStream;

/**
 * 用于提供应用相关的属性
 *
 * Provider for application related properties
 */
public interface ApplicationProvider extends Provider {
  /**
   * @return the application's app id
   */
  String getAppId();

  /**
   * @return whether the application's app id is set or not
   */
  boolean isAppIdSet();

  /**
   * Initialize the application provider with the specified input stream
   */
  void initialize(InputStream in);
}
