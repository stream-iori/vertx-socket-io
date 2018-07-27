package me.streamis.transport;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import me.streamis.EIOSocket;
import me.streamis.EIOTransport;
import me.streamis.State;
import me.streamis.parser.Packet;
import me.streamis.parser.PacketOption;

/**
 * Created by stream.
 */
public abstract class AbsTransport implements EIOTransport {

  protected boolean discarded = false;
  protected boolean writable = false;
  protected State state;
  protected HttpServerRequest request;
  protected Handler<Throwable> errorHandler;
  protected Handler<Packet> packetHandler;
  protected Handler<Void> drainHandler;
  protected Handler<Void> closeHandler;
  protected EIOSocket eioSocket;

  protected AbsTransport(HttpServerRequest request) {
    this.request = request;
  }

  @Override
  public void discard() {
    this.discarded = true;
  }

  @Override
  public void close(Handler<Void> handler) {
    if (state == State.CLOSED || state == State.CLOSING) return;
    state = State.CLOSING;
    doClose(handler);
  }

  @Override
  public State state() {
    return state;
  }

  @Override
  public boolean isDiscard() {
    return discarded;
  }

  @Override
  public boolean isWritable() {
    return this.writable;
  }

  @Override
  public void setEIOSocket(EIOSocket eioSocket) {
    this.eioSocket = eioSocket;
  }

  @Override
  public void errorHandler(Handler<Throwable> errorHandler) {
    this.errorHandler = errorHandler;
  }

  @Override
  public void closeHandler(Handler<Void> closeHandler) {
    this.closeHandler = closeHandler;
  }

  @Override
  public void packetHandler(Handler<Packet> packetHandler) {
    this.packetHandler = packetHandler;
  }

  @Override
  public void drainHandler(Handler<Void> drainHandler) {
    this.drainHandler = drainHandler;
  }

  protected void dataHandle(Buffer data) {
    this.packetHandler.handle(Packet.decode(data));
  }

  protected void dataHandle(PacketOption option, Buffer data) {
    this.packetHandler.handle(Packet.decode(data, option));
  }

  @Override
  public void handleRequest(HttpServerRequest request) {
    this.request = request;
  }

  protected abstract void doClose(Handler<Void> handler);

  /**
   * Called upon transport close.
   */
  protected void onClose() {
    this.state = State.CLOSED;
    if (closeHandler != null) closeHandler.handle(null);
  }

  /**
   * Called with a transport error.
   *
   * @param throwable
   */
  protected void onError(Throwable throwable) {
    if (errorHandler != null) {
      this.errorHandler.handle(new EIOTransportException("Transport Error",throwable));
    }
  }


}
