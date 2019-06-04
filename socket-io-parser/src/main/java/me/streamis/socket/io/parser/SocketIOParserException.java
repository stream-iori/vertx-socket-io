package me.streamis.socket.io.parser;

public class SocketIOParserException extends RuntimeException{


  public SocketIOParserException(String message) {
    super(message);
  }

  public SocketIOParserException(String message, Throwable cause) {
    super(message, cause);
  }

  public SocketIOParserException(Throwable cause) {
    super(cause);
  }

  protected SocketIOParserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
