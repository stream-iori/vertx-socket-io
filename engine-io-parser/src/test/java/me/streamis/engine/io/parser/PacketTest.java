package me.streamis.engine.io.parser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by stream.
 */
public class PacketTest {

  @Test
  public void stringEqual() {
    //String
    String encodePacket = Packet.encodeAsString(new Packet(PacketType.MESSAGE, "test😯"));
    Packet packet = Packet.decodeWithString(encodePacket);
    assertEquals(PacketType.MESSAGE, packet.getType());
    assertEquals("test😯", packet.getData());

    //binary
    Buffer encodeBinPacket = Packet.encodeAsBuffer(new Packet(PacketType.MESSAGE, Buffer.buffer("test")));
    packet = Packet.decodeWithBuffer(encodeBinPacket);
    assertEquals(PacketType.MESSAGE, packet.getType());
    assertEquals(Buffer.buffer("test"), packet.getData());

    //base64
    String encodeB64Packet = Packet.encodeAsString(new Packet(PacketType.MESSAGE, Buffer.buffer("test")));
    packet = Packet.decodeWithString(encodeB64Packet);
    assertEquals(PacketType.MESSAGE, packet.getType());
    assertEquals("test", packet.getData().toString());
  }

  @Test
  void decodePayLoad() {
    List<Packet> packets = Packet.decodePayload("9:4你好，世界!😀1:5");
    assertEquals(2, packets.size());
    assertEquals(PacketType.MESSAGE, packets.get(0).type);
    assertEquals("你好，世界!😀", packets.get(0).data);
    assertEquals(PacketType.UPGRADE, packets.get(1).type);
    assertEquals("", packets.get(1).data.toString());
  }

  @Test
  void payLoadCodec() {
    Packet packet1 = new Packet(PacketType.MESSAGE, "H你好😓");
    Packet packet2 = new Packet(PacketType.UPGRADE, "");
    String str = Packet.encodePayload(false, packet1, packet2);
    List<Packet> packets = Packet.decodePayload(str);
    assertEquals(2, packets.size());
    assertEquals(packet1.type, packets.get(0).type);
    assertEquals(packet1.data, packets.get(0).data.toString());
    assertEquals(packet2.type, packets.get(1).type);

    packet1 = new Packet(PacketType.MESSAGE, Buffer.buffer("你好，世界!你好😓"));
    packet2 = new Packet(PacketType.UPGRADE, Buffer.buffer(""));
    Buffer buff = Packet.encodePayLoadAsBuffer(packet1, packet2);
    packets = Packet.decodePayloadAsBuffer(buff);
    assertEquals(2, packets.size());
    assertEquals(packet1.type, packets.get(0).type);
    assertEquals(packet1.data, packets.get(0).data);
    assertEquals(packet2.type, packets.get(1).type);
  }

  @Test
  void codecJsonp() {
    JsonObject jsonObject = new JsonObject().put("id", 1).put("content", "\"this is a test content.\\n哈哈!\\u2028\\u2029\"");
    Packet packet = new Packet(PacketType.MESSAGE, jsonObject.toBuffer());
    PacketCodec<Buffer, String> b64Codec = new Base64PacketCodec();
    String str = b64Codec.encode(packet);
    Packet decodedPacket = b64Codec.decode(Buffer.buffer(str));
    assertEquals(jsonObject.toString(), decodedPacket.data.toString());
  }
}
