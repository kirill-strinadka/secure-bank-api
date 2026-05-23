CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    date_of_birth DATE NOT NULL,
    password VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    balance NUMERIC(19, 2) NOT NULL,
    initial_balance NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT account_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT account_initial_balance_non_negative CHECK (initial_balance >= 0)
);

CREATE TABLE email_data (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    email VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE phone_data (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    phone VARCHAR(13) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE transfer (
    id BIGSERIAL PRIMARY KEY,
    from_user_id BIGINT NOT NULL REFERENCES users(id),
    to_user_id BIGINT NOT NULL REFERENCES users(id),
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT transfer_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_email_data_user_id ON email_data(user_id);
CREATE INDEX idx_phone_data_user_id ON phone_data(user_id);
CREATE INDEX idx_transfer_from_user_id ON transfer(from_user_id);
CREATE INDEX idx_transfer_to_user_id ON transfer(to_user_id);
