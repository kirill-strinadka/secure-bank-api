# Database Schema

The database schema is created by Flyway migrations located in:

`src/main/resources/db/migration`

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar name
        date date_of_birth
        varchar password
        timestamp created_at
        timestamp updated_at
    }

    ACCOUNT {
        bigint id PK
        bigint user_id FK
        numeric balance
        numeric initial_balance
        timestamp created_at
        timestamp updated_at
        bigint version
    }

    EMAIL_DATA {
        bigint id PK
        bigint user_id FK
        varchar email
        timestamp created_at
        timestamp updated_at
    }

    PHONE_DATA {
        bigint id PK
        bigint user_id FK
        varchar phone
        timestamp created_at
        timestamp updated_at
    }

    TRANSFER {
        bigint id PK
        bigint from_user_id FK
        bigint to_user_id FK
        numeric amount
        varchar status
        timestamp created_at
    }

    USERS ||--|| ACCOUNT : owns
    USERS ||--o{ EMAIL_DATA : has
    USERS ||--o{ PHONE_DATA : has
    USERS ||--o{ TRANSFER : sends
    USERS ||--o{ TRANSFER : receives
```

## Tables

| Table | Purpose |
| --- | --- |
| `users` | Bank users with personal data and password hash. |
| `account` | One account per user with balance, initial balance, and optimistic lock version. |
| `email_data` | User email addresses. |
| `phone_data` | User phone numbers. |
| `transfer` | Money transfer history between users. |

## Seed Data

Test users are inserted by `V2__insert_test_users.sql`.

All seeded users use the password `password123`, stored as a BCrypt hash.
