# Sleep Logger API

A REST API for logging and reviewing nightly sleep, built for the Noom backend
take-home. Users record last night's sleep, fetch the most recent log, and view
averages over the last 30 days.

## Tech stack

- Kotlin + Spring Boot 2.7 (Web, JDBC)
- PostgreSQL with Flyway migrations
- Spring `NamedParameterJdbcTemplate` for persistence (no ORM)
- JUnit 5, Testcontainers, and Mockito for tests

## Running the project

Requires Docker and free ports `5432` (Postgres) and `8080` (API).

```bash
docker-compose up --build
```

The API is then available at `http://localhost:8080`. Flyway applies the
migrations on startup, including a seeded default user (`id = 1`).

## The concept of a user

Authentication and authorization are out of scope, but every sleep log belongs
to a user. Requests carry the user via the `X-User-Id` header; when omitted it
defaults to the seeded user (`1`), so the API is usable out of the box. Unknown
user ids are rejected with `404`.

## Data model

| Table        | Purpose                                                            |
|--------------|-------------------------------------------------------------------|
| `users`      | Minimal user record (seeded with a default user).                 |
| `sleep_logs` | One row per user per night: the in-bed interval, derived total minutes in bed, and the morning feeling. |

The in-bed interval is stored as two wall-clock timestamps, so total time in bed
stays correct when sleep crosses midnight. A unique `(user_id, sleep_date)`
constraint enforces at most one log per night.

## API

Base URL: `http://localhost:8080`

### Create last night's sleep log — `POST /api/sleep-logs`

`sleepDate` is optional and defaults to the date the user got to bed.

```bash
curl -X POST http://localhost:8080/api/sleep-logs \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 1' \
  -d '{
        "inBedStart": "2026-09-01T22:53:00",
        "inBedEnd": "2026-09-02T07:05:00",
        "morningFeeling": "GOOD"
      }'
```

Returns `201 Created` with the persisted log:

```json
{
  "id": 1,
  "sleepDate": "2026-09-01",
  "inBedStart": "2026-09-01T22:53:00",
  "inBedEnd": "2026-09-02T07:05:00",
  "totalMinutesInBed": 492,
  "morningFeeling": "GOOD",
  "createdAt": "2026-09-02T07:10:11.123"
}
```

### Fetch last night's sleep — `GET /api/sleep-logs/last-night`

```bash
curl http://localhost:8080/api/sleep-logs/last-night -H 'X-User-Id: 1'
```

Returns the most recent log, or `404` if the user has none yet.

### 30-day averages — `GET /api/sleep-logs/averages?days=30`

`days` is optional and defaults to `30`.

```bash
curl 'http://localhost:8080/api/sleep-logs/averages?days=30' -H 'X-User-Id: 1'
```

```json
{
  "rangeStart": "2026-08-03",
  "rangeEnd": "2026-09-02",
  "logCount": 12,
  "averageTotalMinutesInBed": 434,
  "averageInBedStart": "23:11",
  "averageInBedEnd": "07:05",
  "feelingFrequencies": { "BAD": 3, "OK": 14, "GOOD": 9 }
}
```

When the range has no logs, the averages are `null` and all frequencies are `0`.

Average get-to-bed and get-out-of-bed times use a **circular mean**: times are
mapped onto a 24-hour circle before averaging, so bedtimes on either side of
midnight (e.g. `23:50` and `00:10`) average to `00:00` instead of collapsing
toward midday.

## Errors

All failures return a consistent body:

```json
{ "status": 404, "error": "Not Found", "message": "...", "timestamp": "..." }
```

| Status | When                                                        |
|--------|-------------------------------------------------------------|
| 400    | Malformed body, invalid interval, or non-positive `days`.   |
| 404    | Unknown user, or no sleep log for the user.                 |
| 409    | A sleep log already exists for that user and night.         |

## Testing

```bash
cd sleep && ./gradlew test
```

Repository and context tests run against a real Postgres via Testcontainers, so
a running Docker daemon is required. Service/business-logic tests are pure unit
tests and need no infrastructure.

## Trying the API

- Import [`postman/sleep-logger-api.postman_collection.json`](postman/sleep-logger-api.postman_collection.json)
  into Postman.
- Or run the smoke script against a running instance:

  ```bash
  ./scripts/smoke-test.sh
  ```

## Project structure

```
sleep/src/main/kotlin/com/noom/interview/fullstack/sleep
├── config       # Clock bean
├── domain       # SleepLog, MorningFeeling, SleepAverages
├── repository   # JDBC repositories
├── service      # business logic (validation, averages)
└── web          # controller, DTOs, error handling
sleep/src/main/resources/db/migration  # Flyway migrations
```
