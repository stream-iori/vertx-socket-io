package me.streamis.engine.io.parser;

/**
 * Created by stream.
 */
public enum PacketType {
  OPEN, CLOSE, PING, PONG, MESSAGE, UPGRADE, NOOP;

  static byte typeToByteIndex(PacketType type) {
    return (byte) type.ordinal();
  }

  static PacketType byteIndexToType(int index) {
    PacketType[] types = PacketType.values();
    if (index > types.length - 1 || index < 0) {
      throw new EngineIOParserException("unKnow packet type " + index);
    }
    return types[index];
  }


}
