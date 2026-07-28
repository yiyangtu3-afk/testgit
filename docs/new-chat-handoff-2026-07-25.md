# CampusLink new-chat handoff — July 25, 2026

This handoff captures the repository and local-runtime state after the eighth
Kafka and microservice upgrade slice. Read it before starting a new feature so you can
preserve the Vue migration baseline, the local MySQL history, and the legacy
frontend regression boundary.

## Repository state

The project is located at `/Users/linus_k/Documents/test` on branch `main`.
The remote is `https://github.com/yiyangtu3-afk/testgit.git`.

On July 25, 2026, `git status --short --branch` reported a clean worktree on
`main...origin/main`. The latest commits are:

- `580ecdf Add Kafka transactional outbox foundation` — phase-one eventing
  foundation.
- `df19538 Harden activity check-in workflow` — latest pre-eventing feature.
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
  idempotent `(consumer_name, event_id)` receipt. Failed publication retries
  with a bounded policy, then remains as a durable `dead_letter` Outbox row.
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

## Phase two: Kafka-backed activity notification projection

The second eventing slice is complete. It keeps the current notification API,
unread count, WebSocket delivery, Vue UI, JWT checks, and legacy frontend
unchanged while making activity-derived notification creation asynchronous in
the `eventing` profile.

- Registration, waitlist, and promotion Outbox messages now include the
  activity title and waitlist position needed to render a notification without
  a cross-service lookup. The projection can still read a title and queue
  position from the current repositories for phase-one messages that lack the
  new fields.
- With `eventing` disabled, the existing in-process dispatcher keeps ordinary
  local development behavior unchanged. With `eventing` enabled, the activity
  transaction defers these notifications to a Kafka listener in consumer group
  `campuslink-activity-notification-v1`.
- The projection records its `(consumer_name, event_id)` receipt and creates
  the notification in one transaction. A duplicate Kafka delivery therefore
  cannot create a duplicate notification. After commit, the existing real-time
  notification listener delivers the same WebSocket event as before.

The explicit-Byte-Buddy targeted Maven suite ran 22 tests and passed. It covers
the existing registration service and controller boundaries, Outbox messages,
publisher retry behavior, receipt idempotency, duplicate notification delivery,
waitlist and promotion copy, and compatibility with phase-one messages. A full
Maven attempt again could not access MySQL from the restricted runner and could
not start Testcontainers without Docker; its failures were environmental
initialization failures. A host-capable backend restart connected to preserved
MySQL and returned the normal `UP` health response. The Vue login page's
"数据来源：尚未连接" text is its normal pre-login state, not a connectivity
diagnostic; a later browser login verified the Vue Vite proxy uses the Java API.

## Phase three: bounded retries, dead letters, and replay

The third eventing slice is complete. It makes notification failures observable
and recoverable without rolling back an already committed activity registration.

- Outbox publication retries at most three times. After the final failure, the
  row remains in `outbox_events` with `dead_letter` status, failure text, and
  attempt count. An administrator can requeue it through the protected API.
- The notification consumer uses a bounded Kafka retry policy. Its final failed
  delivery is published to `campuslink.activity.events.v1.DLT`; the DLT
  listener persists or refreshes one `event_dead_letters` record per logical
  `(consumer_name, event_id)` with the original event and failure metadata.
- `GET /api/admin/eventing/operations` exposes pending, retrying, Outbox
  dead-letter, and consumer dead-letter counts. The replay endpoint requires
  an administrator JWT and an explicit `confirm=true` value. Each replay is
  audited through the eventing operations route.
- The Vue administrator console shows the same counters, failure reason, and a
  confirmation-gated replay action. It keeps real API errors rather than
  falling back to Mock; Mock provides only offline demonstration data.

The explicit-Byte-Buddy targeted backend suite ran 23 tests and passed. Vue
ran 48 tests, the production build, and the legacy smoke checks successfully.
The full Maven attempt again failed only where the restricted runner denies
Java loopback access to MySQL and has no Docker for Testcontainers. A
host-capable backend restart returned `UP`, created `event_dead_letters` in
the preserved MySQL database, and returned `401` for an unauthenticated event
operations request. Kafka/Docker are unavailable in this environment, so the
live broker-to-DLT route still needs Compose or another Kafka-capable host.

## Phase four: Spring Cloud Gateway migration

The fourth eventing slice is complete. Spring Cloud Gateway is a separate
WebFlux application in `gateway/`; it is not mixed into the existing MVC API.

