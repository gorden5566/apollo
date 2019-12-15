package com.ctrip.framework.apollo.spring.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.core.Ordered;
import org.w3c.dom.Element;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * 用于解析 XML 配置
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class NamespaceHandler extends NamespaceHandlerSupport {
  private static final Splitter NAMESPACE_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

  @Override
  public void init() {
    // 注册 BeanDefinitionParser
    registerBeanDefinitionParser("config", new BeanParser());
  }

  static class BeanParser extends AbstractSingleBeanDefinitionParser {
    @Override
    protected Class<?> getBeanClass(Element element) {
      // xml 中 config 元素对应的 bean 类型
      return ConfigPropertySourcesProcessor.class;
    }

    @Override
    protected boolean shouldGenerateId() {
      return true;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
      // 解析namespace
      String namespaces = element.getAttribute("namespaces");
      //default to application
      if (Strings.isNullOrEmpty(namespaces)) {
        namespaces = ConfigConsts.NAMESPACE_APPLICATION;
      }

      // 解析order
      int order = Ordered.LOWEST_PRECEDENCE;
      String orderAttribute = element.getAttribute("order");

      if (!Strings.isNullOrEmpty(orderAttribute)) {
        try {
          order = Integer.parseInt(orderAttribute);
        } catch (Throwable ex) {
          throw new IllegalArgumentException(
              String.format("Invalid order: %s for namespaces: %s", orderAttribute, namespaces));
        }
      }

      // 与spring整合
      PropertySourcesProcessor.addNamespaces(NAMESPACE_SPLITTER.splitToList(namespaces), order);
    }
  }
}
