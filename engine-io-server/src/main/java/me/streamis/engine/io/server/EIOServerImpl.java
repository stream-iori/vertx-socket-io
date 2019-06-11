package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetSocket;
import me.streamis.engine.io.server.transport.AbsEIOTransport;
import me.streamis.engine.io.server.transport.PollingXHRTransport;
import me.streamis.engine.io.server.transport.WebSocketEIOTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static me.streamis.engine.io.server.ServerErrors.*;

/**
 * Created by stream.
 */
public class EIOServerImpl implements EIOServer {
  private Vertx vertx;
  private EngineOptions options;
  private Map<String, EIOSocket> clients;
  private Handler<EIOSocket> connectionHandler;

  private final static Logger LOGGER = LoggerFactory.getLogger(EIOServer.class);

  public EIOServerImpl(Vertx vertx, EngineOptions options) {
    this.vertx = vertx;
    this.options = options;
    this.clients = vertx.sharedData().getLocalMap("__eio__Clients");
  }

  @Override
  public EIOServer connectionHandler(Handler<EIOSocket> handler) {
    this.connectionHandler = handler;
    return this;
  }

  @Override
  public Map<String, EIOSocket> clients() {
    return clients;
  }

  @Override
  public List<String> upgrades(String transport) {
    if (!this.options.isAllowUpgrades()) return new ArrayList<>(0);
    else return options.getTransports().get(transport);
  }

  @Override
  public EIOSocket getClientById(String id) {
    return clients.get(id);
  }

  @Override
  public int clientsCount() {
    return clients.size();
  }


  @Override
  public EIOServer attach(HttpServer httpServer, Handler<HttpServerRequest> httpServerRequestHandler, EngineOptions options) {
    //TODO format path
    String path = options.getPath();

    Function<HttpServerRequest, Boolean> check = request -> {
      if (request.method() == HttpMethod.OPTIONS && !options.isHandlePreflightRequest()) return false;
      return request.path().equals(path);
    };

    httpServer.requestHandler(httpServerRequest -> {
      if (check.apply(httpServerRequest)) {
        this.handleRequest(httpServerRequest);
      } else {
        httpServerRequestHandler.handle(httpServerRequest);
      }

      //handle webSocket
      String transport = httpServerRequest.getParam("transport");
      if (options.getTransports().containsKey("websocket") && transport != null && !transport.equals("websocket")) {
        httpServer.websocketHandler(webSocket -> {
          if (check.apply(httpServerRequest)) {
            //TODO buffer
            this.handleUpgrade(httpServerRequest, webSocket, null);
          }
        });
      }
    });
    return this;
  }

  @Override
  public EIOServer close() {
    LOGGER.debug("closing all open client.");
    for (EIOSocket socket : clients.values()) socket.close(true);
    return this;
  }

  private void handleRequest(HttpServerRequest request) {
    this.verify(request, false, (serverErrors, isSuccess) -> {
      if (!isSuccess) {
        sendError(request, serverErrors.getMessage(), serverErrors.ordinal());
      }
      String sid = request.getParam("sid");
      if (sid != null) {
        LOGGER.debug("setting new request for existing client");
        this.clients.get(sid).getTransport().onRequest(request);
      } else {
        this.handshake(request.getParam("transport"), request);
      }
    });
  }

  private void handleUpgrade(HttpServerRequest request, ServerWebSocket webSocket, List<Buffer> tailBuffer) {
    this.verify(request, true, (serverErrors, isSuccess) -> {
      if (!isSuccess) {
        abortConnection(request.netSocket(), serverErrors);
        return;
      }

      //TODO tailBuffer
      //TODO
      if (options.getTransports().get(request.getParam("transport")) != null) {
        LOGGER.debug("transport doesn't handle upgraded requests");
        webSocket.close();
        return;
      }

      String id = request.getParam("sid");
      //TODO close webSocket ,request also be closed?
      if (id != null) {
        EIOSocketImpl eioSocket = (EIOSocketImpl) clients.get(id);
        if (eioSocket == null) {
          LOGGER.debug("upgrade attempt for closed client");
          webSocket.close();
        } else if (((EIOUpgradeSocket) eioSocket).isUpgrading()) {
          LOGGER.debug("transport has already been trying to upgrade");
          webSocket.close();
        } else if (((EIOUpgradeSocket) eioSocket).isUpgraded()) {
          LOGGER.debug("transport had already been upgraded");
          webSocket.close();
        } else {
          LOGGER.debug("upgrading existing transport");
          EIOTransport transport = new WebSocketEIOTransport(webSocket, isSupportsBinary(request));
          ((EIOUpgradeSocket) eioSocket).maybeUpgrade(transport);
        }
      } else {
        handshake(request.getParam("transport"), request);
      }
    });
  }

