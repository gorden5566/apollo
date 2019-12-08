package com.ctrip.framework.foundation.internals;

import com.google.common.base.Strings;

/**
 * 工具类
 */
public class Utils {
  /**
   * 字符串是否为空
   *
   * @param str
   * @return
   */
  public static boolean isBlank(String str) {
    return Strings.nullToEmpty(str).trim().isEmpty();
  }

  /**
   * 判断是否为 windows 系统
   *
   * @return
   */
  public static boolean isOSWindows() {
    String osName = System.getProperty("os.name");
    if (Utils.isBlank(osName)) {
      return false;
    }
    return osName.startsWith("Windows");
  }
}
