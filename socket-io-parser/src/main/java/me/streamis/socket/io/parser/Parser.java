package me.streamis.socket.io.parser;

import io.vertx.core.Handler;

interface Parser {

  interface Encoder {
    void encode(Packet obj, Handler<Object[]> callback);
  }

  interface Decoder {

    void add(String obj);

    void add(byte[] obj);

    void destroy();

    void onDecoded(Handler<Packet> callback);
  }
}
