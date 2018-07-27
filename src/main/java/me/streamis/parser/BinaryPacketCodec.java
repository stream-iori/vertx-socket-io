package me.streamis.parser;

import io.vertx.core.buffer.Buffer;

/**
 * Created by stream.
 */
public class BinaryPacketCodec implements PacketCodec {

  @Override
  public Packet decode(Buffer data) {
    checkLength(data);
    PacketType type = PacketType.byteIndexToType(data.getByte(0));
    return new Packet(type, PacketOption.BINARY, data.getBuffer(1, data.length()));
  }

  @Override
  public Buffer encode(Packet packet) {
    Buffer buffer = Buffer.buffer();
    buffer.appendByte(PacketType.typeToByteIndex(packet.type));
    if (packet.data != null && packet.data.length() > 1)
      buffer.appendBuffer(packet.data);
    return buffer;
  }
}
