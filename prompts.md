# AI Usage Documentation — IssueFlow

## Models Used
- **Google Gemini 2.0 Pro** (gemini.google.com)
- **Claude Sonnet 4.6** (claude.ai chat)
- **Claude Sonnet 4.6 via Claude Code** (VS Code extension)

---

## Phase 1 — Initial Planning & Requirements Analysis
**Tool:** Google Gemini 2.0 Pro

**Summary of Interaction:**

**Requirement Analysis:**
Provided the assignment's PDF instructions and requested a high-level explanation of the system flow and core business logic (Users, Projects, Tickets, and Comments).

**Architecture & Technology Strategy:**
Used the AI to map out the required technology stack (Java 21/25, Spring Boot, PostgreSQL, Docker) and identify key architectural challenges, such as handling simultaneous edits (optimistic locking), auto-assignment, and background scheduling for ticket escalation.

**Task Management & Planning:**
Requested a structured, step-by-step execution plan divided over a 3-day timeframe to ensure all MVP features, advanced automation tasks, and testing requirements would be completed on schedule.

**What it helped with:** High-level understanding of the system, architecture decisions, and a concrete execution plan before writing a single line of code.

---

## Phase 2 — Understanding the Assignment & Choosing the Stack
**Tool:** Claude Sonnet 4.6 (claude.ai)

**Prompt 1 — Understanding the assignment:**
> "I got a homework assignment from AT&T as part of the TDP program. I'll add here the zip of the instructions. Help me to understand fully what are the instructions. Read it carefully without missing any important data. When finished present me a full description of the task and suggest a work plan in order to accomplish it. It should include steps of implementation, and estimated time for each step and mission."

**What it helped with:** Full breakdown of all requirements (core + 8 extended features), 3-day work plan with time estimates per step.

---

**Prompt 2 — Choosing the stack and where to begin:**
> "I have exactly 3 days to complete the assignment, and I will take the java path, where to begin?"

**What it helped with:** Day-by-day schedule, step-by-step starting instructions including schema.sql, package structure, and JWT dependencies.

---

**Prompt 3 — Understanding the tech stack:**
> "Explain further about Java Spring Boot and the SQL in the project and all the technologies that this project will use."

**What it helped with:** Deep explanation of Spring Boot layers, JPA/Hibernate, JWT auth flow, PostgreSQL, Maven, Lombok, Apache Commons CSV, Docker Compose, and how they all connect.

---

**Prompt 4 — Connecting VS Code:**
> "I would also want to connect this chat with my code in VS Code, how to perform this?"

**What it helped with:** Setting up Claude Code VS Code extension and generating a context-transfer prompt to continue the work inside the editor.

---

## Phase 3 — Full Backend Implementation
**Tool:** Claude Sonnet 4.6 via Claude Code (VS Code extension)

This phase covered the complete implementation of the backend. The following prompts shaped the most significant code and decisions.

---

**Prompt 1 — Schema and project structure:**
> "Build the full IssueFlow backend: schema.sql, Spring Boot entities, repositories, services, and controllers for Users, Projects, Tickets, Comments, Dependencies, Attachments, Audit Logs, CSV export/import, Soft Delete, Mentions, Auto-assignment, and the Escalation Scheduler."

**What it helped with:** End-to-end scaffolding of the entire project — JPA entities, repositories, service layer with business logic, REST controllers, JWT filter and security configuration, and the scheduled escalation task. Understanding this output required reading and verifying each layer individually to confirm correctness of the domain model and API surface.

---

**Prompt 2 — Diagnosing server startup failure (circular dependency):**
> "The server won't start. Here's the error log: [pasted full stack trace showing jpaSharedEM_entityManagerFactory circular dependency in JwtFilter]"

**What it helped with:** Identified that `@RequiredArgsConstructor` on `JwtFilter` caused Spring Security to eagerly initialize `TokenBlocklistRepository` before the JPA `EntityManagerFactory` was ready — a non-obvious Spring bean lifecycle ordering issue. Fix: replaced the Lombok annotation with an explicit constructor using `@Lazy` on the repository parameter. Understanding this required learning how Spring Security's filter chain initialization interacts with JPA bootstrapping.

