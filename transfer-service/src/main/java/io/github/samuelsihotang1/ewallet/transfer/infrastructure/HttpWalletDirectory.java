package io.github.samuelsihotang1.ewallet.transfer.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectory;
import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectoryUnavailableException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Looks wallets up over the wallet service's REST API. The client is built with short timeouts
 * (see {@code TransferConfiguration}): a transfer request waits for this answer.
 */
public class HttpWalletDirectory implements WalletDirectory {

  private final RestClient wallets;

  public HttpWalletDirectory(RestClient wallets) {
    this.wallets = wallets;
  }

  @Override
  public Optional<String> ownerOf(UUID walletId) {
    try {
      var wallet = wallets.get().uri("/api/v1/wallets/{id}", walletId)
          .retrieve()
          .body(WalletResponse.class);
      return Optional.ofNullable(wallet).map(WalletResponse::userId);
    } catch (HttpClientErrorException.NotFound unknown) {
      return Optional.empty();
    } catch (RestClientException e) {
      throw new WalletDirectoryUnavailableException(
          "wallet service did not answer for wallet " + walletId, e);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record WalletResponse(String userId) {}
}
