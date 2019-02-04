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

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEIOTransport.class);

  public WebSocketEIOTransport(ServerWebSocket webSocket, boolean supportsBinary) {
    super(supportsBinary);
    this.webSocket = webSocket;
    this.webSocket.binaryMessageHandler(buffer -> this.onData(buffer, true));
    this.webSocket.textMessageHandler(str -> this.onData(Buffer.buffer(str), false));
    this.webSocket.closeHandler(avoid -> onClose());
    this.webSocket.exceptionHandler(this::onError);
    this.writable = true;
  }

  public WebSocketEIOTransport(HttpServerRequest request, boolean supportBinary) {
    this(request.upgrade(), supportBinary);
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
  public void onRequest(HttpServerRequest request) {
    //WebSocket don't care HttpServerRequest
  }

  @Override
  public void send(List<Packet> packets) {
    try {
      for (Packet packet : packets) {
        writable = false;
        webSocket.writeTextMessage(Packet.encodeAsString(packet));
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
    return "websocket";
  }

}
