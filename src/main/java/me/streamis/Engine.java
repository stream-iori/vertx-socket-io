package me.streamis;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

/**
 * Created by stream.
 */
public interface Engine {

  default void close() {
    close(null);
  }

  void close(Handler<AsyncResult<Void>> handler);

  Handler<Void> attach(HttpServerRequest request);

  Engine onConnect(Handler<EIOSocket> handler);

  String[] upgrades(String transportName);

}
