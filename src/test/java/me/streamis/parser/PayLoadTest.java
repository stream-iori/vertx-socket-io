package me.streamis.parser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by stream.
 */
class PayLoadTest {

  @Test
  void decodePayLoad() {
    List<Packet> packets = PayLoad.decodePayLoad("8:4你好，世界!😀1:5".getBytes());
    assertEquals(2, packets.size());
    assertEquals(PacketType.MESSAGE, packets.get(0).type);
    assertEquals("你好，世界!😀", packets.get(0).data.toString("UTF-8"));
    assertEquals(PacketType.UPGRADE, packets.get(1).type);
    assertEquals("", packets.get(1).data.toString());
  }

  @Test
  void payLoadCodec() {
    Packet packet1 = new Packet(PacketType.MESSAGE, PacketOption.BINARY, Buffer.buffer("Hello,World!你好，世界!😀"));
    Packet packet2 = new Packet(PacketType.UPGRADE, PacketOption.BINARY, Buffer.buffer());
    Buffer buffer = PayLoad.encodePayload(packet1, packet2);
    List<Packet> packets = PayLoad.decodePayLoad(buffer.getBytes());
    assertEquals(2, packets.size());
    assertEquals(packet1.type, packets.get(0).type);
    assertEquals(packet1.data.toString(), packets.get(0).data.toString("UTF-8"));
    assertEquals(packet2.type, packets.get(1).type);
    assertEquals(packet2.data.length(), packets.get(1).data.length());
  }

  @Test
  void codecJsonp() {
    JsonObject jsonObject = new JsonObject().put("id", 1).put("content", "\"this is a test content.\\n哈哈!\\u2028\\u2029😀\"");
    System.out.println(jsonObject);
    Packet packet = new Packet(PacketType.MESSAGE, PacketOption.BINARY, jsonObject.toBuffer());
    Buffer buffer = PacketCodecFactory.getByType(PacketCodec.Type.BASE64).encode(packet);
    Packet decodedPacket = PacketCodecFactory.getByType(PacketCodec.Type.BASE64).decode(buffer);
    assertEquals(jsonObject, decodedPacket.data.toJsonObject());
  }

}
