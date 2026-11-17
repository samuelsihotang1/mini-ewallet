package io.github.samuelsihotang1.ewallet.transfer.infrastructure;

import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.client.RestClient;

/** Looks wallets up over the wallet service's REST API. */
public class HttpWalletDirectory implements WalletDirectory {

  public HttpWalletDirectory(RestClient wallets) {}

  @Override
  public Optional<String> ownerOf(UUID walletId) {
    throw new UnsupportedOperationException("not implemented");
  }
}
