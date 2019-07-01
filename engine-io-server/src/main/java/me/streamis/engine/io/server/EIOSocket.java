package me.streamis.engine.io.server;


import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.Shareable;
import me.streamis.engine.io.parser.Packet;

import java.util.List;

/**
 * Created by stream.
 */
public interface EIOSocket extends Shareable {

  /**
   * get current state
   */
  State getState();

  /**
   * Fired when the client is disconnected.
   */
  EIOSocket closeHandler(Handler<String> handler);

  /**
   * Fired when the client get a error;
   */
  EIOSocket errorHandler(Handler<Throwable> handler);

  /**
   * Fired when the client sends a message.
   */
  EIOSocket messageHandler(Handler<Object> handler);

  /**
   * Called when the write buffer is being flushed.
   */
  EIOSocket flushHandler(Handler<List<Packet>> handler);

  /**
   * Called when the write buffer is drained
   */
  EIOSocket drainHandler(Handler<Void> handler);

  /**
   * Called when a socket received a packet (message, ping)
   */
  EIOSocket packetHandler(Handler<Packet> handler);

  /**
   * Called before a socket sends a packet (message, pong)
   */
  EIOSocket packetCreateHandler(Handler<Packet> handler);

  /**
   * send a message in String format.
   */
  EIOSocket send(String data);

  /**
   * send a message in Buffer format.
   */
  EIOSocket send(Buffer data);

  /**
   * Disconnects the client
   */
  void close(boolean discard);

  /**
   * get socket id;
   */
  String getId();

  /**
   * get current active transport
   */
  EIOTransport getTransport();

}
