package me.streamis;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import me.streamis.parser.Packet;

import java.util.Arrays;
import java.util.List;

/**
 * Created by stream.
 */
public interface EIOTransport {

  enum Type {
    POLLING("polling"), WEBSOCKET("websocket");

    private final String name;

    Type(final String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  Type type();

  State state();

  boolean isWritable();

  void discard();

  boolean isDiscard();

  default void send(Packet... packets) {
    send(Arrays.asList(packets));
  }

  void send(List<Packet> packets);

  void close(Handler<Void> handler);

  void handleRequest(HttpServerRequest request);

  void errorHandler(Handler<Throwable> errorHandler);

  void closeHandler(Handler<Void> closeHandler);

  void packetHandler(Handler<Packet> packetHandler);

  void drainHandler(Handler<Void> drainHandler);

  void setEIOSocket(EIOSocket eioSocket);

}
