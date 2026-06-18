ALTER TABLE campaigns
    ALTER COLUMN start_time TYPE TIMESTAMP(6) WITH TIME ZONE
        USING start_time AT TIME ZONE 'Europe/Berlin';

ALTER TABLE campaign_reservations
    ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE outbox_events
    ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN next_attempt_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING next_attempt_at AT TIME ZONE 'UTC',
    ALTER COLUMN set_next_attempt_at TYPE TIMESTAMP(6) WITH TIME ZONE
        USING set_next_attempt_at AT TIME ZONE 'UTC';
