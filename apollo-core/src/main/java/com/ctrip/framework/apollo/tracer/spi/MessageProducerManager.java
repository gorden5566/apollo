package com.ctrip.framework.apollo.tracer.spi;

/**
 * MessageProducer Factory
 * 用于创建 MessageProducer
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface MessageProducerManager {
  /**
   * @return the message producer
   */
  MessageProducer getProducer();
}
