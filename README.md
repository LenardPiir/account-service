# Account Service

REST API for multi-currency bank accounts: add money, debit money, get balances and exchange currency.

Java 25, Spring Boot 4.1, PostgreSQL 18, Flyway.

## Run

Requires Docker.

```bash
docker compose up --build
```

- API: http://localhost:8080/api/v1/accounts
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/accounts` | Create an account |
| `GET` | `/api/v1/accounts/{id}` | Get an account |
| `GET` | `/api/v1/accounts/{id}/balances` | Balances in every supported currency |
| `POST` | `/api/v1/accounts/{id}/credits` | Add money |
| `POST` | `/api/v1/accounts/{id}/debits` | Debit money |
| `POST` | `/api/v1/accounts/{id}/exchanges` | Exchange between currencies |

Errors are RFC 9457 Problem Details: `400` invalid request or unsupported currency, `404` unknown account, `409` concurrent update, `422` insufficient funds or reused idempotency key, `503` external system unavailable.

## Behaviour

- Supported currencies are EUR, USD, SEK and GBP. A currency is supported when it has a rate in `application.properties` (`exchange.rates.*`).
- Rates are fixed, 1 EUR = x, taken from the ECB reference rates of 2026-10-14. Conversion between two non-EUR currencies goes through EUR. Results are rounded half up to the target currency's decimal places.
- Amounts must be positive and have at most as many decimal places as the currency.
- A debit uses one currency only and cannot overdraw it.
- Before a debit, the service calls an external system (`external-logging.url`). It uses https://httpbin.org/status/200 because https://httpstat.us was unavailable. If the call fails or times out, nothing is debited and the API returns `503`. A debit with insufficient funds is rejected before the call.
- An exchange does not call the external system, since no money leaves the account.
- Every money operation locks the account row in the database, so concurrent requests on the same account run one at a time.
- Credits, debits and exchanges accept an optional `Idempotency-Key` header, up to 64 characters. A retry with the same key returns the first result without applying it again. The same key with a different request returns `422`. A failed request is not remembered, so it can be retried with the same key. The header is optional so the API is easy to try out. In production it would be required for every operation that moves money.

## Development

Requires JDK 25 and Docker. Tests start their own PostgreSQL with Testcontainers.

```bash
./gradlew test
```

To run the application outside Docker, start only the database:

```bash
docker compose up -d postgres
./gradlew bootRun
```

## Not included

- Expiry of idempotency keys. In production, a scheduled job would delete keys older than 24 hours.
- Authentication. The database credentials in `compose.yaml` are for local use only.
