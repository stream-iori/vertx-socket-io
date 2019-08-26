package me.streamis.socket.io.server;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.socket.io.parser.IOParser;
import me.streamis.socket.io.parser.Packet;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SIOSocketImpl implements SIOSocket {
  private Namespace namespace;
  private String id;
  private Client client;
  private SIOServer server;
  private Handler<String> disconnectHandler;
  private Map<String, String> rooms;
  private Set<String> roomSet;
  //TODO
  private Map<Integer, Handler<Object>> acks;
  private Map<String, Handler<Object[]>> eventHandlers = new HashMap<>();
  private IOParser.Decoder decoder;
  private boolean connected;
  private boolean disconnected;

  private List<Handler<Throwable>> errorHandlers = new ArrayList<>();

  //

  private static final Logger LOGGER = LoggerFactory.getLogger(SIOSocketImpl.class);
  private static Set<String> blackEvents = new HashSet<String>() {{
    add("error");
    add("connect");
    add("disconnect");
    add("disconnecting");
    add("newListener");
    add("removeListener");
  }};

  public SIOSocketImpl(Vertx vertx, SIOServer sioServer, Namespace namespace, Client client) {
    this.client = client;
    this.server = sioServer;
    this.connected = true;
    this.namespace = namespace;
    this.id = namespace.getName().equals("/") ? client.id : namespace.getName() + "#" + client.id;
    this.rooms = new HashMap<>();
    this.roomSet = new HashSet<>();
    this.acks = new HashMap<>();
    this.decoder = new IOParser.Decoder();
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public Namespace namespace() {
    return namespace;
  }

  @Override
  public MultiMap query() {
    return this.client.query;
  }

  @Override
  public SIOSocket on(String event, Handler<Object[]> messageHandler) {
    this.eventHandlers.put(event, messageHandler);
    return this;
  }

  @Override
  public SIOSocket disconnect(boolean close) {
    if (!this.connected) return this;
    if (close) {
      this.client.disconnect();
    } else {
      this.packet(new Packet(Packet.PacketType.DISCONNECT));
      this.onClose("server namespace disconnect.");
    }
    return this;
  }

  @Override
  public SIOSocket onDisconnect(Handler<String> disconnectHandler) {
    this.disconnectHandler = disconnectHandler;
    return this;
  }

  @Override
  public SIOSocket to(String roomName) {
    if (!roomSet.contains(roomName)) roomSet.add(roomName);
    return this;
  }

  @Override
  public Emitter emit(String event, Object... args) {
    if (!blackEvents.contains(event) && eventHandlers.containsKey(event)) {
      //flat jsonArray data
      if (args.length == 1 && args[0] instanceof JsonArray) {
        JsonArray jsonArray = (JsonArray) args[0];
        eventHandlers.get(event).handle(jsonArray.getList().toArray());
      } else {
        eventHandlers.get(event).handle(args);
      }
      return this;
    }

    //TODO binary check
    JsonArray data = new JsonArray().add(event);
    for (Object arg : args) data.add(arg);
    Packet packet = new Packet(Packet.PacketType.EVENT, data);

    // access last argument to see if it's an ACK callback
    if (args[args.length - 1] instanceof Handler) {
      //TODO flag
      if (this.roomSet.size() > 0) {
        throw new SocketIOExecption("Callback are not supported when broadcasting.");
      }
      int ids = ((NamespaceImpl) this.namespace).ids;
      LOGGER.debug("emitting packet with ack id " + ids);
      this.acks.put(ids, (Handler<Object>) args[args.length - 1]);
      packet.setId(ids);
      ((NamespaceImpl) this.namespace).ids++;
    }

    if (roomSet.size() > 0) {
      //TODO broadcast
    } else {
      this.packet(packet);
    }
    return this;
  }

  /**
   * Writes a packet
   *
   * @param packet
   */
  @Override
  public void packet(Packet packet) {
    packet.setNamespace(this.namespace.getName());
    this.client.packet(packet);
  }

  @Override
  public SIOSocket onConnect() {
    ((NamespaceImpl) this.namespace).connected.put(id, this);
    this.join(null, id);
    //TODO fns
    if (this.namespace.getName().equals("/")) {

    } else {
      this.packet(new Packet(Packet.PacketType.CONNECT));
    }
    return this;
  }

  @Override
  public SIOSocket broadcast(String event, Object... args) {
    ((NamespaceImpl) this.namespace).connected.values().forEach(sioSocket -> {
      if (!sioSocket.id().equals(id)) {
        //TODO binary check
        JsonArray data = new JsonArray().add(event);
        for (Object arg : args) data.add(arg);
        Packet packet = new Packet(Packet.PacketType.EVENT, data);
        sioSocket.packet(packet);
      }
    });
    return this;
  }

  void onPacket(Packet packet) {
    switch (packet.getType()) {
      case EVENT:
      case BINARY_EVENT:
        this.onEvent(packet);
        break;
      case ACK:
      case BINARY_ACK:
        this.onAck(packet);
        break;
      case DISCONNECT:
        if (LOGGER.isDebugEnabled()) LOGGER.debug("got disconnect packet");
        this.onClose("client namespace disconnect");
        break;
      case ERROR:
        this.onError(new SocketIOExecption(packet.getData().toString()));
        break;
    }
  }

  private void onEvent(Packet packet) {
    JsonArray args = packet.getData() != null ? (JsonArray) packet.getData() : new JsonArray();
    if (packet.getId() >= 0) {
      if (LOGGER.isDebugEnabled()) LOGGER.debug("attaching ack callback to event");
      args.add(this.ack(packet.getId()));
    }
    this.dispatch(args);
  }

  void onClose(String reason) {
    if (!this.connected) return;
    if (LOGGER.isDebugEnabled()) LOGGER.debug("closing socket - reason " + reason);
    this.leaveAll();
    this.namespace.remove(this);
    this.client.remove(this);
    this.connected = false;
    this.disconnected = true;
    ((NamespaceImpl) this.namespace).connected.remove(this.id);
    if (disconnectHandler != null) {
      this.disconnectHandler.handle(reason);
    }
  }

  private void onAck(Packet packet) {
    Handler<Object> h = this.acks.get(packet.getId());
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("calling ack %s with %s.", packet.getId(), packet.getData()));
    }
    h.handle(packet.getData());
    this.acks.remove(packet.getId());
  }

  void onError(Throwable throwable) {
    if (errorHandlers.size() > 0) {
      errorHandlers.forEach(h -> h.handle(throwable));
    }
  }

  /**
   * Produces an ack callback to emit with an event.
   */
  private Handler<Object> ack(int packetId) {
    AtomicBoolean sent = new AtomicBoolean(false);
    return arg -> {
      if (sent.get()) return;
      if (LOGGER.isDebugEnabled()) LOGGER.debug("sending ack " + arg);
      Packet packet = new Packet(Packet.PacketType.ACK);
      packet.setId(packetId);
      //TODO binary check
      this.packet(packet);
      sent.set(true);
    };
  }

  private void dispatch(JsonArray args) {
    //TODO middleware and run on context
    String eventName = (String) args.remove(0);
    emit(eventName, args);
  }

  void errorPacket(String err) {
    this.packet(new Packet(Packet.PacketType.ERROR, err));
  }


  SIOSocket join(Consumer<Void> consumer, String... rooms) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("joining room " + rooms);
    }
    Set<String> roomSet = Arrays.stream(rooms).filter(room -> !this.rooms.containsKey(room)).collect(Collectors.toSet());
    if (roomSet.size() > 0 && consumer != null) {
      consumer.accept(null);
      return this;
    }
    //TODO adapter addAll, we should using HazelCast or redis?
    roomSet.forEach(room -> this.rooms.put(room, room));
    if (consumer != null) consumer.accept(null);
    return this;
  }

  SIOSocket leave(String room, Consumer<Void> consumer) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("leave room " + room);
    }
    //TODO adapter del
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("left room " + rooms);
    }
    this.rooms.remove(room);
    if (consumer != null) consumer.accept(null);
    return this;
  }

  SIOSocket leaveAll() {
    //TODO adapter
    this.rooms.clear();
    return this;
  }


}
































