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

package org.vertx.java.old.stomp;

import org.vertx.java.core.Handler;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StompServer {

  public static NetServer createServer() {

    return new NetServer().connectHandler(new Handler<NetSocket>() {

      private ConcurrentMap<String, List<StompConnection>> subscriptions = new ConcurrentHashMap<>();

      private synchronized void subscribe(String dest, StompConnection conn) {
        List<StompConnection> conns = subscriptions.get(dest);
        if (conns == null) {
          conns = new CopyOnWriteArrayList<>();
          subscriptions.put(dest, conns);
        }
        conns.add(conn);
      }

      private synchronized void unsubscribe(String dest, StompConnection conn) {
        List<StompConnection> conns = subscriptions.get(dest);
        if (conns == null) {
          conns.remove(conn);
          if (conns.isEmpty()) {
            subscriptions.remove(dest);
          }
        }
      }

      private void checkReceipt(Frame frame, StompConnection conn) {
        String receipt = frame.headers.get("receipt");
        if (receipt != null) {
          conn.write(Frame.receiptFrame(receipt));
        }
      }

      public void handle(final NetSocket sock) {
        final StompServerConnection conn = new StompServerConnection(sock);
        conn.frameHandler(new FrameHandler() {
          public void onFrame(Frame frame) {
            if ("CONNECT".equals(frame.command)) {
              conn.write(Frame.connectedFrame(UUID.randomUUID().toString()));
              return;
            }
            //The following can have optional receipt
            switch (frame.command) {
              case "SUBSCRIBE": {
                String dest = frame.headers.get("destination");
                subscribe(dest, conn);
                break;
              }
              case "UNSUBSCRIBE": {
                String dest = frame.headers.get("destination");
                unsubscribe(dest, conn);
                break;
              }
              case "SEND": {
                String dest = frame.headers.get("destination");
                frame.command = "MESSAGE";
                List<StompConnection> conns = subscriptions.get(dest);
                if (conns != null) {
                  for (StompConnection conn : conns) {
                    frame.headers.put("message-id", UUID.randomUUID().toString());
                    conn.write(frame);
                  }
                }
                break;
              }
            }
            checkReceipt(frame, conn);
          }
        });
      }
    });
  }
}
