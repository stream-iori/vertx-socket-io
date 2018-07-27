package me.streamis.transport;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.EIOTransport;
import me.streamis.parser.Packet;
import me.streamis.parser.PacketOption;
import me.streamis.parser.PacketType;
import me.streamis.parser.PayLoad;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by stream.
 */
abstract class AbsPollingTransport extends AbsTransport implements EIOTransport {

  protected Vertx vertx;
  protected long closeTimeout = 30 * 1000L;
  protected long closeTimeoutTimer = 0;

  protected Consumer<Handler<Void>> shouldClose;

  private static final Logger logger = LoggerFactory.getLogger(AbsPollingTransport.class);

  protected AbsPollingTransport(HttpServerRequest request) {
    super(request);
  }

  @Override
  public Type type() {
    return Type.POLLING;
  }

  @Override
  public void handleRequest(HttpServerRequest request) {
    switch (request.method()) {
      case GET:
        pollRequest(request);
        break;
      case POST:
        dataRequest(request, request.response());
        break;
      default:
        request.response().setStatusCode(500).setStatusMessage("unsupported method").end();
    }
  }


  @Override
  protected void doClose(Handler<Void> handler) {
    if (logger.isDebugEnabled()) logger.debug("polling close.");
    //lazy close
    shouldClose = h -> {
      vertx.cancelTimer(closeTimeoutTimer);
      if (h != null) h.handle(null);
      onClose();
    };
    if (this.writable) {
      if (logger.isDebugEnabled()) logger.debug("transport writable - closing right away");
      this.send(Collections.singletonList(new Packet(PacketType.CLOSE)));
      shouldClose.accept(handler);
    } else if (this.discarded) {
      if (logger.isDebugEnabled()) logger.debug("transport discarded - closing right away");
      shouldClose.accept(handler);
    } else {
      if (logger.isDebugEnabled()) logger.debug("transport writable - buffering orderly close.");
      closeTimeoutTimer = vertx.setTimer(closeTimeout, aLong -> shouldClose.accept(handler));
    }
  }

  private void dataRequest(HttpServerRequest request, HttpServerResponse response) {
    boolean isBinary = "application/octet-stream".equals(request.headers().get("content-type"));
    Buffer chunk = Buffer.buffer();

    request.bodyHandler(data -> {
      if (isBinary) {
        chunk.appendBuffer(data);
      } else {
        chunk.appendString(data.toString());
      }
      //TODO Pump
      // if (contentLength > maxHttpBufferSize) {
      //   this.dataRequest.connection().close();
      // }
    });

    //no more data to read
    request.endHandler(aVoid -> {
      dataHandle(chunk);
      if (isBinary) {
        dataHandle(PacketOption.BINARY, chunk);
      } else {
        dataHandle(chunk);
      }
      response.putHeader("Content-Type", "text/html");
      response.headers().addAll(headers(request));
      response.setStatusCode(200).end("ok");
    });

  }

  /**
   * The client sends a request awaiting for us to send data.
   *
   * @param request HttpServerRequest
   */
  private void pollRequest(HttpServerRequest request) {
    if (logger.isDebugEnabled()) logger.debug("setting request.");
    this.request = request;
    this.request.netSocket().closeHandler(aVoid -> onError(new EIOTransportException("poll connection closed prematurely")));

    this.writable = true;
    if (drainHandler != null) drainHandler.handle(null);
    // if we're still writable but had a pending close, trigger an empty send
    if (this.writable && this.shouldClose != null) {
      if (logger.isDebugEnabled()) logger.debug("triggering empty send to append close packet");
      this.send(new Packet(PacketType.NOOP));
    }
  }

  @Override
  protected void dataHandle(Buffer data) {
    if (logger.isDebugEnabled()) logger.debug("received " + data);
    List<Packet> packets = PayLoad.decodePayLoad(data.getBytes());
    for (Packet packet : packets) {
      if (packet.getType() == PacketType.CLOSE) {
        if (logger.isDebugEnabled()) logger.debug("got xhr close packet.");
        onClose();
        return;
      }
      packetHandler.handle(packet);
    }
  }


  @Override
  public void send(List<Packet> packets) {
    this.writable = false;
    if (this.shouldClose != null) {
      if (logger.isDebugEnabled()) logger.debug("appending close packet to payload.");
      packets.add(new Packet(PacketType.CLOSE));
      this.shouldClose.accept(null);
      this.shouldClose = null;
    }
    Buffer buffer = PayLoad.encodePayload(packets.toArray(new Packet[packets.size()]));
    write(buffer);
  }


  protected void write(Buffer data) {
    if (logger.isDebugEnabled()) logger.debug("writing " + data);
    HttpServerResponse response = request.response();
    response.putHeader("Content-Type", "text/plain; charset=UTF-8");
    response.headers().addAll(headers(request));
    response.setStatusCode(200).end(data.toString());
    cleanup();
  }

  @Override
  protected void onClose() {
    if (writable) {
      //close pending poll request
      this.send(new Packet(PacketType.NOOP));
    }
    super.onClose();
  }

  private void cleanup() {
    this.request = null;
  }

  protected MultiMap headers(HttpServerRequest request) {
    String userAgent = request.getHeader("user-agent").trim();
    MultiMap header = new VertxHttpHeaders();
    if (userAgent.contains(";MSIE") || userAgent.contains("Trident/")) {
      header.add("X-XSS-Protection", "0");
    }
    return header;
  }
}
