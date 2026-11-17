package io.github.samuelsihotang1.ewallet.notification.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Errors as RFC 7807 problem documents with a stable {@code code}, as in the other services. */
@RestControllerAdvice
class ApiExceptionHandler {

  @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
  ResponseEntity<ProblemDetail> malformed(Exception e) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    problem.setProperty("code", "INVALID_REQUEST");
    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
