package me.streamis.socket.io.parser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

import static me.streamis.socket.io.parser.Packet.PacketType.*;

public class Parser {

  public static class Decode {
    private MessageProducer<Packet> emit;
    private ReConstructor reConstructor = null;

    public Decode(MessageProducer<Packet> emit) {
      this.emit = emit;
    }

    public void add(Object encodeData) {
      Packet packet;
      if (encodeData instanceof String) {
        packet = decodeAsString(encodeData.toString());
        if (isBinaryType(packet.type)) {
          reConstructor = new ReConstructor(packet);
          if (reConstructor.packet.attachments == 0) {
            emit.send(packet);
          }
        } else {
          emit.send(packet);
        }
      } else if (encodeData instanceof Buffer) { //TODO b64
        if (reConstructor == null) {
          throw new SocketIOParserException("got binary data when not reconstructing a packet");
        } else {
          packet = reConstructor.takeBinaryData((Buffer) encodeData);
          if (packet != null) {
            emit.send(packet);
          }
        }
      } else {
        throw new SocketIOParserException("UnKnow data type:" + encodeData);
      }
    }
  }


  private static class ReConstructor {
    private Packet packet;
    private List<Buffer> buffers;

    ReConstructor(Packet packet) {
      this.packet = packet;
      this.buffers = new ArrayList<>();
    }

    Packet takeBinaryData(Buffer data) {
      this.buffers.add(data);
      if (this.buffers.size() == this.packet.getAttachments()) {
        Packet packet = reconstructPacket(this.packet, this.buffers);
        this.buffers.clear();
        this.packet = null;
        return packet;
      }
      return null;
    }

    private Packet reconstructPacket(Packet packet, List<Buffer> buffers) {
      Object originData = packet.getData();
      if (originData != null) {
        String stringify = Helper.deStringify(originData);
        Object dataObj = null;
        if (Helper.isValidJson(stringify)) {
          if (Helper.isJsonArray(stringify)) {
            dataObj = new JsonArray(stringify);
          } else if (Helper.isJsonObject(stringify)) {
            dataObj = new JsonObject(stringify);
          }
          packet.setData(reconstructPacket(dataObj, buffers));
        }
      }
      packet.setAttachments(-1);
      return packet;
    }

    private Object reconstructPacket(Object data, List<Buffer> buffers) {
      if (data == null) return null;
      if (data instanceof JsonObject) {
        JsonObject dataJson = (JsonObject) data;
        if (dataJson.getBoolean("_placeholder") != null && dataJson.getBoolean("_placeholder")) {
          return buffers.get(dataJson.getInteger("num"));
        } else {
          Map<String, Object> dataMap = dataJson.getMap();
          Map<String, Object> dataResult = new HashMap<>(dataMap.size());
          dataMap.forEach((key, value) -> dataResult.put(key, reconstructPacket(value, buffers)));
          data = dataMap;
        }
      } else if (data instanceof JsonArray) {
        JsonArray dataArray = (JsonArray) data;
        List dataList = dataArray.getList();
        for (int i = 0; i < dataArray.size(); i++) {
          dataList.set(i, reconstructPacket(dataArray.getValue(i), buffers));
        }
        data = dataList;
      }
      return data;
    }
  }


