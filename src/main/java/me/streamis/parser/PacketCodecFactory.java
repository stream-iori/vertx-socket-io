package me.streamis.parser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by stream.
 */
class PacketCodecFactory {
  private static final Map<PacketCodec.Type, PacketCodec> packetCodecMap = new HashMap<>(3);

  static {
    packetCodecMap.put(PacketCodec.Type.STRING, new StringPacketCodec());
    packetCodecMap.put(PacketCodec.Type.BINARY, new BinaryPacketCodec());
    packetCodecMap.put(PacketCodec.Type.BASE64, new Base64PacketCodec());
  }

  static PacketCodec getByType(PacketCodec.Type type) {
    return packetCodecMap.get(type);
  }

}
