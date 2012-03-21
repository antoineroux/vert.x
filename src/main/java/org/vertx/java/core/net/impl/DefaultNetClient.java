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

package org.vertx.java.core.net.impl;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.impl.Context;
import org.vertx.java.core.impl.EventLoopContext;
import org.vertx.java.core.impl.VertxInternal;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.core.net.NetSocket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultNetClient {

  private static final Logger log = LoggerFactory.getLogger(DefaultNetClient.class);

  private final Context ctx;
  private final TCPSSLHelper tcpHelper = new TCPSSLHelper();
  private ClientBootstrap bootstrap;
  private NioClientSocketChannelFactory channelFactory;
  private Map<Channel, DefaultNetSocket> socketMap = new ConcurrentHashMap<>();
  private Handler<Exception> exceptionHandler;
  private int reconnectAttempts;
  private long reconnectInterval = 1000;

  public DefaultNetClient() {
    ctx = VertxInternal.instance.getOrAssignContext();
    if (VertxInternal.instance.isWorker()) {
      throw new IllegalStateException("Cannot be used in a worker application");
    }
  }

  public void connect(int port, String host, final Handler<NetSocket> connectHandler) {
    connect(port, host, connectHandler, reconnectAttempts);
  }

  public void connect(int port, Handler<NetSocket> connectCallback) {
    connect(port, "localhost", connectCallback);
  }

  public void close() {
    for (NetSocket sock : socketMap.values()) {
      sock.close();
    }
  }

  public void setReconnectAttempts(int attempts) {
    if (attempts < -1) {
      throw new IllegalArgumentException("reconnect attempts must be >= -1");
    }
    this.reconnectAttempts = attempts;
  }

  public int getReconnectAttempts() {
    return reconnectAttempts;
  }

  public void setReconnectInterval(long interval) {
    if (interval < 1) {
      throw new IllegalArgumentException("reconnect interval nust be >= 1");
    }
    this.reconnectInterval = interval;
  }

  public long getReconnectInterval() {
    return reconnectInterval;
  }

  public void exceptionHandler(Handler<Exception> handler) {
    this.exceptionHandler = handler;
  }

  public Boolean isTCPNoDelay() {
    return tcpHelper.isTCPNoDelay();
  }

  public Integer getSendBufferSize() {
    return tcpHelper.getSendBufferSize();
  }

  public Integer getReceiveBufferSize() {
    return tcpHelper.getReceiveBufferSize();
  }

  public Boolean isTCPKeepAlive() {
    return tcpHelper.isTCPKeepAlive();
  }

  public Boolean isReuseAddress() {
    return tcpHelper.isReuseAddress();
  }

  public Boolean isSoLinger() {
    return tcpHelper.isSoLinger();
  }

  public Integer getTrafficClass() {
    return tcpHelper.getTrafficClass();
  }

  public void setTCPNoDelay(Boolean tcpNoDelay) {
    tcpHelper.setTCPNoDelay(tcpNoDelay);
  }

  public void setSendBufferSize(Integer size) {
    tcpHelper.setSendBufferSize(size);
  }

  public void setReceiveBufferSize(Integer size) {
    tcpHelper.setReceiveBufferSize(size);
  }

  public void setTCPKeepAlive(Boolean keepAlive) {
    tcpHelper.setTCPKeepAlive(keepAlive);
  }

  public void setReuseAddress(Boolean reuse) {
    tcpHelper.setReuseAddress(reuse);
  }

  public void setSoLinger(Boolean linger) {
    tcpHelper.setSoLinger(linger);
  }

  public void setTrafficClass(Integer trafficClass) {
    tcpHelper.setTrafficClass(trafficClass);
  }

  public boolean isSSL() {
    return tcpHelper.isSSL();
  }

  public String getKeyStorePath() {
    return tcpHelper.getKeyStorePath();
  }

  public String getKeyStorePassword() {
    return tcpHelper.getKeyStorePassword();
  }

  public String getTrustStorePath() {
    return tcpHelper.getTrustStorePath();
  }

  public String getTrustStorePassword() {
    return tcpHelper.getTrustStorePassword();
  }

  public TCPSSLHelper.ClientAuth getClientAuth() {
    return tcpHelper.getClientAuth();
  }

  public SSLContext getSSLContext() {
    return tcpHelper.getSSLContext();
  }

  public boolean isTrustAll() {
    return tcpHelper.isTrustAll();
  }

  public void setSSL(boolean ssl) {
    tcpHelper.setSSL(ssl);
  }

  public void setKeyStorePath(String path) {
    tcpHelper.setKeyStorePath(path);
  }

  public void setKeyStorePassword(String pwd) {
    tcpHelper.setKeyStorePassword(pwd);
  }

  public void setTrustStorePath(String path) {
    tcpHelper.setTrustStorePath(path);
  }

  public void setTrustStorePassword(String pwd) {
    tcpHelper.setTrustStorePassword(pwd);
  }

  public void setTrustAll(boolean trustAll) {
    tcpHelper.setTrustAll(trustAll);
  }

  private void connect(final int port, final String host, final Handler<NetSocket> connectHandler,
                       final int remainingAttempts) {

    if (bootstrap == null) {
      channelFactory = new NioClientSocketChannelFactory(
          VertxInternal.instance.getAcceptorPool(),
          VertxInternal.instance.getWorkerPool());
      bootstrap = new ClientBootstrap(channelFactory);

      tcpHelper.checkSSL();

      bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
        public ChannelPipeline getPipeline() throws Exception {
          ChannelPipeline pipeline = Channels.pipeline();
          if (tcpHelper.isSSL()) {
            SSLEngine engine = tcpHelper.getSSLContext().createSSLEngine();
            engine.setUseClientMode(true); //We are on the client side of the connection
            pipeline.addLast("ssl", new SslHandler(engine));
          }
          pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());  // For large file / sendfile support
          pipeline.addLast("handler", new ClientHandler());
          return pipeline;
        }
      });
    }

    //Client connections share context with caller
    EventLoopContext ectx;
    if (ctx instanceof EventLoopContext) {
      //It always will be
      ectx = (EventLoopContext)ctx;
    } else {
      ectx = null;
    }
    channelFactory.setWorker(ectx.getWorker());

    bootstrap.setOptions(tcpHelper.generateConnectionOptions());
    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
    future.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture channelFuture) throws Exception {
        final NioSocketChannel ch = (NioSocketChannel) channelFuture.getChannel();

        if (channelFuture.isSuccess()) {

          if (tcpHelper.isSSL()) {
            // TCP connected, so now we must do the SSL handshake

            SslHandler sslHandler = (SslHandler)ch.getPipeline().get("ssl");

            ChannelFuture fut = sslHandler.handshake();
            fut.addListener(new ChannelFutureListener() {

              public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                  connected(ch, connectHandler);
                } else {
                  failed(ch, new SSLHandshakeException("Failed to create SSL connection"));
                }
              }
            });
          } else {
            connected(ch, connectHandler);
          }
        } else {
          if (remainingAttempts > 0 || remainingAttempts == -1) {
            tcpHelper.runOnCorrectThread(ch, new Runnable() {
              public void run() {
                VertxInternal.instance.setContext(ctx);
                log.debug("Failed to create connection. Will retry in " + reconnectInterval + " milliseconds");
                //Set a timer to retry connection
                Vertx.instance.setTimer(reconnectInterval, new Handler<Long>() {
                  public void handle(Long timerID) {
                    connect(port, host, connectHandler, remainingAttempts == -1 ? remainingAttempts : remainingAttempts
                        - 1);
                  }
                });
               }
            });
          } else {
            failed(ch, channelFuture.getCause());
          }
        }
      }
    });
  }

  private void connected(final NioSocketChannel ch, final Handler<NetSocket> connectHandler) {
    tcpHelper.runOnCorrectThread(ch, new Runnable() {
      public void run() {
        VertxInternal.instance.setContext(ctx);
        DefaultNetSocket sock = new DefaultNetSocket(ch, ctx);
        socketMap.put(ch, sock);
        connectHandler.handle(sock);
      }
    });
  }

  private void failed(NioSocketChannel ch, final Throwable t) {
    if (t instanceof Exception && exceptionHandler != null) {
      tcpHelper.runOnCorrectThread(ch, new Runnable() {
        public void run() {
          VertxInternal.instance.setContext(ctx);
          exceptionHandler.handle((Exception) t);
        }
      });
    } else {
      log.error("Unhandled exception", t);
    }
  }

  private class ClientHandler extends SimpleChannelUpstreamHandler {

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
      final NioSocketChannel ch = (NioSocketChannel) e.getChannel();
      final DefaultNetSocket sock = socketMap.remove(ch);
      if (sock != null) {
        tcpHelper.runOnCorrectThread(ch, new Runnable() {
          public void run() {
            sock.handleClosed();
          }
        });
      }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
      DefaultNetSocket sock = socketMap.get(ctx.getChannel());
      if (sock != null) {
        ChannelBuffer cb = (ChannelBuffer) e.getMessage();
        sock.handleDataReceived(new Buffer(cb));
      }
    }

    @Override
    public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
      final NioSocketChannel ch = (NioSocketChannel) e.getChannel();
      final DefaultNetSocket sock = socketMap.get(ch);
      ChannelState state = e.getState();
      if (state == ChannelState.INTEREST_OPS) {
        tcpHelper.runOnCorrectThread(ch, new Runnable() {
          public void run() {
            sock.handleInterestedOpsChanged();
          }
        });
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
      final NioSocketChannel ch = (NioSocketChannel) e.getChannel();
      final NetSocket sock = socketMap.remove(ch);
      final Throwable t = e.getCause();
      if (sock != null && t instanceof Exception) {
        tcpHelper.runOnCorrectThread(ch, new Runnable() {
          public void run() {
            sock.handleException((Exception) t);
            ch.close();
          }
        });
      } else {
        // Ignore - any exceptions before a channel exists will be passed manually via the failed(...) method
      }
    }
  }

}

