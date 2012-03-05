# Copyright 2011-2012 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

include Java
require "buffer"
require "composition"

module Stomp
  class StompClient
    class ConnectCallback < org.vertx.java.core.stomp.StompConnectHandler

      def initialize(connect_block)
        super()
        @connect_block = connect_block
      end

      def onConnect(java_connection)
        conn = Connection.new(java_connection)
        @connect_block.call(conn)
      end
    end

    #Can take either a proc or a block
    def StompClient.connect(port, host = "localhost", proc = nil, &connect_block)
      connect_block = proc if proc
      StompClient.new(port, host, connect_block)
    end

    def initialize(port, host, connect_block)
      super()
      @java_client = org.vertx.java.core.stomp.StompClient.connect(port, host, ConnectCallback.new(connect_block))
    end


  end

  class Connection

    def initialize(java_connection)
      @java_connection = java_connection
    end

    def message(proc = nil, &message_block)
      message_block = proc if proc
      @message_block = message_block
    end

    def error(proc = nil, &error_block)
      error_block = proc if proc
      @error_block = error_block
    end

    def close
      @java_connection.close
    end

    def send(dest, body)
      java_buff = body._to_java_buffer if body != nil
      @java_connection.send(dest, java_buff)
    end

    def send_with_headers(dest, headers, body)
      java_buff = body._to_java_buffer if body != nil
      @java_connection.send(dest, headers, java_buff)
    end

    def subscribe(dest, proc = nil, &message_block)
      message_block = proc if proc
      @java_connection.subscribe(dest, MsgCallback.new(message_block))
    end

    def request(dest, headers, body, proc = nil, &response_block)
      response_block = proc if proc
      java_completion = @java_connection.request(dest, headers, body, MsgCallback.new(response_block))
      Completion.create_from_java_completion(java_completion)
    end

    class MsgCallback < org.vertx.java.core.stomp.StompMsgCallback
      def initialize(message_block)
        super()
        @message_block = message_block
      end

      def onMessage(java_headers, java_body)
        @message_block.call(java_headers, InternalBufferFactory.createBuffer(java_body))
      end
    end

    class ReceiptCallback < org.vertx.java.core.Runnable
      def initialize(receipt_block)
        super()
        @receipt_block = receipt_block
      end

      def onDone
        @receipt_block.call()
      end
    end



  end
end

