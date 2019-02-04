package me.streamis.engine.io.parser;

import io.vertx.core.buffer.Buffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by stream.
 */
public class Packet {
  PacketType type;

  //could be buffer or string
  Object data;

  private static final PacketCodec<Buffer, String> b64Codec = new Base64PacketCodec();
  private static final PacketCodec<String, String> strCodec = new StringPacketCodec();
  private static final PacketCodec<Buffer, Buffer> binCodec = new BinaryPacketCodec();

  public Packet(PacketType type, Object data) {
    this.type = type;
    this.data = data;
  }

  public Packet(PacketType type) {
    this.type = type;
  }

  public PacketType getType() {
    return this.type;
  }

  public Object getData() {
    return this.data;
  }

  public static Packet decodeWithBuffer(Buffer data) {
    return binCodec.decode(data);
  }

  public static Packet decodeWithString(String data) {
    byte b = data.getBytes()[0];
    if (b == 'b') return b64Codec.decode(Buffer.buffer(data));
    return strCodec.decode(data);
  }

  /**
   * for Buffer
   * The data type of packet should be buffer
   */
  public static Buffer encodeAsBuffer(Packet packet) {
    return binCodec.encode(packet);
  }

  /**
   * for String or Base64
   * The data type of packet could be binary or String
   */
  public static String encodeAsString(Packet packet) {
    //data is Buffer but target do not supports binary, we should encode as Base64
    if (packet.data instanceof Buffer) {
      return b64Codec.encode(packet);
    } else {
      return strCodec.encode(packet);
    }
  }

  public static Object encodePayload(boolean supportsBinary, Packet... packets) {
    return encodePayload(supportsBinary, Arrays.asList(packets));
  }

  public static Object encodePayload(boolean supportsBinary, List<Packet> packets) {
    if (supportsBinary) {
      Buffer result = Buffer.buffer();
      if (packets.size() == 0) return result;
      if (packets.get(0).data instanceof String) {
        StringBuilder resultStr = new StringBuilder();
        for (Packet packet : packets) {
          String content = encodeAsString(packet);
          resultStr
            .append(content.length())
            .append(":")
            .append(content);
        }
        return resultStr.toString();
      } else {
        for (Packet packet : packets) {
          Buffer content = encodeAsBuffer(packet);
          result
            .appendString(String.valueOf(content.length()))
            .appendString(":")
            .appendString(content.toString());
        }
        return result;
      }
    } else {
      if (packets.size() == 0) return "0:";
      StringBuilder resultStr = new StringBuilder();
      for (Packet packet : packets) {
        String content = encodeAsString(packet);
        resultStr
          .append(content.codePointCount(0, content.length()))
          .append(":")
          .append(content);
      }
      return resultStr.toString();
    }
  }


  public static List<Packet> decodePayload(String dataStr) {
    if (dataStr == null || dataStr.equals("")) throw new EngineIOParserException("payload is empty.");
    byte[] data = dataStr.getBytes();
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
      if (stringSize > 0) throw new EngineIOParserException("read payload failed:" + new String(data));
      Packet packet = decodeWithString(new String(Arrays.copyOfRange(data, packetStart, ++i)));
      packets.add(packet);
      contentLengthIndex = i;
    }
    return packets;
  }

  public static List<Packet> decodePayload(Buffer data) {
    if (data == null) throw new EngineIOParserException("payload is empty.");
    List<Packet> packets = new ArrayList<>();
    for (int i = 1, offset = 0; i < data.length(); ) {
      if (data.getByte(i) != ':') {
        i++;
        continue;
      }
      int contentLength = Integer.valueOf(data.getString(offset, i));
      //skip :
      i++;
      Packet packet = decodeWithBuffer(data.getBuffer(i, i + contentLength));
      packets.add(packet);
      i += contentLength;
      offset = i;
    }
    return packets;
  }

  @Override
  public String toString() {
    return "Packet{" +
      "type=" + type +
      ", data=" + data +
      '}';
  }
}
