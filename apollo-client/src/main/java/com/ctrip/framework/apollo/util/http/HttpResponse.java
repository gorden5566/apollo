package com.ctrip.framework.apollo.util.http;

/**
 * http 响应
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class HttpResponse<T> {
  /**
   * 状态码
   */
  private final int m_statusCode;

  /**
   * 内容
   */
  private final T m_body;

  public HttpResponse(int statusCode, T body) {
    this.m_statusCode = statusCode;
    this.m_body = body;
  }

  public int getStatusCode() {
    return m_statusCode;
  }

  public T getBody() {
    return m_body;
  }
}
