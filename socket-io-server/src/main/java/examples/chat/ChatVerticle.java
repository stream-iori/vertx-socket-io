package examples.chat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import me.streamis.socket.io.server.SIOServer;
import me.streamis.socket.io.server.SocketIOOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatVerticle extends AbstractVerticle {
  private SIOServer sioServer;
  private Map<String, String> names = new HashMap<>();
  private int userCount = 0;

  @Override
  public void start() {
    HttpServer httpServer = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.get("/").handler(ctx -> ctx.response().end("this is socket io server powered by vert.x"));
    router.route("/chat/public/*").handler(StaticHandler.create());
    httpServer.requestHandler(router);

    sioServer = new SIOServer(vertx, httpServer);
    sioServer.attach(httpServer, router, new SocketIOOptions());

    httpServer.listen(3000, event -> {
      if (event.failed()) event.cause().printStackTrace();
      else {
        System.out.println("3000 up");
        initSIOServer();
      }
    });

  }

  private void initSIOServer() {
    sioServer.of("/").onConnect(sioSocket -> {
      AtomicBoolean addedUser = new AtomicBoolean(false);
      System.out.println("server receive connect from client.");
      sioSocket.on("new message", o -> {
        // we tell the client to execute 'new message'
        String message = (String) o[0];
        sioSocket.broadcast("new message", new JsonObject().put("username", names.get(sioSocket.id())).put("message", message));
      });

      sioSocket.on("add user", data -> {
        if (addedUser.get()) return;
        names.put(sioSocket.id(), (String) data[0]);
        userCount++;
        addedUser.set(true);
        sioSocket.emit("login", new JsonObject().put("numUsers", userCount));
        // echo globally (all clients) that a person has connected
        sioSocket.broadcast("user joined", new JsonObject().put("username", names.get(sioSocket.id())).put("numUsers", userCount));
      });

      sioSocket.on("typing", data -> {
        sioSocket.broadcast("typing", new JsonObject().put("username", names.get(sioSocket.id())));
      });

      sioSocket.on("stop typing", data -> {
        sioSocket.broadcast("stop typing", new JsonObject().put("username", names.get(sioSocket.id())));
      });

      sioSocket.onDisconnect(reason -> {
        if (addedUser.get()) {
          this.userCount--;
          sioSocket.broadcast("user left", new JsonObject().put("username", names.get(sioSocket.id())).put("numUsers", userCount));
        }
      });


    });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new ChatVerticle(), asyncResult -> {
      if (asyncResult.succeeded()) {

      } else {
        asyncResult.cause().printStackTrace();
      }
    });
  }
}

