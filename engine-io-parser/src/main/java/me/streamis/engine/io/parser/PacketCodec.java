package me.streamis.engine.io.parser;

import io.vertx.core.buffer.Buffer;

/**
 * Created by stream.
 */
interface PacketCodec<IN, OUT> {

  Packet decode(IN data);

  OUT encode(Packet packet);

  default void checkLength(Object data) {
    if (data == null) throw new EngineIOParserException("packet data is empty.");
    if (data instanceof String) {
      if (((String) data).length() < 1) throw new EngineIOParserException("packet data is empty.");
    }
    if (data instanceof Buffer) {
      if (((Buffer) data).length() < 1) throw new EngineIOParserException("packet data is empty.");
    }
  }

}
