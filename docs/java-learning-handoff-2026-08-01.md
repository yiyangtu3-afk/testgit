# Java learning handoff — August 1, 2026

This handoff lets the next learning session continue without repeating the
completed Spring Boot basics or changing product code.

## Learning progress

The learner knows Java fundamentals and basic Spring Boot. Complete topics are:

- HTTP request flow from Controller through Service and MyBatis Mapper to MySQL.
- Spring Beans, constructor injection, `final`, and why tests can inject Mocks.
- MyBatis `@Mapper`, `@Select`, interfaces, and SQL result mapping.
- Java `Map`, `LinkedHashMap`, key-value pairs, and JSON serialization.
- Public endpoints, JWT-protected endpoints, HTTP 401, and HTTP 403.
- The `Authorization: Bearer <JWT>` request-header format and token parsing.

The current position is Week 2, lesson 1: an authenticated activity-list
request. The next answer to request is: what
`authTokenService.requireUser(authorization)` does before
`activityService.published(...)` runs.

## Code to read next

Read these files in order and explain each statement before moving on:

1. `backend/src/main/java/com/campuslink/controller/ActivityController.java`
   — start with `published(...)`.
2. `backend/src/main/java/com/campuslink/service/AuthTokenService.java`
   — explain JWT validation and current-user lookup.
3. `backend/src/main/java/com/campuslink/service/ActivityService.java`
   — explain category and date normalization in `published(...)`.
4. `frontend-vue/src/services/api/activity-api.js` — show where the Vue client
   requests `/api/activities`.

Use beginner-friendly Chinese. Explain code line by line before discussing
architecture. Ask one short question at a time, wait for the learner's answer,
then correct it gently with a practical example. Do not jump to Redis,
microservices, or concurrency until the learner understands the current flow.

## Verified runtime snapshot

This snapshot was verified on August 1, 2026. Don't assume it persists in a
later session.

- Native backend: `http://127.0.0.1:8080/api/database/health` returned `UP`,
  database `campuslink`, and 9 demo users.
- Native Vue learning page: `http://127.0.0.1:5180` returned HTTP 200.
- Compose Gateway: `http://127.0.0.1:18084/api/database/health` returned `UP`,
  using its separate Docker MySQL data with 5 demo users.
- Compose Vue preview: `http://127.0.0.1:5179` returned HTTP 200.

The native page uses preserved host MySQL at `127.0.0.1:3306`. Compose uses
separate Docker named volumes. Never interpret their different data counts as
data loss.

## Code and safety baseline

The branch is `main`; the product and learning-code baseline includes through
`e5d4ce7 Record JWT header learning progress`. High-concurrency work is complete
and audited in commits `9efbd3f`, `522ed64`, `3f11c6f`, `24db144`, and
`343ae0a`. Do not implement a new product feature without user authorization.

Before work, run `git status --short --branch` and read the root, frontend, and
backend `AGENTS.md` files. Never run `git reset --hard`, `git clean`, or
`docker compose down -v`. Never reset, seed, empty, truncate, or clean host
MySQL data. Preserve the root static frontend files and `frontend/js/`.

## Native startup order

If the native services are not running, use this order:

1. Confirm host MySQL is reachable at `127.0.0.1:3306`.
2. Run `./script/run_backend_idea.sh`.
3. Verify `curl -fsS http://127.0.0.1:8080/api/database/health`.
4. Run `./script/run_frontend_demo.sh`.
5. Open `http://127.0.0.1:5180`.

## Next steps

Resume with the activity-list request flow, record a completed lesson only
after the learner can explain it, and keep
`docs/java-backend-learning-roadmap.md` current. Learning-progress documents
must be committed and pushed separately from product changes.
