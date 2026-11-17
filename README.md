# mini-ewallet

A backend for an e-wallet: three Spring Boot services that keep balances in a
double-entry ledger and move money between wallets with a Kafka saga. This page
is for developers and reviewers who want to run it and see how it works.

**Status (2026-11-17):** the wallet service is complete: wallets, top-up,
balance and history. The transfer saga and notifications are in progress.

## Stack

Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, MySQL 8.4, Kafka 4.3 (KRaft),
JUnit 5, AssertJ, Testcontainers, springdoc-openapi, Docker Compose.

## Run it

Prerequisites: Docker with Compose v2.

```bash
docker compose up -d --build --wait
```

This starts MySQL, Kafka and the three services. The wallet API and its Swagger
UI are at `http://localhost:8081/swagger-ui/index.html`.

| Service | Port | State |
|---|---|---|
| wallet-service | 8081 | wallets, top-up, balance, history |
| transfer-service | 8082 | health check only, saga in progress |
| notification-service | 8083 | health check only, in progress |

## Try the API

Open a wallet:

```bash
curl -s -X POST localhost:8081/api/v1/wallets \
  -H 'Content-Type: application/json' -d '{"userId":"user-42"}'
```

```json
{"walletId":"2dca2728-5b6e-4911-b85d-7342673b73be","userId":"user-42","balance":"0.00","currency":"IDR"}
```

Top it up. The `Idempotency-Key` header makes the request safe to retry:

```bash
curl -s -X POST localhost:8081/api/v1/wallets/<walletId>/top-ups \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: tu-001' \
  -d '{"amount":"150000.00"}'
```

| Request | Response |
|---|---|
| first call | `201 Created`, the receipt |
| same key, same amount | `200 OK`, the same receipt, `Idempotent-Replayed: true`, no second credit |
| same key, other amount | `422`, `"code":"IDEMPOTENCY_KEY_REUSED"` |
| amount `10.005` | `400`, `"code":"INVALID_AMOUNT"` |
| amount above 10,000,000.00 | `422`, `"code":"TOP_UP_LIMIT_EXCEEDED"` |

Read the balance and the history, newest first:

```bash
curl -s localhost:8081/api/v1/wallets/<walletId>
curl -s "localhost:8081/api/v1/wallets/<walletId>/transactions?page=0&size=20"
```

Amounts are decimal strings with at most two decimals, so no client parses
them into a float. Errors are RFC 7807 problem documents with a stable `code`.

## How the money is kept right

- **Double-entry ledger.** Every movement is a journal whose debits equal its
  credits. A top-up debits the system account `TOP_UP_SOURCE` and credits the
  wallet. Ledger rows are only ever inserted. The wallet's `balance` column is
  a cached sum, updated in the same transaction as its entries, and a test
  checks that the two agree.
- **No hot row.** System accounts keep no stored balance and are never locked,
  so top-ups to different wallets never queue behind one shared row.
- **Locking.** A top-up locks the wallet row (`SELECT ... FOR UPDATE`) for the
  length of its transaction. `@Version` is a second guard against a write that
  skipped the lock.
- **Idempotency.** `top_up` has `UNIQUE (wallet_id, idempotency_key)`. The
  service locks the wallet before it looks the key up, under READ COMMITTED,
  so two retries of one key run one after the other and the second returns
  the stored receipt.
- **The database has the last word.** `CHECK (balance >= 0)`,
  `CHECK (amount > 0)` and `UNIQUE (owner_id)` reject a bad write even if the
  service has a bug.
- **Domain without a framework.** `Money` (BigDecimal, scale 2, IDR, no silent
  rounding), `Wallet` and `Journal` are plain Java. Only a `Journal` can change
  a balance.

## Tests

Every behavior was written test first: each `test(...)` commit holds the
failing tests, and the `feat(...)` commit after it makes them pass.

```bash
./mvnw verify               # unit + integration tests (Docker must be running)
bash scripts/ci-local.sh    # the CI: compose check, build, tests, coverage
```

| Level | Runs on | Example |
|---|---|---|
| Domain and application unit tests | plain JVM, in-memory fakes | `TopUpServiceTest`, `JournalTest` |
| Integration tests | real MySQL 8.4 via Testcontainers | `WalletPersistenceIT`, `WalletApiIT` |

The build fails when line coverage of the `domain` and `application` packages
drops below 85 %.

## Layout

```
common-events/          Kafka event contracts shared by the services
wallet-service/         domain/ application/ infrastructure/ api/ per DDD layer
transfer-service/
notification-service/
docker/mysql/init.sql   one schema and one least-privilege user per service
scripts/ci-local.sh     the CI, run locally before every push
```
