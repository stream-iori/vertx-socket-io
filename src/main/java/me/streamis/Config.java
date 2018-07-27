package me.streamis;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static me.streamis.EIOTransport.*;
import static me.streamis.EIOTransport.Type.POLLING;
import static me.streamis.EIOTransport.Type.WEBSOCKET;

/**
 * Created by stream.
 */
class Config {

  long PING_INTERVAL = TimeUnit.SECONDS.toMillis(25);
  long PING_TIMEOUT = TimeUnit.SECONDS.toMillis(60);
  long UPGRADE_TIMEOUT = TimeUnit.SECONDS.toMillis(15);
  boolean COOKIE = true;
  boolean COOKIE_HTTP_ONLY = true;
  String COOKIE_PATH = "/";

  boolean ALLOW_UPGRADES = true;
  List<Type> allowTransports = Arrays.asList(POLLING, WEBSOCKET);
  Map<String, List<Type>> transportAllowUpgrade = new HashMap<String, List<Type>>() {{
    put(POLLING.name(), Collections.singletonList(WEBSOCKET));
  }};
}
