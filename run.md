# IssueFlow – Setup, Build & Run

## Quick Start (TL;DR)

```bash
# 1. Start the database
docker compose up -d

# 2. Start the server  (Windows: .\mvnw.cmd spring-boot:run)
./mvnw spring-boot:run

# 3. Run the tests (no database needed)
./mvnw test
```

Server is ready on **http://localhost:8080** when the log prints:
```
Started IssueFlowApplication in X.XXX seconds
```

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| **Docker Desktop** | Latest | Runs the PostgreSQL container. Download from https://www.docker.com/products/docker-desktop |
| **JDK 21** | 21 | Tested with Eclipse Temurin 21. The Maven Wrapper is bundled — no separate Maven install needed. |

> The Maven Wrapper (`mvnw` / `mvnw.cmd`) is checked into the repo and downloads the correct Maven version automatically on first use.

---

## 1. Database Setup

The project ships with a `compose.yml` that starts a PostgreSQL container pre-configured for IssueFlow. No manual SQL setup is needed.

### Start the database

```bash
docker compose up -d
```

This creates a container with:

| Setting | Value |
|---|---|
| Container name | `issueflow-java-db-1` |
| Host port | `5432` |
| Database | `issueflow` |
| Username | `issueflow` |
| Password | `issueflow` |

### Verify the container is running

```bash
docker compose ps
```

Expected output:
```
NAME                   IMAGE      STATUS
issueflow-java-db-1    postgres   running
```

### Schema

The full database schema (`users`, `projects`, `tickets`, `comments`, `comment_mentions`, `ticket_dependencies`, `attachments`, `audit_logs`, `token_blocklist`) is applied automatically on every startup via `src/main/resources/schema.sql`.

All `CREATE TABLE` statements use `IF NOT EXISTS`, so restarting the server is always safe — no data is lost.

### Stop the database

```bash
docker compose down
```

---

## 2. Build

### Windows (PowerShell)

```powershell
.\mvnw.cmd clean package -DskipTests
```

### macOS / Linux

```bash
./mvnw clean package -DskipTests
```

The JAR is produced at `target/issueflow-0.0.1-SNAPSHOT.jar`.

To build and run the full test suite in one step:

```bash
./mvnw clean package    # or .\mvnw.cmd clean package on Windows
```

---

## 3. Run the Server

Make sure the Docker database container is running before starting the server.

### Windows (PowerShell)

```powershell
.\mvnw.cmd spring-boot:run
```

If you see `'java' is not recognized`, set `JAVA_HOME` first:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\mvnw.cmd spring-boot:run
```

### macOS / Linux

```bash
./mvnw spring-boot:run
```

### Run the packaged JAR (any OS)

```bash
java -jar target/issueflow-0.0.1-SNAPSHOT.jar
```

### Confirming startup

The server prints the following when ready:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
...
 :: Spring Boot ::                (v3.4.2)

Started IssueFlowApplication in 8.285 seconds
```

Server listens on **http://localhost:8080**.

---

## 4. Environment Variables

Both variables are optional for local development — safe defaults are built in.

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | `issueflow-secret-key-for-tdp2026-must-be-at-least-256-bits-long` | HMAC-SHA256 signing key. Must be at least 32 characters (256 bits). |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in milliseconds. Default is 24 hours. |

### Override (Windows PowerShell)

```powershell
$env:JWT_SECRET = "my-custom-secret-must-be-at-least-32-chars"
$env:JWT_EXPIRATION_MS = "3600000"
.\mvnw.cmd spring-boot:run
```

### Override (macOS / Linux)

```bash
JWT_SECRET=my-custom-secret JWT_EXPIRATION_MS=3600000 ./mvnw spring-boot:run
```

---

## 5. Run Tests

The test suite uses an **H2 in-memory database** — no PostgreSQL or Docker is required.

### Windows

```powershell
.\mvnw.cmd test
```

### macOS / Linux

```bash
./mvnw test
```

### What the tests cover

