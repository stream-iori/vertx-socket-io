package me.streamis.engine.io.server.transport;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import me.streamis.engine.io.server.EIOTransport;

/**
 * Created by stream.
 */
public class PollingJSONTransport extends AbsEIOPollingTransport implements EIOTransport {
  private String head;

  public PollingJSONTransport(Vertx vertx, HttpServerRequest request, boolean supportsBinary) {
    super(vertx, supportsBinary);
    String j = request.getParam("j") == null ? "" : request.getParam("j");
    this.head = String.format("___eio[%s](\"", j.replace("[^0-9]", ""));
  }

  @Override
  protected void onData(Buffer data, boolean isBinary) {
    // client will send already escaped newlines as \\\\n and newlines as \\n
    // \\n must be replaced with \n and \\\\n with \\n
    String replaceSlash = data.toString().replaceAll("(\\\\)?\\\\n", "\n").replaceAll("\\\\\\\\n", "\\n");
    super.onData(Buffer.buffer(replaceSlash), isBinary);
  }

  @Override
  protected void write(Object data) {
    String jsonStr = head + data.toString()
      .replaceAll("\\\\u2028", "\\\\\\\\u2028")
      .replaceAll("\\\\u2029", "\\\\\\\\u2029") + "\");";
    super.write(Buffer.buffer(jsonStr));
  }
}
