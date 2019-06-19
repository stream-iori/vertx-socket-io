package me.streamis.socket.io.parser;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

import static me.streamis.socket.io.parser.Packet.PacketType.*;


public class Packet {

  public static final int protocol = 4;

  public enum PacketType {
    CONNECT, DISCONNECT, EVENT, ACK, ERROR, BINARY_EVENT, BINARY_ACK;

    public static boolean inTypeRange(int type) {
      return type <= BINARY_ACK.ordinal() && type >= CONNECT.ordinal();
    }
  }

  private PacketType type;

  private String namespace = "/";
  private long id;
  private int attachments;
  private Object data;

  public PacketType getType() {
    return type;
  }

  public void setType(PacketType type) {
    this.type = type;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public int getAttachments() {
    return attachments;
  }

  public void setAttachments(int attachments) {
    this.attachments = attachments;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "Packet{" +
      "type=" + type +
      ", namespace='" + namespace + '\'' +
      ", id=" + id +
      ", attachments=" + attachments +
      ", data=" + data +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Packet packet = (Packet) o;
    return id == packet.id &&
      attachments == packet.attachments &&
      type == packet.type &&
      Objects.equals(namespace, packet.namespace) &&
      Objects.equals(data, packet.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, namespace, id, attachments, data);
  }

  public static void decodePacket(Object encodeData, Handler<Packet> packetHandler) {
    Packet packet;
    ReConstructor reConstructor = null;
    if (encodeData instanceof String) {
      packet = decodeAsString(encodeData.toString());
      if (isBinaryType(packet.type)) {
        reConstructor = new ReConstructor(packet);
        if (reConstructor.packet.attachments == 0) {
          packetHandler.handle(packet);
        }
      } else {
        packetHandler.handle(packet);
      }
    } else if (encodeData instanceof Buffer) { //TODO b64
      if (reConstructor == null) {
        //TODO async result ?
        throw new SocketIOParserException("got binary data when not reconstructing a packet");
      } else {
        packet = reConstructor.takeBinaryData((Buffer) encodeData);
        if (packet != null) {
          packetHandler.handle(packet);
        }
      }
    } else {
      throw new SocketIOParserException("UnKnow data type:" + encodeData);
    }
  }

  private static Packet decodeAsString(String encodeData) {
    Packet packet = new Packet();
    int i = 0;
    int type = encodeData.charAt(i) & 0xF;
    if (!PacketType.inTypeRange(type)) {
      return errorPacket("unKnow packet type " + type);
    }
    packet.type = PacketType.values()[type];

    if (isBinaryType(packet.type)) {
      String attachmentsStr = "";
      while (encodeData.charAt(++i) != '-') {
        attachmentsStr += encodeData.charAt(i);
        if (i == encodeData.length()) break;
      }
      if (Integer.valueOf(attachmentsStr) >= 0 || encodeData.charAt(i) != '-') {
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

  public static List<Buffer> encodeAsBinary(Packet packet) {
    Map<String, Object> packetMap = deconstructPacket(packet);
    String packetAsString = encodeAsString((Packet) packetMap.get("packet"));
    List<Buffer> buffers = (List<Buffer>) packetMap.get("buffers");
    buffers.add(0, Buffer.buffer(packetAsString));
    return buffers;
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

  private static class ReConstructor {
    private Packet packet;
    private List<Buffer> buffers;

    ReConstructor(Packet packet) {
      this.packet = packet;
      this.buffers = new ArrayList<>();
    }

    Packet takeBinaryData(Buffer data) {
      this.buffers.add(data);
      if (this.buffers.size() == this.packet.attachments) {
        Packet packet = reconstructPacket(this.packet, this.buffers);
        this.buffers.clear();
        this.packet = null;
        return packet;
      }
      return null;
    }
  }


  private static Packet reconstructPacket(Packet packet, List<Buffer> buffers) {
    packet.data = reconstructPacket(packet.data, buffers);
    packet.attachments = -1;
    return packet;
  }

  private static Object reconstructPacket(Object data, List<Buffer> buffers) {
    if (data == null) return null;
    if (data instanceof String) {
      String dataStr = (String) data;
      if (dataStr.startsWith("{") && dataStr.endsWith("}")) {
        JsonObject dataJson = new JsonObject(dataStr);
        return buffers.get(dataJson.getInteger("num"));
      } else if (dataStr.startsWith("[") && dataStr.endsWith("]")) {
        JsonArray dataArray = new JsonArray(dataStr);
        List dataList = dataArray.getList();
        for (int i = 0; i < dataArray.size(); i++) {
          dataList.set(i, reconstructPacket(dataArray.getValue(i), buffers));
        }
        data = dataList;
      }
    } else {
      throw new SocketIOParserException("reconstruct packet failed, packet's type should be String");
    }
    return data;
  }


  private static boolean isBinaryType(PacketType type) {
    return (type == BINARY_EVENT || type == BINARY_ACK);
  }

  private static Packet errorPacket(String message) {
    Packet packet =  new Packet();
    packet.type = ERROR;
    packet.data = "parser error: " + message;
    return packet;
  }

}
