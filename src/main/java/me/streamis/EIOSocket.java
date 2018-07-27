package me.streamis;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import me.streamis.parser.Packet;

/**
 * Created by stream.
 */
public interface EIOSocket {

  String getId();

  void setNewTransport(EIOTransport transport);

  EIOTransport getTransport();

  void send(Buffer buffer, Handler<Packet> handler);

  default void send(Buffer buffer) {
    send(buffer, null);
  }

  void maybeUpgrade(EIOTransport newTransport);

  void close(Throwable throwable);

  /**
   * Closes the socket and underlying transport.
   */
  void close(boolean discard);

  EIOSocket errorHandler(Handler<Throwable> handler);

  EIOSocket closeHandler(Handler<Throwable> handler);

  boolean isUpgrading();

  boolean isUpgraded();

  void destroy(String message);
}
