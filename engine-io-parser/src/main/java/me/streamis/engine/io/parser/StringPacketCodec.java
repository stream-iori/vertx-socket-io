package me.streamis.engine.io.parser;

import io.vertx.core.buffer.Buffer;


/**
 * Created by stream.
 */
class StringPacketCodec implements PacketCodec<String, String> {

  @Override
  public Packet decode(String data) {
    checkLength(data);
    int index = Integer.valueOf(new String(new byte[]{data.getBytes()[0]}));
    PacketType packetType = PacketType.byteIndexToType(index);
    String content = "";
    if (data.length() > 1) {
      content += data.substring(1);
    }
    return new Packet(packetType, content);
  }

  @Override
  public String encode(Packet packet) {
    Buffer buffer = Buffer.buffer();
    buffer.appendString(Byte.toString(PacketType.typeToByteIndex(packet.type)));
    if (packet.data instanceof String) {
      buffer.appendString((String) packet.data);
    }
    return buffer.toString("UTF-8");
  }
}
