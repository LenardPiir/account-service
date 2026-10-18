CREATE TABLE account (
    id          UUID PRIMARY KEY,
    owner_name  VARCHAR(255) NOT NULL,
    version     BIGINT NOT NULL
);

CREATE TABLE balance (
    account_id  UUID NOT NULL REFERENCES account(id),
    currency    VARCHAR(3) NOT NULL,
    amount      DECIMAL(19, 4) NOT NULL,
    PRIMARY KEY (account_id, currency),
    CONSTRAINT chk_balance_non_negative CHECK (amount >= 0)
);
