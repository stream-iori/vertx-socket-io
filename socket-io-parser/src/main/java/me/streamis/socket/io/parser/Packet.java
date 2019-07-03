package me.streamis.socket.io.parser;

import io.vertx.core.json.JsonObject;

import java.util.*;


public class Packet<T> {

  public static final int protocol = 4;

  public enum PacketType {
    CONNECT, DISCONNECT, EVENT, ACK, ERROR, BINARY_EVENT, BINARY_ACK;

    public static boolean inTypeRange(int type) {
      return type <= BINARY_ACK.ordinal() && type >= CONNECT.ordinal();
    }
  }

  public Packet() {
  }

  public Packet(PacketType type) {
    this.type = type;
  }

  public Packet(PacketType type, T data) {
    this.type = type;
    this.data = data;
  }

  private PacketType type;
  private String namespace = "/";
  private int id = -1;
  private int attachments;
  private T data;

  public PacketType getType() {
    return type;
  }

  public void setType(PacketType type) {
    this.type = type;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getAttachments() {
    return attachments;
  }

  public void setAttachments(int attachments) {
    this.attachments = attachments;
  }

  public Object getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "Packet{" +
      "type=" + type +
      ", namespace='" + namespace + '\'' +
      ", id=" + id +
      ", attachments=" + attachments +
      ", data=" + data +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Packet packet = (Packet) o;
    return id == packet.id &&
      attachments == packet.attachments &&
      type == packet.type &&
      Objects.equals(namespace, packet.namespace) &&
      Objects.equals(data, packet.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, namespace, id, attachments, data);
  }


}
