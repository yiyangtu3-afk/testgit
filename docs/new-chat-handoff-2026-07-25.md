# CampusLink new-chat handoff — July 25, 2026

This handoff captures the repository and local-runtime state after the activity
check-in work was completed. Read it before starting a new feature so you can
preserve the Vue migration baseline, the local MySQL history, and the legacy
frontend regression boundary.

## Repository state

The project is located at `/Users/linus_k/Documents/test` on branch `main`.
The remote is `https://github.com/yiyangtu3-afk/testgit.git`.

On July 25, 2026, `git status --short --branch` reported a clean worktree on
`main...origin/main`. The latest commits are:

- `df19538 Harden activity check-in workflow` — latest functional change.
- `5dd1a72 Restrict activity registration to students`.
- `48c511c Fix organizer activity check-in loading`.
- `2c363f5 Refresh project handoff snapshot` — earlier handoff-only commit.
- `cfa4623 Add authenticated chat image previews`.

The stable behavior baseline remains
`98c2dad Add notification read and target actions`.

## Product state

Vue is the default frontend. It includes seven completed domain slices:

- Authentication and workspace shell.
- Contacts, friend requests, chat, image previews, and real-time events.
- Feed, likes, comments, and moderation-state feedback.
- Activities, registration, waitlists, organizer operations, and check-in.
- Notification history, read actions, and protected target navigation.
- Administrator metrics, moderation, audit history, reports, and local
  explainable review suggestions.
- JWT, API/Mock, and role boundaries shared by the above slices.

The legacy static frontend remains a regression and fallback baseline. Do not
move, replace, or rewrite root `index.html`, `app.js`, `styles.css`, or
`frontend/js/`. Do not move the Vue image-preview implementation back into the
legacy frontend.

## Activity check-in baseline

The latest completed feature is the live activity check-in workflow.

- Only a student with a `registered` activity registration can request a
  current check-in credential.
- Credentials are opaque and rotate when shown. MySQL stores only their
  SHA-256 digests.
- The activity creator validates a credential in **My activity operations**.
  The server verifies JWT identity, activity ownership, credential, and the
  registration's current state before recording `checked_in`.
- Teachers, club managers, and administrators do not see an invalid
  registration action. Public activity cards identify the organizer; only an
  eligible student sees **Register now**.
- Check-in controls and rosters are available only for `published` and `full`
  activities. The backend also rejects check-in for pending or rejected
  activities.

The real API acceptance run used the `揍康鹏` activity created by `王社长`:
`林一` obtained a credential and was checked in successfully. That normal
activity, registration, image-message, and audit history remains in local
MySQL. Do not clean it.

## Verified test baseline

The July 23, 2026, activity handoff records these completed checks:

- Full Maven test run with the explicit Byte Buddy agent: 157 tests passed.
- Vue unit tests: 46 tests in 16 files passed.
- Vue production build and legacy frontend regression check passed.
- Live browser and API checks covered credential rotation, authorization,
  duplicate check-in rejection, manual check-in, waitlist promotion, and
  unpublishable-activity rejection.

The checks above are a recorded feature baseline, not a claim that they were
rerun during a later eventing phase.

## Phase one: Kafka and Transactional Outbox foundation

The first Spring Cloud and Kafka upgrade slice is complete and awaiting the
next approved slice. It keeps the current modular monolith and notification
behavior intact while preparing a reliable event boundary.

- Spring Cloud `2025.0.3` is managed through the Spring Cloud BOM alongside
  Spring Boot `3.5.0`.
- Activity registration, waitlist, cancellation, promotion, and check-in now
  write versioned `activity.registration.*.v1` messages to `outbox_events` in
  the same local transaction as their existing state and event history.
- The `eventing` profile publishes ready Outbox rows to Kafka and writes an
  idempotent `(consumer_name, event_id)` receipt. A failed publish retains the
  row as `retry`; dead-letter handling is intentionally deferred to phase three.
