package me.streamis.engine.io.server.transport;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import me.streamis.engine.io.server.EIOTransport;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;

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
    String[] dataStrArray = data.toString().split("d=", 2);
    if (dataStrArray.length == 2) {
      String dataContent = dataStrArray[1];
      try {
        dataContent = URLDecoder.decode(dataContent, "utf-8");
        System.out.println(dataContent);
        if (dataContent != null) {
          String replaceSlash = dataContent.replaceAll("(\\\\)?\\\\n", "\n").replaceAll("\\\\\\\\n", "\\n");
          super.onData(Buffer.buffer(replaceSlash), isBinary);
        }
      } catch (UnsupportedEncodingException e) {
        throw new TransportException(e);
      }
    }
  }

  @Override
  protected void write(Object data) {
    //json.stringify
    String content = Json.encode(data.toString());
    content = content.substring(1, content.length() - 1);
    String jsonStr = head + content
      .replaceAll("\\\\u2028", "\\\\\\\\u2028")
      .replaceAll("\\\\u2029", "\\\\\\\\u2029") + "\");";
    super.write(jsonStr);
  }
}
