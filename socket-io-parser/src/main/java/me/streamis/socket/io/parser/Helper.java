package me.streamis.socket.io.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;


class Helper {

  static boolean isValidJson(String data) {
    try {
      Json.mapper.readTree(data);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  static boolean isJsonArray(String jsonData) {
    try {
      new JsonArray(jsonData);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  static boolean isJsonObject(String jsonData) {
    try {
      new JsonObject(jsonData);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  static Object tryStringify(Object data) {
    if (data.getClass().isPrimitive() || data instanceof String) {
      return "\"" + data + "\"";
    } else if (data instanceof JsonObject || data instanceof JsonArray) {
      return data;
    } else if (data instanceof Map) {
      Map<String, Object> mapData = (Map) data;
      if (!HasBinary.hasBinary(mapData)) return new JsonObject(mapData);
      else throw new SocketIOParserException("can not tryStringify object. " + data);
    } else if (data instanceof List) {
      List<Object> listData = (List)data;
      if (!HasBinary.hasBinary(listData)) return new JsonArray(listData);
      else throw new SocketIOParserException("can not tryStringify object. " + data);
    } else {
      throw new SocketIOParserException("can not tryStringify object. " + data);
    }
  }

  static String deStringify(Object data) {
    Objects.nonNull(data);
    String dataStr = (String) data;
    if (dataStr.length() >= 2 && (
      (dataStr.startsWith("\"") && dataStr.endsWith("\"")) ||
        (dataStr.startsWith("'") && dataStr.endsWith("'")))
    ) {
      return dataStr.substring(1, dataStr.length() - 1);
    } else {
      return dataStr;
    }
  }
}
