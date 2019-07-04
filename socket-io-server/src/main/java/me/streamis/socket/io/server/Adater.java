package me.streamis.socket.io.server;

import io.vertx.core.Handler;
import me.streamis.socket.io.parser.Packet;

import java.util.Arrays;
import java.util.List;

public interface Adater {

  /**
   * Adds a socket to a list of room
   */
  void addAAll(String id, List<String> rooms, Handler<Void> handler);

  /**
   * Adds a socket to a room
   */
  default void add(String id, String room, Handler<Void> handler) {
    addAAll(id, Arrays.asList(room), handler);
  }

  /**
   * Removes a socket from a room.
   */
  void del(String id, String room, Handler<Void> handler);

  /**
   * Removes a socket from all rooms it's joined.
   */
  void delAll(String id, Handler<Void> handler);

  /**
   * Broadcasts a packet.
   */
  void broadcast(Packet packet, List<String> excepts, List<String> rooms);

  /**
   * Gets a list of clients by sid.
   *
   * @param {Array} explicit set of rooms to check.
   */
  void clients(List<String> exceptRooms, Handler<List<String>> handler);


  void addToRoom(String id);


  void delFromRoom(String id);
}
