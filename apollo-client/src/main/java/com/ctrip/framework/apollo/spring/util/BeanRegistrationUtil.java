package com.ctrip.framework.apollo.spring.util;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class BeanRegistrationUtil {
  /**
   * 注册 BeanDefinition
   *
   * @param registry
   * @param beanName
   * @param beanClass
   * @return
   */
  public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, String beanName,
      Class<?> beanClass) {
    return registerBeanDefinitionIfNotExists(registry, beanName, beanClass, null);
  }

  /**
   * 注册 BeanDefinition
   *
   * @param registry
   * @param beanName
   * @param beanClass
   * @param extraPropertyValues 要添加的 properties
   * @return
   */
  public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, String beanName,
                                                          Class<?> beanClass, Map<String, Object> extraPropertyValues) {
    // 已注册过 beanName
    if (registry.containsBeanDefinition(beanName)) {
      return false;
    }

    String[] candidates = registry.getBeanDefinitionNames();

    for (String candidate : candidates) {
      BeanDefinition beanDefinition = registry.getBeanDefinition(candidate);
      // 已注册过该类型
      if (Objects.equals(beanDefinition.getBeanClassName(), beanClass.getName())) {
        return false;
      }
    }

    // 构造 BeanDefinition
    BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(beanClass).getBeanDefinition();

    // 添加 properties
    if (extraPropertyValues != null) {
      for (Map.Entry<String, Object> entry : extraPropertyValues.entrySet()) {
        beanDefinition.getPropertyValues().add(entry.getKey(), entry.getValue());
      }
    }

    // 注册 beanDefinition
    registry.registerBeanDefinition(beanName, beanDefinition);

    return true;
  }


}
