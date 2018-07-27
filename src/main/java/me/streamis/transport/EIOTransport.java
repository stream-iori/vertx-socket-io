package me.streamis.transport;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.State;
import me.streamis.parser.Packet;

import java.util.ArrayList;
import java.util.List;

import static me.streamis.State.*;


/**
 * Created by stream.
 */
public abstract class EIOTransport {


  protected HttpServerRequest request;
  protected State state = OPEN;
  protected boolean isDiscarded = false;
  protected boolean writable = true;

  protected List<Handler<Throwable>> errorHandlers = new ArrayList<>();
  protected List<Handler<Packet>> packetHandlers = new ArrayList<>();
  protected List<Handler<Void>> drainHandlers = new ArrayList<>();
  protected Handler<Throwable> errorHandler;
  protected Handler<Throwable> closeHandler;

  private static final Logger logger = LoggerFactory.getLogger(EIOTransport.class);


  EIOTransport(HttpServerRequest request) {
    this.request = request;
  }

  public void discard() {
    this.isDiscarded = true;
  }

  public boolean isWritable() {
    return writable;
  }

  public State state() {
    return state;
  }

  void onRequest(HttpServerRequest request) {
    this.request = request;
    if (logger.isDebugEnabled()) logger.debug("setting request.");
  }

  public void close(Handler<Void> handler) {
    if (CLOSED == state || CLOSING == state) return;
    this.state = CLOSING;
    this.doClose(handler);
  }

  void onError(Throwable eioException) {
    if (errorHandlers.size() > 0) {
      for (Handler<Throwable> handler : errorHandlers)
        handler.handle(eioException);
    } else {
      logger.debug("ignored transport error", eioException);
    }
  }

  public void errorHandler(Handler<Throwable> handler) {
    this.errorHandler = handler;
  }

  public void addPacketHandler(Handler<Packet> handler) {
    this.packetHandlers.add(handler);
  }

  public void drainHandler(Handler<Void> handler) {
    this.drainHandlers.add(handler);
  }

  public void closeHandler(Handler<Throwable> handler) {
    this.closeHandler = handler;
  }

  public void removePacketHandler(Handler<Packet> handler) {
    this.packetHandlers.remove(handler);
  }

  public void removeDrainHandler(Handler<Void> handler) {
    this.drainHandlers.remove(handler);
  }

  public abstract boolean isSupportsFraming();




  /**
   * Called with parsed out a packets from the data stream.
   *
   * @param packet Packet
   */
  public void onPacket(Packet packet) {
    for (Handler<Packet> handler : packetHandlers)
      handler.handle(packet);
  }

  /**
   * Called with the encoded packet data.
   *
   * @param buffer Buffer
   */
  void onData(Buffer buffer) {
    this.onPacket(Packet.decode(buffer));
  }

 public void send(Packet... packets) {

  }

  public abstract void doClose(Handler<Void> handler);
  public abstract String name();

}
