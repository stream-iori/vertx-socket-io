package me.streamis.socket.io.parser;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import me.streamis.socket.io.parser.Packet.PacketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static me.streamis.socket.io.parser.Packet.PacketType.BINARY_ACK;
import static me.streamis.socket.io.parser.Packet.PacketType.BINARY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(VertxExtension.class)
public class PacketTest {

  private static IOParser.Encoder encoder = new IOParser.Encoder();

  @BeforeEach
  public void setup() {
  }

  @Test
  public void encodeAndDecodeInString() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();

    Packet packet = new Packet();
    packet.setType(PacketType.EVENT);
    packet.setData("test-packet");
    packet.setId(13);
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.CONNECT);
    packet.setNamespace("/woot");
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.DISCONNECT);
    packet.setNamespace("/woot");
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.EVENT);
    packet.setData(new JsonArray().add("a").add(1).add(new JsonObject()));
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.EVENT);
    packet.setId(1);
    packet.setNamespace("/test");
    packet.setData(new JsonArray().add("a").add(1).add(new JsonObject()));
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.EVENT);
    packet.setId(2);
    packet.setNamespace("/test");
    packet.setData(new JsonObject().put("say", "hello"));
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.ACK);
    packet.setId(123);
    packet.setNamespace("/");
    packet.setData(new JsonArray().add("a").add(1).add(new JsonObject()));
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.ERROR);
    packet.setNamespace("/");
    packet.setData("Unauthorized");
    assertPacketAsString(packet, testContext);

    IOParser.Decoder decoder = new IOParser.Decoder();
    decoder.onDecoded(newPacket -> {
      assertEquals(PacketType.ERROR, newPacket.getType());
      testContext.completeNow();
    });
    decoder.add("442[\"some\", \"data\"");
    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void encodeAndDecodeInBinary() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();
    Packet<byte[]> packet = new Packet();
    packet.setType(BINARY_EVENT);
    packet.setId(23);
    packet.setNamespace("/cool");
    packet.setData("abc".getBytes(Charset.forName("UTF-8")));
    assertPacketAsBinary(packet, testContext);

    packet = new Packet<>();
    packet.setType(BINARY_ACK);
    packet.setId(127);
    packet.setNamespace("/back");
    packet.setData(new byte[2]);
    assertPacketAsBinary(packet, testContext);

    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void encodeAndDecodeInBinaryWithJson() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();
    Packet<Map<String, Object>> packet = new Packet<>(BINARY_EVENT);
    Map<String, Object> data = new HashMap<>();
    data.put("a", "hi");
    data.put("b", "why".getBytes(Charset.forName("UTF-8")));
    packet.setData(data);
    packet.setId(999);
    packet.setNamespace("/deep");
    assertPacketAsBinary(packet, testContext);
    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void encodeBinaryAckWithByteArray() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();
    List<Object> data = new ArrayList<>();
    data.add("a");
    data.add("xxx".getBytes(Charset.forName("UTF-8")));
    data.add(new JsonObject());
    Packet<List> packet = new Packet<>(BINARY_ACK);
    packet.setData(data);
    packet.setId(127);
    packet.setNamespace("/back");
    assertPacketAsBinary(packet, testContext);
    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
  }


  private void assertPacketAsString(Packet packet, VertxTestContext testContext) {
    encoder.encode(packet, encodeData -> {
      Parser.Decoder decoder = new IOParser.Decoder();
      decoder.onDecoded(newPacket -> {
        assertPacketMetadata(packet, newPacket);
        assertEquals(packet.getData(), newPacket.getData());
        testContext.completeNow();
      });
      decoder.add((String) encodeData[0]);
    });
  }

  private void assertPacketAsBinary(Packet packet, VertxTestContext testContext) {
    final Object originalData = packet.getData();
    encoder.encode(packet, encodedData -> {
      Parser.Decoder decoder = new IOParser.Decoder();
      decoder.onDecoded(newPacket -> {
        packet.setData(originalData);
        packet.setAttachments(-1);
        assertPacketMetadata(packet, newPacket);
        if (originalData instanceof Map) {
          assertEquals(((Map) originalData).size(), ((Map) newPacket.getData()).size());
        } else if (originalData instanceof List) {
          assertEquals(((List) originalData).size(), ((List) newPacket.getData()).size());
        }
        testContext.completeNow();
      });

      for (Object p : encodedData) {
        if (p instanceof String) decoder.add((String) p);
        else if (p instanceof byte[]) decoder.add((byte[]) p);
      }
    });
  }

  private void assertPacketMetadata(Packet p1, Packet p2) {
    assertThat(p1.getType()).isEqualTo(p2.getType());
    assertThat(p1.getId()).isEqualTo(p2.getId());
    assertThat(p1.getNamespace()).isEqualTo(p2.getNamespace());
    assertThat(p1.getAttachments()).isEqualTo(p2.getAttachments());
  }

}
