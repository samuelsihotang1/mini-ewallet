package io.github.samuelsihotang1.ewallet.events;

/** Kafka topic names. Each topic is owned and written by exactly one service. */
public final class Topics {

  /** Events about a transfer's state, written by the transfer service. */
  public static final String TRANSFER_EVENTS = "ewallet.transfer.events.v1";

  /** Events about money moving in wallets, written by the wallet service. */
  public static final String WALLET_EVENTS = "ewallet.wallet.events.v1";

  private Topics() {}
}