- Docker Compose now starts a KRaft Kafka broker and enables `eventing` for its
  API container. The regular local backend script keeps Kafka disabled, so it
  stores Outbox rows as `pending` without requiring a broker.
- The product plan is in
  [`plans/spring-cloud-kafka-microservices-upgrade.md`](../plans/spring-cloud-kafka-microservices-upgrade.md).

The targeted backend suite ran 19 tests with the explicit Byte Buddy agent and
passed. The frontend smoke check also passed. A full Maven run was attempted
with the explicit agent, but this restricted runner blocked Java loopback access
to MySQL and did not provide Docker for Testcontainers; the resulting failures
were environment initialization failures, not new test assertion failures.

The host-capable acceptance run started the real Java API against the preserved
local MySQL history and verified the Vue Vite proxy health endpoint. It then
reactivated student `u-1001`'s previously cancelled registration for activity
`e831a69bdf264e1499f48786b84ddb5a`. MySQL contained the matching
`activity.registration.registered.v1` Outbox row with `pending` status. That
normal acceptance history remains in MySQL and must not be cleaned.

## Local runtime status

At handoff time, local MySQL was listening at `127.0.0.1:3306`. Vue on `5180`
and the Java API on `8080` were not running, so do not assume they are alive in
a new session.

On July 24, starting the backend from a host-capable environment successfully
connected to the existing MySQL database. Its health response was:

```json
{"status":"UP","database":"campuslink","demoUsers":9}
```

The initial failure was environmental: a sandboxed Java process was denied a
loopback connection to MySQL. It was not caused by a schema, credential, or
application-code change. If a similarly restricted runner reports
`java.net.SocketException: Operation not permitted` while connecting to MySQL,
start the backend in an environment permitted to reach local `127.0.0.1:3306`.
Do not reset or reseed the database as a workaround.

Start and verify local services in this order:

1. Confirm MySQL is running on `127.0.0.1:3306`.
2. Run `./script/run_backend_idea.sh` from the repository root.
3. Verify `curl -fsS http://127.0.0.1:8080/api/database/health`.
4. Run `./script/run_frontend_demo.sh` from the repository root.
5. Open `http://127.0.0.1:5180`.

The important local addresses are:

- Vue default entry: `http://127.0.0.1:5180`
- Vue chat: `http://127.0.0.1:5180/workspace/contacts`
- Java API: `http://127.0.0.1:8080`
- Legacy fallback: `http://127.0.0.1:5179/?v=20260715-signed-jwt-logout-v1`

## Required boundaries

- Use Mock only when the Java API is completely unreachable. Show real API
  `401`, `403`, `409`, and `500` errors to the user.
- Preserve JWT authentication, role checks, API/Mock separation, and the
  legacy regression path when changing Vue.
- Do not run `git reset --hard`, `git clean`, or delete untracked files.
- Do not reset, reseed, truncate, empty, or otherwise clean local MySQL data.
- Do not add a query version parameter to imports of `frontend/js/state.js`.
- For backend changes, run the complete Maven test command with the explicit
  Byte Buddy agent defined in `backend/AGENTS.md`.
- For each verified feature stage, update the relevant handoff documentation,
  commit only the stage's files, and push `main`.

## Recommended next action

The user approved phase two: turn registration-result notifications into an
idempotent Kafka consumer projection while preserving the current notification
API, Vue behavior, JWT boundary, and legacy frontend regression baseline.

## Related records

Use the following documents for detailed historical context:

- [`resume-project-roadmap.md`](resume-project-roadmap.md)
- [`vue-migration-handoff.md`](vue-migration-handoff.md)
- [`phase-two-activity-handoff.md`](phase-two-activity-handoff.md)
- [`new-chat-handoff-2026-07-08.md`](new-chat-handoff-2026-07-08.md)
- [`admin-review-workbench-handoff.md`](admin-review-workbench-handoff.md)
- [`admin-moderation-content-module-fix.md`](admin-moderation-content-module-fix.md)
