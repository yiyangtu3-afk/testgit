# CampusLink Java backend learning roadmap

This roadmap teaches CampusLink from simple Java and Spring Boot concepts to
the project's concurrency and microservice design. Complete each lesson by
reading the listed code, running its test, explaining the request flow aloud,
and making one small safe change.

## Current progress

- Started: July 31, 2026.
- Background: Java fundamentals and basic Spring Boot only.
- Current lesson: Week 1, lesson 8 — JWT request headers and token parsing.
- Native learning environment: host MySQL is reachable on `127.0.0.1:3306`;
  backend is healthy on `127.0.0.1:8080`, and Vue is running on
  `127.0.0.1:5180`.
- Completed topics: Week 1, lesson 1 — followed `GET /api/database/health`
  from the Controller through Service and MyBatis Mapper to host MySQL; Week 1,
  lesson 2 — explained Spring beans, constructor injection, `final`, and Mock
  replacement in unit tests; Week 1, lesson 3 — explained MyBatis Mapper
  interfaces, `@Select`, and SQL result mapping; Week 1, lesson 4 — explained
  `Map`, `LinkedHashMap`, key-value pairs, and JSON serialization; Week 1,
  lesson 5 — explained public health checks, `permitAll`, `authenticated`, and
  JWT-protected API boundaries; Week 1, lesson 6 — distinguished HTTP 401
  authentication failures from HTTP 403 authorization failures; Week 1, lesson
  7 — observed a real unauthenticated API response with HTTP 401.

## Learning sequence

### Week 1: Java and Spring Boot request flow

Learn Java collections, exceptions, interfaces, constructor injection,
annotations, and the meaning of `@RestController`, `@Service`, and
`@Transactional`. Follow the database health request through
`backend/src/main/java/com/campuslink/controller/DatabaseController.java`, its
service or repository dependency, the MyBatis mapper, and MySQL.

You must be able to explain why a controller does not write SQL and why the
same request can return JSON without manual serialization.

### Week 2: Login, JWT, and authorization

Follow `AuthController`, `AuthService`, `JwtTokenCodec`,
`JwtAuthenticationFilter`, and `SecurityConfig`. Learn the difference between
401 and 403, why the server does not trust a client-submitted user ID, and how
the bearer token reaches protected endpoints.

### Week 3: Layered activity business logic

Trace activity creation, review, and publishing across DTOs, controllers,
services, repositories, mappers, and `backend/src/main/resources/schema.sql`.
Learn state transitions and the purpose of DTOs, entities, and transactions.

### Week 4: Registration transactions and MySQL concurrency

Study `activity-service/src/main/java/com/campuslink/activity/service/ActivityRegistrationApplicationService.java`
and `ActivityRegistrationConcurrencyIntegrationTest.java`. Explain activity
row locks, the `(activity_id, attendee_id)` unique constraint, waitlists, and
why MySQL remains the source of truth for capacity and ordering.

### Week 5: Redis cache, idempotency, and rate limiting

Study the activity catalog cache, registration idempotency, and
`RedisActivityRegistrationRateLimiter`. Explain cache invalidation after
commit, `Idempotency-Key`, HTTP 409 and 429, HMAC-fingerprinted Redis keys,
and why Redis failure falls back to MySQL for registration correctness.

### Week 6: Gateway, Nacos, Kafka, and Outbox

Trace a request from Vue through Gateway to activity-service, then from the
transactional Outbox through Kafka to notification-service. Explain the roles
of service discovery, configuration, consumer idempotency, and eventual
consistency.

### Week 7: Docker, metrics, and controlled load testing

Read `compose.yml`, inspect Prometheus and Grafana, and use
`script/ActivityRegistrationLoadTest.java` only with isolated Compose data.
Learn throughput, P50, P95, error rates, and how the rate-limit metrics expose
load shedding.

### Week 8: Interview rehearsal

For each completed module, give a three-minute explanation without reading
code. Practice answering why `@Transactional` alone is insufficient for
concurrency, how duplicate registration is prevented, why Outbox is used, and
what Gateway, Nacos, Kafka, Redis, and MySQL each own.

## Lesson routine

Use this routine for every lesson:

1. Run the related feature and observe its response.
2. Draw the request and data flow on paper.
3. Read controller, service, persistence, and test code in that order.
4. Explain the design in your own words.
5. Make a small guided change and run the relevant test.
6. Record the completed lesson in the progress section.
