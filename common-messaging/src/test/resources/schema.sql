-- The same tables each service creates in its own Flyway migration.
CREATE TABLE IF NOT EXISTS outbox_event (
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

CREATE TABLE IF NOT EXISTS processed_event (
    event_id     CHAR(36)     NOT NULL,
    consumer     VARCHAR(64)  NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id, consumer)
);
