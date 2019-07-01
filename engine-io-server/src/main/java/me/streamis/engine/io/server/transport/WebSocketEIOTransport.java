package me.streamis.engine.io.server.transport;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.engine.io.parser.Packet;
import me.streamis.engine.io.server.EIOTransport;

import java.util.List;

/**
 * Created by stream.
 */
public class WebSocketEIOTransport extends AbsEIOTransport implements EIOTransport {

  private ServerWebSocket webSocket;
  private HttpServerRequest request;
  private boolean supportsBinary;

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEIOTransport.class);

  public WebSocketEIOTransport(ServerWebSocket webSocket, boolean supportsBinary) {
    super(supportsBinary);
    this.webSocket = webSocket;
    this.webSocket.textMessageHandler(this::onData);
    this.webSocket.closeHandler(avoid -> onClose());
    this.webSocket.exceptionHandler(this::onError);
    this.writable = true;
  }

  @Override
  public boolean writable() {
    return writable;
  }

  @Override
  public boolean supportsFraming() {
    return true;
  }

  @Override
  public boolean isSupportsBinary() {
    return true;
  }

  @Override
  public void setSupportsBinary(boolean isSupport) {
    this.supportsBinary = isSupport;
  }

  @Override
  public void onRequest(HttpServerRequest request) {
    this.request = request;
  }

  @Override
  public HttpServerRequest getRequest() {
    return this.request;
  }

  @Override
  public void send(List<Packet> packets) {
    if (packets.size() == 0) return;
    try {
      this.writable = false;
      boolean isBuffer = packets.get(0).getData() instanceof Buffer;
      if (isBuffer && supportsBinary) {
        for (Packet packet : packets) {
          webSocket.writeBinaryMessage(Packet.encodeAsBuffer(packet));
        }
      } else {
        for (Packet packet : packets) {
          webSocket.writeTextMessage(Packet.encodePacket(packet, this.supportsBinary));
        }
      }
    } catch (Throwable e) {
      LOGGER.error("send packet exception", e);
    } finally {
      writable = true;
    }
  }

  @Override
  public void doClose(Handler<Void> callback) {
    this.webSocket.close();
    if (callback != null) callback.handle(null);
  }

  @Override
  public String name() {
    return Type.WEBSOCKET.name().toLowerCase();
  }

}
