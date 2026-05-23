INSERT INTO users (id, name, date_of_birth, password)
VALUES
    (1, 'Ivan Petrov', '1993-05-01', '$2a$10$0a6lf9Q8GyViiNbafds5DOrtyUZBBWPYN79nd.53tR.m/n15YYcZq'),
    (2, 'Petr Ivanov', '1995-03-10', '$2a$10$0a6lf9Q8GyViiNbafds5DOrtyUZBBWPYN79nd.53tR.m/n15YYcZq'),
    (3, 'Anna Smirnova', '1990-12-20', '$2a$10$0a6lf9Q8GyViiNbafds5DOrtyUZBBWPYN79nd.53tR.m/n15YYcZq');

ALTER SEQUENCE users_id_seq RESTART WITH 4;

INSERT INTO account (user_id, balance, initial_balance)
VALUES
    (1, 1000.00, 1000.00),
    (2, 500.00, 500.00),
    (3, 2000.00, 2000.00);

INSERT INTO email_data (user_id, email)
VALUES
    (1, 'ivan@mail.com'),
    (2, 'petr@mail.com'),
    (3, 'anna@mail.com');

INSERT INTO phone_data (user_id, phone)
VALUES
    (1, '79207865431'),
    (2, '79207865432'),
    (3, '79207865433');
