package me.streamis.socket.io.parser;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import me.streamis.socket.io.parser.Packet.PacketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static me.streamis.socket.io.parser.Packet.PacketType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(VertxExtension.class)
public class PacketTest {

  private Vertx vertx;
  private EventBus packetEventBus;

  @BeforeEach
  public void setup() {
    vertx = Vertx.vertx();
    packetEventBus = vertx.eventBus();
    packetEventBus.registerDefaultCodec(Packet.class, new PacketLocalMessageCodec());
  }

  @Test
  public void encodeAndDecodeInString() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();

    Packet packet = new Packet();
    packet.setType(PacketType.EVENT);
    packet.setData("test-packet");
    packet.setId(13L);
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
    packet.setId(1L);
    packet.setNamespace("/test");
    packet.setData(new JsonArray().add("a").add(1).add(new JsonObject()));
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.ACK);
    packet.setId(123L);
    packet.setNamespace("/");
    packet.setData(new JsonArray().add("a").add(1).add(new JsonObject()));
    assertPacketAsString(packet, testContext);

    packet = new Packet();
    packet.setType(PacketType.ERROR);
    packet.setNamespace("/");
    packet.setData("Unauthorized");
    assertPacketAsString(packet, testContext);

    //TODO bad binary
    String address = UUID.randomUUID().toString();
    MessageProducer<Packet> emit = packetEventBus.publisher(address);
    Parser.Decode decode = new Parser.Decode(emit);

    packetEventBus.localConsumer(address, (Handler<Message<Packet>>) packetMessage -> {
      Packet decodedPacket = packetMessage.body();
      assertEquals(PacketType.ERROR, decodedPacket.getType());
      assertEquals("parser error: invalid payload", decodedPacket.getData());
      testContext.completeNow();
    });
    decode.add("442[\"some\", \"data\"");

    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void encodeAndDecodeInBinary() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();
    Packet packet = new Packet();
    packet.setType(BINARY_EVENT);
    packet.setId(23);
    packet.setNamespace("/cool");
    List<Object> data = new ArrayList<>(2);
    data.add("a");
    data.add(Buffer.buffer().appendString("abc", "utf8"));
    packet.setData(data);
    assertPacketAsBinary(packet, testContext);

    packet = new Packet();
    packet.setType(BINARY_ACK);
    packet.setId(127);
    packet.setNamespace("/back");
    data = new ArrayList<>(3);
    data.add("a");
    data.add(Buffer.buffer().appendString("xxx", "utf8"));
    data.add(new JsonObject());
    packet.setData(data);
    assertPacketAsBinary(packet, testContext);

    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
  }



  private void assertPacketAsString(Packet packet, VertxTestContext testContext) {
    String encodeData = Parser.encodeAsString(packet);

    //event bus address should be socket id
    String address = UUID.randomUUID().toString();
    MessageProducer<Packet> emit = packetEventBus.publisher(address);
    Parser.Decode decode = new Parser.Decode(emit);

    packetEventBus.localConsumer(address, (Handler<Message<Packet>>) packetMessage -> {
      Packet decodedPacket = packetMessage.body();
      testContext.verify(() -> {
        assertPacketMetadata(packet, decodedPacket);
        assertEquals(packet.getAttachments(), decodedPacket.getAttachments());
        assertEquals(packet.getData() != null ? Helper.stringify(packet.getData()) : null,
          decodedPacket.getData());
        testContext.completeNow();
      });
    }).completionHandler(event -> {
      if (event.succeeded()) {
        decode.add(encodeData);
      } else {
        testContext.failNow(event.cause());
      }
    });
  }

  private void assertPacketAsBinary(Packet packet, VertxTestContext testContext) {
    Packet originalPacket = new Packet();
    originalPacket.setData(packet.getData());

    List<Object> encodedPackets = Parser.encodeAsBinary(packet);

    String address = UUID.randomUUID().toString();
    MessageProducer<Packet> emit = packetEventBus.publisher(address);
    Parser.Decode decode = new Parser.Decode(emit);

    packetEventBus.localConsumer(address, (Handler<Message<Packet>>) packetMessage -> {
      Packet decodedPacket = packetMessage.body();
      testContext.verify(() -> {
        packet.setAttachments(-1);
        packet.setData(originalPacket.getData());
        assertPacketMetadata(packet, decodedPacket);
        assertEquals(packet.getData(), decodedPacket.getData(), "should be equal " + packet.getData() + " != " + decodedPacket.getData());
        testContext.completeNow();
      });
    }).completionHandler(event -> {
      if (event.succeeded()) {
        encodedPackets.forEach(decode::add);
      } else {
        testContext.failNow(event.cause());
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
