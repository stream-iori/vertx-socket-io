package me.streamis.parser;

import io.vertx.core.buffer.Buffer;
import me.streamis.EngineIOException;

/**
 * Created by stream.
 */
public interface PacketCodec {

  enum Type {
    STRING, BINARY, BASE64
  }

  Packet decode(Buffer data);

  Buffer encode(Packet packet);

  default void checkLength(Buffer data) {
    if (data == null || data.length() < 1) throw new EngineIOException("packet data is empty.");
  }

  static PacketCodec factory(Type type) {
    return PacketCodecFactory.getByType(type);
  }

}
