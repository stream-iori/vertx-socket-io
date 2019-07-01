package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.NetSocket;

import java.util.List;
import java.util.Map;

/**
 * Created by stream.
 */
public interface EIOServer {

  //Events
  EIOServer connectionHandler(Handler<EIOSocket> handler);

  //properties
  Map<String, EIOSocket> clients();

  List<String> upgrades(String transport);

  EIOSocket getClientById(String id);

  int clientsCount();

  //

  /**
   * close engine
   */
  EIOServer close();

  EIOServer attach(HttpServer httpServer, Handler<HttpServerRequest> httpServerRequestHandler, EngineOptions options);

  default EIOServer attach(HttpServer httpServer, Handler<HttpServerRequest> httpServerRequestHandler) {
    return attach(httpServer, httpServerRequestHandler, new EngineOptions().toBuild());
  }

  String generateId();
}
