package me.streamis.socket.io.server;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.engine.io.server.*;
import me.streamis.socket.io.parser.IOParser;
import me.streamis.socket.io.parser.Packet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SIOServer {
  private Vertx vertx;
  private HttpServer httpServer;
  private String origins;
  EIOServer eioServer;
  Map<String, NamespaceImpl> namespaces = new ConcurrentHashMap<>();
  Map<String, ParentNamespace> parentNamespaces = new ConcurrentHashMap<>();

  private Handler<SIOSocket> connectionHandler;
  private IOParser.Encoder encoder = new IOParser.Encoder();

  private static final Logger LOGGER = LoggerFactory.getLogger(SIOSocket.class);


  public SIOServer(Vertx vertx, HttpServer httpServer) {
    this.vertx = vertx;
    this.httpServer = httpServer;
  }

  //TODO middleware
  void checkNamespace(String name, MultiMap query, Consumer<Namespace> consumer) {
//    if (this.parentNamespaces.size() == 0) consumer.accept(null);
//    ParentNamespace namespace = this.parentNamespaces.get(name);
//    if (namespace != null) consumer.accept(namespace);
//    else {
//      namespace = new ParentNamespace(vertx, this);
//      consumer.accept(namespace.createChild(name));
//    }

    NamespaceImpl namespace = namespaces.get(name);
    if (namespace != null) {
      consumer.accept(namespace);
    } else {
      namespace = new NamespaceImpl(vertx, this, name);
      namespaces.put(name, namespace);
    }

  }

  public SIOServer connectionHandler(Handler<SIOSocket> handler) {
    this.connectionHandler = handler;
    return this;
  }


  //TODO engineOptions
  public SIOServer attach(Handler<HttpServerRequest> handler, SocketIOOptions socketIOOptions) {
    EngineOptions engineOptions = new EngineOptions();
    engineOptions.setPath(socketIOOptions.getPath());
    //TODO allow request
    Packet connectPacket = new Packet(Packet.PacketType.CONNECT);
    connectPacket.setNamespace("/");
    this.encoder.encode(connectPacket, encodedPacket -> {
      engineOptions.setInitialPacket(((String[]) encodedPacket)[0]);
      this.initEngine(handler, engineOptions);
    });
    return this;
  }

  private void initEngine(Handler<HttpServerRequest> httpServerRequestHandler, EngineOptions engineOptions) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("creating engine.io instance with opts " + engineOptions);
    }
    this.eioServer = new EIOServerImpl(vertx, engineOptions).attach(httpServer, httpServerRequestHandler);
    this.bind(eioServer);
  }

  public SIOServer setOrigin(String origin) {
    this.origins = origin;
    return this;
  }

  public Namespace of(String name) {
    //TODO parentNamespace

    if (!name.startsWith("/")) name = "/" + name;
    NamespaceImpl namespace = this.namespaces.get(name);
    if (namespace == null) {
      if (LOGGER.isDebugEnabled()) LOGGER.debug("initializing namespace " + name);
      namespace = new NamespaceImpl(vertx, this);
      namespace.name = name;
      this.namespaces.put(name, namespace);
    }
    return namespace;
  }

  public void close() {
    this.namespaces.get("/").sockets.values().forEach(sioSocket -> {
      ((SIOSocketImpl) sioSocket).onClose("normal close.");
    });
    this.eioServer.close();
  }

  public SIOServer bind(EIOServer eioServer) {
    this.eioServer = eioServer;
    this.eioServer.connectionHandler(eioSocket -> {
      if (LOGGER.isDebugEnabled()) LOGGER.debug("incoming connection with id " + eioSocket.getId());
      Client client = new Client(vertx, this, eioSocket);
      client.connect("/", null);
    });
    return this;
  }


}
