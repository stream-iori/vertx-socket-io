package me.streamis.engine.io.server;

/**
 * Created by stream.
 */
enum ServerErrors {
  UNKNOWN_TRANSPORT(0, "Transport unknown"),
  UNKNOWN_SID(1, "Session ID unknown"),
  BAD_HANDSHAKE_METHOD(2, "Bad handshake method"),
  BAD_REQUEST(3, "Bad request"),
  FORBIDDEN(4, "Forbidden");

  private final int code;
  private final String message;

  ServerErrors(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