| Test class | Feature verified |
|---|---|
| `AuthFlowTest` | Registration, login, JWT token, `GET /auth/me`, logout and token invalidation |
| `TicketStatusMachineTest` | Valid/invalid status transitions, DONE lock, dependency blocker gate |
| `SoftDeleteRestoreTest` | Ticket and project soft delete, 404 after delete, ADMIN-only restore |
| `AutoEscalationSchedulerTest` | Overdue tickets get priority bumped one level; DONE tickets are skipped |
| `AutoAssignmentTest` | Auto-assign to least-loaded DEVELOPER, tiebreaking, `GET /projects/{id}/workload` |
| `MentionTest` | `@username` parsing in comments, unknown-handle validation, `GET /users/{id}/mentions` |

---

## 6. End-to-End API Walkthrough

This section demonstrates every major feature in sequence.  
Commands are shown for **Windows PowerShell** and **Bash (macOS / Linux)**.

> **Windows note:** PowerShell 5.1's `curl` command is an alias that strips double-quotes from JSON arguments.  
> Always use `Invoke-RestMethod` (aliased as `irm`) on Windows — never `curl.exe` — when sending JSON.

---

### Step 1 – Register an ADMIN user

**PowerShell:**
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/users `
  -ContentType "application/json" `
  -Body '{"username":"alice","email":"alice@test.com","full_name":"Alice Admin","role":"ADMIN","password":"pass123"}'
```

**Bash:**
```bash
curl -s -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@test.com","full_name":"Alice Admin","role":"ADMIN","password":"pass123"}'
```

Expected response:
```json
{
  "id": 1,
  "username": "alice",
  "email": "alice@test.com",
  "full_name": "Alice Admin",
  "role": "ADMIN"
}
```

Note: `POST /users` and `POST /auth/login` are the only public endpoints. All other requests require a JWT bearer token.

---

### Step 2 – Register a DEVELOPER user

**PowerShell:**
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/users `
  -ContentType "application/json" `
  -Body '{"username":"bob","email":"bob@test.com","full_name":"Bob Dev","role":"DEVELOPER","password":"pass123"}'
```

**Bash:**
```bash
curl -s -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","email":"bob@test.com","full_name":"Bob Dev","role":"DEVELOPER","password":"pass123"}'
```

Expected response:
```json
{ "id": 2, "username": "bob", "email": "bob@test.com", "full_name": "Bob Dev", "role": "DEVELOPER" }
```

---

### Step 3 – Log in and capture the JWT token

**PowerShell:**
```powershell
$resp = Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"alice","password":"pass123"}'

$TOKEN = $resp.access_token
Write-Host "Token: $TOKEN"
```

**Bash:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"pass123"}' | jq -r '.access_token')

echo "Token: $TOKEN"
```

Expected login response:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 86400000
}
```

Verify your identity:

**PowerShell:**
```powershell
Invoke-RestMethod -Uri http://localhost:8080/auth/me `
  -Headers @{ Authorization = "Bearer $TOKEN" }
```

**Bash:**
```bash
curl -s http://localhost:8080/auth/me -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{ "id": 1, "username": "alice", "role": "ADMIN" }
```

---

### Step 4 – Create a project

**PowerShell:**
```powershell
$project = Invoke-RestMethod -Method Post -Uri http://localhost:8080/projects `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $TOKEN" } `
  -Body '{"name":"Demo Project","description":"End-to-end walkthrough","owner_id":1}'
```

**Bash:**
```bash
curl -s -X POST http://localhost:8080/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Demo Project","description":"End-to-end walkthrough","owner_id":1}'
```

Expected response:
```json
{ "id": 1, "name": "Demo Project", "description": "End-to-end walkthrough", "owner_id": 1 }
```

---

### Step 5 – Create a ticket (with auto-assignment)

Omit `assignee_id` to trigger **auto-assignment**. The system finds the DEVELOPER in the project with the fewest open tickets and assigns the ticket to them automatically.

**PowerShell:**
```powershell
$ticket = Invoke-RestMethod -Method Post -Uri http://localhost:8080/tickets `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $TOKEN" } `
  -Body '{"title":"Fix login bug","description":"Users cannot log in on mobile","priority":"HIGH","type":"BUG","project_id":1}'
