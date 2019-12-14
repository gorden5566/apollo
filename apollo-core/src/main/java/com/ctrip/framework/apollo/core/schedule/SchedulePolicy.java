package com.ctrip.framework.apollo.core.schedule;

/**
 * 调度策略
 *
 * Schedule policy
 * @author Jason Song(song_s@ctrip.com)
 */
public interface SchedulePolicy {

  /**
   * 调度失败
   *
   * @return 延迟时间
   */
  long fail();

  /**
   * 调度成功
   */
  void success();
}
