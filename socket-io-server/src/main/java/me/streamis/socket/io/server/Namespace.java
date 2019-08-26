package me.streamis.socket.io.server;

import io.vertx.core.Handler;

public interface Namespace extends Emitter {

  String getName();

  Namespace to(String name);

  default Namespace in(String name) {
    return to(name);
  }

  /**
   * send a `message` event to all clients.
   *
   * @param message
   * @return
   */
  default Emitter send(Object message) {
    return this.emit("message", message);
  }

  default Emitter write(Object message) {
    return send(message);
  }

  Namespace onConnect(Handler<SIOSocket> connectHandler);

  void remove(SIOSocket sioSocket);



}
