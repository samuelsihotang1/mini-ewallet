package io.github.samuelsihotang1.ewallet.transfer.api;

import io.github.samuelsihotang1.ewallet.transfer.application.TransferView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** Request and response bodies; amounts are decimal strings so no client parses a float. */
final class TransferDtos {

  private TransferDtos() {}

  record TransferRequest(
      @NotNull UUID sourceWalletId,
      @NotNull UUID destinationWalletId,
      @NotBlank String amount) {}

  record TransferResponse(
      String transferId,
      String sourceWalletId,
      String destinationWalletId,
      String amount,
      String currency,
      String status,
      String failureReason,
      Instant createdAt,
      Instant updatedAt) {

    static TransferResponse of(TransferView view) {
      return new TransferResponse(
          view.id().toString(),
          view.sourceWalletId().toString(),
          view.destinationWalletId().toString(),
          view.amount().amount().toPlainString(),
          view.amount().currency().getCurrencyCode(),
          view.status().name(),
          view.failureReason(),
          view.createdAt(),
          view.updatedAt());
    }
  }
}