- The gateway listens on `8081` and routes `/api/**` to the current API on
  `8080`, and `/ws/**` to its existing chat WebSocket endpoint. Vue Vite and
  Compose Nginx now use `8081` as their only live API target.
- The gateway verifies the existing HS256 JWT signature and expiration before
  forwarding a protected request. It forwards the original bearer token rather
  than trusting a forwarded identity header. The downstream API still verifies
  the corresponding MySQL session, immediate logout revocation, and all role
  checks.
- Public login and database-health routes retain their existing behavior. A
  rejected protected request still returns the established JSON
  `{ "message": "..." }` shape, so Vue displays a real `401`, `403`, `409`, or
  `500` instead of using Mock data.
- Compose now starts `gateway` after `api`; the frontend waits for the gateway
  health check. The gateway adds `X-CampusLink-Gateway: campuslink-gateway` to
  responses as a non-sensitive routing diagnostic.

The gateway unit suite has five passing tests. Vue has 48 passing tests, the
production build and legacy smoke checks pass, and the live equivalence check
passes through the Vue proxy while the legacy client continues to reach its
direct API path. Real local acceptance returned `200` for gateway health and
proxied database health, and returned the expected JSON `401` before an
unauthenticated administrator request reached the downstream API.

During Compose verification, the historical `bitnami/kafka:3.9.0` reference no
longer resolved from the registry. Compose now uses the available official
`apache/kafka:3.9.0` KRaft image with equivalent single-node listener and
transaction settings. The verification also exposed a pre-existing eventing
startup cycle: Kafka listener methods on the configuration class required an
event replay bean from that same class. Receipt, notification, and dead-letter
listeners now live in separate components, leaving the configuration class to
declare Kafka beans only. The full explicit-Byte-Buddy Maven suite now has 168
passing tests. A temporary `eventing` API on `18080` connected to the preserved
MySQL history and the healthy Compose Kafka broker, then returned the normal
database health response.

## Phase five: Notification service extraction

The fifth eventing slice extracts registration-result notification projection
into `notification-service/`, a standalone Spring Boot and MyBatis application
on port `8082`.

- It consumes `campuslink.activity.events.v1` with the established
  `campuslink-activity-notification-v1` consumer name. It persists only
  `activity_notifications` and processing receipts, and uses the title,
  waitlist position, and recipient present in the versioned event instead of
  reading activity or registration tables.
- It exposes the unchanged activity notification summary and read endpoints,
  validates the existing HS256 JWT, and verifies the matching MySQL session
  after the gateway. Social notifications remain on the core API in this
  incremental slice.
- After durable persistence, it emits a delivery event. The core API consumes
  that event only to publish the existing authenticated
  `activity.notification.created` WebSocket message, so Vue's notification
  store has no protocol change.
- Gateway sends `/api/activity-notifications/**` to `8082`; Compose disables
  the legacy local projection and starts the new service before gateway. A
  stopped notification service cannot roll back an activity transaction; Kafka
  retries and receipt idempotency let it catch up after recovery.

Validation: notification-service has two passing unit tests for duplicate
delivery and the read-summary contract. Gateway has five passing unit tests.
The core Maven suite, run with the explicit Byte Buddy agent against the
available MySQL and Testcontainers runtime, has 168 passing tests.

### Kafka contract compatibility correction

Kafka records now use JSON without Java type headers. Each consumer explicitly
selects its local contract DTO, so a package name change in one deployable
service cannot break another service's consumer. The WebSocket delivery
listener likewise declares its delivery DTO explicitly.

Real Compose acceptance created, approved, and registered a student for an
isolated activity. The core API returned `registered`, and
`notification-service` consumed the event and returned the persisted
`activity.registration.registered` notification with the expected activity
title and unread count.

## Phase six: Activity query and review service extraction

The sixth migration slice extracts published activity browsing, organizer
creation, and administrator review into `activity-service/` on port `8083`.
Gateway routes `/api/activities`, `/api/activities/managed`, and
`/api/admin/activities/**` to it; registration and check-in child routes stay
on the core API until phase seven.

The service revalidates the signed JWT and persisted session, applies the
existing organizer and administrator role rules, and owns direct access to
`activities` and `activity_reviews`. It uses a separate user-directory lookup
for display names instead of a cross-service join. A review decision and its
versioned `activity.review.*.v1` message are persisted in the same transaction
in the transitional Outbox. The core publisher sends it to Kafka, and the
notification service creates the organizer notification idempotently.

