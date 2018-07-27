package me.streamis.parser;

import io.vertx.core.buffer.Buffer;
import me.streamis.EngineIOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.streamis.parser.PacketCodec.Type.BASE64;
import static me.streamis.parser.PacketCodec.Type.STRING;
import static me.streamis.parser.PacketCodec.factory;

/**
 * Created by stream.
 */
public class PayLoad {

  public static List<Packet> decodePayLoad(byte[] data) {
    List<Packet> packets = new ArrayList<>();
    for (int i = 0, contentLengthIndex = 0; i < data.length; ) {
      if (data[i] != ':') {
        i++;
        continue;
      }
      int stringSize = Integer.valueOf(new String(Arrays.copyOfRange(data, contentLengthIndex, i)));
      int packetStart = i + 1;
      int packetIndex = 1;
      while (stringSize > 0 && i < data.length) {
        if (GuavaUTF8.isWellFormed(data, packetStart, packetIndex++)) stringSize--;
        i++;
      }
      if (stringSize > 0) throw new EngineIOException("read payload failed:" + new String(data));
      Packet packet = readPacket(Buffer.buffer(Arrays.copyOfRange(data, packetStart, ++i)));
      packets.add(packet);
      contentLengthIndex = i;
    }
    return packets;
  }

  public static Buffer encodePayload(Packet... packets) {
    if (packets.length < 1) throw new EngineIOException("packet is empty.");
    Buffer buffer = Buffer.buffer();
    for (Packet packet : packets) {
      buffer.appendBuffer(writePacket(packet));
    }
    return buffer;
  }

  private static Packet readPacket(Buffer buffer) {
    return buffer.getByte(0) != 'b' ? factory(STRING).decode(buffer) : factory(BASE64).decode(buffer);
  }

  private static Buffer writePacket(Packet packet) {
    Buffer data;
    int contentLength;
    if (packet.option != PacketOption.BINARY) {
      data = factory(STRING).encode(packet);
      contentLength = GuavaUTF8.encodedLength(data.toString());
    } else {
      data = factory(BASE64).encode(packet);
      contentLength = data.length();
    }
    Buffer result = Buffer.buffer().appendString(contentLength + ":");
    result.appendBuffer(data);
    return result;
  }


}
