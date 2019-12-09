package com.ctrip.framework.apollo.internals;

import java.util.Properties;

/**
 * 仓库事件监听器
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface RepositoryChangeListener {
  /**
   * Invoked when config repository changes.
   * @param namespace the namespace of this repository change
   * @param newProperties the properties after change
   */
  void onRepositoryChange(String namespace, Properties newProperties);
}
