package com.ctrip.framework.apollo.spring.annotation;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.google.common.base.Preconditions;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Apollo Annotation Processor for Spring Application
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloAnnotationProcessor extends ApolloProcessor {

  @Override
  protected void processField(Object bean, String beanName, Field field) {
    // 获取字段上的注解
    ApolloConfig annotation = AnnotationUtils.getAnnotation(field, ApolloConfig.class);
    if (annotation == null) {
      return;
    }

    // 字段类型校验
    Preconditions.checkArgument(Config.class.isAssignableFrom(field.getType()),
        "Invalid type: %s for field: %s, should be Config", field.getType(), field);

    // 获取配置
    String namespace = annotation.value();
    Config config = ConfigService.getConfig(namespace);

    // 设置 bean 的值
    ReflectionUtils.makeAccessible(field);
    ReflectionUtils.setField(field, bean, config);
  }

  @Override
  protected void processMethod(final Object bean, String beanName, final Method method) {
    // 获取方法上的注解
    ApolloConfigChangeListener annotation = AnnotationUtils
        .findAnnotation(method, ApolloConfigChangeListener.class);
    if (annotation == null) {
      return;
    }

    // 方法参数校验
    Class<?>[] parameterTypes = method.getParameterTypes();
    Preconditions.checkArgument(parameterTypes.length == 1,
        "Invalid number of parameters: %s for method: %s, should be 1", parameterTypes.length,
        method);
    Preconditions.checkArgument(ConfigChangeEvent.class.isAssignableFrom(parameterTypes[0]),
        "Invalid parameter type: %s for method: %s, should be ConfigChangeEvent", parameterTypes[0],
        method);

    ReflectionUtils.makeAccessible(method);

    // 注解配置
    String[] namespaces = annotation.value();
    String[] annotatedInterestedKeys = annotation.interestedKeys();
    String[] annotatedInterestedKeyPrefixes = annotation.interestedKeyPrefixes();

    // 构建 ConfigChangeListener
    ConfigChangeListener configChangeListener = new ConfigChangeListener() {
      @Override
      public void onChange(ConfigChangeEvent changeEvent) {
        ReflectionUtils.invokeMethod(method, bean, changeEvent);
      }
    };

    Set<String> interestedKeys = annotatedInterestedKeys.length > 0 ? Sets.newHashSet(annotatedInterestedKeys) : null;
    Set<String> interestedKeyPrefixes = annotatedInterestedKeyPrefixes.length > 0 ? Sets.newHashSet(annotatedInterestedKeyPrefixes) : null;

    for (String namespace : namespaces) {
      // 获取 config
      Config config = ConfigService.getConfig(namespace);

      // 添加 ConfigChangeListener
      if (interestedKeys == null && interestedKeyPrefixes == null) {
        config.addChangeListener(configChangeListener);
      } else {
        config.addChangeListener(configChangeListener, interestedKeys, interestedKeyPrefixes);
      }
    }
  }
}
