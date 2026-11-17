Feature: Wallet-to-wallet transfer
  A transfer runs as a saga across the wallet, transfer and notification services.
  Every scenario runs against the real stack: MySQL, Kafka and the three services.

  Background:
    Given a wallet "alice" holding "100000.00"
    And a wallet "bob" holding "0.00"

  Scenario: A transfer moves the money and tells both users
    When "alice" transfers "40000.00" to "bob" with idempotency key "t-1"
    Then the transfer ends as "COMPLETED"
    And "alice" holds "60000.00"
    And "bob" holds "40000.00"
    And "alice" is told "You sent 40000.00 IDR to bob"
    And "bob" is told "You received 40000.00 IDR from alice"

  Scenario: A transfer above the balance fails and moves nothing
    When "alice" transfers "150000.00" to "bob" with idempotency key "t-2"
    Then the transfer ends as "FAILED" because "INSUFFICIENT_BALANCE"
    And "alice" holds "100000.00"
    And "bob" holds "0.00"
    And "alice" is told "failed: INSUFFICIENT_BALANCE"

  Scenario: A retried request moves the money once
    When "alice" transfers "40000.00" to "bob" with idempotency key "t-3"
    And "alice" retries the last transfer with the same idempotency key
    Then the retry is answered with 200 and the same transfer
    And the transfer ends as "COMPLETED"
    And "alice" holds "60000.00"
    And "bob" holds "40000.00"

  Scenario: A credit the destination refuses is refunded to the source
    Given a wallet "carol" holding "19999990.00"
    When "alice" transfers "50.00" to "carol" with idempotency key "t-4"
    Then the transfer ends as "COMPENSATED" because "BALANCE_LIMIT_EXCEEDED"
    And "alice" holds "100000.00"
    And "carol" holds "19999990.00"
    And "alice" is told "The money is back in your wallet."
