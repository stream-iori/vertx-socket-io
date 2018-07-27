package me.streamis;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.transport.PollingXHR;
import me.streamis.transport.WebSocketTransport;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by stream.
 */
public class EngineImpl implements Engine {

  private EngineOptions engineOptions;
  private Map<String, EIOSocket> clients = new HashMap<>();
  private Vertx vertx;
  private Handler<EIOSocket> handler;
  private static final Logger logger = LoggerFactory.getLogger(EngineImpl.class);


  private enum Error {
    UNKNOW_TRANSPORT, UNKNOW_SID, BAD_HANDSHAKE_METHOD, BAD_REQUEST, FORBIDDEN
  }


  public EngineImpl(EngineOptions engineOptions) {
    this.engineOptions = engineOptions;
    logger.info("haha.");
  }

  private void initWebSocketServer() {
    for (String transportName : engineOptions.getTransports().keySet()) {
      if (transportName.equals("websocket")) {
        //TODO: init webSocket
        break;
      }
    }
  }

  private void verify(HttpServerRequest request, boolean upgrade, Consumer<Error> consumer) {
    //Transport check
    String transportName = request.getParam("transport");
    if (engineOptions.getTransports().get(transportName) == null) {
      logger.error("unKnow transport " + transportName);
      consumer.accept(Error.UNKNOW_TRANSPORT);
      return;
    }
    String sid = request.getParam("sid");
    if (sid != null) {
      EIOSocket socket = clients.get(sid);
      if (socket == null) {
        logger.error("unKnow session id");
        consumer.accept(Error.UNKNOW_SID);
        return;
      }
      if (!upgrade && !socket.getTransport().type().name().equals(transportName)) {
        logger.error("bad request: unexpected transport without upgrade.");
        consumer.accept(Error.BAD_REQUEST);
      }
    } else {
      //handshake is GET only
      if (request.method() != HttpMethod.GET) {
        logger.error("bad handshake method.");
        consumer.accept(Error.BAD_HANDSHAKE_METHOD);
        return;
      }
      if (!engineOptions.isAllowUpgrades()) consumer.accept(null);
      else {
        //TODO allowRequest(req)
      }
    }
  }

  @Override
  public void close(Handler<AsyncResult<Void>> handler) {
    if (logger.isDebugEnabled()) {
      logger.debug("closing all open client.");
    }
    for (EIOSocket socket : clients.values()) socket.close(true);
    //TODO close WS
  }


  public void handleRequest(HttpServerRequest request) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("handling %s http request %s", request.method(), request.uri()));
    }
    verify(request, false, error -> {
      if (error != null) {
        sendError(request, error.name(), error.ordinal());
        return;
      }
      if (request.getParam("sid") != null) {
        clients.get(request.getParam("sid")).getTransport().handleRequest(request);
      } else {
        handshake(request.getParam("transport"), request);
      }
    });
  }

  public void handleUpgrade(HttpServerRequest request, EIOSocket socket) {
    verify(request, true, error -> {
      if (error != null) {
        abortConnection(socket, error.toString());
      } else {
        //upgrade to ws
        String transportName = request.getParam("transport");
        String sid = request.getParam("sid");
        if (sid != null) {
          if (this.clients.get(sid) == null) {
            logger.error("upgrade attempt for closed client");
            socket.close(true);
          } else if (socket.isUpgrading()) {
            logger.error("transport has already been trying to upgrade");
            socket.close(true);
          } else if (socket.isUpgraded()) {
            logger.error("transport had already been upgraded");
            socket.close(true);
          } else {
            logger.info("upgrading existing transport");
            if (transportName.equals("websocket")) {
              EIOTransport transport = new WebSocketTransport(request);
              //TODO binary support
              socket.maybeUpgrade(transport);
            }
          }
        } else {
          handshake(transportName, request);
        }
      }
    });
  }

  @Override
  public Handler<Void> attach(HttpServerRequest request) {
    return Avoid -> {
      if (request.method() == HttpMethod.OPTIONS) {
        sendError(request, Error.BAD_REQUEST.toString(), Error.BAD_REQUEST.ordinal());
      } else if (request.path().equals("upgrade")) {
        handleUpgrade(request, this.clients.get(request.getParam("sid")));
      } else {
        handleRequest(request);
      }
    };
  }

  @Override
  public Engine onConnect(Handler<EIOSocket> handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public String[] upgrades(String transportName) {
    if (engineOptions.isAllowUpgrades()) {
      return engineOptions.getTransports().get(transportName);
    } else {
      return new String[0];
    }
  }

  private void handshake(String transportName, HttpServerRequest request) {
    String sid = Helper.randomSessionID();
    if (logger.isDebugEnabled()) {
      logger.debug("handshaking client" + sid);
    }

    EIOTransport transport;
    if (transportName.equals("websocket")) {
      transport = new WebSocketTransport(request);
    } else if (transportName.equals("polling")) {
      transport = new PollingXHR(request);
    } else {
      sendError(request, Error.BAD_REQUEST.name(), Error.BAD_REQUEST.ordinal());
      return;
    }
    EIOSocket socket = new EIOSocketImpl(sid, vertx, request, transport, engineOptions);

    if (!engineOptions.isCookie()) {
      String cookie;
      if (engineOptions.isCookieHttpOnly()) {
        cookie = String.format("io=%s; Path=%s; HttpOnly", socket.getId(), engineOptions.getCookiePath());
      } else {
        cookie = String.format("io=%s; Path=%s;", socket.getId(), engineOptions.getCookiePath());
      }
      request.response().putHeader("Set-Cookie", cookie);
    }
    transport.handleRequest(request);
    clients.put(sid, socket);
    socket.closeHandler(aVoid -> clients.remove(sid));
    this.handler.handle(socket);
  }

  private void abortConnection(EIOSocket socket, String message) {
    socket.destroy(message);
    clients.remove(socket.getId());
  }

  private void sendError(HttpServerRequest request, String errorMessage, int code) {
    request.response().putHeader("Content-Type", "application/json; charset=UTF8");
    if (request.headers().get("Origin") != null) {
      request.response().putHeader("Access-Control-Allow-Credentials", "true");
      request.response().putHeader("Access-Control-Allow-Origin", request.headers().get("origin"));
    } else {
      request.response().putHeader("Access-Control-Allow-Origin", "*");
    }
    for (Error e : Error.values()) {
      if (e.ordinal() == code) {
        request.response()
          .setStatusCode(403)
          .end(new JsonObject().put("code", Error.FORBIDDEN.ordinal()).put("message", Error.FORBIDDEN.name()).toString());
        return;
      }
    }
    request.response().setStatusCode(400).end(new JsonObject().put("code", code).put("message", errorMessage).toString());
  }

}
