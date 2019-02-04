package me.streamis.engine.io.parser;

import io.vertx.core.buffer.Buffer;

/**
 * Created by stream.
 */
class BinaryPacketCodec implements PacketCodec<Buffer, Buffer> {

  @Override
  public Packet decode(Buffer data) {
    checkLength(data);
    PacketType type = PacketType.byteIndexToType(data.getByte(0) & 0xFF);
    return new Packet(type, data.getBuffer(1, data.length()));
  }

  @Override
  public Buffer encode(Packet packet) {
    Buffer buffer = Buffer.buffer();
    buffer.appendByte(PacketType.typeToByteIndex(packet.type));
    if (packet.data != null) {
      if (packet.data instanceof Buffer)
        buffer.appendBuffer((Buffer) packet.data);
      else if (packet.data instanceof byte[]) {
        buffer.appendBytes((byte[]) packet.data);
      } else if (packet.data instanceof String) {
        buffer.appendString((String) packet.data);
      } else {
        throw new EngineIOParserException("unKnow data type.");
      }
    }
    return buffer;
  }

}
