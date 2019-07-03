package me.streamis.socket.io.server;

public class SocketIOOptions {

  private String path = "/socket.io";
  private String origins = "*:*";

  public String getPath() {
    return path;
  }
}
