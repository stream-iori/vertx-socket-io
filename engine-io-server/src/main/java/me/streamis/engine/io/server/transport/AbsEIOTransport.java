package me.streamis.engine.io.server.transport;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import me.streamis.engine.io.parser.Packet;
import me.streamis.engine.io.server.EIOTransport;
import me.streamis.engine.io.server.State;

/**
 * Created by stream.
 */
abstract class AbsEIOTransport implements EIOTransport {
  private State state = State.CLOSED;
  protected boolean discarded;
  protected boolean writable;
  private boolean supportsBinary;
  private String sid;

  private Handler<Packet> packetHandler;
  protected Handler<Void> drainHandler;
  private Handler<Void> closeHandler;
  private Handler<Throwable> errorHandler;

  AbsEIOTransport(boolean supportsBinary) {
    this.supportsBinary = supportsBinary;
  }

  public void addPacketHandler(Handler<Packet> packetHandler) {
    this.packetHandler = packetHandler;
  }

  public void addDrainHandler(Handler<Void> drainHandler) {
    this.drainHandler = drainHandler;
  }

  public void addCloseHandler(Handler<Void> closeHandler) {
    this.closeHandler = closeHandler;
  }

  public void addErrorHandler(Handler<Throwable> errorHandler) {
    this.errorHandler = errorHandler;
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
  protected void onPacket(Packet packet) {
    packetHandler.handle(packet);
  }

  /**
   * Called with the encoded packet data.
   */
  protected void onData(Buffer data, boolean isSupportsBinary) {
      this.onPacket(Packet.decodeWithString(data.toString()));
//    if (isSupportsBinary) {
//      this.onPacket(Packet.decodeWithBuffer(data));
//    } else {
//      this.onPacket(Packet.decodeWithString(data.toString()));
//    }
  }


  /**
   * Called with a transport error.
   */
  protected void onError(Throwable exception) {
    if (errorHandler != null) {
      errorHandler.handle(exception);
    }
  }

  public void close(Handler<Void> callback) {
    if (state == State.CLOSED || state == State.CLOSING) return;
    state = State.CLOSING;
    doClose(callback);
  }

  public abstract void doClose(Handler<Void> callback);

  public abstract String name();

  /**
   * Called upon transport close.
   */
  protected void onClose() {
    state = State.CLOSED;
    closeHandler.handle(null);
  }
}
