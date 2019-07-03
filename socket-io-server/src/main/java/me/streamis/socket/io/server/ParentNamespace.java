package me.streamis.socket.io.server;

import io.vertx.core.Vertx;

import java.util.HashSet;
import java.util.Set;

public class ParentNamespace extends NamespaceImpl {

  private Vertx vertx;
  private long count = 0;
  //TODO multi instance
  private Set<Namespace> children = new HashSet<>();

  public ParentNamespace(Vertx vertx, SIOServer server) {
    super(vertx, server);
    this.vertx = vertx;
    this.name = "/_" + (count++);
  }

  @Override
  public Emitter emit(String event, Object... args) {
    children.forEach(namespace -> {
      ((NamespaceImpl)namespace).rooms = this.rooms;
      namespace.emit(event, args);
    });
    return this;
  }

  public Namespace createChild(String name) {
    NamespaceImpl namespace = new NamespaceImpl(vertx, server, name);
    namespace.onConnect(this.connectHandler);
    this.children.add(namespace);
    this.server.namespaces.put(name, namespace);
    return namespace;
  }
}
