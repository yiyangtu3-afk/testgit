# Plan: Spring Cloud Kafka microservices upgrade

This plan evolves CampusLink from a modular monolith to an event-driven,
service-oriented system without changing the Vue default entry, JWT boundaries,
legacy regression path, or existing MySQL history.

> Source: July 25, 2026 architecture planning conversation.

## Architectural decisions

- **Spring platform:** Keep Spring Boot 3.5 and use the Spring Cloud 2025.0.x
  release train through its BOM.
- **Messaging:** Use Kafka as the only broker. Do not add RabbitMQ.
- **Reliable delivery:** Write each business change and its `outbox_events` row
  in one local MySQL transaction. Publishers may retry, and consumers must be
  idempotent.
- **Security:** The gateway and every downstream service validate the existing
  JWT. Internal requests do not rely only on forwarded identity headers.
- **Data ownership:** Each extracted service owns its domain tables. During the
  migration, services may use separate schemas in the existing MySQL instance;
  they must not use cross-service joins or distributed transactions.
- **Configuration and discovery:** Use Nacos after the initial Kafka and
  service-extraction slices are stable.
- **Frontend:** Vue remains the default application. It accesses services only
  through the gateway when the gateway phase is complete.

---

## Phase 1: Reliable activity-registration event foundation

**User stories:** A student can register, waitlist, cancel, be promoted, or
check in exactly as before, while each successful state transition produces a
durable event that can be published after the transaction commits.

### What to build

Add Kafka to the local Compose environment, define versioned activity
registration event contracts, and introduce a transactional Outbox. The
existing activity workflow writes its current state, event history, and an
Outbox event in the same local transaction. With the `eventing` profile
enabled, a publisher sends pending events to Kafka and an idempotent receipt
consumer records delivery. Existing in-process notifications remain unchanged
in this phase.

### Acceptance criteria

- [x] Existing registration, waitlist, cancellation, promotion, and check-in
  behavior remains unchanged.
- [x] Each successful registration state transition creates one versioned Outbox
  event in the same local transaction.
- [x] The local Compose stack defines Kafka and the eventing-enabled API.
- [x] A Kafka receipt consumer processes each event at most once logically.
- [x] Automated tests cover Outbox creation, publish retry behavior, and
  idempotent receipt handling.

---

## Phase 2: Kafka-backed activity notifications

**User stories:** A student receives the same registration or waitlist result
notification even when notification processing is asynchronous.

### What to build

Make registration-result notifications a Kafka consumer projection. Preserve
the current notification API and WebSocket behavior while moving event-derived
notification creation behind idempotent event consumption.

### Acceptance criteria

- [x] Registration, waitlist, and promotion notifications are projected from
  Kafka events when the `eventing` profile is active.
- [x] Duplicate Kafka delivery does not create duplicate notifications.
- [x] Existing Vue notification views and unread counts remain compatible.
- [x] Notification processing stays decoupled from the activity transaction;
  the outbox retains events until a broker accepts them.

---

## Phase 3: Failure handling and replay

**User stories:** A failed notification event is visible and recoverable without
blocking the student's activity registration.

### What to build

Add bounded retries, a dead-letter topic, failure metadata, and a protected
operator replay action for activity notification events. Expose Outbox and
consumer health metrics.

### Acceptance criteria

- [ ] A consumer failure does not roll back a completed activity registration.
- [ ] Failed messages reach a dead-letter path after configured retries.
- [ ] Administrators can inspect and replay an eligible failed event.
- [ ] Metrics distinguish pending Outbox events, retries, and dead-lettered
  events.

---

## Phase 4: Gateway migration

**User stories:** A user uses the existing Vue application through one stable
API entry point without seeing changed authorization or error behavior.

### What to build

Add Spring Cloud Gateway as the public API and WebSocket entry point. Start by
routing all current routes to the existing application while preserving JWT,
CORS, error responses, and health checks.

### Acceptance criteria

- [ ] Vue login, chat, activities, notifications, and administrator flows work
  through the gateway.
- [ ] Gateway and downstream application both enforce the existing JWT rules.
- [ ] API errors remain visible to the frontend and never trigger Mock fallback.
- [ ] Legacy static frontend regression checks remain intact.

---

## Phase 5: Notification service extraction

**User stories:** Notification delivery can scale and restart independently of
activity registration.

### What to build

Extract notification persistence, read actions, unread counts, and real-time
delivery into `notification-service`. It consumes versioned Kafka events and
owns its notification data rather than reading activity tables directly.

### Acceptance criteria

- [ ] Notification service runs independently from the activity-producing
  application.
- [ ] Activity registration succeeds while notification service is unavailable.
- [ ] The service catches up from Kafka after recovery without duplicates.
- [ ] Vue notification behavior remains unchanged through the gateway.

---

## Phase 6: Activity query and review service extraction

**User stories:** Users browse activities and organizers create or submit
activities while the activity domain can be deployed independently.

### What to build

Extract published activity queries, activity creation, and administrator review
from the core application into `activity-service`. Route the activity APIs via
the gateway and keep all current role checks.

### Acceptance criteria

- [ ] Published activity filtering, creation, and review work through
  `activity-service`.
- [ ] Existing roles and review audit data remain enforced and available.
- [ ] The activity service owns the migrated activity data boundary.
- [ ] Vue activities and administrator review flows remain functional.

---

## Phase 7: Activity registration and check-in service extraction

**User stories:** A student registers, waits, is promoted, or checks in through
the extracted activity domain with reliable events and unchanged rules.

### What to build

Move registration, waitlist promotion, credential verification, check-in, and
the activity Outbox into `activity-service`. Key Kafka records by `activityId`
to preserve event order within an activity.

### Acceptance criteria

- [ ] Capacity control, waitlist order, and check-in rules remain correct.
- [ ] Registration events remain durable, versioned, ordered per activity, and
  idempotently consumed.
- [ ] No cross-service database transaction or join is required.
- [ ] Existing browser and API activity acceptance checks pass through the
  gateway.

---

## Phase 8: Nacos, resilience, and operational proof

**User stories:** Operators can discover services, manage environment-specific
configuration, trace requests, and diagnose failures in the multi-service
system.

### What to build

Add Nacos for service discovery and centralized configuration. Add bounded
synchronous-call resilience, distributed traces, metrics dashboards, container
integration tests, and CI coverage for the Compose stack.

### Acceptance criteria

- [ ] Gateway, activity service, and notification service register with Nacos.
- [ ] Service configuration changes are environment-scoped and versioned.
- [ ] Traces connect a gateway request, activity transaction, Outbox publish,
  Kafka consumption, and notification delivery.
- [ ] Dashboards expose HTTP, Kafka, Outbox, retry, and dead-letter health.
- [ ] CI verifies the complete multi-service stack without using local MySQL
  history.

## Next steps

Complete and review each phase independently. Do not begin the next phase until
the current phase has passed its relevant tests and browser or API acceptance
checks, and the user has approved the result for commit and push.
