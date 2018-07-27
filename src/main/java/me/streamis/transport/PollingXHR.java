package me.streamis.transport;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import me.streamis.EIOTransport;

/**
 * Created by stream.
 */
public class PollingXHR extends AbsPollingTransport implements EIOTransport {

  public PollingXHR(HttpServerRequest request) {
    super(request);
  }

  @Override
  public void handleRequest(HttpServerRequest request) {
    if (request.method() == HttpMethod.OPTIONS) {
      MultiMap header = this.headers(request);
      header.add("Access-Control-Allow-Headers", "Content-Type");
      request.response().headers().addAll(header);
      request.response().setStatusCode(200).end();
    } else {
      super.handleRequest(request);
    }
  }

  @Override
  protected MultiMap headers(HttpServerRequest request) {
    MultiMap header = super.headers(request);
    String origin = request.headers().get("Origin");
    if (origin != null && origin.length() > 0) {
      header.add("Access-Control-Allow-Credentials", "true");
      header.add("Access-Control-Allow-Origin", origin);
    } else {
      header.add("Access-Control-Allow-Origin", "*");
    }
    return header;
  }
}
