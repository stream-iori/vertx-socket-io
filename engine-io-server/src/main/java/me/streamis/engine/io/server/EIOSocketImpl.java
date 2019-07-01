package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import me.streamis.engine.io.parser.Packet;
import me.streamis.engine.io.parser.PacketType;
import me.streamis.engine.io.server.transport.TransportException;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static me.streamis.engine.io.server.State.*;

/**
 * Created by stream.
 */
public class EIOSocketImpl implements EIOSocket, EIOUpgradeSocket {

  private Vertx vertx;
  private String id;
  private EngineOptions options;
  private EIOTransport transport;
  private State state;
  private Queue<Packet> packets = new LinkedBlockingQueue<>();

  private boolean upgraded = false;
  private boolean upgrading = false;

  private long checkIntervalTimer;
  private long upgradeTimeoutTimer;
  private long pingTimeoutTimer;

  private List<Handler<String>> closeHandlers = new ArrayList<>();
  private Handler<Packet> packetHandler;
  private Handler<Object> messageHandler;
  private Handler<Packet> packetCreateHandler;
  private Handler<List<Packet>> flushHandler;
  private Handler<Throwable> errorHandler;
  private List<Handler<Void>> drainHandlers = new ArrayList<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(EIOSocketImpl.class);


  EIOSocketImpl(Vertx vertx, String id, EngineOptions engineOptions, EIOTransport transport) {
    this.id = id;
    this.vertx = vertx;
    this.transport = transport;
    this.state = OPENING;
    this.options = engineOptions;

    this.setTransport(transport);
    this.onOpen();
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public EIOSocket closeHandler(Handler<String> handler) {
    this.closeHandlers.add(handler);
    return this;
  }

  @Override
  public EIOSocket errorHandler(Handler<Throwable> handler) {
    this.errorHandler = handler;
    return this;
  }

  @Override
  public EIOSocket messageHandler(Handler<Object> handler) {
    this.messageHandler = handler;
    return this;
  }

  @Override
  public EIOSocket packetHandler(Handler<Packet> handler) {
    this.packetHandler = handler;
    return this;
  }

  @Override
  public EIOSocket packetCreateHandler(Handler<Packet> handler) {
    this.packetCreateHandler = handler;
    return this;
  }

  @Override
  public EIOSocket flushHandler(Handler<List<Packet>> handler) {
    this.flushHandler = handler;
    return this;
  }

  @Override
  public EIOSocket drainHandler(Handler<Void> handler) {
    this.drainHandlers.add(handler);
    return this;
  }

  public EIOSocket send(Buffer data) {
    Packet packet = new Packet(PacketType.MESSAGE, data);
    sendPacket(packet);
    return this;
  }

  public EIOSocket send(String data) {
    Packet packet = new Packet(PacketType.MESSAGE, data);
    sendPacket(packet);
    return this;
  }

  @Override
  public void close(boolean discard) {
    if (state != OPEN) return;
    state = CLOSING;
    if (packets.size() > 0) {
      this.drainHandlers.add(aVoid -> closeTransport(discard));
    } else {
      closeTransport(discard);
    }
  }

  @Override
  public String getId() {
    return this.id;
  }


  @Override
  public EIOTransport getTransport() {
    return this.transport;
  }

  private void setTransport(EIOTransport transport) {
    this.transport = transport;
    this.transport.addErrorHandler(this::onError);
    this.transport.addPacketHandler(this::onPacket);
    this.transport.addDrainHandler(aVoid -> this.flush());
    this.transport.addCloseHandler(aVoid -> this.onClose("transport close"));
  }

  private void onOpen() {
    state = OPEN;
    this.transport.setSid(id);
    JsonObject openPacketData = new JsonObject().put("sid", id)
      .put("upgrades", new JsonArray(getAvailableUpgrades()))
      .put("pingInterval", options.getPingInterval())
      .put("pingTimeout", options.getPingTimeout());
    LOGGER.debug("open packet is " + openPacketData);
    this.sendPacket(new Packet(PacketType.OPEN, openPacketData.encode()));
    if (options.getInitialPacket() != null) this.sendPacket(options.getInitialPacket());
    this.setPingTimeout();
  }


  /**
   * Called upon transport error.
   */
  private void onError(Throwable exception) {
    LOGGER.debug(id + " transport error.");
    this.onClose(exception.getMessage());
  }

  private void onPacket(Packet packet) {
    if (state != OPEN) return;
    LOGGER.debug(id + " receive packet");
    if (packetHandler != null) packetHandler.handle(packet);
    setPingTimeout();
    switch (packet.getType()) {
      case PING:
        LOGGER.debug(id + " got ping.");
        this.sendPacket(new Packet(PacketType.PONG));
        break;
      case MESSAGE:
        if (messageHandler != null) messageHandler.handle(packet.getData());
        break;
      default:
        LOGGER.error("unKnow packet received " + packet);
        break;
    }
  }

  private void sendPacket(Packet packet) {
    LOGGER.debug("send Packet " + state);
    if (state == CLOSING || state == CLOSED) return;
    // exports packetCreate event
    if (packetCreateHandler != null) packetCreateHandler.handle(packet);
    packets.offer(packet);
    flush();
  }

  private void flush() {
    if (state == CLOSED || !transport.writable() || packets.size() <= 0) return;
    LOGGER.debug("flushing buffer to transport");
    List<Packet> sendPackets = new ArrayList<>();
    Packet packet;
    while ((packet = packets.poll()) != null) {
      sendPackets.add(packet);
    }
    if (flushHandler != null) flushHandler.handle(sendPackets);
    this.transport.send(sendPackets);
    drainHandlers.forEach(handler -> handler.handle(null));
  }

  private void setPingTimeout() {
    vertx.cancelTimer(pingTimeoutTimer);
    pingTimeoutTimer = vertx.setTimer(options.getPingInterval() + options.getPingTimeout(),
      aLong -> onClose("ping timeout"));
  }

  private List<String> getAvailableUpgrades() {
    return options.getTransports().get(transport.name());
  }

  /**
   * Called upon transport considered closed.
   * Possible reasons: `ping timeout`, `client error`, `parse error`,
   * `transport error`, `server close`, `transport close`
   */
  private void onClose(String reason) {
    if (CLOSED == this.state) return;
    this.state = CLOSED;
    vertx.cancelTimer(pingTimeoutTimer);
    vertx.cancelTimer(checkIntervalTimer);
    vertx.cancelTimer(upgradeTimeoutTimer);
    cleanTransport();
    for (Handler<String> handler : closeHandlers) {
      handler.handle(reason);
    }
  }

  private void closeTransport(boolean discard) {
    if (discard) this.transport.discard();
    this.transport.close(aVoid -> onClose("forced close"));
  }

  /**
   * Clears listeners and timers associated with current transport.
   */
  private void cleanTransport() {
    this.transport.close(null);
    vertx.cancelTimer(pingTimeoutTimer);
    this.transport = null;
  }

  @Override
  public void maybeUpgrade(final EIOTransport transport) {
    LOGGER.debug("might upgrade socket transport from " + this.transport.name() + " to " + transport.name());
    Handler<Void> cleanup = aVoid -> {
      upgrading = false;
      vertx.cancelTimer(checkIntervalTimer);
      vertx.cancelTimer(upgradeTimeoutTimer);
      closeHandlers.add(reason -> this.onError(new TransportException(reason)));
    };
    this.upgrading = true;
    upgradeTimeoutTimer = vertx.setTimer(options.getUpgradeTimeout(), aLong -> {
      LOGGER.debug("client did not complete upgrade - closing transport.");
      cleanup.handle(null);
      if (state == OPEN) transport.close(null);
    });

    Handler<Throwable> onError = e -> {
      LOGGER.debug("client did not complete upgrade", e);
      cleanup.handle(null);
      transport.close(null);
    };

    transport.addPacketHandler(packet -> {
      if (packet.getType() == PacketType.PING && packet.getData().toString().equals("probe")) {
        transport.send(new Packet(PacketType.PONG, "probe"));
        vertx.cancelTimer(checkIntervalTimer);
        checkIntervalTimer = vertx.setTimer(100L, aLong -> {
          if (this.transport.name().equals("polling") && this.transport.writable()) {
            LOGGER.debug("writing a noop packet to polling for fast upgrade");
            this.transport.send(new Packet(PacketType.NOOP));
          }
        });
      } else if (packet.getType() == PacketType.UPGRADE && state != CLOSED) {
        LOGGER.debug("got upgrade packet - upgrading");
        cleanup.handle(null);
        this.transport.discard();
        this.upgraded = true;
        this.cleanTransport();
        this.setTransport(transport);
        //we skip upgrade notify.
        this.setPingTimeout();
        this.flush();
        if (state == CLOSING) {
          transport.close(aVoid -> this.onClose("force close."));
        }
      } else {
        cleanup.handle(null);
        transport.close(null);
      }
    });
    transport.addCloseHandler(aVoid -> onError.handle(new TransportException("transport closed.")));
    transport.addErrorHandler(onError);
  }

  @Override
  public boolean isUpgrading() {
    return upgrading;
  }

  @Override
  public boolean isUpgraded() {
    return upgraded;
  }
}
