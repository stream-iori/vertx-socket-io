package me.streamis.parser;

import io.vertx.core.buffer.Buffer;

/**
 * Created by stream.
 */
public class StringPacketCodec implements PacketCodec {

  @Override
  public Packet decode(Buffer data) {
    checkLength(data);
    PacketType packetType = PacketType.byteIndexToType(data.getByte(0));
    Buffer content = Buffer.buffer();
    if (data.length() > 1) {
      content = data.getBuffer(1, data.length());
    }
    return new Packet(packetType, PacketOption.BINARY, content);
  }

  @Override
  public Buffer encode(Packet packet) {
    Buffer buffer = Buffer.buffer();
    buffer.appendString(Byte.toString(PacketType.typeToByteIndex(packet.type)));
    if (packet.data != null && packet.data.length() > 1) {
      buffer.appendBuffer(packet.data);
    }
    return buffer;
  }
}
