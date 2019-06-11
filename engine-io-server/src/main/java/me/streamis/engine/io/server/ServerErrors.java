package me.streamis.engine.io.server;

/**
 * Created by stream.
 */
enum ServerErrors {
  UNKNOWN_TRANSPORT("Transport unknown"),
  UNKNOWN_SID("Session ID unknown"),
  BAD_HANDSHAKE_METHOD("Bad handshake method"),
  BAD_REQUEST("Bad request"),
  FORBIDDEN("Forbidden");

  private String message;

  ServerErrors(String message) {
    this.message = message;
  }

  public String getMessage() {
    return this.message;
  }

  public static boolean isInCodeRange(int code) {
    return code >= 0 && code <= 4;
  }

  public static String getMessageByCode(int code) {
    for (ServerErrors value : ServerErrors.values()) {
      if (value.ordinal() == code) return value.message;
    }
    return null;
  }
}
