package me.streamis.socket.io.parser;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class Binary {

  static final String KEY_PLACEHOLDER = "_placeholder";

  static final String KEY_NUM = "num";

  public static DeconstructedPacket deconstructPacket(Packet packet) {
    List<byte[]> buffers = new ArrayList<>();

    packet.setData(deconstructPacket0(packet.getData(), buffers));
    packet.setAttachments(buffers.size());

    DeconstructedPacket result = new DeconstructedPacket();
    result.packet = packet;
    result.buffers = buffers.toArray(new byte[buffers.size()][]);
    return result;
  }

  private static Object deconstructPacket0(Object data, List<byte[]> buffers) {
    if (data == null) return null;

    if (data instanceof byte[]) {
      Map<String, Object> placeholder = new HashMap<>();
      placeholder.put(KEY_PLACEHOLDER, true);
      placeholder.put(KEY_NUM, buffers.size());
      buffers.add((byte[]) data);
      return placeholder;
    } else if (data instanceof List) {
      List newData = new ArrayList();
      List _data = (ArrayList) data;
      for (Object v : _data) {
        newData.add(deconstructPacket0(v, buffers));
      }
      return newData;
    } else if (data instanceof Map) {
      Map<String, Object> newData = new HashMap();
      Map<String, Object> _data = (HashMap) data;
      for (Map.Entry<String, Object> stringObjectEntry : _data.entrySet()) {
        newData.put(stringObjectEntry.getKey(), deconstructPacket0(stringObjectEntry.getValue(), buffers));
      }
      return newData;
    }
    return data;
  }

  public static Packet reconstructPacket(Packet packet, byte[][] buffers) {
    packet.setData(reconstructPacket0(packet.getData(), buffers));
    packet.setAttachments(-1);
    return packet;
  }

  private static Object reconstructPacket0(Object data, byte[][] buffers) {
    if (data instanceof List) {
      List _data = (List) data;
      List<Object> newData = new ArrayList<>(_data.size());
      for (Object v : _data) {
        newData.add(Objects.requireNonNull(reconstructPacket0(v, buffers)));
      }
      return newData;
    } else if (data instanceof Map) {
      Map<String, Object> _data = (HashMap) data;
      Map<String, Object> newData = new HashMap<>(_data.size());
      if (_data.get(KEY_PLACEHOLDER) != null && (boolean) _data.get(KEY_PLACEHOLDER)) {
        int num = (int) _data.getOrDefault(KEY_NUM, -1);
        return num >= 0 && num < buffers.length ? buffers[num] : null;
      }
      for (Map.Entry<String, Object> stringObjectEntry : _data.entrySet()) {
        newData.put(stringObjectEntry.getKey(), reconstructPacket0(stringObjectEntry.getValue(), buffers));
      }
      return _data;
    } else if (data instanceof JsonObject) {
      return reconstructPacket0(((JsonObject) data).getMap(), buffers);
    } else if (data instanceof JsonArray) {
      return reconstructPacket0(((JsonArray) data).getList(), buffers);
    }
    return data;
  }

  static class DeconstructedPacket {
    Packet packet;
    byte[][] buffers;
  }
}