---

**Prompt 3 — Fixing data.sql startup error:**
> "[pasted error] Failed to execute database script from resource [data.sql] — 'script' must not be null or empty"

**What it helped with:** Identified that `sql.init.mode: always` rejects a file that is entirely comments (empty after stripping). Added `SELECT 1;` as a harmless no-op placeholder. This clarified the distinction between `schema.sql` (DDL, runs first) and `data.sql` (seed data, runs after) in Spring Boot's SQL initialization lifecycle.

---

**Prompt 4 — Exception logging:**
> "[pasted 500 error response with no details] I'm getting a 500 but can't see what's wrong."

**What it helped with:** Found that `GlobalExceptionHandler` was silently swallowing all unexpected exceptions with no logging — making diagnosis impossible. Added `@Slf4j` and `log.error("Unexpected error", ex)` so full stack traces appear in the server log. This change is important for ongoing maintainability, not just the immediate bug.

---

**Prompt 5 — Live end-to-end demo:**
> "First I want to commit and push the bug fixes. Then I would like to see the app runs live. Walk me through it."

**What it helped with:** Committed and pushed the three fixes, then ran a full live demonstration against the real PostgreSQL database covering: user registration (ADMIN + DEVELOPER roles), JWT login, `/auth/me`, project creation, ticket creation with auto-assignment (system selected the least-loaded DEVELOPER automatically), status transition (`TODO → IN_PROGRESS`), comment with `@mention`, `/users/{id}/mentions` (paginated), `/projects/{id}/workload`, soft delete (ticket returns 404, then appears in deleted list), and restore — confirming all features worked end-to-end. Also uncovered and explained the PowerShell `curl.exe` JSON quoting issue (double-quotes stripped), requiring `Invoke-RestMethod` on Windows.

---

**Prompt 6 — Progress checkpoint:**
> "Give me a percentage of how much of the project we completed so far (0–100%), and tell me if the pace is good or if we need to speed up."

**What it helped with:** Getting a clear picture of overall completion across all features (core API, extended features, tests, documentation) and adjusting the work plan accordingly. Using percentage-based checkpoints throughout development made it easy to identify which areas were lagging and re-prioritize remaining tasks before the deadline.

---

**Prompt 7 — Iterative task breakdown:**
> "Let's continue step by step. What is the next small task we should implement?"

**What it helped with:** Rather than requesting large blocks of code at once, breaking the work into focused, single-responsibility steps (one endpoint, one service method, one test class at a time) made each output easier to review, test, and understand. This approach also reduced the risk of generating code with hidden dependencies or assumptions that would be hard to catch in a larger diff.

---

**Prompt 8 — Comprehensive run.md:**
> "In my opinion the run.md file isn't good enough. I want it to be fully documented as requested in the task. Now it's really poor. I want to insert more specific examples and how to run things. It should be very informative because that's the first thing they will look at in my project."

**What it helped with:** Rewrote `run.md` from a minimal 6-section skeleton into a full 9-section guide: Quick Start, Prerequisites, Docker database setup, Build, Run (Windows + macOS/Linux), Environment Variables, Test suite overview table, 12-step end-to-end API walkthrough with parallel PowerShell and Bash commands and real expected JSON responses for every endpoint, and a troubleshooting table covering 9 common failure scenarios.

---

## Notes
- All code generated by AI was reviewed, understood, and manually verified before submission.
- Architectural decisions (package structure, status machine design, security configuration, scheduler logic) were discussed with the AI but final choices were made and owned by **Amit Ben Yehuda**.
- The AI was used as a senior pair-programmer — it accelerated development and caught non-obvious bugs, but every file was read, understood, and tested personally before being committed.
- Work was deliberately broken into small, focused tasks rather than requesting large code dumps. Each step was reviewed and verified before moving to the next.
- Progress was tracked using explicit 0–100% checkpoints throughout development to monitor pace and re-prioritize when needed.
