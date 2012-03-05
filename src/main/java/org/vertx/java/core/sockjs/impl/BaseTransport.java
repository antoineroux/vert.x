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

package org.vertx.java.core.sockjs.impl;

import org.vertx.java.core.Handler;
import org.vertx.java.core.SimpleHandler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.impl.StringEscapeUtils;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.core.sockjs.AppConfig;
import org.vertx.java.core.sockjs.SockJSSocket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
class BaseTransport {

  private static final Logger log = LoggerFactory.getLogger(BaseTransport.class);

  protected final Map<String, Session> sessions;
  protected AppConfig config;

  protected static final String COMMON_PATH_ELEMENT_RE = "\\/[^\\/\\.]+\\/([^\\/\\.]+)\\/";

  public BaseTransport(Map<String, Session> sessions, AppConfig config) {
    this.sessions = sessions;
    this.config = config;
  }

  protected Session getSession(final long timeout, final long heartbeatPeriod, final String sessionID,
                               Handler<SockJSSocket> sockHandler) {
    Session session = sessions.get(sessionID);
    if (session == null) {
      session = new Session(timeout, heartbeatPeriod, sockHandler, new SimpleHandler() {
        public void handle() {
          sessions.remove(sessionID);
        }
      });
      sessions.put(sessionID, session);
    }
    return session;
  }

  protected void sendInvalidJSON(HttpServerResponse response) {
    response.statusCode = 500;
    response.end("Broken JSON encoding.");
  }

  protected String escapeForJavaScript(String str) {
    try {
       str = StringEscapeUtils.escapeJavaScript(str);
    } catch (Exception e) {
      log.error("Failed to escape", e);
      str = null;
    }
    return str;
  }

  static void setJSESSIONID(AppConfig config, HttpServerRequest req) {
    String cookies = req.getHeader("Cookie");
    if (config.isInsertJSESSIONID()) {
      //Preserve existing JSESSIONID, if any
      if (cookies != null) {
        String[] parts;
        if (cookies.contains(";")) {
          parts = cookies.split(";");
        } else {
          parts = new String[] {cookies};
        }
        for (String part: parts) {
          if (part.startsWith("JSESSIONID")) {
            cookies = part + "; path=/";
            break;
          }
        }
      }
      if (cookies == null) {
        cookies = "JSESSIONID=dummy; path=/";
      }
      req.response.putHeader("Set-Cookie", cookies);
    }
  }

  static void setCORS(HttpServerRequest req) {
    String origin = req.getHeader("Origin");
    if (origin == null) {
      origin = "*";
    }
    req.response.putHeader("Access-Control-Allow-Origin", origin);
    req.response.putHeader("Access-Control-Allow-Credentials", "true");
  }

  static Handler<HttpServerRequest> createCORSOptionsHandler(final AppConfig config, final String methods) {
    return new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        req.response.putHeader("Cache-Control", "public,max-age=31536000");
        long oneYearSeconds = 365 * 24 * 60 * 60;
        long oneYearms = oneYearSeconds * 1000;
        String expires = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").format(new Date(System.currentTimeMillis() + oneYearms));
        req.response.putHeader("Expires", expires);
        req.response.putHeader("Allow", methods);
        req.response.putHeader("Access-Control-Max-Age", String.valueOf(oneYearSeconds));
        setCORS(req);
        setJSESSIONID(config, req);
        req.response.statusCode = 204;
        req.response.end();
      }
    };
  }
}
