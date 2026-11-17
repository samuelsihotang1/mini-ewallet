-- One row per user per event; the unique key backs the inbox against a double notification.
CREATE TABLE notification (
    id          CHAR(36)     NOT NULL,
    user_id     VARCHAR(64)  NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    message     VARCHAR(255) NOT NULL,
    event_id    CHAR(36)     NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_event_user UNIQUE (event_id, user_id),
    INDEX ix_notification_user_created (user_id, created_at)
);

-- Idempotent consumer: an event id handled once per consumer.
CREATE TABLE processed_event (
    event_id     CHAR(36)     NOT NULL,
    consumer     VARCHAR(64)  NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (event_id, consumer)
);

-- Records parked on this service's dead-letter topics, for an operator.
CREATE TABLE dead_letter (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    original_topic VARCHAR(128) NOT NULL,
    record_key     VARCHAR(128) NULL,
    payload        TEXT         NULL,
    error          TEXT         NULL,
    received_at    TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);