  private static Packet decodeAsString(String encodeData) {
    Packet packet = new Packet();
    int i = 0;
    int type = encodeData.charAt(i) & 0xF;
    if (!inTypeRange(type)) {
      return errorPacket("unKnow packet type " + type);
    }
    packet.type = values()[type];

    if (isBinaryType(packet.type)) {
      String attachmentsStr = "";
      while (encodeData.charAt(++i) != '-') {
        attachmentsStr += encodeData.charAt(i);
        if (i == encodeData.length()) break;
      }
      if (!isNumber(attachmentsStr) || encodeData.charAt(i) != '-') {
        return errorPacket("Illegal attachments");
      }
      packet.attachments = Integer.valueOf(attachmentsStr);
    }

    if (encodeData.charAt(i + 1) == '/') {
      packet.namespace = "";
      while (++i < encodeData.length()) {
        char c = encodeData.charAt(i);
        if (c == ',') break;
        packet.namespace += c;
      }
    } else {
      packet.namespace = "/";
    }

    if (i < encodeData.length() - 1) {
      char next = encodeData.charAt(i + 1);
      if (next != 0 && Character.isDigit(next)) {
        String idStr = "";
        while (++i < encodeData.length()) {
          char c = encodeData.charAt(i);
          if (c == 0 ||
            !Character.isDigit(c) ||
            (c & 0xF) != Integer.valueOf(String.valueOf(c))) {
            --i;
            break;
          }
          idStr += encodeData.charAt(i);
        }
        packet.id = Long.valueOf(idStr);
      }
    }

    if (++i < encodeData.length() && encodeData.charAt(i) != 0) {
      String payload = encodeData.substring(i);
      if (Helper.isValidJson(payload)) {
        if (Helper.isJsonObject(payload)) {
          packet.data = new JsonObject(payload);
        } else if (Helper.isJsonArray(payload)) {
          packet.data = new JsonArray(payload);
        } else {
          packet.data = payload;
        }
      } else {
        packet = errorPacket("invalid payload");
      }
    }

    return packet;
  }


  public static String encodeAsString(Packet packet) {
    StringBuilder result = new StringBuilder();
    //type
    result.append(packet.type.ordinal());
    if (isBinaryType(packet.type) && packet.attachments > 0) {
      result.append(packet.attachments).append("-");
    }
    if (packet.namespace != null && !packet.namespace.equals("/")) {
      result.append(packet.namespace).append(",");
    }
    if (packet.id != 0) {
      result.append(packet.id);
    }
    if (packet.data != null) {
      //stringify
      result.append("\"" + packet.data + "\"");
    }
    return result.toString();
  }

  public static List<Object> encodeAsBinary(Packet packet) {
    Map<String, Object> packetMap = deconstructPacket(packet);
    String packetAsString = encodeAsString((Packet) packetMap.get("packet"));
    List<Buffer> buffers = (List<Buffer>) packetMap.get("buffers");
    List<Object> result = new ArrayList<>(buffers);
    result.add(0, packetAsString);
    return result;
  }

  private static Map<String, Object> deconstructPacket(Packet packet) {
    List<Buffer> buffers = new ArrayList<>();
    packet.data = deconstructPacket(packet.data, buffers);
    packet.attachments = buffers.size();
    return new HashMap<String, Object>() {{
      put("packet", packet);
      put("buffers", buffers);
    }};
  }

  private static Object deconstructPacket(Object data, List<Buffer> buffers) {
    if (data == null) return null;
    if (data instanceof Buffer) {
      String placeHolder = "{\"_placeholder\":true,\"num\":" + buffers.size() + "}";
      buffers.add((Buffer) data);
      return new JsonObject(placeHolder);
    } else if (data instanceof Collection) {
      Object[] dataArr = ((Collection) data).toArray();
      JsonArray newData = new JsonArray();
      for (int i = 0; i < dataArr.length; i++) {
        newData.add(deconstructPacket(dataArr[i], buffers));
      }
      return newData;
    } else if (data instanceof Map) {
      Map<String, Object> dataMap = (Map) data;
      JsonObject newData = new JsonObject();
      dataMap.entrySet().forEach(entry -> newData.put(entry.getKey(), deconstructPacket(entry.getValue(), buffers)));
      return newData;
    }
    return data;
  }


  private static boolean isBinaryType(Packet.PacketType type) {
    return (type == BINARY_EVENT || type == BINARY_ACK);
  }

  private static Packet errorPacket(String message) {
    Packet packet = new Packet();
    packet.type = ERROR;
    packet.data = "parser error: " + message;
    return packet;
  }

  private static boolean isNumber(String str) {
    for (int i = str.length(); --i >= 0; ) {
      int chr = str.charAt(i);
      if (chr < 48 || chr > 57)
        return false;
    }
    return true;
  }


}
