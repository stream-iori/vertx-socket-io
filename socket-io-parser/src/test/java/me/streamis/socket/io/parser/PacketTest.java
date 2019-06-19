package me.streamis.socket.io.parser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import me.streamis.socket.io.parser.Packet.PacketType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PacketTest {

  @Test
  public void encodeAndDecodeInString() {
    Packet packet = new Packet();
    packet.setType(PacketType.EVENT);
    packet.setData("test-packet");
    packet.setId(13L);
    assertPacketAsString(packet);

    packet = new Packet();
    packet.setType(PacketType.CONNECT);
    packet.setNamespace("/woot");
    assertPacketAsString(packet);

    packet = new Packet();
    packet.setType(PacketType.DISCONNECT);
    packet.setNamespace("/woot");
    assertPacketAsString(packet);

    packet = new Packet();
    packet.setType(PacketType.EVENT);
    packet.setData(new JsonArray().add("a").add(1).add(new JsonObject()));
    assertPacketAsString(packet);

    packet = new Packet();
    packet.setType(PacketType.EVENT);
    packet.setId(1L);
    packet.setNamespace("/test");
    packet.setData(new JsonArray().add("a").add(1).add(new JsonObject()));
    assertPacketAsString(packet);

    packet = new Packet();
    packet.setType(PacketType.ACK);
    packet.setId(123L);
    packet.setNamespace("/");
    packet.setData(new JsonArray().add("a").add(1).add(new JsonObject()));
    assertPacketAsString(packet);

    packet = new Packet();
    packet.setType(PacketType.ERROR);
    packet.setNamespace("/");
    packet.setData("Unauthorized");
    assertPacketAsString(packet);

    Packet.decodePacket("442[\"some\", \"data\"", decodePacket -> {
      assertEquals(PacketType.ERROR, decodePacket.getType());
      assertEquals("parser error: invalid payload", decodePacket.getData());
    });

  }

  @Test
  public void encodeAndDecodeInBinary() {
    Packet packet = new Packet();
    packet.setType(PacketType.BINARY_EVENT);
    packet.setId(23);
    packet.setNamespace("/cool");
    List<Object> data = new ArrayList<>(2);
    data.add("a");
    data.add(Buffer.buffer().appendString("abc", "utf8"));
    packet.setData(data);
    assertPacketAsBinary(packet);
  }


  private void assertPacketAsString(Packet packet) {
    String encodeData = Packet.encodeAsString(packet);
    Packet.decodePacket(encodeData, decodedPacket -> {
      assertPacketMetadata(packet, decodedPacket);
      assertEquals(packet.getAttachments(), decodedPacket.getAttachments());
      assertEquals(packet.getData() != null ? Helper.stringify(packet.getData()) : null,
        decodedPacket.getData());
    });
  }

  private void assertPacketAsBinary(Packet packet) {
    Packet originalPacket = new Packet();
    originalPacket.setData(packet.getData());

    List<Buffer> encodedPackets = Packet.encodeAsBinary(packet);
    encodedPackets.forEach(encodedPacket -> {
      Packet.decodePacket(encodedPacket, decodedPacket -> {
        packet.setAttachments(-1);
        packet.setData(originalPacket.getData());
        assertPacketMetadata(packet, decodedPacket);
        System.out.println(decodedPacket.getData());
        System.out.println(packet.getData());
      });
    });
  }

  private void assertPacketMetadata(Packet p1, Packet p2) {
    assertEquals(p1.getType(), p2.getType());
    assertEquals(p1.getId(), p2.getId());
    assertEquals(p1.getNamespace(), p2.getNamespace());
  }


}
