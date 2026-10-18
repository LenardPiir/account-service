CREATE TABLE processed_request (
    id               UUID PRIMARY KEY,
    account_id       UUID NOT NULL REFERENCES account(id),
    idempotency_key  VARCHAR(64) NOT NULL,
    fingerprint      VARCHAR(255) NOT NULL,
    result           DECIMAL(19, 4) NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_processed_request_key UNIQUE (account_id, idempotency_key)
);

CREATE INDEX idx_processed_request_created_at ON processed_request (created_at);
