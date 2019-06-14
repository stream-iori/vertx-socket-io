package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import me.streamis.engine.io.parser.Packet;
import me.streamis.engine.io.server.transport.PollingXHRTransport;

import java.util.Arrays;
import java.util.List;

import static me.streamis.engine.io.server.EIOTransport.Type.*;

/**
 * Created by stream.
 */
public interface EIOTransport {
  enum Type {
    POLLING, WEBSOCKET
  }

  static boolean isHandlesUpgrades(String transport) {
    Type type = Type.valueOf(transport.toUpperCase());
    switch (type) {
      case WEBSOCKET:
        return true;
      case POLLING:
      default:
        return false;
    }
  }

  void addPacketHandler(Handler<Packet> packetHandler);

  void addDrainHandler(Handler<Void> drainHandler);

  void addCloseHandler(Handler<Void> closeHandler);

  void addErrorHandler(Handler<Throwable> errorHandler);

  boolean writable();

  boolean supportsFraming();

  boolean isSupportsBinary();

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
