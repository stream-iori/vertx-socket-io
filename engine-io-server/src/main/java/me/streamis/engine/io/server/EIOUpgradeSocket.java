package me.streamis.engine.io.server;

import io.vertx.core.Handler;

/**
 * Created by stream.
 */
interface EIOUpgradeSocket {
  
  void maybeUpgrade(EIOTransport transport);

  void upgradeHandler(Handler<EIOTransport> handler);

  boolean isUpgrading();

  boolean isUpgraded();
}
