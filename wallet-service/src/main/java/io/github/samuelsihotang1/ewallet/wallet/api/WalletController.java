package io.github.samuelsihotang1.ewallet.wallet.api;

import io.github.samuelsihotang1.ewallet.wallet.api.WalletDtos.OpenWalletRequest;
import io.github.samuelsihotang1.ewallet.wallet.api.WalletDtos.TopUpRequest;
import io.github.samuelsihotang1.ewallet.wallet.api.WalletDtos.TopUpResponse;
import io.github.samuelsihotang1.ewallet.wallet.api.WalletDtos.TransactionResponse;
import io.github.samuelsihotang1.ewallet.wallet.api.WalletDtos.WalletResponse;
import io.github.samuelsihotang1.ewallet.wallet.application.OpenWalletService;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpCommand;
import io.github.samuelsihotang1.ewallet.wallet.application.TopUpService;
import io.github.samuelsihotang1.ewallet.wallet.application.WalletQueryService;
import io.github.samuelsihotang1.ewallet.wallet.domain.Money;
import io.github.samuelsihotang1.ewallet.wallet.domain.WalletId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public REST API of the wallet service. */
@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallets", description = "Open a wallet, top it up, read its balance and history")
class WalletController {

  /** Header that makes a POST safe to retry; see {@link TopUpService#topUp}. */
  static final String IDEMPOTENCY_KEY = "Idempotency-Key";

  /** Set to {@code true} on a response that replays an earlier request with the same key. */
  static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

  private final OpenWalletService openWallet;
  private final TopUpService topUps;
  private final WalletQueryService queries;

  WalletController(OpenWalletService openWallet, TopUpService topUps, WalletQueryService queries) {
    this.openWallet = openWallet;
    this.topUps = topUps;
    this.queries = queries;
  }

  @Operation(summary = "Open the user's wallet (one per user)")
  @PostMapping
  ResponseEntity<WalletResponse> open(@Valid @RequestBody OpenWalletRequest request) {
    var wallet = openWallet.open(request.userId());
    return ResponseEntity.created(URI.create("/api/v1/wallets/" + wallet.id()))
        .body(WalletResponse.of(wallet));
  }

  @Operation(summary = "Balance of a wallet")
  @GetMapping("/{walletId}")
  WalletResponse get(@PathVariable WalletId walletId) {
    return WalletResponse.of(queries.get(walletId));
  }

  @Operation(
      summary = "Top up a wallet",
      description =
          "201 for a new top-up. A retry with the same Idempotency-Key and amount returns the"
              + " first receipt with 200 and Idempotent-Replayed: true; another amount is 422.")
  @PostMapping("/{walletId}/top-ups")
  ResponseEntity<TopUpResponse> topUp(
      @PathVariable WalletId walletId,
      @Parameter(description = "Unique per wallet; reuse it only to retry the same request")
          @RequestHeader(IDEMPOTENCY_KEY)
          IdempotencyKey idempotencyKey,
      @Valid @RequestBody TopUpRequest request) {
    var receipt =
        topUps.topUp(new TopUpCommand(walletId, Money.of(request.amount()), idempotencyKey.value()));
    var status = receipt.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
    return ResponseEntity.status(status)
        .header(IDEMPOTENT_REPLAYED, String.valueOf(receipt.replayed()))
        .body(TopUpResponse.of(receipt));
  }

  @Operation(summary = "Transaction history, newest first")
  @GetMapping("/{walletId}/transactions")
  List<TransactionResponse> transactions(
      @PathVariable WalletId walletId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return queries.mutations(walletId, page, size).stream().map(TransactionResponse::of).toList();
  }
}
