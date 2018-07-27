package me.streamis.transport;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.EIOTransport;
import me.streamis.parser.Packet;
import me.streamis.parser.PacketOption;

import java.util.List;

/**
 * Created by stream.
 */
public class WebSocketTransport extends AbsTransport implements EIOTransport {

  private ServerWebSocket webSocket;
  private static final Logger logger = LoggerFactory.getLogger(WebSocketTransport.class);

  public WebSocketTransport(HttpServerRequest request) {
    super(request);
    webSocket = request.upgrade();
    webSocket.handler(this::dataHandle);
    webSocket.closeHandler(aVoid -> onClose());
    webSocket.exceptionHandler(this::onError);
    writable = true;
  }

  @Override
  public Type type() {
    return Type.WEBSOCKET;
  }


  @Override
  public void send(List<Packet> packets) {
    for (Packet packet : packets) {
      Buffer buffer;
      if (packet.getOption() == PacketOption.BINARY) {
        buffer = Packet.encode(PacketOption.BINARY, packet);
        webSocket.writeBinaryMessage(buffer);
      } else {
        buffer = Packet.encode(PacketOption.DEFAULT, packet);
        webSocket.writeTextMessage(buffer.toString());
      }
      if (logger.isDebugEnabled()) logger.debug("websocket writing " + buffer);
    }
  }

  @Override
  protected void doClose(Handler<Void> handler) {
    if (logger.isDebugEnabled()) logger.debug("websocket transport closing.");
    webSocket.close();
    if (handler != null) handler.handle(null);
  }
}
