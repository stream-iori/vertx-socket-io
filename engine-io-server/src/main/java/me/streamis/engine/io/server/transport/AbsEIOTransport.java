package me.streamis.engine.io.server.transport;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import me.streamis.engine.io.parser.Packet;
import me.streamis.engine.io.server.EIOTransport;
import me.streamis.engine.io.server.State;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by stream.
 */
public abstract class AbsEIOTransport implements EIOTransport {
  private State state = State.CLOSED;
  protected Handler<Void> closeHandler;
  protected List<Handler<Throwable>> errorsHandlers = new ArrayList<>();
  protected Handler<Packet> packetHandler;
  protected List<Handler<Void>> drainHandlers = new ArrayList<>();
  protected boolean discarded;
  protected boolean writable;
  private boolean supportsBinary;
  private String sid;


  AbsEIOTransport(boolean supportsBinary) {
    this.supportsBinary = supportsBinary;
  }

  @Override
  public void addPacketHandler(Handler<Packet> packetHandler) {
    this.packetHandler = packetHandler;
  }

  @Override
  public void addDrainHandler(Handler<Void> drainHandler) {
    this.drainHandlers.add(drainHandler);
  }

  @Override
  public void addCloseHandler(Handler<Void> closeHandler) {
    this.closeHandler = closeHandler;
  }

  @Override
  public void addErrorHandler(Handler<Throwable> errorHandler) {
    this.errorsHandlers.add(errorHandler);
  }

  @Override
  public void discard() {
    discarded = true;
  }

  @Override
  public boolean isSupportsBinary() {
    return supportsBinary;
  }

  @Override
  public void setSupportsBinary(boolean support) {
    this.supportsBinary = support;
  }

  @Override
  public void setSid(String sid) {
    this.sid = sid;
  }

  @Override
  public String getSid() {
    return this.sid;
  }

  /**
   * Called with parsed out a packets from the data stream.
   *
   * @param packet Packet
   */
  void onPacket(Packet packet) {
    if (packetHandler != null) packetHandler.handle(packet);
  }

  /**
   * Called with the encoded packet data.
   */
  protected void onData(String data) {
    this.onPacket(Packet.decodePacket(data));
  }

  /**
   * Called with a transport error.
   */
  protected void onError(Throwable exception) {
    if (errorsHandlers.size() > 0) {
      if (!(exception instanceof TransportException)) {
        exception = new TransportException(exception);
      }
      for (Handler<Throwable> errorsHandler : errorsHandlers) {
        errorsHandler.handle(exception);
      }
    }
  }

  @Override
  public void close(Handler<Void> callback) {
    if (state == State.CLOSED || state == State.CLOSING) return;
    state = State.CLOSING;
    doClose(callback);
  }

  protected abstract void doClose(Handler<Void> callback);

  public abstract String name();

  /**
   * Called upon transport close.
   */
  protected void onClose() {
    state = State.CLOSED;
    if (closeHandler != null) closeHandler.handle(null);
  }
}
