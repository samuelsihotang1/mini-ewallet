package io.github.samuelsihotang1.ewallet.transfer.api;

import io.github.samuelsihotang1.ewallet.transfer.api.TransferDtos.TransferRequest;
import io.github.samuelsihotang1.ewallet.transfer.api.TransferDtos.TransferResponse;
import io.github.samuelsihotang1.ewallet.transfer.application.RequestTransferCommand;
import io.github.samuelsihotang1.ewallet.transfer.application.RequestTransferService;
import io.github.samuelsihotang1.ewallet.transfer.application.TransferQueryService;
import io.github.samuelsihotang1.ewallet.transfer.domain.Money;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Wallet-to-wallet transfers. */
@RestController
@RequestMapping("/api/v1/transfers")
class TransferController {

  /** Header that makes a POST safe to retry. */
  static final String IDEMPOTENCY_KEY = "Idempotency-Key";

  /** Set to {@code true} on a response that replays an earlier request with the same key. */
  static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

  private final RequestTransferService requests;
  private final TransferQueryService queries;

  TransferController(RequestTransferService requests, TransferQueryService queries) {
    this.requests = requests;
    this.queries = queries;
  }

  @Operation(
      summary = "Transfer money to another wallet",
      description =
          "202 Accepted: the transfer runs as a saga; poll its Location until the status is"
              + " COMPLETED, FAILED or COMPENSATED. A retry with the same Idempotency-Key returns"
              + " the same transfer with 200 and Idempotent-Replayed: true.")
  @PostMapping
  ResponseEntity<TransferResponse> request(
      @Parameter(description = "Unique per source wallet; reuse it only to retry the same request")
          @RequestHeader(IDEMPOTENCY_KEY)
          IdempotencyKey idempotencyKey,
      @Valid @RequestBody TransferRequest request) {
    var receipt = requests.request(new RequestTransferCommand(
        request.sourceWalletId(),
        request.destinationWalletId(),
        Money.of(request.amount()),
        idempotencyKey.value()));
    var status = receipt.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED;
    return ResponseEntity.status(status)
        .location(URI.create("/api/v1/transfers/" + receipt.transfer().id()))
        .header(IDEMPOTENT_REPLAYED, String.valueOf(receipt.replayed()))
        .body(TransferResponse.of(receipt.transfer()));
  }

  @Operation(summary = "A transfer and its saga status")
  @GetMapping("/{transferId}")
  TransferResponse get(@PathVariable TransferId transferId) {
    return TransferResponse.of(queries.get(transferId));
  }
}
