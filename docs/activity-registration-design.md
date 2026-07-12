# Activity registration and waitlist design

This design defines the next CampusLink activity-domain slice: a student can
browse published activities, register, cancel, and join a fair waitlist when
an activity is full. It preserves the existing `ActivityService` boundary and
does not place registration rules in `FeedService` or `AdminService`.

## Scope and terms

This slice implements registration and cancellation only. Notifications,
organizer rosters, check-in, exports, and administrator metrics remain later
slices. The existing activity review workflow remains unchanged.

- **Registered** means the student holds a capacity slot.
- **Waitlisted** means the student has no slot and waits in a stable queue.
- **Promotion** means the first waitlisted student receives a released slot.
- **Active registration** means a registration in `registered` or `waitlisted`
  status.

## Data model

The current `activities.capacity` remains the source of truth for the maximum
number of registered students. The service derives counts from active
registration rows instead of persisting a mutable counter that can drift.

### `activity_registrations`

This table holds one current registration row per activity and attendee.

| Column | Purpose |
| --- | --- |
| `id` | Server-generated 32-character registration identifier. |
| `activity_id` | The activity being joined. |
| `attendee_id` | The authenticated student. It never comes from a request body. |
| `status` | `registered`, `waitlisted`, `checked_in`, or `cancelled`. |
| `registered_at` | Most recent time the attendee received a slot. |
| `waitlisted_at` | Time used to order the active waitlist. |
| `cancelled_at` | Most recent cancellation time. |
| `created_at` and `updated_at` | Current-row audit timestamps. |

The table has `unique (activity_id, attendee_id)`. A cancelled attendee keeps
the same row and may register again; a new waitlist attempt gets a new
`waitlisted_at` and therefore joins at the end of the queue. Indexes support
active-status counting, current-user lookup, and the ordered waitlist query.

### `activity_registration_events`

This append-only table preserves transitions that the current row no longer
shows. It contains `id`, `registration_id`, `activity_id`, `attendee_id`,
`actor_id`, `event_type`, `from_status`, `to_status`, and `created_at`.

The initial event types are `registered`, `waitlisted`, `cancelled`, and
`promoted`. The actor is the student for registration and cancellation; it is
the service actor for an automatic promotion. A later notification slice can
reliably consume this history without inferring events from mutable rows.

The schema continues the repository's application-level user references and
doesn't introduce foreign keys solely for this slice. It adds indexes on
`(activity_id, status)`, `(activity_id, status, waitlisted_at, id)`, and
`(attendee_id, updated_at)`.

## State machines

The existing activity states remain fixed. Registration changes only the two
participant-facing activity states below.

```text
published --last slot taken--> full
full --registered attendee cancels and no waitlist--> published
full --registered attendee cancels and first waitlisted attendee promotes--> full
published/full --close or cancel--> closed/cancelled
```

The participant state machine is independent of review decisions.

```text
new --slot available--> registered --cancel--> cancelled
new --full------------> waitlisted --cancel--> cancelled
waitlisted --first slot released--> registered
cancelled --register again--> registered or waitlisted
registered --check in (later slice)--> checked_in
```

`checked_in` is reserved for the organizer check-in slice. It consumes a slot
just like `registered` and this slice does not expose a check-in endpoint.
Only activities in `published` or `full` accept registrations. Draft, pending,
closed, and cancelled activities reject both new registration and cancellation
requests as applicable.

## Permission matrix

All endpoints resolve the caller from the bearer token. They don't accept an
attendee, organizer, reviewer, or actor ID from the client.

| Capability | Student | Teacher | Club leader | Administrator | Organizer of activity |
| --- | --- | --- | --- | --- | --- |
| Browse published or full activities | Allowed | Allowed | Allowed | Allowed | Allowed |
| Read own registration state | Allowed | Denied | Denied | Denied | Denied |
| Register or join the waitlist | Allowed | Denied | Denied | Denied | Denied |
| Cancel own active registration | Allowed | Denied | Denied | Denied | Denied |
| Register as another attendee | Denied | Denied | Denied | Denied | Denied |
| Register for own activity | Denied | Denied | Denied | Denied | Denied |

The current demo identifies a student through a role containing `学生`.
`requireStudentAttendee` belongs in the independent activity-registration
service, not the generic authentication service. The explicit organizer check
prevents a future dual-role account from consuming its own capacity.

## API contract

The current authenticated `GET /api/activities` endpoint expands to include
both `published` and `full` activities. It accepts optional `category`,
`startsFrom`, and `startsTo` filters so the student UI can browse by category
and time. It returns capacity, `registeredCount`, `waitlistedCount`,
`remainingCapacity`, and the caller's current registration summary.

The registration endpoints are all authenticated:

