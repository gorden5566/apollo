package com.ctrip.framework.apollo.enums;

/**
 * 配置来源：远程、本地、失败
 *
 * To indicate the config's source type, i.e. where is the config loaded from
 *
 * @since 1.1.0
 */
public enum ConfigSourceType {
  REMOTE("Loaded from remote config service"), LOCAL("Loaded from local cache"), NONE("Load failed");

  private final String description;

  ConfigSourceType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
