package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import me.streamis.engine.io.server.transport.PollingXHRTransport;
import me.streamis.engine.io.server.transport.WebSocketEIOTransport;

/**
 * Created by stream.
 */
public class EIOServerImpl implements EIOServer {
  private Vertx vertx;
  private EngineOptions options;
  private LocalMap<String, EIOSocket> clients;
  private Handler<EIOSocket> connectionHandler;
  private Handler<Void> closeHandler;
  private final static Logger LOGGER = LoggerFactory.getLogger(EIOServer.class);

  public EIOServerImpl(Vertx vertx, EngineOptions options) {
    this.vertx = vertx;
    this.options = options;
    this.clients = vertx.sharedData().getLocalMap("eioClients");
  }

  @Override
  public EIOServer connectionHandler(Handler<EIOSocket> handler) {
    this.connectionHandler = handler;
    return this;
  }

  @Override
  public EIOServer attach(HttpServer httpServer, Handler<HttpServerRequest> requestHandler) {
    //TODO format path
    String path = options.getPath();
    httpServer.requestHandler(request -> {

      if (requestHandler != null) requestHandler.handle(request);

      if (request.path().equals(path) && request.method() != HttpMethod.OPTIONS) {
        ServerErrors serverError = verify(request, false);
        if (serverError != null) {
          sendError(request, serverError.name(), serverError.ordinal());
          return;
        }
        if (request.getParam("sid") != null) {
          LOGGER.debug("setting new request for existing client");
          clients.get(request.getParam("sid")).getTransport().onRequest(request);
        } else {
          LOGGER.debug("handshake");
          handshake(request.getParam("transport"), request);
        }

        if (options.getTransports().containsKey("websocket")) {
          //upgrade to webSocket from Polling.
//          httpServer.websocketHandler(webSocket -> {
//            String id = request.getParam("sid");
//            if (id != null) {
//              EIOSocket eioSocket = clients.get(id);
//              if (eioSocket == null) {
//                LOGGER.debug("upgrade attempt for closed client");
//                webSocket.close();
//              } else if (((EIOUpgradeSocket) eioSocket).isUpgrading()) {
//                LOGGER.debug("transport has already been trying to upgrade");
//                webSocket.close();
//              } else if (((EIOUpgradeSocket) eioSocket).isUpgraded()) {
//                LOGGER.debug("transport had already been upgraded");
//                webSocket.close();
//              } else {
//                LOGGER.debug("upgrading existing transport");
//                EIOTransport transport = new WebSocketEIOTransport(webSocket, isSupportsBinary(request));
//                ((EIOUpgradeSocket) eioSocket).maybeUpgrade(transport);
//              }
//            } else {
//              handshake(request.getParam("transport"), request);
//            }
//          });
        }
      }
    });
    return this;
  }

  @Override
  public void close() {
    LOGGER.debug("closing all open client.");
    for (EIOSocket socket : clients.values()) socket.close(true);
  }

  private ServerErrors verify(HttpServerRequest request, boolean isUpgrade) {
    String transportName = request.getParam("transport");
    if (transportName != null && options.getTransports().get(transportName) == null) {
      LOGGER.error("unKnow transport " + transportName);
      return ServerErrors.UNKNOWN_TRANSPORT;
    }

    if (request.headers().get("Origin") == null) {
      return ServerErrors.BAD_REQUEST;
    }

    String sid = request.getParam("sid");
    if (sid != null) {
      LOGGER.debug("session id is " + sid);
      EIOSocket socket = clients.get(sid);
      if (socket == null) {
        LOGGER.error("unKnow session id");
        return ServerErrors.UNKNOWN_SID;
      }
      if (!isUpgrade && !socket.getTransport().name().equals(transportName)) {
        LOGGER.error("bad request: unexpected transport without upgrade.");
        return ServerErrors.BAD_REQUEST;
      }
    } else {
      //handshake is GET only
      if (request.method() != HttpMethod.GET) {
        LOGGER.error("bad handshake method.");
        return ServerErrors.BAD_HANDSHAKE_METHOD;
      }
    }
    return null;
  }

  private boolean isSupportsBinary(HttpServerRequest request) {
    return request.getParam("b64") == null;
  }

  private void handshake(String transportName, HttpServerRequest request) {
    String sid = Helper.randomSessionID();
    LOGGER.debug("handshaking client" + sid);
    EIOTransport transport;
    switch (transportName) {
      case "websocket":
        transport = new WebSocketEIOTransport(request, isSupportsBinary(request));
        break;
      case "polling":
        transport = new PollingXHRTransport(vertx, false);
        break;
      default:
        sendError(request, ServerErrors.BAD_REQUEST.name(), ServerErrors.BAD_REQUEST.ordinal());
        return;
    }
    EIOSocket eioSocket = new EIOSocketImpl(vertx, sid, options, transport);
    if (!options.isCookie()) {
      String cookie;
      if (options.isCookieHttpOnly()) {
        cookie = String.format("io=%s; Path=%s; HttpOnly", eioSocket.getId(), options.getCookiePath());
      } else {
        cookie = String.format("io=%s; Path=%s;", eioSocket.getId(), options.getCookiePath());
      }
      request.response().putHeader("Set-Cookie", cookie);
    }
    transport.onRequest(request);
    clients.put(sid, eioSocket);
    eioSocket.closeHandler(ex -> clients.remove(sid));
    if (connectionHandler != null) connectionHandler.handle(eioSocket);
  }

  private void sendError(HttpServerRequest request, String errorMessage, int code) {
    request.response().putHeader("Content-Type", "application/json; charset=UTF8");
    if (request.headers().get("Origin") != null) {
      request.response().putHeader("Access-Control-Allow-Credentials", "true");
      request.response().putHeader("Access-Control-Allow-Origin", request.headers().get("origin"));
    } else {
      request.response().putHeader("Access-Control-Allow-Origin", "*");
    }
    for (ServerErrors e : ServerErrors.values()) {
      if (e.ordinal() == code) {
        request.response()
          .setStatusCode(403)
          .end(new JsonObject()
            .put("code", ServerErrors.FORBIDDEN.ordinal())
            .put("message", ServerErrors.FORBIDDEN.name()).toString());
        return;
      }
    }
    request.response().setStatusCode(400).end(
      new JsonObject()
        .put("code", code)
        .put("message", errorMessage)
        .toString());
  }
}
