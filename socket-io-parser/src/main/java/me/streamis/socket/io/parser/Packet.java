package me.streamis.socket.io.parser;

import io.vertx.core.buffer.Buffer;


public class Packet {

  public enum PacketType {
    CONNECT, DISCONNECT, EVENT, ACK, ERROR, BINARY_EVENT, BINARY_ACK
  }

  private String namespace;
  private PacketType packetType;
  private long id;
  private Buffer data;


  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public PacketType getPacketType() {
    return packetType;
  }

  public void setPacketType(PacketType packetType) {
    this.packetType = packetType;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Buffer getData() {
    return data;
  }

  public void setData(Buffer data) {
    this.data = data;
  }
}
