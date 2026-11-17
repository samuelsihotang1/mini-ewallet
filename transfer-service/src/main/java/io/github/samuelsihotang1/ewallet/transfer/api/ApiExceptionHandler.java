package io.github.samuelsihotang1.ewallet.transfer.api;

import io.github.samuelsihotang1.ewallet.transfer.application.WalletDirectoryUnavailableException;
import io.github.samuelsihotang1.ewallet.transfer.domain.DomainException;
import io.github.samuelsihotang1.ewallet.transfer.domain.InvalidMoneyException;
import io.github.samuelsihotang1.ewallet.transfer.domain.TransferNotFoundException;
import io.github.samuelsihotang1.ewallet.transfer.domain.WalletNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns every rejected request into an RFC 7807 problem with a stable {@code code} property, the
 * value clients branch on; {@code detail} is for humans and may change.
 */
@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(DomainException.class)
  ResponseEntity<ProblemDetail> domain(DomainException e) {
    return problem(statusOf(e), e.code(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> invalidBody(MethodArgumentNotValidException e) {
    var detail =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .sorted()
            .toList();
    return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", String.join("; ", detail));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<ProblemDetail> missingHeader(MissingRequestHeaderException e) {
    if (TransferController.IDEMPOTENCY_KEY.equalsIgnoreCase(e.getHeaderName())) {
      return problem(
          HttpStatus.BAD_REQUEST,
          "IDEMPOTENCY_KEY_REQUIRED",
          "send an Idempotency-Key header so the request can be retried safely");
    }
    return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingRequestValueException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<ProblemDetail> malformed(Exception e) {
    return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage());
  }

  /**
   * A unique or check constraint caught a request that raced past the service's own check, such as
   * two simultaneous "open wallet" calls for one user.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ProblemDetail> conflict(DataIntegrityViolationException e) {
    log.warn("constraint violation", e);
    return problem(HttpStatus.CONFLICT, "CONFLICT", "the request conflicts with stored data");
  }

  /** The wallet lookup failed; the request stored nothing and can be retried as is. */
  @ExceptionHandler(WalletDirectoryUnavailableException.class)
  ResponseEntity<ProblemDetail> walletServiceDown(WalletDirectoryUnavailableException e) {
    log.warn("wallet service unavailable", e);
    return problem(HttpStatus.SERVICE_UNAVAILABLE, "WALLET_SERVICE_UNAVAILABLE",
        "the wallet service did not answer; retry the request with the same Idempotency-Key");
  }

  private static HttpStatus statusOf(DomainException e) {
    return switch (e) {
      case WalletNotFoundException ignored -> HttpStatus.NOT_FOUND;
      case TransferNotFoundException ignored -> HttpStatus.NOT_FOUND;
      case InvalidMoneyException ignored -> HttpStatus.BAD_REQUEST;
      default -> HttpStatus.UNPROCESSABLE_ENTITY;
    };
  }

  private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty("code", code);
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
  }
}