Validation used the isolated Compose stack only. An activity was created and
approved through activity-service; another was rejected, its Outbox row reached
`published`, and notification-service persisted **活动审核未通过** with the
stored reason. A temporary Gateway on port `18082` returned the published
activity with its diagnostic response header; a registration child route stayed
with the core API and returned the expected `403` for a teacher. Activity
service has two passing unit tests, notification-service has three, Gateway has
five, and the explicit-Byte-Buddy core Maven suite has 168 passing tests.

## Local runtime status

At handoff time, local MySQL was listening at `127.0.0.1:3306`, and the Java
API had been restarted on `8080` against the current phase-four code. Do not
assume either it or Vue remains alive in a new session.

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
4. Run `./script/run_gateway_idea.sh` from the repository root.
5. Verify `curl -fsS http://127.0.0.1:8081/api/database/health`.
6. Run `./script/run_frontend_demo.sh` from the repository root.
7. Open `http://127.0.0.1:5180`.

The important local addresses are:

- Vue default entry: `http://127.0.0.1:5180`
- Vue chat: `http://127.0.0.1:5180/workspace/contacts`
- Public gateway: `http://127.0.0.1:8081`
- Downstream Java API: `http://127.0.0.1:8080`
- Legacy fallback: `http://127.0.0.1:5179/?v=20260715-signed-jwt-logout-v1`

## Phase seven: Activity registration and check-in extraction

The seventh migration slice moves activity registration, waitlists, promotion,
credential rotation, credential verification, manual check-in, and activity
metrics into `activity-service`. Gateway now routes `/api/activities/**` and
`/api/admin/activity-metrics` to that service.

The activity service locks the activity before changing capacity-sensitive
registration state. It owns direct registration and credential table access,
uses separate user-directory lookups for roster names, and stores only a
SHA-256 credential digest. Each state transition writes a versioned event in
the same transactional Outbox row, with `activityId` used as the aggregate ID
and Kafka key. The existing core Outbox publisher is transitional until the
activity-owned publisher moves in a later infrastructure slice.

Validation through a temporary Gateway created and approved an activity, then
registered a student, rotated a credential, and checked the student in through
the organizer credential endpoint. The registration and check-in Outbox rows
were both `published`; notification-service persisted the matching
`activity.registration.registered` notification. Activity-service unit tests
cover waitlist creation and cancellation promotion in addition to phase six.

## Phase eight: Nacos, resilience, and operational proof

Nacos 3.0.3 now provides discovery and versioned central configuration for the
core API, activity service, notification service, and Gateway. The Compose
`nacos-config` job publishes the four `CAMPUSLINK_DEV` YAML files before the
applications start; every service imports its matching file and registers with
Nacos. Gateway uses service names (`lb://`) rather than fixed downstream
hosts, preserves JWT verification on both hops, and has Resilience4j circuit
breakers with explicit `503` fallback messages for activity and notification
routes.

Compose now also starts Jaeger, Prometheus, and Grafana. Micrometer publishes
HTTP, Kafka, Outbox, retry, and dead-letter metrics; the core API management
endpoint is isolated on its internal `8085` port for Prometheus. The normal
API port does not expose those metrics without an administrator JWT. The
Prometheus endpoint is public only when the request reaches a separate
management port, and it remains `ROLE_ADMIN`-protected when management shares
the local API port. Gateway, activity-service and notification-service now use
their own loopback-by-default management ports (`8084`, `8086`, `8087`); Compose
opens them only on its private network for Prometheus. HTTP timer histograms are
enabled for dashboard P95 latency.
OpenTelemetry tracing is enabled in all four applications and Kafka
producer/listener observation is enabled for the eventing services.
The extracted service Maven builds now declare the Byte Buddy agent explicitly
for Surefire, so Mockito inline mocks don't rely on a JDK self-attachment
capability that newer JVMs can disable.

The July 26 full eight-step audit corrected three operational defects: CI now
unwraps Nacos configuration JSON before checking the config version; the core
API keeps Prometheus administrator-protected when it shares the application
port; and all four applications publish standard HTTP histograms for the
Grafana request-rate and P95 panels. The audit also separated Gateway, activity,
and notification Actuator ports from their business ports, bound Compose host
ports to `127.0.0.1`, and confirmed the five Prometheus targets, P95 query,
Nacos configuration, Gateway health, and internal-only management endpoints.
The full explicit-Byte-Buddy core suite still passed 168 tests; activity,
notification, and Gateway passed 4, 3, and 6 tests; Vue passed 48 tests,
built successfully, and the legacy smoke check passed.

