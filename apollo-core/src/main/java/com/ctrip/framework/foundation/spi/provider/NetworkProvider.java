package com.ctrip.framework.foundation.spi.provider;

/**
 * 用于提供网络相关的属性
 *
 * Provider for network related properties
 */
public interface NetworkProvider extends Provider {
  /**
   * 获取主机地址
   *
   * @return the host address, i.e. ip
   */
  String getHostAddress();

  /**
   * 获取主机名
   *
   * @return the host name
   */
  String getHostName();
}
