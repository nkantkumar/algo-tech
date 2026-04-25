CREATE TABLE IF NOT EXISTS trade_history (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    trader_id VARCHAR(64) NOT NULL,
    instrument_id VARCHAR(64) NOT NULL,
    quantity BIGINT NOT NULL,
    price NUMERIC(18,6) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