| Method and path | Result |
| --- | --- |
| `GET /api/activities/{activityId}/registration` | Returns the caller's current registration or an explicit no-registration result. |
| `POST /api/activities/{activityId}/registrations` | Creates or reactivates the caller's registration. Returns `201` with `registered` or `waitlisted`, plus queue position when waitlisted. |
| `DELETE /api/activities/{activityId}/registrations/current` | Cancels the caller's active registration and returns the updated activity summary. |

A duplicate active registration returns `409`, as does cancelling an already
cancelled or missing registration. A non-student or organizer attempting to
register returns `403`. A missing activity returns `400` under the existing
error convention, and a non-registerable lifecycle state returns `409` with a
clear message. The request body is empty for this slice.

`RegistrationView` exposes only the current caller's registration ID, status,
registration time, waitlist time, queue position when applicable, and activity
summary. It doesn't expose other attendees or the organizer roster.

## Transaction and concurrency boundary

`ActivityRegistrationService` owns the whole transition and is annotated with
`@Transactional`. It depends on `ActivityRegistrationRepository` and extends
the activity repository only with activity-domain queries. Controllers never
call a mapper or repository directly.

Each registration or cancellation obtains the activity row with `SELECT ...
FOR UPDATE`. This serializes capacity decisions for one activity while leaving
unrelated activities concurrent. Within that lock, the service reads or locks
the caller's current row, counts `registered` and `checked_in` rows, and then
writes the current row, event row, and any activity-status transition together.

On cancellation of a `registered` attendee, the service locks the first row
where `status = 'waitlisted'`, ordered by `waitlisted_at, id`. It promotes that
one attendee in the same transaction and appends both the cancellation and
promotion events. If no candidate exists, it changes `full` back to
`published`; otherwise it remains `full`. Cancelling a waitlisted attendee
doesn't affect capacity or activity status.

An exception from any registration update, event insertion, or activity-status
update rolls back all writes. The design deliberately doesn't publish a
WebSocket event in this transaction. A later notification slice consumes the
committed event history after transaction completion.

## Test design

Implementation proceeds test first, with focused tests at the service,
controller, and MyBatis levels.

### Service rules

- A student receives `registered` while capacity remains and the final slot
  changes the activity to `full`.
- A student receives `waitlisted` when capacity is full, and queue order uses
  `waitlisted_at, id`.
- A student cannot create a duplicate active registration or register for an
  organizer-owned activity.
- Teacher, club-leader, and administrator accounts receive `403` for attendee
  actions.
- Cancelling a waitlisted registration only cancels that row.
- Cancelling a registered row promotes exactly the oldest waitlisted attendee.
- Cancelling the final registered row without a waitlist changes `full` to
  `published`.
- Draft, pending, closed, and cancelled activities reject registration; a
  student cannot cancel someone else's record because the API never accepts an
  attendee ID.

### HTTP boundaries

- Category and time filters include only `published` and `full` activities.
- Registration responses expose the authenticated caller's state, not a
  client-supplied attendee identity.
- Full capacity returns `201` with `waitlisted`, not an error.
- Duplicate registration and repeated cancellation return `409` in the
  existing `{ "message": "..." }` error format.
- Missing authentication returns `401`, and all denied roles return `403`.

### MyBatis and rollback integration

`ActivityRegistrationRepositoryIntegrationTest` uses local MySQL with
`spring.sql.init.mode=never`, `@Transactional`, and `@Rollback`, following the
existing activity repository test. It verifies row mapping, registration and
event creation, cancellation plus promotion, activity-status transitions, and
the waitlist ordering query. After each test it confirms matching test activity
and registration rows are absent, so local historical data remains intact.

A dedicated transaction-failure integration case makes the event write fail
after the registration write and verifies that neither the current registration
nor the activity status changed. A contention integration test invokes multiple
independent service transactions against capacity one and verifies exactly one
registered attendee, all remaining callers waitlisted, and no count exceeds
capacity. Its fixture uses a unique test prefix and rolls back or removes only
its own fixture rows after verification; it never resets the demo database.

## Implementation order

Implement the next vertical slice in this order:

1. Add the two tables, entities, mapper queries, repository interface, and
   rollback-safe MyBatis integration tests.
2. Add `ActivityRegistrationService`, DTOs, and service tests for the state
   machine and transaction rules.
3. Add the authenticated controller endpoints and MockMvc boundary tests.
4. Extend the live and Mock frontend adapters together, then add activity-card
   registration controls and clear registered, waitlisted, and cancelled
   feedback.
5. Run relevant Maven tests, `./script/run_frontend_check.sh`, and browser
   checks for student registration, administrator content review feedback, and
   chat before committing the slice.

## Next steps

The next implementation task adds the registration persistence layer and its
rollback-safe tests. It does not yet add frontend controls, notifications,
roster management, or check-in.
