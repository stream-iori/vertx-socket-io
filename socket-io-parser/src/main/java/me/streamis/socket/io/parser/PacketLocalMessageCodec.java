package me.streamis.socket.io.parser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class PacketLocalMessageCodec implements MessageCodec<Packet, Packet> {

  @Override
  public void encodeToWire(Buffer buffer, Packet packet) {
    throw new UnsupportedOperationException("this codec for local message only.");
  }

  @Override
  public Packet decodeFromWire(int pos, Buffer buffer) {
    throw new UnsupportedOperationException("this codec for local message only.");
  }

  @Override
  public Packet transform(Packet packet) {
    return packet;
  }

  @Override
  public String name() {
    return "SocketIOPacketLocal";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
