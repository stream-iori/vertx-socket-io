package me.streamis.engine.io.server;

/**
 * Created by stream.
 */
interface EIOUpgradeSocket {

  void maybeUpgrade(final EIOTransport transport);

  boolean isUpgrading();

  boolean isUpgraded();
}
