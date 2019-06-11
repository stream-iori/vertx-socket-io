package me.streamis.engine.io.server;

import io.vertx.core.Handler;
import me.streamis.engine.io.server.transport.AbsEIOTransport;

/**
 * Created by stream.
 */
interface EIOUpgradeSocket {

  void maybeUpgrade(final EIOTransport transport);

  void upgradeHandler(Handler<EIOTransport> handler);

  boolean isUpgrading();

  boolean isUpgraded();
}
