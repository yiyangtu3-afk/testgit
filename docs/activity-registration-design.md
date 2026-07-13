# Activity registration and waitlist design

This design records the completed CampusLink activity-registration domain: a
student can register, cancel, and join a fair waitlist, while the organizer can
read the roster, check in registered attendees, and export roster data. It
preserves the `ActivityService` boundary and doesn't place activity rules in
`FeedService` or `AdminService`.

## Scope and terms

The current slice includes registration, cancellation, organizer rosters,
check-in, frontend CSV export, and real administrator activity metrics.
Persistent activity notifications remain in their independent notification
module. The existing activity review workflow remains unchanged.

- **Registered** means the student holds a capacity slot.
- **Waitlisted** means the student has no slot and waits in a stable queue.
- **Promotion** means the first waitlisted student receives a released slot.
- **Active registration** means a registration in `registered` or `waitlisted`
  status.
- **Occupied registration** means a registration in `registered` or
  `checked_in` status and consumes one capacity slot.

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
| `checked_in_at` | Time when the organizer confirmed attendance. |
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

The event types are `registered`, `waitlisted`, `cancelled`, `promoted`, and
`checked_in`. The actor is the student for registration and cancellation, the
actor that released the slot for automatic promotion, and the organizer for
check-in. Notifications can consume this history without inferring events from
mutable rows.

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
registered --organizer check in--> checked_in
```

`checked_in` consumes a slot just like `registered`. A checked-in registration
can't be cancelled through the student endpoint. Only activities in
`published` or `full` accept registrations. Draft, pending, closed, and
cancelled activities reject new registrations.

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
| Read managed activity list | Denied | Own only | Own only | Denied | Allowed |
| Read activity roster | Denied | Own only | Own only | Denied | Allowed |
| Check in a registered attendee | Denied | Own only | Own only | Denied | Allowed |
| Read aggregate activity metrics | Denied | Denied | Denied | Allowed | Denied |

The current demo identifies a student through a role containing `学生`.
`requireStudentAttendee` belongs in the independent activity-registration
service, not the generic authentication service. The explicit organizer check
prevents a future dual-role account from consuming its own capacity.

## API contract

The authenticated `GET /api/activities` endpoint includes both `published` and
`full` activities. It accepts optional `category`, `from`, and `to` filters so
the UI can browse by category and start date. The frontend loads the caller's
registration separately for every visible activity.

The registration endpoints are all authenticated:

| Method and path | Result |
| --- | --- |
| `GET /api/activities/{activityId}/registrations/current` | Returns the caller's current registration or `204` when none exists. |
| `POST /api/activities/{activityId}/registrations` | Creates or reactivates the caller's registration. Returns `201` with `registered` or `waitlisted`, plus queue position when waitlisted. |
| `DELETE /api/activities/{activityId}/registrations/current` | Cancels the caller's active registration. |
| `GET /api/activities/managed` | Returns activities owned by the current teacher or club leader. |
| `GET /api/activities/{activityId}/registrations/roster` | Returns the organizer-owned activity roster and counts. |
| `POST /api/activities/{activityId}/registrations/{registrationId}/check-in` | Checks in one registered attendee. |
| `GET /api/admin/activity-metrics` | Returns real occupied-registration and check-in counts to an administrator. |

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

An exception from any registration update, event insertion, activity-status
update, or notification write rolls back all writes. WebSocket delivery runs
after the transaction commits, so clients never receive a state change that
the database later rolls back.

Check-in locks the organizer-owned activity and selected registration. The
service updates the current row to `checked_in`, stores `checked_in_at`, and
appends the `checked_in` event before the transaction commits. Any failure
rolls back both writes.

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
- Only the activity organizer can read its roster or check in a participant.
- Check-in accepts only `registered`; repeated check-in and waitlist check-in
  return `409`.
- Administrator metrics count `registered` plus `checked_in` as occupied and
  count `checked_in` separately.

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
event creation, cancellation plus promotion, organizer roster mapping, and the
check-in timestamp and event. Each test transaction rolls back, so local
historical data remains intact.

## Implemented structure

The completed vertical slice follows this structure:

1. MyBatis entities, mapper queries, repositories, and rollback-safe MySQL
   integration tests own current rows, events, rosters, and metrics.
2. `ActivityRegistrationService` owns capacity, cancellation, promotion,
   roster authorization, check-in, and transaction boundaries.
3. Activity-specific controllers expose attendee, organizer, and administrator
   endpoints without routing activity logic through generic services.
4. Live and Mock frontend adapters use the same response shapes and error
   boundaries. The activity operations renderer owns roster, check-in, and CSV
   presentation.
5. The full Maven suite, frontend smoke check, and live browser regression
   cover activity operations, administrator review, moderation feedback, and
   chat.

## Next steps

The activity registration and organizer operations slices are complete. The
next roadmap task expands persistent notifications to the remaining social
events and makes likes user-scoped and reversible.
