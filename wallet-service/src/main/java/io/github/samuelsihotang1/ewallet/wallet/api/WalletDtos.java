package io.github.samuelsihotang1.ewallet.wallet.api;

import io.github.samuelsihotang1.ewallet.wallet.application.Mutation;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpReceipt;
import io.github.samuelsihotang1.ewallet.wallet.application.WalletView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Request and response bodies of the wallet API. Amounts travel as decimal strings so that no
 * client turns them into binary floating point.
 */
final class WalletDtos {

  private WalletDtos() {}

  record OpenWalletRequest(
      @Schema(example = "user-42") @NotBlank @Size(max = 64) String userId) {}

  record TopUpRequest(
      @Schema(example = "150000.00", description = "IDR, at most two decimals") @NotBlank
          String amount) {}

  record WalletResponse(String walletId, String userId, String balance, String currency) {

    static WalletResponse of(WalletView view) {
      return new WalletResponse(
          view.id().toString(),
          view.ownerId(),
          view.balance().amount().toPlainString(),
          view.balance().currency().getCurrencyCode());
    }
  }

  record TopUpResponse(
      String topUpId, String walletId, String amount, String balanceAfter, Instant createdAt) {

    static TopUpResponse of(TopUpReceipt receipt) {
      return new TopUpResponse(
          receipt.topUpId().toString(),
          receipt.walletId().toString(),
          receipt.amount().amount().toPlainString(),
          receipt.balanceAfter().amount().toPlainString(),
          receipt.createdAt());
    }
  }

  record TransactionResponse(
      String journalId,
      String type,
      String direction,
      String amount,
      String balanceAfter,
      String reference,
      Instant postedAt) {

    static TransactionResponse of(Mutation mutation) {
      return new TransactionResponse(
          mutation.journalId().toString(),
          mutation.type().name(),
          mutation.direction().name(),
          mutation.amount().amount().toPlainString(),
          mutation.balanceAfter().amount().toPlainString(),
          mutation.reference(),
          mutation.postedAt());
    }
  }
}
