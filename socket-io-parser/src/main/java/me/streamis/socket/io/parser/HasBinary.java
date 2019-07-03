package me.streamis.socket.io.parser;

import io.vertx.core.buffer.Buffer;

import java.util.List;
import java.util.Map;

public final class HasBinary {

  private HasBinary() {
  }

  public static boolean hasBinary(Object data) {
    return hasBinary0(data);
  }

  private static boolean hasBinary0(Object obj) {
    if (obj == null) return false;

    if (obj instanceof byte[] || obj instanceof Buffer) {
      return true;
    }

    if (obj instanceof List) {
      List array = (List) obj;
      for (Object v : array) if (hasBinary0(v)) return true;
      return false;
    } else if (obj instanceof Map) {
      Map<String, Object> jsonObj = (Map) obj;
      for (Map.Entry<String, Object> stringObjectEntry : jsonObj.entrySet()) {
        if (hasBinary0(stringObjectEntry.getValue())) return true;
      }
    }
    return false;
  }

}
