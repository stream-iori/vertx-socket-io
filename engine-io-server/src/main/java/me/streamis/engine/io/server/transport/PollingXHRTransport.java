package me.streamis.engine.io.server.transport;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import me.streamis.engine.io.server.EIOTransport;

/**
 * Created by stream.
 */
public class PollingXHRTransport extends AbsEIOPollingTransport implements EIOTransport {

  public PollingXHRTransport(Vertx vertx, boolean supportsBinary) {
    super(vertx, supportsBinary);
  }

  @Override
  public void onRequest(HttpServerRequest request) {
    if (request.method() == HttpMethod.OPTIONS) {
      headers(request).response().putHeader("Access-Control-Allow-Headers", "Content-Type").setStatusCode(200).end();
    } else {
      super.onRequest(request);
    }
  }

  @Override
  protected HttpServerRequest headers(HttpServerRequest request) {
    String origin = request.headers().get("Origin");
    if (origin != null && origin.length() > 0) {
      request.response().headers()
        .add("Access-Control-Allow-Credentials", "true")
        .add("Access-Control-Allow-Origin", origin);
    } else {
      request.response().headers().add("Access-Control-Allow-Origin", "*");
    }
    return super.headers(request);
  }
}
