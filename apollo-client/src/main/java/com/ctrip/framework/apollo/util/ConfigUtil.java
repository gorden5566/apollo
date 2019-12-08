package com.ctrip.framework.apollo.util;

import com.google.common.util.concurrent.RateLimiter;
import java.io.File;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.MetaDomainConsts;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.enums.EnvUtils;
import com.ctrip.framework.foundation.Foundation;
import com.google.common.base.Strings;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigUtil {
  private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);

  /**
   * 刷新间隔，默认 5 minutes
   */
  private int refreshInterval = 5;
  private TimeUnit refreshIntervalTimeUnit = TimeUnit.MINUTES;

  /**
   * 连接超时时间，默认 1 second
   */
  private int connectTimeout = 1000;

  /**
   * 读取超时时间，默认 5 seconds
   */
  private int readTimeout = 5000;

  /**
   * 集群
   */
  private String cluster;

  /**
   * 加载配置 qps，默认 2
   */
  private int loadConfigQPS = 2;

  /**
   * 长轮询 qps，默认 2
   */
  private int longPollQPS = 2;
  /**
   * 错误重试间隔，默认 1 second
   * for on error retry
   */
  private long onErrorRetryInterval = 1;
  private TimeUnit onErrorRetryIntervalTimeUnit = TimeUnit.SECONDS;

  /**
   * 最大配置缓存大小，默认 500 个缓存 key
   * for typed config cache of parser result, e.g. integer, double, long, etc.
   */
  private long maxConfigCacheSize = 500;

  /**
   * 配置缓存过期时间，默认 1 minute
   */
  private long configCacheExpireTime = 1;
  private TimeUnit configCacheExpireTimeUnit = TimeUnit.MINUTES;

  /**
   * 长轮询初始延迟时间（单位：毫秒）
   */
  private long longPollingInitialDelayInMills = 2000;//2 seconds

  /**
   * 是否自动注入spring属性
   */
  private boolean autoUpdateInjectedSpringProperties = true;

  /**
   * 警告日志输出速率限制
   */
  private final RateLimiter warnLogRateLimiter;

  public ConfigUtil() {
    // 1 warning log output per minute
    warnLogRateLimiter = RateLimiter.create(0.017);
    initRefreshInterval();
    initConnectTimeout();
    initReadTimeout();
    initCluster();
    initQPS();
    initMaxConfigCacheSize();
    initLongPollingInitialDelayInMills();
    initAutoUpdateInjectedSpringProperties();
  }

  /**
   * 获取当前应用的 AppId
   *
   * Get the app id for the current application.
   *
   * @return the app id or ConfigConsts.NO_APPID_PLACEHOLDER if app id is not available
   */
  public String getAppId() {
    String appId = Foundation.app().getAppId();
    if (Strings.isNullOrEmpty(appId)) {
      appId = ConfigConsts.NO_APPID_PLACEHOLDER;
      if (warnLogRateLimiter.tryAcquire()) {
        logger.warn(
            "app.id is not set, please make sure it is set in classpath:/META-INF/app.properties, now apollo will only load public namespace configurations!");
      }
    }
    return appId;
  }

  /**
   * Get the data center info for the current application.
   *
   * @return the current data center, null if there is no such info.
   */
  public String getDataCenter() {
    return Foundation.server().getDataCenter();
  }

  private void initCluster() {
    //Load data center from system property
    cluster = System.getProperty(ConfigConsts.APOLLO_CLUSTER_KEY);

    //Use data center as cluster
    if (Strings.isNullOrEmpty(cluster)) {
      cluster = getDataCenter();
    }

    //Use default cluster
    if (Strings.isNullOrEmpty(cluster)) {
      cluster = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
  }

  /**
   * Get the cluster name for the current application.
   *
   * @return the cluster name, or "default" if not specified
   */
  public String getCluster() {
    return cluster;
  }

  /**
   * 获取当前环境
   *
   * Get the current environment.
   *
   * @return the env, UNKNOWN if env is not set or invalid
   */
  public Env getApolloEnv() {
    return EnvUtils.transformEnv(Foundation.server().getEnvType());
  }

  /**
   * 获取本地 IP
   *
   * @return
   */
  public String getLocalIp() {
    return Foundation.net().getHostAddress();
  }

  public String getMetaServerDomainName() {
    return MetaDomainConsts.getDomain(getApolloEnv());
  }

  /**
   * 初始化连接超时时间
   */
  private void initConnectTimeout() {
    String customizedConnectTimeout = System.getProperty("apollo.connectTimeout");
    if (!Strings.isNullOrEmpty(customizedConnectTimeout)) {
      try {
        connectTimeout = Integer.parseInt(customizedConnectTimeout);
      } catch (Throwable ex) {
        logger.error("Config for apollo.connectTimeout is invalid: {}", customizedConnectTimeout);
      }
    }
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * 初始化读取超时时间
   */
  private void initReadTimeout() {
    String customizedReadTimeout = System.getProperty("apollo.readTimeout");
    if (!Strings.isNullOrEmpty(customizedReadTimeout)) {
      try {
        readTimeout = Integer.parseInt(customizedReadTimeout);
      } catch (Throwable ex) {
        logger.error("Config for apollo.readTimeout is invalid: {}", customizedReadTimeout);
      }
    }
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  /**
   * 初始刷新间隔
   */
  private void initRefreshInterval() {
    String customizedRefreshInterval = System.getProperty("apollo.refreshInterval");
    if (!Strings.isNullOrEmpty(customizedRefreshInterval)) {
      try {
        refreshInterval = Integer.parseInt(customizedRefreshInterval);
      } catch (Throwable ex) {
        logger.error("Config for apollo.refreshInterval is invalid: {}", customizedRefreshInterval);
      }
    }
  }

  public int getRefreshInterval() {
    return refreshInterval;
  }

  public TimeUnit getRefreshIntervalTimeUnit() {
    return refreshIntervalTimeUnit;
  }

  /**
   * 初始化qps
   */
  private void initQPS() {
    String customizedLoadConfigQPS = System.getProperty("apollo.loadConfigQPS");
    if (!Strings.isNullOrEmpty(customizedLoadConfigQPS)) {
      try {
        loadConfigQPS = Integer.parseInt(customizedLoadConfigQPS);
      } catch (Throwable ex) {
        logger.error("Config for apollo.loadConfigQPS is invalid: {}", customizedLoadConfigQPS);
      }
    }

    String customizedLongPollQPS = System.getProperty("apollo.longPollQPS");
    if (!Strings.isNullOrEmpty(customizedLongPollQPS)) {
      try {
        longPollQPS = Integer.parseInt(customizedLongPollQPS);
      } catch (Throwable ex) {
        logger.error("Config for apollo.longPollQPS is invalid: {}", customizedLongPollQPS);
      }
    }
  }

  public int getLoadConfigQPS() {
    return loadConfigQPS;
  }

  public int getLongPollQPS() {
    return longPollQPS;
  }

  public long getOnErrorRetryInterval() {
    return onErrorRetryInterval;
  }

  public TimeUnit getOnErrorRetryIntervalTimeUnit() {
    return onErrorRetryIntervalTimeUnit;
  }

  /**
   * 获取默认本地缓存目录
   *
   * @return
   */
  public String getDefaultLocalCacheDir() {
    // 获取定制缓存目录
    String cacheRoot = getCustomizedCacheRoot();

    if (!Strings.isNullOrEmpty(cacheRoot)) {
      return cacheRoot + File.separator + getAppId();
    }

    // 获取默认目录
    cacheRoot = isOSWindows() ? "C:\\opt\\data\\%s" : "/opt/data/%s";
    return String.format(cacheRoot, getAppId());
  }

  /**
   * 获取定制缓存目录
   *
   * @return
   */
  private String getCustomizedCacheRoot() {
    // 1. Get from System Property
    String cacheRoot = System.getProperty("apollo.cacheDir");
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 2. Get from OS environment variable
      cacheRoot = System.getenv("APOLLO_CACHEDIR");
    }
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 3. Get from server.properties
      cacheRoot = Foundation.server().getProperty("apollo.cacheDir", null);
    }
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 4. Get from app.properties
      cacheRoot = Foundation.app().getProperty("apollo.cacheDir", null);
    }

    return cacheRoot;
  }

  /**
   * 是否为本地模式
   *
   * @return
   */
  public boolean isInLocalMode() {
    try {
      return Env.LOCAL == getApolloEnv();
    } catch (Throwable ex) {
      //ignore
    }
    return false;
  }

  /**
   * 是否为 windows 系统
   *
   * @return
   */
  public boolean isOSWindows() {
    String osName = System.getProperty("os.name");
    if (Strings.isNullOrEmpty(osName)) {
      return false;
    }
    return osName.startsWith("Windows");
  }

  /**
   * 初始化最大配置缓存
   */
  private void initMaxConfigCacheSize() {
    String customizedConfigCacheSize = System.getProperty("apollo.configCacheSize");
    if (!Strings.isNullOrEmpty(customizedConfigCacheSize)) {
      try {
        maxConfigCacheSize = Long.parseLong(customizedConfigCacheSize);
      } catch (Throwable ex) {
        logger.error("Config for apollo.configCacheSize is invalid: {}", customizedConfigCacheSize);
      }
    }
  }

  public long getMaxConfigCacheSize() {
    return maxConfigCacheSize;
  }

  public long getConfigCacheExpireTime() {
    return configCacheExpireTime;
  }

  public TimeUnit getConfigCacheExpireTimeUnit() {
    return configCacheExpireTimeUnit;
  }

  /**
   * 初始化长轮询初始延迟时间
   */
  private void initLongPollingInitialDelayInMills() {
    String customizedLongPollingInitialDelay = System.getProperty("apollo.longPollingInitialDelayInMills");
    if (!Strings.isNullOrEmpty(customizedLongPollingInitialDelay)) {
      try {
        longPollingInitialDelayInMills = Long.parseLong(customizedLongPollingInitialDelay);
      } catch (Throwable ex) {
        logger.error("Config for apollo.longPollingInitialDelayInMills is invalid: {}", customizedLongPollingInitialDelay);
      }
    }
  }

  public long getLongPollingInitialDelayInMills() {
    return longPollingInitialDelayInMills;
  }

  /**
   * 初始化是否自动注入spring属性
   */
  private void initAutoUpdateInjectedSpringProperties() {
    // 1. Get from System Property
    String enableAutoUpdate = System.getProperty("apollo.autoUpdateInjectedSpringProperties");
    if (Strings.isNullOrEmpty(enableAutoUpdate)) {
      // 2. Get from app.properties
      enableAutoUpdate = Foundation.app().getProperty("apollo.autoUpdateInjectedSpringProperties", null);
    }
    if (!Strings.isNullOrEmpty(enableAutoUpdate)) {
      autoUpdateInjectedSpringProperties = Boolean.parseBoolean(enableAutoUpdate.trim());
    }
  }

  public boolean isAutoUpdateInjectedSpringPropertiesEnabled() {
    return autoUpdateInjectedSpringProperties;
  }
}
