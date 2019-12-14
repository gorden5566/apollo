package com.ctrip.framework.apollo.core.schedule;

/**
 * 指数级调度策略
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ExponentialSchedulePolicy implements SchedulePolicy {
  /**
   * 延迟时间上限
   */
  private final long delayTimeLowerBound;

  /**
   * 延迟时间下限
   */
  private final long delayTimeUpperBound;

  /**
   * 上次延迟时间
   */
  private long lastDelayTime;

  public ExponentialSchedulePolicy(long delayTimeLowerBound, long delayTimeUpperBound) {
    this.delayTimeLowerBound = delayTimeLowerBound;
    this.delayTimeUpperBound = delayTimeUpperBound;
  }

  @Override
  public long fail() {
    long delayTime = lastDelayTime;

    if (delayTime == 0) {
      delayTime = delayTimeLowerBound;
    } else {
      // 延迟时间翻倍
      delayTime = Math.min(lastDelayTime << 1, delayTimeUpperBound);
    }

    lastDelayTime = delayTime;

    return delayTime;
  }

  @Override
  public void success() {
    // 重置延迟时间
    lastDelayTime = 0;
  }
}
