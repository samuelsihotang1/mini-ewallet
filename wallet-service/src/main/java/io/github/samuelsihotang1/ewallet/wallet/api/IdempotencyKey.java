package io.github.samuelsihotang1.ewallet.wallet.api;

/** The value of an {@code Idempotency-Key} header: 1 to 100 visible ASCII characters. */
record IdempotencyKey(String value) {

  private static final int MAX_LENGTH = 100;

  IdempotencyKey {
    if (value == null || value.isBlank() || value.length() > MAX_LENGTH
        || !value.chars().allMatch(c -> c > 0x20 && c < 0x7f)) {
      throw new IllegalArgumentException(
          "Idempotency-Key must be 1 to " + MAX_LENGTH + " visible ASCII characters");
    }
  }
}
