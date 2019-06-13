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

  //TODO Uint8
  public static Packet decodeAsBuffer(Buffer data) {
    return binCodec.decode(data);
  }

  public static Packet decodePacket(String data) {
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
  public static String encodePacket(Packet packet, boolean supportsBinary) {
    if (packet.data instanceof Buffer) {
      if (supportsBinary) {
        throw new UnsupportedOperationException("please invoke encodeAsBuffer");
      } else {
        return b64Codec.encode(packet);
      }
    } else {
      return strCodec.encode(packet);
    }
  }

  public static String encodePayload(boolean supportsBinary, Packet... packets) {
    return encodePayload(supportsBinary, Arrays.asList(packets));
  }

  /**
   * Encodes multiple messages (payload).
   *
   * <length>:data
   * <p>
   * Example:
   * <p>
   * 11:hello world2:hi
   * <p>
   * If any contents are binary, they will be encoded as base64 strings. Base64
   * encoded strings are marked with a b before the length specifier
   *
   * @param packets
   * @return String
   */
  public static String encodePayload(boolean supportsBinary, List<Packet> packets) {
    if (supportsBinary && packets.size() > 0 && packets.get(0).data instanceof Buffer) {
      throw new UnsupportedOperationException("please invoke method of Packet.encodePayloadAsBuffer as your packet in buffer.");
    }
    if (packets.size() == 0) return "0:";
    StringBuilder resultStr = new StringBuilder();
    for (Packet packet : packets) {
      String packetStr = encodePacket(packet, supportsBinary);
      resultStr.append(packetStr.length()).append(":").append(packetStr);
    }
    return resultStr.toString();
  }

  public static Buffer encodePayLoadAsBuffer(Packet... packets) {
    return encodePayLoadAsBuffer(Arrays.asList(packets));
  }

  /**
   * Encodes multiple messages (payload) as binary.
   * <p>
   * <1 = binary, 0 = string><number from 0-9><number from 0-9>[...]<number
   * 255><data>
   * <p>
   * Example:
   * 1 3 255 1 2 3, if the binary contents are interpreted as 8 bit integers
   *
   * @param packets
   * @return Buffer
   */
  public static Buffer encodePayLoadAsBuffer(List<Packet> packets) {
    Buffer result = Buffer.buffer();
    if (packets.size() == 0) return result;
    Buffer packetBuffer;
    for (Packet packet : packets) {
      if (packet.data instanceof String) {
        //+1 for pack type + 1 for 255 mark
        String size = "" + (2 + ((String) packet.data).length());
        //mark as string
        packetBuffer = Buffer.buffer().appendByte((byte) 0);
        //packet size in bytes
        packetBuffer.appendBytes(size.getBytes());
        packetBuffer.appendUnsignedByte((short) 255);
        for (byte aByte : binCodec.encode(packet).getBytes()) {
          packetBuffer.appendUnsignedByte(aByte);
        }
      } else {
        if (packet.data instanceof Buffer) {
          //+1 for pack type + 1 for 255 mark
          String size = "" + (2 + ((Buffer) packet.data).length());
          //mark as binary
          packetBuffer = Buffer.buffer().appendByte((byte) 1);
          packetBuffer.appendBytes(size.getBytes());
          packetBuffer.appendUnsignedByte((short) 255);
          packetBuffer.appendBuffer(binCodec.encode(packet));
        } else {
          throw new EngineIOParserException("unKnow format of packet.");
        }
      }
      result.appendBuffer(packetBuffer);
    }
    return result;
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
      ++i;
      int charStartPoint = i, packetStart = i,  charLength = 1;
      if (charStartPoint + stringSize > data.length)
        throw new EngineIOParserException("read payload failed, data size larger than packet size");
      while (stringSize > 0 && i < data.length) {
        if (GuavaUTF8.isWellFormed(data, charStartPoint, charLength)) {
          stringSize--;
          charStartPoint = i;
          charLength = 1;
        } else {
          charLength++;
          //for utf16 like emoji
          if (charLength == 4) stringSize--;
        }
        i++;
      }
      if (stringSize > 0) throw new EngineIOParserException("read payload failed:" + new String(data));
      Packet packet = decodePacket(new String(Arrays.copyOfRange(data, packetStart, i)));
      packets.add(packet);
      contentLengthIndex = i;
    }
    return packets;
  }

  /**
   * Decodes data when a payload is maybe expected. Strings are decoded by
   * interpreting each byte as a key code for entries marked to start with 0.
   * 1 3 255 1 2 3, if the binary contents are interpreted as 8 bit integers
   *
   * @param data
   * @return
   */
  public static List<Packet> decodePayloadAsBuffer(Buffer data) {
    if (data == null) throw new EngineIOParserException("payload is empty.");
    List<Packet> packets = new ArrayList<>();

    for (int i = 0, sizeOffset; i < data.length(); ) {
      boolean isString = data.getByte(i) == 0;
      i++;
      sizeOffset = i; //start of size
      while (Byte.toUnsignedInt(data.getByte(i)) != 255 && i < data.length()) i++;
      //now i point to the byte of 255 or the end of data
      long size = Long.valueOf(new String(data.getBytes(sizeOffset, i)));
      if (size > Integer.MAX_VALUE || size > i + size + 1)
        throw new EngineIOParserException("packet size is too long.");
      i++;// index of content
      int endIndex = (int) size + i - 1;
      if (isString) {
        byte[] content = data.getBytes(i, endIndex);
        packets.add(binCodec.decode(Buffer.buffer(content)));
      } else {
        Buffer content = data.getBuffer(i, endIndex);
        packets.add(binCodec.decode(content));
      }
      i = endIndex;
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
