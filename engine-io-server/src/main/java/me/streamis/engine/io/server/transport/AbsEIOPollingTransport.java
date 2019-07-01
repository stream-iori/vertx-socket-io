package me.streamis.engine.io.server.transport;

import io.vertx.core.Handler;
import io.vertx.core.TimeoutStream;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.engine.io.parser.Packet;
import me.streamis.engine.io.parser.PacketType;
import me.streamis.engine.io.server.EIOTransport;

import java.util.List;

/**
 * Created by stream.
 */
abstract class AbsEIOPollingTransport extends AbsEIOTransport implements EIOTransport {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbsEIOPollingTransport.class);
  private static final long closeTimeout = 30 * 1000L;

  private Vertx vertx;
  private Handler<Void> shouldClose;

  protected HttpServerRequest request;
  protected HttpServerRequest dataRequest;

  AbsEIOPollingTransport(Vertx vertx, boolean supportsBinary) {
    super(supportsBinary);
    this.vertx = vertx;
  }

  @Override
  public boolean writable() {
    return this.writable;
  }

  @Override
  public boolean supportsFraming() {
    return false;
  }

  @Override
  public void onRequest(HttpServerRequest request) {
    if (request.method() == HttpMethod.GET) {
      this.onPollRequest(request);
    } else if (request.method() == HttpMethod.POST) {
      this.onDataRequest(request);
    } else {
      request.response().setStatusCode(500).end();
    }
  }

  @Override
  public HttpServerRequest getRequest() {
    return this.request;
  }

  /**
   * The client sends a request awaiting for us to send data.
   */
  private void onPollRequest(HttpServerRequest request) {
    if (this.request == request) {
      onError(new TransportException("overlap from client"));
      request.response().setStatusCode(500).end();
      return;
    }
    this.request = request;
    request.connection().closeHandler(aVoid -> onError(new TransportException("poll connection closed prematurely.")));
    this.writable = true;
    if (drainHandlers.size() > 0) {
      drainHandlers.forEach(h -> h.handle(null));
    }

    // if we're still writable but had a pending close, trigger an empty send
    if (this.writable && shouldClose != null) {
      LOGGER.debug("triggering empty send to append close packet");
      this.send(new Packet(PacketType.NOOP));
    }
  }

  /**
   * The client sends a request with data.
   */
  private void onDataRequest(HttpServerRequest request) {
    if (this.dataRequest == request) {
      onError(new TransportException("data request overlap from client."));
      request.response().setStatusCode(500).end();
      return;
    }
    this.dataRequest = request;
    this.dataRequest.connection().closeHandler(aVoid -> {
      this.dataRequest = null;
      onError(new TransportException("data request connection closed prematurely"));
    });

    this.dataRequest.bodyHandler(data -> {
      boolean isBinary = "application/octet-stream".equals(request.headers().get("content-type"));
      this.onData(data, isBinary);
      request.response().putHeader("Content-Type", "text/html");
      if (!isBinary) request.response().putHeader("charset", "utf-8");
      headers(request);
      request.response().setStatusCode(200).end("ok");
      this.dataRequest = null;
    });
  }


  protected void onData(Buffer data, boolean isBinary) {
    List<Packet> packets;
    if (isBinary) packets = Packet.decodePayloadAsBuffer(data);
    else packets = Packet.decodePayload(data.toString());
    for (Packet packet : packets) {
      if (packet.getType() == PacketType.CLOSE) {
        LOGGER.debug("got xhr close packet.");
        onClose();
        return;
      }
      onPacket(packet);
    }
  }

  @Override
  protected void onClose() {
    if (this.writable) {
      //close pending poll request
      this.send(new Packet(PacketType.NOOP));
    }
    super.onClose();
  }

  @Override
  public void send(List<Packet> packets) {
    this.writable = false;

    if (this.shouldClose != null) {
      LOGGER.debug("appending close packet to payload");
      packets.add(new Packet(PacketType.CLOSE));
      shouldClose.handle(null);
      shouldClose = null;
    }
    Object data = Packet.encodePayload(isSupportsBinary(), packets);
    write(data);
  }

  protected void write(Object data) {
    String contentType = data instanceof Buffer ? "application/octet-stream" : "text/plain; charset=UTF-8";
    headers(this.request);
    HttpServerResponse response = this.request.response().putHeader("Content-Type", contentType).setStatusCode(200);
    //skip httpCompression
    if (data instanceof Buffer && isSupportsBinary()) {
      response.end((Buffer) data);
    } else {
      response.end(data.toString());
    }
    //data have been write,so cleanup the request which in GET method.
    LOGGER.debug("send message." + data);
    this.request = null;
  }

  @Override
  public void doClose(Handler<Void> callback) {
    LOGGER.debug("closing.");
    if (this.dataRequest != null) {
      LOGGER.debug("aborting ongoing data request.");
      this.dataRequest.netSocket().close();
    }

    final TimeoutStream closeTimeoutTimer = vertx.timerStream(closeTimeout);

    Handler<Void> innerClose = aVoid -> {
      closeTimeoutTimer.cancel();
      if (callback != null) callback.handle(null);
      onClose();
    };

    if (this.writable) {
      LOGGER.debug("transport writable - closing right away");
      this.send(new Packet(PacketType.CLOSE));
      innerClose.handle(null);
    } else if (this.discarded) {
      LOGGER.debug("transport discarded - closing right away");
      innerClose.handle(null);
    } else {
      LOGGER.debug("transport not writable - buffering orderly close");
      this.shouldClose = innerClose;
      closeTimeoutTimer.handler(aLong -> innerClose.handle(null));
    }
  }

  protected void headers(HttpServerRequest request) {
    String userAgent = request.getHeader("user-agent");
    if (userAgent != null && (userAgent.contains(";MSIE") || userAgent.contains("Trident/"))) {
      request.response().headers().add("X-XSS-Protection", "0");
    }
  }

  @Override
  public String name() {
    return Type.POLLING.name().toLowerCase();
  }


}
