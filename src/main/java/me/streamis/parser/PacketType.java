package me.streamis.parser;

import me.streamis.EngineIOException;

/**
 * Created by stream.
 */
public enum PacketType {
  OPEN, CLOSE, PING, PONG, MESSAGE, UPGRADE, NOOP;

  static byte typeToByteIndex(PacketType type) {
    return (byte) type.ordinal();
  }

  static PacketType byteIndexToType(byte indexByte) {
    PacketType[] types = PacketType.values();
    int index = Integer.valueOf(new String(new byte[]{indexByte}));
    if (index > types.length - 1 || index < 0) {
      throw new EngineIOException("unKnow packet type.");
    }
    return types[index];
  }


}
