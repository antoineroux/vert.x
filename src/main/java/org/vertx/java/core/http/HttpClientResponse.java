/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vertx.java.core.http;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.impl.HttpReadStreamBase;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Represents a client-side HTTP response.
 * An instance of this class is provided to the user via an {@link org.vertx.java.core.Handler}
 * instance that was specified when one of the* HTTP method operations, or the
 * generic {@link HttpClient#request(String, String, org.vertx.java.core.Handler)}
 * method was called on an instance of {@link HttpClient}.
 * <p>
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class HttpClientResponse extends HttpReadStreamBase {

  private static final Logger log = LoggerFactory.getLogger(HttpClientResponse.class);

  protected HttpClientResponse(int statusCode, String statusMessage) {
    this.statusCode = statusCode;
    this.statusMessage = statusMessage;
  }

  /**
   * The HTTP status code of the response
   */
  public final int statusCode;

  /**
   * The HTTP status message of the response
   */
  public final String statusMessage;


  /**
   * Returns the header value for the specified {@code key}, or null, if there is no such header in the response.
   */
  public abstract String getHeader(String key);

  /**
   * Returns a set of all header names in the response.
   */
  public abstract Set<String> getHeaderNames();

  /**
   * Returns the trailer value for the specified {@code key}, or null, if there is no such header in the response.<p>
   * Trailers will only be available in the response if the server has sent a HTTP chunked response where headers have
   * been inserted by the server on the last chunk. In such a case they won't be available on the client until the last chunk has
   * been received.
   */
  public abstract String getTrailer(String key);

  /**
   * Returns a map of all headers in the response, If the response contains multiple headers with the same key, the values
   * will be concatenated together into a single header with the same key value, with each value separated by a comma, as specified
   * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">here</a>.
   */
  public abstract Map<String, String> getAllHeaders();

  /**
   * Returns a map of all trailers in the response, If the response contains multiple trailers with the same key, the values
   * will be concatenated together into a single header with the same key value, with each value separated by a comma, as specified
   * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">here</a>.<p>
   * If trailers have been sent by the server, they won't be available on the client side until the last chunk is received.
   */
  public abstract Map<String, String> getAllTrailers();

  /**
   * Returns a set of all trailer names in the response.<p>
   * If trailers have been sent by the server, they won't be available on the client side until the last chunk is received.
   */
  public abstract Set<String> getTrailerNames();

  /**
   * Specify a data handler for the response. If the response has a body, the {@code dataHandler} will get called when some of the response body has
   * been read from the wire. If the response is chunked, then it will be called once for each HTTP chunk, otherwise it
   * will be called one or more times until the full response body has been delivered.<p>
   * If the response has no body it will not be called at all.
   *
   * @param dataHandler
   */
  public abstract void dataHandler(Handler<Buffer> dataHandler);

  /**
   * Specify an end handler for the response. The {@code endHandler} is called once the entire response has been read.
   */
  public abstract void endHandler(Handler<Void> endHandler);

  /**
   * Specify an exception handler for the response. The {@code exceptionHandler} is called if an exception occurs
   * when handling the response.
   */
  public abstract void exceptionHandler(Handler<Exception> exceptionHandler);

  /**
   * Pause the response. Once the response has been paused, the system will stop reading any more chunks of the response
   * from the wire, thus pushing back to the server. You may however still receive a few more chunks corresponding to those
   * chunks which have already been read but not delivered to the {@link #dataHandler} yet.<p>
   * Pause is often used in conjunction with a {@link org.vertx.java.core.streams.Pump} to pump data between streams and implement flow control.
   */
  public abstract void pause();

  /**
   * Resume a paused response. The response will resume receiving chunks of the response from the wire.<p>
   * Resume is often used in conjunction with a {@link org.vertx.java.core.streams.Pump} to pump data between streams and implement flow control.
   */
  public abstract void resume();
}
