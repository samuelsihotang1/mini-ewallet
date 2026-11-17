package io.github.samuelsihotang1.ewallet.transfer.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectoryUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpWalletDirectoryTest {

  private final RestClient.Builder builder = RestClient.builder().baseUrl("http://wallet");
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  private final HttpWalletDirectory directory = new HttpWalletDirectory(builder.build());
  private final UUID walletId = UUID.randomUUID();

  @Test
  void a_known_wallet_returns_its_owner() {
    server.expect(requestTo("http://wallet/api/v1/wallets/" + walletId))
        .andRespond(withSuccess(
            """
            {"walletId":"%s","userId":"user-42","balance":"0.00","currency":"IDR"}"""
                .formatted(walletId),
            MediaType.APPLICATION_JSON));

    assertThat(directory.ownerOf(walletId)).contains("user-42");
  }

  @Test
  void an_unknown_wallet_is_empty() {
    server.expect(requestTo("http://wallet/api/v1/wallets/" + walletId))
        .andRespond(withResourceNotFound());

    assertThat(directory.ownerOf(walletId)).isEmpty();
  }

  @Test
  void a_server_error_means_the_directory_is_unavailable() {
    server.expect(requestTo("http://wallet/api/v1/wallets/" + walletId))
        .andRespond(withServerError());

    assertThatThrownBy(() -> directory.ownerOf(walletId))
        .isInstanceOf(WalletDirectoryUnavailableException.class);
  }
}
