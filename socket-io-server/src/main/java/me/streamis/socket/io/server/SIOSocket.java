package me.streamis.socket.io.server;

import io.vertx.core.Handler;

public interface SIOSocket extends Emitter {

  String id();

  Namespace namespace();

  SIOSocket on(String event, Handler<Object[]> messageHandler);

  /**
   * Disconnects this client.
   *
   * @param close if true, closes the underlying connection
   * @return self
   */
  SIOSocket disconnect(boolean close);

  SIOSocket onConnect();

  default Emitter send(Object... args) {
    return this.emit("message", args);
  }

  default Emitter write(Object... args) {
    return send(args);
  }

  SIOSocket to(String roomName);

  default SIOSocket in(String roomName) {
    return to(roomName);
  }

}
