package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;

/**
 * Created by stream.
 */
interface EIOServer {

  EIOServer connectionHandler(Handler<EIOSocket> handler);

  default EIOServer attach(HttpServer httpServer) {
    return attach(httpServer, null);
  }

  EIOServer attach(HttpServer httpServer, Handler<HttpServerRequest> requestHandler);

  void close();

}
