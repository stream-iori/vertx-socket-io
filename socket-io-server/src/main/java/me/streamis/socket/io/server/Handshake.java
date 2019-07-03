package me.streamis.socket.io.server;

import io.vertx.core.MultiMap;

class Handshake {
  MultiMap headers;
  String time;
  String address;
  boolean xdomain;
  boolean secure;
  long issued;
  String url;
  MultiMap query;
}
