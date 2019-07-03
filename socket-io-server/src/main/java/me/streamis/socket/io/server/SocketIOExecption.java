package me.streamis.socket.io.server;

public class SocketIOExecption extends RuntimeException{
  public SocketIOExecption(String message) {
    super(message);
  }

  public SocketIOExecption(String message, Throwable cause) {
    super(message, cause);
  }

  public SocketIOExecption(Throwable cause) {
    super(cause);
  }

  public SocketIOExecption(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
