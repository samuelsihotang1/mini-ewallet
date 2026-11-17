-- One row per transfer; status follows the saga (see TransferStatus).
CREATE TABLE transfer (
    id                    CHAR(36)       NOT NULL,
    source_wallet_id      CHAR(36)       NOT NULL,
    source_owner_id       VARCHAR(64)    NOT NULL,
    destination_wallet_id CHAR(36)       NOT NULL,
    destination_owner_id  VARCHAR(64)    NOT NULL,
    amount                DECIMAL(19, 2) NOT NULL,
    idempotency_key       VARCHAR(100)   NOT NULL,
    status                VARCHAR(16)    NOT NULL,
    failure_reason        VARCHAR(64)    NULL,
    created_at            TIMESTAMP(6)   NOT NULL,
    updated_at            TIMESTAMP(6)   NOT NULL,
    version               BIGINT         NOT NULL,
    PRIMARY KEY (id),
    -- Idempotency: a key is unique per source wallet; a retry reads the stored transfer back.
    CONSTRAINT uk_transfer_source_key UNIQUE (source_wallet_id, idempotency_key),
    CONSTRAINT ck_transfer_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transfer_two_wallets CHECK (source_wallet_id <> destination_wallet_id),
    CONSTRAINT ck_transfer_status CHECK (status IN
        ('PENDING', 'DEBITED', 'COMPLETED', 'FAILED', 'COMPENSATING', 'COMPENSATED')),
    -- Finds sagas that have not moved for a while.
    INDEX ix_transfer_status_updated (status, updated_at)
);

-- Transactional outbox: events committed with the change they report; a relay sends them.
CREATE TABLE outbox_event (
    seq           BIGINT       NOT NULL AUTO_INCREMENT,
    id            CHAR(36)     NOT NULL,
    topic         VARCHAR(128) NOT NULL,
    partition_key VARCHAR(64)  NOT NULL,
    event_type    VARCHAR(64)  NOT NULL,
    payload       JSON         NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    published_at  TIMESTAMP(6) NULL,
    PRIMARY KEY (seq),
    CONSTRAINT uk_outbox_event_id UNIQUE (id),
    INDEX ix_outbox_event_unpublished (published_at, seq)
);

-- Idempotent consumer: an event id handled once per consumer.
CREATE TABLE processed_event (
    event_id     CHAR(36)     NOT NULL,
    consumer     VARCHAR(64)  NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id, consumer)
);