```

**Bash:**
```bash
curl -s -X POST http://localhost:8080/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Fix login bug","description":"Users cannot log in on mobile","priority":"HIGH","type":"BUG","project_id":1}'
```

Expected response — note `assignee_id` is populated automatically:
```json
{
  "id": 1,
  "title": "Fix login bug",
  "description": "Users cannot log in on mobile",
  "status": "TODO",
  "priority": "HIGH",
  "type": "BUG",
  "project_id": 1,
  "assignee_id": 2,
  "is_overdue": false
}
```

---

### Step 6 – Update ticket status

The status machine enforces: `TODO → IN_PROGRESS → IN_REVIEW → DONE`.  
Backward transitions and jumps (e.g. `TODO → DONE`) return `400 Bad Request`.  
A ticket blocked by an open dependency cannot advance to `IN_REVIEW`.

**PowerShell:**
```powershell
Invoke-RestMethod -Method Patch -Uri http://localhost:8080/tickets/1 `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $TOKEN" } `
  -Body '{"status":"IN_PROGRESS"}'
```

**Bash:**
```bash
curl -s -X PATCH http://localhost:8080/tickets/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}'
```

---

### Step 7 – Add a comment with @mention

`@username` handles in comment content are parsed, validated against real users, and stored in `comment_mentions`. Mentions to non-existent usernames return `400 Bad Request`.

**PowerShell:**
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/tickets/1/comments `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $TOKEN" } `
  -Body '{"author_id":2,"content":"@alice can you review this ticket?"}'
```

**Bash:**
```bash
curl -s -X POST http://localhost:8080/tickets/1/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"author_id":2,"content":"@alice can you review this ticket?"}'
```

Expected response:
```json
{
  "id": 1,
  "ticket_id": 1,
  "author_id": 2,
  "content": "@alice can you review this ticket?",
  "mentioned_users": [
    { "id": 1, "username": "alice", "full_name": "Alice Admin" }
  ]
}
```

---

### Step 8 – Retrieve @mentions for a user

Returns all comments in which the user was @mentioned (paginated).

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/users/1/mentions" `
  -Headers @{ Authorization = "Bearer $TOKEN" }
```

**Bash:**
```bash
curl -s "http://localhost:8080/users/1/mentions" \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{
  "data": [
    {
      "id": 1,
      "ticket_id": 1,
      "author_id": 2,
      "content": "@alice can you review this ticket?",
      "mentioned_users": [{ "id": 1, "username": "alice", "full_name": "Alice Admin" }]
    }
  ],
  "total": 1,
  "page": 1
}
```

Optional query params: `?page=1&pageSize=10`

---

### Step 9 – Check project workload

Returns the number of open tickets per DEVELOPER assigned to the project. Used by the auto-assignment logic to find the least-loaded developer.

**PowerShell:**
```powershell
Invoke-RestMethod -Uri http://localhost:8080/projects/1/workload `
  -Headers @{ Authorization = "Bearer $TOKEN" }
```

**Bash:**
```bash
curl -s http://localhost:8080/projects/1/workload \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
[
  { "user_id": 2, "username": "bob", "open_ticket_count": 1 }
]
```

---

### Step 10 – Soft delete a ticket

Marks the ticket as deleted (`deleted_at` is set). The ticket disappears from standard `GET /tickets/{id}` and project ticket lists (returns `404`), but is preserved in the database and restorable by an ADMIN.

**PowerShell:**
```powershell
Invoke-RestMethod -Method Delete -Uri http://localhost:8080/tickets/1 `
  -Headers @{ Authorization = "Bearer $TOKEN" }
```

**Bash:**
```bash
curl -s -X DELETE http://localhost:8080/tickets/1 \
  -H "Authorization: Bearer $TOKEN"
```

After deletion, `GET /tickets/1` returns `404`. View deleted tickets:

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/tickets/deleted?projectId=1" `
  -Headers @{ Authorization = "Bearer $TOKEN" }
```

**Bash:**
```bash
curl -s "http://localhost:8080/tickets/deleted?projectId=1" \
  -H "Authorization: Bearer $TOKEN"
```

---

### Step 11 – Restore a soft-deleted ticket (ADMIN only)

Non-ADMIN users get `403 Forbidden`.

**PowerShell:**
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/tickets/1/restore `
  -Headers @{ Authorization = "Bearer $TOKEN" }
```

**Bash:**
```bash
curl -s -X POST http://localhost:8080/tickets/1/restore \
  -H "Authorization: Bearer $TOKEN"
