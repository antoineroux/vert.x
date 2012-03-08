/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vertx.java.examples.routematch;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.deploy.Verticle;

public class RouteMatchExample implements Verticle {

  private HttpServer server;

  public void start() {

    RouteMatcher rm = new RouteMatcher();

    rm.get("/details/:user/:id", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        req.response.end("User: " + req.getAllParams().get("user") + " ID: " + req.getAllParams().get("id"));
      }
    });

    // Catch all - serve the index page
    rm.getWithRegEx(".*", new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        req.response.sendFile("route_match/index.html");
      }
    });

    server = new HttpServer().requestHandler(rm).listen(8080);
  }

  public void stop() {
    server.close();
  }
}
