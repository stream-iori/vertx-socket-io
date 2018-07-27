package me.streamis;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.parser.Packet;
import me.streamis.parser.PacketType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.streamis.State.OPEN;
import static me.streamis.State.OPENING;
import static me.streamis.parser.PacketType.MESSAGE;

/**
 * Created by stream.
 */
public class EIOSocketImpl implements EIOSocket {

  private String id;
  private Vertx vertx;
  private HttpServerRequest request;
  private EIOTransport transport;
  private TransportUpgrade transportUpgrade;
  private State state;
  private EngineOptions options;
  private boolean upgraded = false;
  private boolean upgrading = false;

  private List<Packet> writeBuffer = new ArrayList<>();

  private long checkIntervalTimer;
  private long upgradeTimeoutTimer;
  private long pingTimeoutTimer;

  //TODO
  private Handler<Buffer> messageHandler;
  private Handler<Throwable> closeHandler;

  private static final Logger logger = LoggerFactory.getLogger(EIOSocketImpl.class);

  private Handler<Void> drainHandler = aVoid -> {
    if (logger.isDebugEnabled()) logger.debug("drain handle trigger.");
  };

  private Handler<Packet> packetHandler = packet -> {
    if (state != State.OPEN) {
      if (logger.isDebugEnabled()) logger.debug("packet received with closed socket");
      return;
    }
    if (logger.isDebugEnabled()) logger.debug("packet received on socket.");
    setPingTimeout();
    switch (packet.getType()) {
      case PING:
        if (logger.isDebugEnabled()) logger.debug("got a ping.");
        this.sendPacket(PacketType.PONG, Buffer.buffer(), null);
        break;
      case MESSAGE:
        this.messageHandler.handle(packet.getData());
        break;
      default:
        cleanup();
        close(new EngineIOException("packet error."));
    }
  };


  public EIOSocketImpl(String id, Vertx vertx, HttpServerRequest request, EIOTransport transport, EngineOptions options) {
    this.id = id;
    this.vertx = vertx;
    this.request = request;
    this.transport = transport;
    this.state = OPENING;
    this.options = options;
    this.setNewTransport(transport);
    open();
  }

