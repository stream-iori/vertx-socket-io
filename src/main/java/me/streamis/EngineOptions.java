package me.streamis;

import me.streamis.EIOTransport.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by stream.
 */
public class EngineOptions {
  private String wsEngine = "ws";
  private long pingTimeout = 5000;
  private long pingInterval = 25000;
  private long upgradeTimeout = 10000;
  private int maxHttpBufferSize = Integer.MAX_VALUE;
  private Map<String, String[]> transports = new HashMap<String, String[]>() {{
    put(Type.POLLING.name(), new String[]{Type.WEBSOCKET.name()});
    put(Type.WEBSOCKET.name(), new String[0]);
  }};
  private boolean allowUpgrades = false;
  private boolean cookie = false;
  private String cookiePath = "/";
  private boolean cookieHttpOnly = false;
  //allowRequest
  //perMessageDeflate
  //httpCompression
  //initialPacket

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

  public EngineOptions setUpgradeTimeout(long upgradeTimeout) {
    this.upgradeTimeout = upgradeTimeout;
    return this;
  }

  public EngineOptions setMaxHttpBufferSize(int maxHttpBufferSize) {
    this.maxHttpBufferSize = maxHttpBufferSize;
    return this;
  }

  public EngineOptions setTransports(Map<String, String[]> transports) {
    this.transports = transports;
    return this;
  }

  public EngineOptions setAllowUpgrades(boolean allowUpgrades) {
    this.allowUpgrades = allowUpgrades;
    return this;
  }

  public EngineOptions setCookie(boolean cookie) {
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

  public long getUpgradeTimeout() {
    return upgradeTimeout;
  }

  public int getMaxHttpBufferSize() {
    return maxHttpBufferSize;
  }

  public Map<String,String[]> getTransports() {
    return transports;
  }

  public boolean isAllowUpgrades() {
    return allowUpgrades;
  }

  public boolean isCookie() {
    return cookie;
  }

  public String getCookiePath() {
    return cookiePath;
  }

  public boolean isCookieHttpOnly() {
    return cookieHttpOnly;
  }

  public EngineOptions toBuild() {
    return this;
  }

  public static void main(String[] args) {
    System.out.println(new EngineOptions().toBuild().getCookiePath());
  }
}
