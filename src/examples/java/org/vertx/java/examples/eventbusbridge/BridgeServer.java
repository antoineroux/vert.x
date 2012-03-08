package org.vertx.java.examples.eventbusbridge;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.SockJSBridge;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.AppConfig;
import org.vertx.java.deploy.Verticle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class BridgeServer implements Verticle {

  HttpServer server;

  public void start() throws Exception {
    server = new HttpServer();

    List<JsonObject> permitted = new ArrayList<>();
    permitted.add(new JsonObject()); // Let everything through
    new SockJSBridge(server, new AppConfig().setPrefix("/eventbus"), permitted);

    // Also serve the static resources. In real life this would probably be done by a CDN
    server.requestHandler(new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        if (req.path.equals("/")) req.response.sendFile("eventbusbridge/index.html"); // Serve the index.html
        if (req.path.endsWith("vertxbus.js")) req.response.sendFile("eventbusbridge/vertxbus.js"); // Serve the js
      }
    });

    server.listen(8080);
  }

  public void stop() throws Exception {
    server.close();
  }
}