  private void open() {
    state = OPEN;
    //  this.transport.sid = this.id;
    this.sendPacket(PacketType.OPEN, new JsonObject()
        .put("sid", id)
        .put("pingInterval", options.getPingInterval())
        .put("pingTimeout", options.getPingTimeout())
        .put("upgrades", new JsonArray(Arrays.asList(this.getAvailableUpgrades()))).toBuffer()
      , null);
    //TODO initial Packet
    // Reset ping timeout on any packet, incoming data is a good sign of
    // other side's liveness
    setPingTimeout();
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setNewTransport(EIOTransport transport) {
    this.transport = transport;
    this.transport.packetHandler(packetHandler);
    this.transport.closeHandler(aVoid -> close(null));
    this.transport.errorHandler(aVoid -> close(new EngineIOException("transport error")));
    this.transport.drainHandler(aVoid -> flush());
  }

  @Override
  public EIOTransport getTransport() {
    return this.transport;
  }

  @Override
  public void maybeUpgrade(EIOTransport newTransport) {
    if (!upgraded) {
      transportUpgrade = new TransportUpgrade(newTransport);
    } else {
      throw new EngineIOException("transport have upgraded.");
    }
  }

  @Override
  public void send(Buffer buffer, Handler<Packet> handler) {
    this.sendPacket(MESSAGE, buffer, handler);
  }

  @Override
  public void close(Throwable throwable) {
    if (state != State.CLOSE) {
      state = State.CLOSED;
      vertx.cancelTimer(pingTimeoutTimer);
      vertx.cancelTimer(checkIntervalTimer);
      vertx.cancelTimer(upgradeTimeoutTimer);
      // clean writeBuffer in next tick, so we can still
      // grab the writeBuffer on 'close' event
      vertx.runOnContext(aVoid -> writeBuffer.clear());
      clearTransport();
      if (closeHandler != null) closeHandler.handle(throwable);
    }
  }

  public void close(boolean discard) {
    if (state != State.OPEN) return;
    this.state = State.CLOSING;

    if (writeBuffer.size() > 0 && drainHandler != null)
      drainHandler = aVoid -> closeTransport(discard);
    else
      this.closeTransport(discard);
  }


  @Override
  public EIOSocket errorHandler(Handler<Throwable> handler) {
    return null;
  }

  @Override
  public EIOSocket closeHandler(Handler<Throwable> handler) {
    this.closeHandler = handler;
    return this;
  }

  @Override
  public boolean isUpgrading() {
    return upgrading;
  }

  @Override
  public boolean isUpgraded() {
    return upgraded;
  }

  @Override
  public void destroy(String message) {
    if (transport.isWritable()) {
      this.request.response().setStatusCode(400).setStatusMessage(message).end();
    }
    this.request.netSocket().close();
  }

  private String[] getAvailableUpgrades() {
    return options.getTransports().get(transport.type().name());
  }

  private void closeTransport(boolean discard) {
    if (discard) transport.discard();
    transport.close(aVoid -> close(new EngineIOException("forced close")));
  }

  private void cleanup() {
    this.upgrading = false;
    vertx.cancelTimer(checkIntervalTimer);
    vertx.cancelTimer(upgradeTimeoutTimer);
  }

  private void clearTransport() {
    // silence further transport errors and prevent uncaught exceptions
    transport.errorHandler(event -> {
      if (logger.isDebugEnabled()) logger.debug("error triggered by discarded transport.");
    });
    transport.close(null);
    vertx.cancelTimer(pingTimeoutTimer);
  }

  private void sendPacket(PacketType type, Buffer buffer, Handler<Packet> handler) {
    if (state == State.CLOSING || state == State.CLOSED) return;
    if (logger.isDebugEnabled()) logger.debug("sending packet. type " + type);
    //TODO packetCreate Event
    Packet packet = new Packet(type, buffer);
    this.writeBuffer.add(packet);
    if (handler != null) handler.handle(packet);
    this.flush();
  }

  private void flush() {
    if (state == State.CLOSED || !this.transport.isWritable() || this.writeBuffer.size() == 0) return;
    if (logger.isDebugEnabled()) logger.debug("flushing buffer to transport");
    if (writeBuffer.size() > 0) {
      transport.send(writeBuffer.toArray(new Packet[writeBuffer.size()]));
    }
    if (drainHandler != null) drainHandler.handle(null);
  }

  private void setPingTimeout() {
    vertx.cancelTimer(pingTimeoutTimer);
    pingTimeoutTimer = vertx
      .setTimer(options.getPingInterval() + options.getPingTimeout(),
        aLong -> close(new EngineIOException("ping timeout")));
  }

  /**
   * Try upgrade old transport to new transport.
   */
  private class TransportUpgrade {
    private EIOTransport newTransport;
    private Handler<Throwable> errorHandler = ex -> {
      if (logger.isDebugEnabled()) logger.debug("client did not complete upgrade.", ex);
      cleanup();
      newTransport.close(null);
      newTransport = null;
    };

    private Handler<Packet> packetHandler = packet -> {
      switch (packet.getType()) {
        case PING:
          if ("probe".equals(packet.getData().toString())) {
            newTransport.send(new Packet(PacketType.PONG, "probe"));
            //TODO invoke upgrading handler
            vertx.cancelTimer(checkIntervalTimer);
            checkIntervalTimer = vertx.setTimer(500L, event -> {
              if ("polling".equals(transport.type().name()) && transport.isWritable()) {
                if (logger.isDebugEnabled())
                  logger.debug("writing a noop packet to polling for fast upgrade");
                transport.send(new Packet(PacketType.NOOP));
              }
            });
          }
          break;
        case UPGRADE:
          if (logger.isDebugEnabled()) {
            logger.debug("got upgrade packet - upgrading.");
          }
          cleanup();
          transport.discard();
          upgraded = true;
          clearTransport();
          setNewTransport(newTransport);
          //TODO invoke upgraded handler
          setPingTimeout();
          flush();//flush rest packet with new transport.
          if (state == State.CLOSING) {
            newTransport.close(aVoid -> close(new EngineIOException("forced close")));
          }
          break;
        default:
          cleanup();
          newTransport.close(null);
      }
    };

    TransportUpgrade(EIOTransport targetTransport) {
      if (logger.isDebugEnabled())
        logger.debug(String.format("might upgrade socket transport from %s to %s", transport.type().name(), newTransport.type().name()));
      upgrading = true;
      this.newTransport = targetTransport;
      upgradeTimeoutTimer = vertx.setTimer(options.getUpgradeTimeout(), aLong -> {
        this.cleanup();
        if (newTransport.state() == State.OPEN) newTransport.close(null);
      });
      newTransport.packetHandler(packetHandler);
      newTransport.closeHandler(aVoid -> errorHandler.handle(new EngineIOException("transport close.")));
      newTransport.errorHandler(errorHandler);
      closeHandler = aVoid -> errorHandler.handle(new EngineIOException("socket closed"));
      this.newTransport = transport;
    }

    private void cleanup() {
      upgrading = false;
      vertx.cancelTimer(checkIntervalTimer);
      vertx.cancelTimer(upgradeTimeoutTimer);
    }

  }
}
