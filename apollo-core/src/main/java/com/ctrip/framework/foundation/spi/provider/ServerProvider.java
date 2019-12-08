package com.ctrip.framework.foundation.spi.provider;

import java.io.IOException;
import java.io.InputStream;

/**
 * 用于提供服务相关的属性
 *
 * Provider for server related properties
 */
public interface ServerProvider extends Provider {
  /**
   * 获取当前的环境
   *
   * @return current environment or {@code null} if not set
   */
  String getEnvType();

  /**
   * 判断当前环境是否设置
   *
   * @return whether current environment is set or not
   */
  boolean isEnvTypeSet();

  /**
   * 获取数据中心
   *
   * @return current data center or {@code null} if not set
   */
  String getDataCenter();

  /**
   * 判断数据中心是否设置
   *
   * @return whether data center is set or not
   */
  boolean isDataCenterSet();

  /**
   * 使用指定的输入流初始化 ServerProvider
   *
   * Initialize server provider with the specified input stream
   *
   * @throws IOException
   */
  void initialize(InputStream in) throws IOException;
}
