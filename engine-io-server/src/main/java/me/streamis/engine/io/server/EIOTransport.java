package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import me.streamis.engine.io.parser.Packet;

import java.util.Arrays;
import java.util.List;

/**
 * Created by stream.
 */
public interface EIOTransport {
  enum Type {
    POLLING, WEBSOCKET
  }

  void addPacketHandler(Handler<Packet> packetHandler);

  void addDrainHandler(Handler<Void> drainHandler);

  void addCloseHandler(Handler<Void> closeHandler);

  void addErrorHandler(Handler<Throwable> errorHandler);

  void appendHeader(String key, String value);

  boolean writable();

  boolean supportsFraming();

  boolean isSupportsBinary();

  boolean isHandlesUpgrades();

  void setSupportsBinary(boolean support);

  void onRequest(HttpServerRequest request);

  void setSid(String sid);

  String getSid();

  void discard();

  String name();

  void close(Handler<Void> callback);

  default void send(Packet... packets) {
    send(Arrays.asList(packets));
  }

  void send(List<Packet> packets);
}
