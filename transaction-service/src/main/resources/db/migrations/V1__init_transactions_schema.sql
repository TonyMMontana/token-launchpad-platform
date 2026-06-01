CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              user_id UUID NOT NULL,
                              campaign_id BIGINT NOT NULL,
                              amount DECIMAL(19, 2) NOT NULL,
                              status VARCHAR(50) NOT NULL,
                              idempotency_key UUID NOT NULL,
                              created_at TIMESTAMP NOT NULL,
                              updated_at TIMESTAMP NOT NULL,
                              CONSTRAINT uk_idempotency_key_and_user_id UNIQUE (user_id, idempotency_key)
);

CREATE TABLE outbox_events (
                               id BIGSERIAL PRIMARY KEY,
                               aggregate_id BIGINT NOT NULL,
                               event_type VARCHAR(255) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(50) NOT NULL,
                               retry_count INT NOT NULL DEFAULT 0,
                               created_at TIMESTAMP NOT NULL,
                               next_attempt_at TIMESTAMP NOT NULL,
                               published_at TIMESTAMP,
                               last_error TEXT
);