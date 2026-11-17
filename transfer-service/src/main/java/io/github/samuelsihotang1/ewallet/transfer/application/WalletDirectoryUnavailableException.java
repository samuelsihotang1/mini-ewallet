package io.github.samuelsihotang1.ewallet.transfer.application;

/** The wallet service did not answer in time or answered with an error; try again later. */
public class WalletDirectoryUnavailableException extends RuntimeException {

  public WalletDirectoryUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
