package me.streamis.engine.io.server;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;

/**
 * Created by stream.
 */
public class MainTest {
  private static HttpServerRequest getRequest;
  private static HttpServerRequest postRequest;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.createHttpServer().requestHandler(request -> {
      if (request.method() == HttpMethod.POST) {
        request.handler(data -> {
          System.out.println("data " + data);
        });
        request.endHandler(aVoid -> {
          System.out.println("end");
          getRequest.response().end("ok");
          request.response().end();
        });
      }
      if (request.method() == HttpMethod.GET) {
        getRequest = request;
      }
    }).listen(3001);
  }
}
