package com.ctrip.framework.apollo.util.http;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.exceptions.ApolloConfigStatusCodeException;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.base.Function;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class HttpUtil {
  /**
   * config util
   */
  private ConfigUtil m_configUtil;

  /**
   * gson
   */
  private Gson gson;

  /**
   * Constructor.
   */
  public HttpUtil() {
    m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    gson = new Gson();
  }

  /**
   * Do get operation for the http request.
   *
   * @param httpRequest  the request
   * @param responseType the response type
   * @return the response
   * @throws ApolloConfigException if any error happened or response code is neither 200 nor 304
   */
  public <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Class<T> responseType) {
    // 响应结果转换
    Function<String, T> convertResponse = new Function<String, T>() {
      @Override
      public T apply(String input) {
        return gson.fromJson(input, responseType);
      }
    };

    return doGetWithSerializeFunction(httpRequest, convertResponse);
  }

  /**
   * Do get operation for the http request.
   *
   * @param httpRequest  the request
   * @param responseType the response type
   * @return the response
   * @throws ApolloConfigException if any error happened or response code is neither 200 nor 304
   */
  public <T> HttpResponse<T> doGet(HttpRequest httpRequest, final Type responseType) {
    Function<String, T> convertResponse = new Function<String, T>() {
      @Override
      public T apply(String input) {
        return gson.fromJson(input, responseType);
      }
    };

    return doGetWithSerializeFunction(httpRequest, convertResponse);
  }

  private <T> HttpResponse<T> doGetWithSerializeFunction(HttpRequest httpRequest,
                                                         Function<String, T> serializeFunction) {
    InputStreamReader isr = null;
    InputStreamReader esr = null;
    int statusCode;
    try {
      // 创建连接
      HttpURLConnection conn = (HttpURLConnection) new URL(httpRequest.getUrl()).openConnection();

      // 请求方式为 GET
      conn.setRequestMethod("GET");

      // 设置连接超时时间
      int connectTimeout = httpRequest.getConnectTimeout();
      if (connectTimeout < 0) {
        connectTimeout = m_configUtil.getConnectTimeout();
      }

      // 设置读超时时间
      int readTimeout = httpRequest.getReadTimeout();
      if (readTimeout < 0) {
        readTimeout = m_configUtil.getReadTimeout();
      }

      conn.setConnectTimeout(connectTimeout);
      conn.setReadTimeout(readTimeout);

      // 建立连接
      conn.connect();

      // 响应状态码
      statusCode = conn.getResponseCode();
      String response;

      try {
        isr = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
        // 返回结果转换为string
        response = CharStreams.toString(isr);
      } catch (IOException ex) {
        /**
         * according to https://docs.oracle.com/javase/7/docs/technotes/guides/net/http-keepalive.html,
         * we should clean up the connection by reading the response body so that the connection
         * could be reused.
         */
        InputStream errorStream = conn.getErrorStream();

        if (errorStream != null) {
          esr = new InputStreamReader(errorStream, StandardCharsets.UTF_8);
          try {
            CharStreams.toString(esr);
          } catch (IOException ioe) {
            //ignore
          }
        }

        // 200 and 304 should not trigger IOException, thus we must throw the original exception out
        if (statusCode == 200 || statusCode == 304) {
          throw ex;
        } else {
          // for status codes like 404, IOException is expected when calling conn.getInputStream()
          throw new ApolloConfigStatusCodeException(statusCode, ex);
        }
      }

      // 正常返回结果
      if (statusCode == 200) {
        return new HttpResponse<>(statusCode, serializeFunction.apply(response));
      }

      // 客户端有缓存
      if (statusCode == 304) {
        return new HttpResponse<>(statusCode, null);
      }
    } catch (ApolloConfigStatusCodeException ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new ApolloConfigException("Could not complete get operation", ex);
    } finally {
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ex) {
          // ignore
        }
      }

      if (esr != null) {
        try {
          esr.close();
        } catch (IOException ex) {
          // ignore
        }
      }
    }

    throw new ApolloConfigStatusCodeException(statusCode,
        String.format("Get operation failed for %s", httpRequest.getUrl()));
  }

}
