package me.streamis.parser;

import io.vertx.core.buffer.Buffer;
import me.streamis.EngineIOException;

import java.util.Base64;

import static me.streamis.parser.PacketOption.BINARY;

/**
 * Created by stream.
 */
public class Base64PacketCodec implements PacketCodec {

  @Override
  public Packet decode(Buffer data) {
    checkLength(data);
    if (data.length() < 2) {
      return new Packet(PacketType.byteIndexToType(data.getByte(0)), BINARY, Buffer.buffer());
    }
    if (data.getByte(0) != 'b') {
      throw new EngineIOException("invalid base64 packet!" + data);
    }
    PacketType type = PacketType.byteIndexToType(data.getByte(1));
    Buffer content = Buffer.buffer();
    if (data.length() > 2) {
      content = content.appendBytes(Base64.getDecoder().decode(data.getString(2, data.length())));
    }
    return new Packet(type, content);
  }

  @Override
  public Buffer encode(Packet packet) {
    Buffer buffer = Buffer.buffer().appendByte((byte) 'b');
    buffer.appendString(Byte.toString(PacketType.typeToByteIndex(packet.type)));
    if (packet.data != null && packet.data.length() > 1) {
      buffer.appendString(Base64.getEncoder().encodeToString(packet.data.getBytes()));
    }
    return buffer;
  }
}
