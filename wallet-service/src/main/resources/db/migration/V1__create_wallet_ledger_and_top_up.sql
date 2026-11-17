-- Wallets hold a cached balance; ledger_entry is the source of truth it must match.
CREATE TABLE wallet (
    id          CHAR(36)       NOT NULL,
    owner_id    VARCHAR(64)    NOT NULL,
    currency    CHAR(3)        NOT NULL,
    balance     DECIMAL(19, 2) NOT NULL,
    version     BIGINT         NOT NULL,
    created_at  TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wallet_owner UNIQUE (owner_id),
    -- Last line of defence: even a bug in the service cannot store a negative balance.
    CONSTRAINT ck_wallet_balance_not_negative CHECK (balance >= 0)
);

-- One balanced posting. Rows here and in ledger_entry are never updated or deleted.
CREATE TABLE journal (
    id         CHAR(36)     NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    reference  VARCHAR(64)  NOT NULL,
    posted_at  TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);

-- account_id is a wallet id or a system account id, so it has no foreign key to wallet.
CREATE TABLE ledger_entry (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    journal_id     CHAR(36)       NOT NULL,
    account_id     CHAR(36)       NOT NULL,
    direction      VARCHAR(6)     NOT NULL,
    amount         DECIMAL(19, 2) NOT NULL,
    balance_after  DECIMAL(19, 2) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ledger_entry_journal FOREIGN KEY (journal_id) REFERENCES journal (id),
    CONSTRAINT ck_ledger_entry_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_ledger_entry_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    INDEX ix_ledger_entry_account (account_id, id)
);

-- Idempotency: a key is unique per wallet; a retry reads the stored result back.
CREATE TABLE top_up (
    id               CHAR(36)       NOT NULL,
    wallet_id        CHAR(36)       NOT NULL,
    idempotency_key  VARCHAR(100)   NOT NULL,
    amount           DECIMAL(19, 2) NOT NULL,
    journal_id       CHAR(36)       NOT NULL,
    balance_after    DECIMAL(19, 2) NOT NULL,
    created_at       TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_top_up_wallet_key UNIQUE (wallet_id, idempotency_key),
    CONSTRAINT fk_top_up_wallet FOREIGN KEY (wallet_id) REFERENCES wallet (id),
    CONSTRAINT fk_top_up_journal FOREIGN KEY (journal_id) REFERENCES journal (id)
);
