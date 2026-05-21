# IssueFlow – Setup, Build & Run

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| PostgreSQL | 14+ |

---

## 1. Database Setup

Create the database and user that match the default configuration:

```sql
CREATE USER issueflow WITH PASSWORD 'issueflow';
CREATE DATABASE issueflow OWNER issueflow;
```

The schema (tables, indexes) is applied automatically on first startup via `src/main/resources/schema.sql`.  
All `CREATE TABLE` statements use `IF NOT EXISTS`, so restarts are safe.

---

## 2. Build

```bash
mvn clean package -DskipTests
```

To build and run all tests in one step:

```bash
mvn clean package
```

---

## 3. Run

**Option A — Maven plugin:**
```bash
mvn spring-boot:run
```

**Option B — JAR:**
```bash
java -jar target/issueflow-0.0.1-SNAPSHOT.jar
```

The server starts on **http://localhost:8080**.

---

## 4. Environment Variables

Both variables have built-in defaults and are optional for local development.

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | `issueflow-secret-key-for-tdp2026-must-be-at-least-256-bits-long` | HMAC-SHA256 signing key |
| `JWT_EXPIRATION_MS` | `86400000` (24 h) | Token lifetime in milliseconds |

Override on the command line if needed:

```bash
JWT_SECRET=my-secret JWT_EXPIRATION_MS=3600000 mvn spring-boot:run
```

---

## 5. Run Tests

Tests use an **H2 in-memory database** — no PostgreSQL required.

```bash
mvn test
```

The test suite covers:
- Auth flow (login, logout, token invalidation)
- Ticket status machine and dependency blocker gate
- Soft delete and restore (tickets and projects)
- Auto-escalation scheduler
- Auto-assignment and workload endpoint
- Comment @mention parsing and mentions endpoint

---

## 6. Quick Smoke Test

Register a user, log in, and create a project:

```bash
# Register
curl -s -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","email":"admin1@test.com","full_name":"Admin One","role":"ADMIN","password":"pass123"}'

# Login — copy the token from the response
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"pass123"}'

# Create a project (replace <TOKEN> with the value from the login response)
curl -s -X POST http://localhost:8080/projects \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Project","description":"First project","owner_id":1}'
```
