package com.ctrip.framework.apollo.build;

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.internals.Injector;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;

/**
 * bean 容器，类似 spring
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloInjector {
  private static volatile Injector s_injector;
  private static final Object lock = new Object();

  /**
   * 单例模式，获取 Injector 实例
   *
   * @return
   */
  private static Injector getInjector() {
    if (s_injector == null) {
      synchronized (lock) {
        if (s_injector == null) {
          try {
            // 获取第一个 Injector spi 实现，默认配置为 DefaultInjector
            s_injector = ServiceBootstrap.loadFirst(Injector.class);
          } catch (Throwable ex) {
            ApolloConfigException exception = new ApolloConfigException("Unable to initialize Apollo Injector!", ex);
            Tracer.logError(exception);
            throw exception;
          }
        }
      }
    }

    return s_injector;
  }

  /**
   * 根据类型获取实例
   *
   * @param clazz
   * @param <T>
   * @return
   */
  public static <T> T getInstance(Class<T> clazz) {
    try {
      return getInjector().getInstance(clazz);
    } catch (Throwable ex) {
      Tracer.logError(ex);
      throw new ApolloConfigException(String.format("Unable to load instance for type %s!", clazz.getName()), ex);
    }
  }

  /**
   * 根据类型和名字获取实例
   *
   * @param clazz
   * @param name
   * @param <T>
   * @return
   */
  public static <T> T getInstance(Class<T> clazz, String name) {
    try {
      return getInjector().getInstance(clazz, name);
    } catch (Throwable ex) {
      Tracer.logError(ex);
      throw new ApolloConfigException(
          String.format("Unable to load instance for type %s and name %s !", clazz.getName(), name), ex);
    }
  }
}
