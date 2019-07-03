package me.streamis.socket.io.parser;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.streamis.socket.io.parser.Packet.PacketType.*;

public class IOParser implements Parser {

  private static Packet<String> error() {
    return new Packet<>(ERROR, "parser error");
  }

  private IOParser() {
  }

  final public static class Encoder implements Parser.Encoder {

    public Encoder() {
    }

    @Override
    public void encode(Packet packet, Handler<Object[]> callback) {
      if ((packet.getType() == EVENT || packet.getType() == ACK) && HasBinary.hasBinary(packet.getData())) {
        packet.setType(packet.getType() == EVENT ? BINARY_EVENT : BINARY_ACK);
      }

      if (BINARY_EVENT == packet.getType() || BINARY_ACK == packet.getType()) {
        encodeAsBinary(packet, callback);
      } else {
        String encoding = encodeAsString(packet);
        callback.handle(new String[]{encoding});
      }
    }

    private String encodeAsString(Packet packet) {
      StringBuilder str = new StringBuilder("" + packet.getType().ordinal());

      if (BINARY_EVENT == packet.getType() || BINARY_ACK == packet.getType()) {
        str.append(packet.getAttachments());
        str.append("-");
      }

      if (packet.getNamespace() != null && packet.getNamespace().length() != 0 && !"/".equals(packet.getNamespace())) {
        str.append(packet.getNamespace());
        str.append(",");
      }

      if (packet.getId() >= 0) {
        str.append(packet.getId());
      }

      if (packet.getData() != null) {
        str.append(Helper.tryStringify(packet.getData()));
      }
      return str.toString();
    }

    private void encodeAsBinary(Packet obj, Handler<Object[]> callback) {
      Binary.DeconstructedPacket deconstruction = Binary.deconstructPacket(obj);
      String pack = encodeAsString(deconstruction.packet);
      List<Object> buffers = new ArrayList<>(Arrays.asList(deconstruction.buffers));

      buffers.add(0, pack);
      callback.handle(buffers.toArray());
    }
  }

  final public static class Decoder implements Parser.Decoder {

    BinaryReconstructor reconstructor;
    private Handler<Packet> onDecodedCallback;

    public Decoder() {
      this.reconstructor = null;
    }

    @Override
    public void add(String obj) {
      Packet packet = decodeString(obj);
      if (BINARY_EVENT == packet.getType() || BINARY_ACK == packet.getType()) {
        this.reconstructor = new BinaryReconstructor(packet);

        if (this.reconstructor.reconPack.getAttachments() == 0) {
          if (this.onDecodedCallback != null) {
            this.onDecodedCallback.handle(packet);
          }
        }
      } else {
        if (this.onDecodedCallback != null) {
          this.onDecodedCallback.handle(packet);
        }
      }
    }

    @Override
    public void add(byte[] obj) {
      if (this.reconstructor == null) {
        throw new SocketIOParserException("got binary data when not reconstructing a packet");
      } else {
        Packet packet = this.reconstructor.takeBinaryData(obj);
        if (packet != null) {
          this.reconstructor = null;
          if (this.onDecodedCallback != null) {
            this.onDecodedCallback.handle(packet);
          }
        }
      }
    }

    private static Packet decodeString(String str) {
      int i = 0;
      int length = str.length();
      int typeIndex = Character.getNumericValue(str.charAt(0));
      if (!Packet.PacketType.inTypeRange(typeIndex)) return error();
      Packet<Object> p = new Packet<>(Packet.PacketType.values()[typeIndex]);
      if (BINARY_EVENT == p.getType() || BINARY_ACK == p.getType()) {
        if (!str.contains("-") || length <= i + 1) return error();
        StringBuilder attachments = new StringBuilder();
        while (str.charAt(++i) != '-') attachments.append(str.charAt(i));
        p.setAttachments(Integer.parseInt(attachments.toString()));
      }

      if (length > i + 1 && '/' == str.charAt(i + 1)) {
        StringBuilder nsp = new StringBuilder();
        while (true) {
          ++i;
          char c = str.charAt(i);
          if (',' == c) break;
          nsp.append(c);
          if (i + 1 == length) break;
        }
        p.setNamespace(nsp.toString());
      } else {
        p.setNamespace("/");
      }

      if (length > i + 1) {
        Character next = str.charAt(i + 1);
        if (Character.getNumericValue(next) > -1) {
          StringBuilder id = new StringBuilder();
          while (true) {
            ++i;
            char c = str.charAt(i);
            if (Character.getNumericValue(c) < 0) {
              --i;
              break;
            }
            id.append(c);
            if (i + 1 == length) break;
          }
          try {
            p.setId(Integer.parseInt(id.toString()));
          } catch (NumberFormatException e) {
            return error();
          }
        }
      }

      if (length > i + 1) {
        str.charAt(++i);
        String data = Helper.deStringify(str.substring(i));
        if (Helper.isValidJson(data)) {
          if (Helper.isJsonObject(data)) p.setData(new JsonObject(data));
          if (Helper.isJsonArray(data)) p.setData(new JsonArray(data));
        } else {
          p.setData(data);
        }
      }
      return p;
    }

    @Override
    public void destroy() {
      if (this.reconstructor != null) {
        this.reconstructor.finishReconstruction();
      }
      this.onDecodedCallback = null;
    }

    @Override
    public void onDecoded(Handler<Packet> callback) {
      this.onDecodedCallback = callback;
    }
  }

  static class BinaryReconstructor {

    public Packet reconPack;
    List<byte[]> buffers;

    BinaryReconstructor(Packet packet) {
      this.reconPack = packet;
      this.buffers = new ArrayList<>();
    }

    public Packet takeBinaryData(byte[] binData) {
      this.buffers.add(binData);
      if (this.buffers.size() == this.reconPack.getAttachments()) {
        Packet packet = Binary.reconstructPacket(this.reconPack,
          this.buffers.toArray(new byte[this.buffers.size()][]));
        this.finishReconstruction();
        return packet;
      }
      return null;
    }

    public void finishReconstruction() {
      this.reconPack = null;
      this.buffers.clear();
    }
  }
}
