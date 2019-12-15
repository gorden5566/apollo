package com.ctrip.framework.apollo.spring.config;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.spring.property.AutoUpdateConfigChangeListener;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.Iterator;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.util.CollectionUtils;

/**
 * Apollo Property Sources processor for Spring Annotation Based Application. <br /> <br />
 *
 * The reason why PropertySourcesProcessor implements {@link BeanFactoryPostProcessor} instead of
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor} is that lower versions of
 * Spring (e.g. 3.1.1) doesn't support registering BeanDefinitionRegistryPostProcessor in ImportBeanDefinitionRegistrar
 * - {@link com.ctrip.framework.apollo.spring.annotation.ApolloConfigRegistrar}
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class PropertySourcesProcessor implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

  /**
   * key: order 优先级
   * value: namespace
   */
  private static final Multimap<Integer, String> NAMESPACE_NAMES = LinkedHashMultimap.create();

  /**
   * spring bean factory
   */
  private static final Set<BeanFactory> AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES = Sets.newConcurrentHashSet();

  /**
   * ConfigPropertySource factory
   */
  private final ConfigPropertySourceFactory configPropertySourceFactory = SpringInjector
      .getInstance(ConfigPropertySourceFactory.class);

  /**
   * config util
   */
  private final ConfigUtil configUtil = ApolloInjector.getInstance(ConfigUtil.class);

  /**
   * environment
   */
  private ConfigurableEnvironment environment;

  /**
   * 添加 namespace
   *
   * @param namespaces namespace 列表
   * @param order 优先级
   * @return
   */
  public static boolean addNamespaces(Collection<String> namespaces, int order) {
    return NAMESPACE_NAMES.putAll(order, namespaces);
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    initializePropertySources();
    initializeAutoUpdatePropertiesFeature(beanFactory);
  }

  /**
   * 初始化 property source
   */
  private void initializePropertySources() {
    // 已经初始化则直接返回
    if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME)) {
      //already initialized
      return;
    }

    CompositePropertySource composite = new CompositePropertySource(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME);

    //sort by order asc
    ImmutableSortedSet<Integer> orders = ImmutableSortedSet.copyOf(NAMESPACE_NAMES.keySet());
    Iterator<Integer> iterator = orders.iterator();

    while (iterator.hasNext()) {
      int order = iterator.next();
      // 按优先级处理
      for (String namespace : NAMESPACE_NAMES.get(order)) {

        // 根据 namespace 获取 config
        Config config = ConfigService.getConfig(namespace);

        // 添加到 CompositePropertySource 和 ConfigPropertySourceFactory
        composite.addPropertySource(configPropertySourceFactory.getConfigPropertySource(namespace, config));
      }
    }

    // clean up
    NAMESPACE_NAMES.clear();

    // ensure ApolloBootstrapPropertySources is still the first
    ensureBootstrapPropertyPrecedence(environment);

    if (CollectionUtils.isEmpty(composite.getPropertySources())) {
      return;
    }
    // add after the bootstrap property source or to the first
    if (environment.getPropertySources()
        .contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
      // 把 composite 添加到 ApolloBootstrapPropertySources 后面
      environment.getPropertySources()
          .addAfter(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME, composite);
    } else {
      // 把 composite 添加到第一个位置
      environment.getPropertySources().addFirst(composite);
    }

  }

  /**
   * 确保 ApolloBootstrapPropertySources 是第一个
   *
   * @param environment
   */
  private void ensureBootstrapPropertyPrecedence(ConfigurableEnvironment environment) {
    MutablePropertySources propertySources = environment.getPropertySources();

    PropertySource<?> bootstrapPropertySource = propertySources
        .get(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);

    // not exists or already in the first place
    if (bootstrapPropertySource == null || propertySources.precedenceOf(bootstrapPropertySource) == 0) {
      return;
    }

    // 先移除，再添加到第一个位置
    propertySources.remove(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);
    propertySources.addFirst(bootstrapPropertySource);
  }

  /**
   * 初始化自动更新 Properties 功能
   *
   * @param beanFactory
   */
  private void initializeAutoUpdatePropertiesFeature(ConfigurableListableBeanFactory beanFactory) {
    // 不是自动更新，或者已经处理过，则直接返回
    if (!configUtil.isAutoUpdateInjectedSpringPropertiesEnabled() ||
        !AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES.add(beanFactory)) {
      return;
    }

    // 创建 config 变更监听器
    AutoUpdateConfigChangeListener autoUpdateConfigChangeListener = new AutoUpdateConfigChangeListener(
        environment, beanFactory);

    // 获取所有的 ConfigPropertySource
    List<ConfigPropertySource> configPropertySources = configPropertySourceFactory.getAllConfigPropertySources();

    // 添加监听器
    for (ConfigPropertySource configPropertySource : configPropertySources) {
      configPropertySource.addChangeListener(autoUpdateConfigChangeListener);
    }
  }

  @Override
  public void setEnvironment(Environment environment) {
    //it is safe enough to cast as all known environment is derived from ConfigurableEnvironment
    this.environment = (ConfigurableEnvironment) environment;
  }

  @Override
  public int getOrder() {
    //make it as early as possible
    return Ordered.HIGHEST_PRECEDENCE;
  }

  // for test only
  static void reset() {
    NAMESPACE_NAMES.clear();
    AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES.clear();
  }
}
