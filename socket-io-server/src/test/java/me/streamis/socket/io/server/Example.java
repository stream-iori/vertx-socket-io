package me.streamis.socket.io.server;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

public class Example {

  private static final Logger logger = LoggerFactory.getLogger(Example.class);

  private Socket createSocketIOClient() {
    IO.Options opts = new IO.Options();
    opts.forceNew = true;
    opts.reconnection = false;
    Socket socket = IO.socket(URI.create("http://localhost:3000"), opts);
    return socket;
  }

  @Test
  public void clientTest() throws InterruptedException {
    Socket socket = createSocketIOClient();
    socket.on(Socket.EVENT_CONNECT, objects -> {
      System.out.println("client event");
    });
    socket.connect();
    System.out.println("connect client.");
    CountDownLatch countDownLatch = new CountDownLatch(1);
    countDownLatch.await();
  }

  @Test
  public void example() throws InterruptedException {
    Vertx vertx = Vertx.vertx();
    Handler<HttpServerRequest> requestHandler = request -> {
      if (request.uri().equals("/")) {
        request.response().end("this is socket io server powered by vert.x");
      }
    };

    HttpServer httpServer = vertx.createHttpServer();
    SIOServer sioServer = new SIOServer(vertx, httpServer);
    sioServer.attach(httpServer, requestHandler, new SocketIOOptions());
    httpServer.listen(3000, event -> {
      if (event.failed()) event.cause().printStackTrace();
      else {
        System.out.println("3000 up");
        sioServer.of("/").onConnect(sioSocket -> {
          System.out.println("server receive connect from client.");
          sioSocket.on("foo", o -> {
            System.out.println("server sio receive event foo");
            Stream.of(o).forEach(System.out::println);
            sioSocket.emit("chat", new JsonObject().put("action", "eating."));
          });
        });

        Socket socket = createSocketIOClient();
        socket.on(Socket.EVENT_CONNECT, objects -> {
          System.out.println("client event");
          socket.emit("foo", "hi");
        });
        socket.on("chat", objects1 -> {
          System.out.println("event chat data " + objects1[0]);
        });
        socket.connect();
      }
    });
    CountDownLatch countDownLatch = new CountDownLatch(1);
    countDownLatch.await();
  }
}
