package io.github.samuelsihotang1.ewallet.notification.domain;

/** What a notification tells its user about. */
public enum NotificationType {
  /** Money arrived from a top-up. */
  TOP_UP,
  /** The user's transfer reached its recipient. */
  TRANSFER_SENT,
  /** Someone's transfer reached the user. */
  TRANSFER_RECEIVED,
  /** The user's transfer did not go through. */
  TRANSFER_FAILED
}
