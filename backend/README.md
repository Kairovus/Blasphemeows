# Blasphemeows Backend

Simple Spring Boot API for authentication, score submission, and a global leaderboard.

## Stack

- Java 17
- Spring Boot 3.4 + Spring Security + JWT + BCrypt
- Spring Data JPA
- PostgreSQL
- Gradle

## API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Create account (`username`, `password`) |
| POST | `/api/auth/login` | No | Login, returns JWT |
| POST | `/api/scores` | JWT | Submit score `{ "score": 12345 }` (user from token) |
| GET | `/api/scores/me` | JWT | Your score history |
| GET | `/api/leaderboard` | No | Top scores (default limit 10) |

## Local setup

### 1. Start PostgreSQL

If you have Docker:

```bash
docker compose up -d db
```

Or create a local database named `blasphemeows` with user/password `postgres`/`postgres`.

### 2. Run the API

```bash
cd backend
./gradlew bootRun
```

Windows:

```bat
cd backend
gradlew.bat bootRun
```

API listens on `http://localhost:8080`.

### 3. Smoke test with curl

```bash
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"username\":\"player1\",\"password\":\"password\"}"

curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"player1\",\"password\":\"password\"}"

# use token from login response
curl -X POST http://localhost:8080/api/scores -H "Authorization: Bearer TOKEN" -H "Content-Type: application/json" -d "{\"score\":3500}"

curl http://localhost:8080/api/leaderboard
```

### 4. Run tests (H2 in-memory, no Postgres needed)

```bash
./gradlew test
```

## Environment variables (local + cloud)

| Variable | Default | Purpose |
|----------|---------|---------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/blasphemeows` | JDBC URL |
| `DATABASE_USERNAME` | `postgres` | DB user |
| `DATABASE_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | (dev placeholder) | HMAC secret, **≥ 32 characters** |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime (24h) |
| `PORT` | `8080` | HTTP port |

## Cloud / Docker deployment

Full stack (Postgres + API):

```bash
docker compose up --build
```

Or build only the JAR and deploy anywhere Java 17 runs:

```bash
cd backend
./gradlew bootJar
java -jar build/libs/blasphemeows-backend-1.0.0.jar
```

Set `DATABASE_*` and `JWT_SECRET` in your host/provider environment before starting.