  @Override
  public String generateId() {
    return Helper.randomSessionID();
  }

  private void verify(HttpServerRequest request, boolean isUpgrade, BiConsumer<ServerErrors, Boolean> consumer) {
    String transportName = request.getParam("transport");
    if (transportName != null && options.getTransports().get(transportName) == null) {
      LOGGER.error("unKnow transport " + transportName);
      consumer.accept(UNKNOWN_TRANSPORT, false);
    }

    if (request.headers().get("Origin") == null) {
      consumer.accept(BAD_REQUEST, false);
    }

    String sid = request.getParam("sid");
    if (sid != null) {
      LOGGER.debug("session id is " + sid);
      EIOSocket socket = clients.get(sid);
      if (socket == null) {
        LOGGER.error("unKnow session id");
        consumer.accept(UNKNOWN_SID, false);
      }
      if (!isUpgrade && !socket.getTransport().name().equals(transportName)) {
        LOGGER.error("bad request: unexpected transport without upgrade.");
        consumer.accept(BAD_REQUEST, false);
      }
    } else {
      //handshake is GET only
      if (request.method() != HttpMethod.GET) {
        LOGGER.error("bad handshake method.");
        consumer.accept(BAD_HANDSHAKE_METHOD, false);
      }
    }
    consumer.accept(null, true);
  }

  private boolean isSupportsBinary(HttpServerRequest request) {
    return request.getParam("b64") == null;
  }

  private void handshake(String transportName, HttpServerRequest request) {
    String sid = this.generateId();
    LOGGER.debug("handshaking client" + sid);
    AbsEIOTransport transport;
    switch (transportName) {
      case "websocket":
        transport = new WebSocketEIOTransport(request.upgrade(), isSupportsBinary(request));
        break;
      case "polling":
        transport = new PollingXHRTransport(vertx, false);
        break;
      default:
        sendError(request, ServerErrors.BAD_REQUEST.getMessage(), ServerErrors.BAD_REQUEST.ordinal());
        return;
    }
    if (request.getParam("b64") != null) {
      transport.setSupportsBinary(false);
    } else {
      transport.setSupportsBinary(true);
    }
    //we have to invoke eventEmitter
    EIOSocketImpl eioSocket = new EIOSocketImpl(vertx, sid, options, transport);
    if (options.getCookie() != null) {
      String cookie = String.format("%s=%s; Path=%s; " + (options.isCookieHttpOnly() ? "httpOnly" : ""),
        options.getCookie(), eioSocket.getId(), options.getCookiePath());
      //TODO listen headers event from transport
      transport.appendHeader("Set-Cookie", cookie);
    }
    transport.onRequest(request);
    clients.put(sid, eioSocket);
    eioSocket.closeHandler(h -> clients.remove(sid));
    //API
    if (connectionHandler != null) connectionHandler.handle(eioSocket);
  }


  private void abortConnection(NetSocket netSocket, ServerErrors errors) {
    if (netSocket != null) {
      String message = errors.getMessage();
      String responseMessage = "HTTP/1.1 400 Bad Request\r\n" +
        "Connection: close\r\n" +
        "Content-type: text/html\r\n" +
        "Content-Length: " + message.length() + "\r\n" +
        "\r\n" +
        message;
      netSocket.write(responseMessage);
      netSocket.close();
    }
  }

  private void sendError(HttpServerRequest request, String errorMessage, int code) {
    request.response().putHeader("Content-Type", "application/json; charset=UTF8");
    if (!ServerErrors.isInCodeRange(code)) {
      request.response().setStatusCode(403)
        .end(new JsonObject()
          .put("code", FORBIDDEN.ordinal())
          .put("message", "" + code).encode()
        );
      return;
    }
    String origin = request.headers().get("Origin");
    if (origin != null) {
      request.response().putHeader("Access-Control-Allow-Credentials", "true");
      request.response().putHeader("Access-Control-Allow-Origin", origin);
    } else {
      request.response().putHeader("Access-Control-Allow-Origin", "*");
    }

    request.response().setStatusCode(400).end(
      new JsonObject()
        .put("code", code)
        .put("message", errorMessage)
        .toString());
  }
}
