package me.streamis.socket.io.server;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class ExampleTest {

  private HttpServer httpServer;
  private SIOServer sioServer;
  private Vertx vertx = Vertx.vertx();

  static {
    System.setProperty(
      "vertx.logger-delegate-factory-class-name",
      "io.vertx.core.logging.SLF4JLogDelegateFactory"
    );
  }

  private Socket createSocketIOClient() {
    IO.Options opts = new IO.Options();
    opts.forceNew = true;
    opts.reconnection = false;
    return IO.socket(URI.create("http://localhost:3000"), opts);
  }

  @BeforeEach
  public void setup() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();
    Handler<HttpServerRequest> requestHandler = request -> {
      if (request.uri().equals("/")) {
        request.response().end("this is socket io server powered by vert.x");
      }
    };

    httpServer = vertx.createHttpServer();
    sioServer = new SIOServer(vertx, httpServer);
    sioServer.attach(requestHandler, new SocketIOOptions());
    httpServer.listen(3000, ar -> {
      System.out.println("socket io server start at 3000.");
      assertTrue(ar.succeeded());
      testContext.completeNow();
    });

    assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();
  }

  @AfterEach
  public void tearDown() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();
    httpServer.close(ar -> {
      assertTrue(ar.succeeded());
      testContext.completeNow();
    });
    assertThat(testContext.awaitCompletion(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void connectCheck() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();
    Socket socket = createSocketIOClient();
    socket.on(Socket.EVENT_CONNECT, objects -> {
      System.out.println("client make connect event");
      testContext.completeNow();
    });
    socket.connect();
    assertThat(testContext.awaitCompletion(5, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  public void eventEmitAndOn() throws InterruptedException {
    VertxTestContext testContext = new VertxTestContext();
    //select default namespace
    sioServer.of("/chats").onConnect(sioSocket -> {
      sioSocket.on("foo", o -> {
        assertEquals("hi", o[0]);
        sioSocket.emit("chat", new JsonObject().put("action", "eating."));
      });
    });

    //
    Socket clientSocket = createSocketIOClient().io().socket("/chats");

    clientSocket.on(Socket.EVENT_CONNECT, objects -> {
      clientSocket.emit("foo", "hi");
    });
    clientSocket.on("chat", objects1 -> {
      try {
        assertEquals(((JSONObject) objects1[0]).getString("action"), "eating.");
      } catch (JSONException e) {
        testContext.failNow(e);
      }
      testContext.completeNow();
    });
    clientSocket.once(Socket.EVENT_DISCONNECT, objects -> {
      System.out.println("disconnect.");
    });
    clientSocket.on(Socket.EVENT_ERROR, objects -> {
      System.out.println("client socket error.");
    });

    clientSocket.connect();

    assertThat(testContext.awaitCompletion(3, TimeUnit.SECONDS)).isTrue();
  }
}
