# Secure Bank API

Spring Boot banking API.

## Database

Database migrations are located in:

`src/main/resources/db/migration`

ER diagram:

[Database schema](docs/database.md)

Local PostgreSQL can be started with:

```bash
docker compose up -d postgres
```

The local datasource defaults to `jdbc:postgresql://localhost:55432/banking`.
