-- One journal per saga step per transfer: the database refuses a second debit for the same
-- transfer even if the service's own check is bypassed. Top-ups reference their unique id.
ALTER TABLE journal ADD CONSTRAINT uk_journal_type_reference UNIQUE (type, reference);

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
