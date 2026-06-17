UPDATE campaigns
SET tokens_sold = 0
WHERE tokens_sold IS NULL;

ALTER TABLE campaigns
    ALTER COLUMN tokens_sold SET DEFAULT 0,
    ALTER COLUMN tokens_sold SET NOT NULL;
