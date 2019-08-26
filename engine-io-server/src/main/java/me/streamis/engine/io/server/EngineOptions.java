package me.streamis.engine.io.server;


import me.streamis.engine.io.parser.Packet;
import me.streamis.engine.io.parser.PacketType;
import me.streamis.engine.io.server.EIOTransport.Type;

import java.util.*;

/**
 * Created by stream.
 */
public class EngineOptions {
  private String wsEngine = "ws";
  private long pingTimeout = 5000;
  private long pingInterval = 25000;
  private long upgradeTimeout = 10000;
  private String path = "/engine.io/";
  private int maxHttpBufferSize = Integer.MAX_VALUE;
  private String[] allowedCorsOrigins = new String[0];
  private String initialPacket;
  private boolean handlePreflightRequest = true;
  private Map<String, List<String>> transports = new HashMap<String, List<String>>() {{
    put(Type.POLLING.name().toLowerCase(), Arrays.asList(Type.WEBSOCKET.name().toLowerCase()));
    put(Type.WEBSOCKET.name().toLowerCase(), new ArrayList<>(0));
  }};
  private boolean allowUpgrades = true;
  private String cookie = "io";
  private String cookiePath = "/";
  private boolean cookieHttpOnly = true;
  //allowRequest
  //perMessageDeflate
  //httpCompression

  public EngineOptions setWsEngine(String wsEngine) {
    this.wsEngine = wsEngine;
    return this;
  }

  public EngineOptions setPingTimeout(long pingTimeout) {
    this.pingTimeout = pingTimeout;
    return this;
  }

  public EngineOptions setPingInterval(long pingInterval) {
    this.pingInterval = pingInterval;
    return this;
  }

  public EngineOptions setAllowedCorsOrigins(String[] corsOrigins) {
    if (corsOrigins != null) {
      allowedCorsOrigins = new String[corsOrigins.length];
      System.arraycopy(corsOrigins, 0, allowedCorsOrigins, 0, corsOrigins.length);
      Arrays.sort(allowedCorsOrigins, String::compareTo);
    }
    return this;
  }

  public EngineOptions setPath(String path) {
    this.path = path;
    return this;
  }

  public EngineOptions setInitialPacket(String initialPacket) {
    this.initialPacket = initialPacket;
    return this;
  }

  public EngineOptions setUpgradeTimeout(long upgradeTimeout) {
    this.upgradeTimeout = upgradeTimeout;
    return this;
  }

  public EngineOptions setMaxHttpBufferSize(int maxHttpBufferSize) {
    this.maxHttpBufferSize = maxHttpBufferSize;
    return this;
  }

  public EngineOptions setTransports(Map<String, List<String>> transports) {
    this.transports = transports;
    return this;
  }

  public EngineOptions setAllowUpgrades(boolean allowUpgrades) {
    this.allowUpgrades = allowUpgrades;
    return this;
  }

  public EngineOptions setCookie(String cookie) {
    this.cookie = cookie;
    return this;
  }

  public EngineOptions setCookiePath(String cookiePath) {
    this.cookiePath = cookiePath;
    return this;
  }

  public EngineOptions setCookieHttpOnly(boolean cookieHttpOnly) {
    this.cookieHttpOnly = cookieHttpOnly;
    return this;
  }

  public String getWsEngine() {
    return wsEngine;
  }

  public long getPingTimeout() {
    return pingTimeout;
  }

  public long getPingInterval() {
    return pingInterval;
  }

  public String[] getAllowedCorsOrigins() {
    return allowedCorsOrigins;
  }

  public String getInitialPacket() {
    return initialPacket;
  }

  public String getPath() {
    return path;
  }

  public long getUpgradeTimeout() {
    return upgradeTimeout;
  }

  public int getMaxHttpBufferSize() {
    return maxHttpBufferSize;
  }

  public Map<String, List<String>> getTransports() {
    return transports;
  }

  public boolean isAllowUpgrades() {
    return allowUpgrades;
  }

  public String getCookie() {
    return cookie;
  }

  public String getCookiePath() {
    return cookiePath;
  }

  public boolean isCookieHttpOnly() {
    return cookieHttpOnly;
  }

  public boolean isHandlePreflightRequest() {
    return handlePreflightRequest;
  }

  public EngineOptions toBuild() {
    return this;
  }

  @Override
  public String toString() {
    return "EngineOptions{" +
      "wsEngine='" + wsEngine + '\'' +
      ", pingTimeout=" + pingTimeout +
      ", pingInterval=" + pingInterval +
      ", upgradeTimeout=" + upgradeTimeout +
      ", path='" + path + '\'' +
      ", maxHttpBufferSize=" + maxHttpBufferSize +
      ", allowedCorsOrigins=" + Arrays.toString(allowedCorsOrigins) +
      ", initialPacket='" + initialPacket + '\'' +
      ", handlePreflightRequest=" + handlePreflightRequest +
      ", transports=" + transports +
      ", allowUpgrades=" + allowUpgrades +
      ", cookie='" + cookie + '\'' +
      ", cookiePath='" + cookiePath + '\'' +
      ", cookieHttpOnly=" + cookieHttpOnly +
      '}';
  }
}