Host-capable verification used isolated API/Gateway mappings `18080`/`18084`
to avoid an existing host API. Nacos readiness, the four healthy registrations,
the `campuslink-gateway.yaml` version and `lb://` route, Gateway-proxied
database health, Jaeger service traces, and all five Prometheus targets were
confirmed. The full explicit-Byte-Buddy core suite passed 168 tests; activity,
notification, and Gateway suites passed 4, 3, and 6 tests; Vue passed 48 tests
and built successfully; the legacy smoke check passed.

## Redis activity catalog cache

After the user approved a post-migration Redis slice on July 27, 2026, the
activity service added an optional public-catalog cache. It only caches the
shared `GET /api/activities` response, whose payload contains no current-user
registration state. The current-registration, roster, credential, notification,
and session routes remain uncached.

- Compose runs Redis only on its internal network, without a host port or
  persistent volume. Native development keeps the cache disabled by default.
- The cache uses a versioned key plus a short TTL with jitter. It stores no
  capacity, registration, check-in, notification, or authentication truth.
- Activity review, registration, and cancellation publish a local cache-change
  event. A transactional listener advances the cache version only after the
  MySQL transaction commits, so a rollback cannot invalidate early.
- Redis errors increment Micrometer cache-error metrics and return the MySQL
  result; they never trigger a Mock fallback or alter the existing transaction.

The activity-service test suite now covers cache hits, MySQL fallback, version
invalidation, and the after-commit invalidation listener. A host-capable run
also verified the adapter against a temporary `redis:7.4.2-alpine`
Testcontainers instance. Docker CLI is unavailable in this restricted shell, so
full Compose runtime validation must still run on a Docker-capable host without
removing its retained named volumes.

## Redis check-in credential rate limit

The follow-up Redis slice adds a narrowly scoped rate limiter in `activity-service`.
It protects only authenticated credential issuance/rotation and organizer
credential verification, with separate, encoded keys for the action, user, and
activity. A Redis Lua script performs `INCR` and the first `PEXPIRE` atomically;
Compose/Nacos configures a one-minute window with limits of 5 credential issues
and 10 credential verifications. Over-limit requests return the real HTTP 429
response, while Redis outages increment `campuslink.redis.check_in_rate_limit`
error metrics and deliberately fail open so a replaceable Redis service cannot
block an in-person check-in. The credential digest and check-in transaction stay
in MySQL. Grafana dashboard version 3 charts allowed, rejected, and Redis-error
counts by action. Activity-service tests cover the 429 boundary, Redis outage
policy, and an actual temporary `redis:7.4.2-alpine` counter.

## Gateway Redis rate limit

The next Redis slice moves protection to the public Gateway boundary. Compose
enables Spring Cloud Gateway `RequestRateLimiter` filters from Nacos for every
HTTP API route, with a five-per-minute anonymous token bucket for `/api/auth/**`
and a 30-per-minute bucket for the other API routes. WebSocket and internal
management traffic are deliberately excluded. The key resolver uses the subject
already verified by the existing JWT filter, or the remote address for public
requests, and stores only an HMAC-SHA-256 fingerprint in Redis. No identity header is
added or trusted downstream. A rejected request remains a real HTTP 429; a
Redis outage at the ingress remains a real failure rather than a Mock fallback.
Gateway accepts the frontend Nginx forwarded client address only in the
loopback-bound Compose topology, so anonymous browsers do not share one bucket.
Gateway starts after Redis in Compose, and Grafana dashboard version 4 shows
Gateway 429 and 5xx events by route.

The July 27 Redis audit also corrected the Compose proxy boundary. Nginx now
replaces, rather than appends, a client-provided `X-Forwarded-For` value before
the Gateway derives an anonymous rate-limit key. This prevents a caller from
choosing the first forwarded address that Spring WebFlux uses for the request's
remote address.

## Redis catalog cache stampede protection

