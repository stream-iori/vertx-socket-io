package me.streamis.socket.io.server;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.engine.io.server.State;

import java.util.*;
import java.util.function.Consumer;

public class NamespaceImpl implements Namespace {
  private Vertx vertx;
  String name;
  SIOServer server;
  Set<String> rooms;
  Handler<SIOSocket> connectHandler;
  Map<String, SIOSocket> connected;
  int ids = 0;
  private Map<String, Handler<Object[]>> eventHandler;
  Map<String, SIOSocket> sockets;

  private static Set<String> blackEvents = new HashSet<String>() {{
    add("connect");
    add("connection");
    add("newListener");
  }};
  private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceImpl.class);

  public NamespaceImpl(Vertx vertx, SIOServer sioServer) {
    this.vertx = vertx;
    this.server = sioServer;
    this.sockets = new HashMap<>();
    this.rooms = new HashSet<>();
    this.eventHandler = new HashMap<>();
    this.connected = new HashMap<>();
  }

  public NamespaceImpl(Vertx vertx, SIOServer sioServer, String name) {
    this(vertx, sioServer);
    this.name = name;
  }


  @Override
  public String getName() {
    return name;
  }

  @Override
  public Namespace to(String name) {
    if (!rooms.contains(name)) this.rooms.add(name);
    return this;
  }

  void add(Client client, MultiMap query, Consumer<SIOSocket> consumer) {
    if (LOGGER.isDebugEnabled()) LOGGER.debug("add socket to nsp " + this.name);
    SIOSocket sioSocket = new SIOSocketImpl(vertx, server, this, client, query);
    //TODO middleware
    vertx.runOnContext(aVoid -> {
      if (client.conn.getState() == State.OPEN) {
        sockets.put(sioSocket.id(), sioSocket);
        sioSocket.onConnect();
        if (consumer != null) consumer.accept(sioSocket);

        //fire user-set events;
        if (connectHandler == null) throw new SocketIOExecption("connect handler can not be null.");
        connectHandler.handle(sioSocket);
      }
    });
  }

  @Override
  public Namespace onConnect(Handler<SIOSocket> connectHandler) {
    this.connectHandler = connectHandler;
    return this;
  }

  @Override
  public void remove(SIOSocket sioSocket) {
    this.sockets.remove(sioSocket.id());
  }

  @Override
  public Emitter emit(String event, Object... args) {
    if (!blackEvents.contains(event) && eventHandler.containsKey(event)) {
      eventHandler.get(event).handle(args);
    }
    //TODO broadcast with MessageProducer
    return this;
  }
}