```

---

### Step 12 – Logout (invalidate the JWT token)

The token's JTI is added to a blocklist table. Any subsequent request using the same token returns `401 Unauthorized`.

**PowerShell:**
```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/auth/logout `
  -Headers @{ Authorization = "Bearer $TOKEN" }
```

**Bash:**
```bash
curl -s -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

---

## 7. Additional Features

### List tickets for a project

```powershell
# PowerShell
Invoke-RestMethod -Uri http://localhost:8080/projects/1/tickets `
  -Headers @{ Authorization = "Bearer $TOKEN" }
```

```bash
# Bash
curl -s http://localhost:8080/projects/1/tickets \
  -H "Authorization: Bearer $TOKEN"
```

### Ticket dependencies (blocker gate)

Add a blocker dependency — ticket 2 is blocked by ticket 3:

```bash
curl -s -X POST http://localhost:8080/tickets/2/dependencies \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"blocked_by":3}'
```

While ticket 3 is not in `DONE` status, advancing ticket 2 past `IN_PROGRESS` returns `423 Locked`.

List dependencies:
```bash
curl -s http://localhost:8080/tickets/2/dependencies \
  -H "Authorization: Bearer $TOKEN"
```

Remove a dependency:
```bash
curl -s -X DELETE http://localhost:8080/tickets/2/dependencies/3 \
  -H "Authorization: Bearer $TOKEN"
```

### CSV Export / Import

Export all tickets in a project to CSV:

```bash
curl -s "http://localhost:8080/tickets/export?projectId=1" \
  -H "Authorization: Bearer $TOKEN" \
  -o tickets.csv
```

Import tickets from CSV (multipart form-data):

```bash
curl -s -X POST http://localhost:8080/tickets/import \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@tickets.csv" \
  -F "projectId=1"
```

Expected import response:
```json
{ "created": 1, "failed": 0, "errors": [] }
```

### File Attachments (max 10 MB per file)

Upload a file to a ticket:

```bash
curl -s -X POST http://localhost:8080/tickets/1/attachments \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@screenshot.png"
```

Expected response:
```json
{ "id": 1, "ticket_id": 1, "filename": "screenshot.png", "content_type": "image/png", "file_size": 204800 }
```

Delete an attachment:
```bash
curl -s -X DELETE http://localhost:8080/tickets/1/attachments/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Audit Log

Every state-changing action (create, update, delete, restore) is recorded automatically.

```bash
# All logs
curl -s http://localhost:8080/audit-logs -H "Authorization: Bearer $TOKEN"

# Filtered by entity
curl -s "http://localhost:8080/audit-logs?entityType=TICKET&entityId=1" \
  -H "Authorization: Bearer $TOKEN"
```

---

## 8. Auto-Escalation Scheduler

A background task runs **every minute** and scans for overdue tickets:

- Tickets with `due_date < now()` and `status != DONE` have `is_overdue` set to `true`
- Their priority is bumped one level: `LOW → MEDIUM → HIGH → CRITICAL`
- Tickets already at `CRITICAL` stay at `CRITICAL`
- Tickets in `DONE` status are never escalated

No configuration is required. The scheduler starts automatically with the server and logs each escalation run.

---

## 9. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Connection refused` on startup | PostgreSQL container not running | Run `docker compose up -d` and wait for it to become healthy |
| `401 Unauthorized` | Token expired or logged out | Call `POST /auth/login` to obtain a fresh token |
| `403 Forbidden` | Non-ADMIN trying an admin-only endpoint | Log in as a user with `role: ADMIN` |
| `400 Bad Request` on PowerShell with curl | PowerShell strips double-quotes from JSON | Use `Invoke-RestMethod` instead of `curl` on Windows |
| `409 Conflict` on user/project creation | Duplicate `username` or `email` | Choose a different username or email |
| `400 Bad Request` on status update | Invalid state transition | Follow the state machine: `TODO → IN_PROGRESS → IN_REVIEW → DONE` |
| `423 Locked` on status update | Ticket has an unresolved blocker dependency | Advance the blocker ticket to `DONE` first |
| `400 Bad Request` on comment with @handle | Mentioned username does not exist | Use the exact username of a registered user |
| Server starts but shows a Spring Security login page | No `Authorization` header | Add `-H "Authorization: Bearer <token>"` to your request |