The public activity catalog cache now uses a five-second Redis lease when it
misses. The owner double-checks the cache after it acquires the lease, loads
MySQL only if it remains empty, and releases only its own lease through a Lua
compare-and-delete script. Other instances retry the cache four times at
25-millisecond intervals, then safely fall back to MySQL rather than waiting
indefinitely. This protection is limited to the user-independent public catalog;
it never locks registration, capacity, check-in, notifications, or authentication.
Micrometer exposes lease-acquired, wait-hit, and timeout counters, and Grafana
dashboard version 5 charts them with the existing cache signals. A real Redis
concurrency test verifies six simultaneous reads produce one database load.

## Redis registration idempotency

`POST /api/activities/{activityId}/registrations` now accepts an optional
`Idempotency-Key`. For a valid key, activity-service HMAC-fingerprints the user,
activity, and client key, claims a 30-second Redis processing marker, and stores
the successful `RegistrationView` for 24 hours after the MySQL transaction
returns. A retry replays the same `201` result without creating another Outbox
event; a concurrent request receives a real `409`. Redis failures retain the
existing MySQL row-lock behavior. Grafana dashboard version 6 reports claims,
replays, in-flight conflicts, and Redis errors. The real Redis test confirms a
replay invokes the registration action only once.

## Redis infrastructure observability

Compose now runs the internal-only `oliver006/redis_exporter:v1.84.0` service.
Prometheus adds a sixth `redis` target at `redis-exporter:9121`; neither Redis
nor the exporter receives a host port or a volume. Grafana dashboard version 7
adds memory, connected-client, keyspace-hit-ratio, and command-rate panels so
the Redis infrastructure can be correlated with the existing application-level
cache, rate-limit, and idempotency counters.

## Redis Prometheus alert rules

Prometheus now mounts `observability/alerts.yml` as an internal rules file. It
records Redis application errors per minute and evaluates warning alerts
when the Redis Exporter stays unavailable for two minutes, when Redis stays
unavailable behind a reachable Exporter for two minutes, or when application
Redis errors continue across every one-minute window for five minutes. Compose
intentionally has no Alertmanager, so the rules only appear in Prometheus or
Grafana alert views and cannot send an external notification or mutate any
retained data.

## Redis alert runtime CI verification

The Compose workflow now checks the current Gateway Nacos config version, six
healthy Prometheus targets, and the Prometheus Rules API for all three Redis
alerts. It catches stale Nacos-version assertions and a mounted rule file that
does not load, without changing or deleting any retained Compose volume.

## July 26, 2026 final handoff snapshot

The latest verified commit is `317a5c0` (`Audit and harden microservice
operations`) on `main`. It follows the phase-eight Nacos and observability
commit `4f5d7dc`. The worktree was clean immediately after the push.

The intended primary development page remains the Vue Vite entry at
`http://127.0.0.1:5180`. It uses the host-native development chain and the
local MySQL history at `127.0.0.1:3306`. The Docker Compose page at
`http://127.0.0.1:5179` is a separate, production-style microservice demo;
it uses the `campuslink-mysql` Docker named volume, so its content can differ
from the host-native page. It is not a replacement for the primary entry. The
legacy regression page is `http://127.0.0.1:5179/legacy/` when Compose runs.

At handoff time, the isolated Compose stack was running with API and Gateway
mapped to `127.0.0.1:18080` and `127.0.0.1:18084` to avoid host services. Its
Vue page, Nacos, Kafka, Jaeger, Prometheus, and Grafana were all bound only to
`127.0.0.1`; do not assume this runtime snapshot persists in a later session.
The Compose MySQL container and Docker named volumes were retained, and the
host MySQL database was never deleted, reset, reseeded, or cleaned.

Before any new work, read the three `AGENTS.md` files and the handoff records,
then run `git status --short --branch`. Confirm MySQL availability before
starting the native backend. For the complete core Maven suite, use the
explicit Byte Buddy command in `backend/AGENTS.md`. All eight authorized
migration steps are complete, so obtain the user's product-priority approval
before implementing another feature.

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

All eight authorized migration phases are complete. Do not start another
feature without the user's product-priority approval.

## Related records

Use the following documents for detailed historical context:

- [`resume-project-roadmap.md`](resume-project-roadmap.md)
- [`vue-migration-handoff.md`](vue-migration-handoff.md)
- [`phase-two-activity-handoff.md`](phase-two-activity-handoff.md)
- [`new-chat-handoff-2026-07-08.md`](new-chat-handoff-2026-07-08.md)
- [`admin-review-workbench-handoff.md`](admin-review-workbench-handoff.md)
- [`admin-moderation-content-module-fix.md`](admin-moderation-content-module-fix.md)
