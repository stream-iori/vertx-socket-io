package me.streamis.engine.io.parser;

import io.vertx.core.buffer.Buffer;

import java.util.Base64;

/**
 * Created by stream.
 */
class Base64PacketCodec implements PacketCodec<Buffer, String> {

  @Override
  public Packet decode(Buffer data) {
    checkLength(data);
    if (!data.getString(0, 1).equals("b")) {
      throw new EngineIOParserException("invalid base64 packet!" + data);
    }
    int index = Integer.valueOf(new String(new byte[]{data.getBytes()[1]}));
    PacketType type = PacketType.byteIndexToType(index);
    Buffer content = Buffer.buffer();
    if (data.length() > 2) {
      content = content.appendBytes(Base64.getDecoder().decode(data.getBytes(2, data.length())));
    }
    return new Packet(type, content);
  }

  @Override
  public String encode(Packet packet) {
    if (!(packet.data instanceof Buffer)) throw new EngineIOParserException("packet data should be binary");
    Buffer buffer = Buffer.buffer().appendString("b");
    buffer.appendString(Byte.toString(PacketType.typeToByteIndex(packet.type)));
    if (packet.data != null) {
      buffer.appendString(Base64.getEncoder().encodeToString(((Buffer) packet.data).getBytes()));
    }
    return buffer.toString();
  }
}
