package me.streamis.transport;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import me.streamis.EIOTransport;

/**
 * Created by stream.
 */
public class PollingJSONP extends AbsPollingTransport implements EIOTransport {

  private String jsonHead;

  protected PollingJSONP(HttpServerRequest request) {
    super(request);
    String j = request.getParam("j") == null ? "" : request.getParam("j");
    this.jsonHead = String.format("___eio[%s](\"", j);
  }

  @Override
  protected void dataHandle(Buffer data) {
    String replaceSlash = data.toString().replaceAll("\\\\\\\\", "\\\\\\\\\\\\\\\\").replaceAll("\\\\", "\\\\\\\\");
    super.dataHandle(Buffer.buffer(replaceSlash));
  }

  @Override
  protected void write(Buffer data) {
    String jsonFoot = "\");";
    String jsonStr = jsonHead + data.toString()
      .replaceAll("\\\\u2028", "\\\\\\\\u2028")
      .replaceAll("\\\\u2029", "\\\\\\\\u2029") + jsonFoot;
    super.write(Buffer.buffer(jsonStr));
  }

}
