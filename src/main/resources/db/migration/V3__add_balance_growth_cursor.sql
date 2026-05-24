ALTER TABLE account
    ADD COLUMN last_balance_growth_at TIMESTAMP NOT NULL DEFAULT now();

CREATE INDEX idx_account_last_balance_growth_at_user_id
    ON account(last_balance_growth_at, user_id);
