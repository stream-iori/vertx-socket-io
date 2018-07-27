package me.streamis.parser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import static me.streamis.parser.PacketCodec.Type.BASE64;
import static me.streamis.parser.PacketCodec.Type.BINARY;
import static me.streamis.parser.PacketCodec.Type.STRING;
import static me.streamis.parser.PacketOption.DEFAULT;

/**
 * Created by stream.
 */
public class Packet {
  PacketType type;
  PacketOption option;
  Buffer data;

  public Packet(PacketType type, PacketOption option, Buffer data) {
    this.type = type;
    this.option = option;
    this.data = data;
  }

  public Packet(PacketType type, JsonObject data) {
    this(type, PacketOption.BINARY, data.toBuffer());
  }

  public Packet(PacketType type, String data) {
    this(type, PacketOption.BINARY, Buffer.buffer(data));
  }

  public Packet(PacketType type, Buffer buffer) {
    this(type, PacketOption.BINARY, buffer);
  }

  public Packet(PacketType type, byte[] data) {
    this(type, PacketOption.BINARY, Buffer.buffer(data));
  }

  public Packet(PacketType type) {
    this(type, PacketOption.BINARY, Buffer.buffer());
  }

  public PacketType getType() {
    return this.type;
  }

  public PacketOption getOption() {
    return this.option;
  }

  public Buffer getData() {
    return this.data;
  }

  public static Packet decode(Buffer data, PacketOption option) {
    switch (option) {
      default:
        return PacketCodec.factory(STRING).decode(data);
      case BINARY:
        return PacketCodec.factory(BINARY).decode(data);
      case BASE64:
        return PacketCodec.factory(BASE64).decode(data);
    }
  }

  public static Packet decode(Buffer data) {
    return decode(data, DEFAULT);
  }

  public static Buffer encode(PacketOption option, Packet packet) {
    switch (option) {
      default:
        return PacketCodec.factory(STRING).encode(packet);
      case BINARY:
        return PacketCodec.factory(BINARY).encode(packet);
      case BASE64:
        return PacketCodec.factory(BASE64).encode(packet);
    }
  }

  @Override
  public String toString() {
    return "Packet{" +
      "type=" + type +
      ", option=" + option +
      ", data=" + data +
      '}';
  }
}
