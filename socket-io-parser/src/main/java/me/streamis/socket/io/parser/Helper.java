package me.streamis.socket.io.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
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

  static String stringify(Object data) {
    return "\"" + data + "\"";
  }

  static String deStringify(Object data) {
    Objects.nonNull(data);
    String dataStr = (String) data;
    if (dataStr.length() >= 2) {
      return dataStr.substring(1, dataStr.length() - 1);
    } else {
      return dataStr;
    }
  }
}
