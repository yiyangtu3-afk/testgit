# CampusLink activity domain design

This document defines the first phase-two backend slice for campus activities.
The slice lets a teacher or club leader submit an activity, then lets an
administrator approve or reject it without trusting client-supplied organizer
identity.

## Scope

The first slice includes activity submission, the administrator's pending
queue, review, and the published activity list. Registration, waitlisting,
notifications, organizer roster management, check-in, and metrics remain later
vertical slices.

Activity code remains an independent domain. `ActivityService`, activity DTOs,
the activity repository, and the activity MyBatis mapper own this workflow.
`FeedService` and `AdminService` don't contain activity rules.

## Data model

The slice adds `activities` for current state and `activity_reviews` for an
append-only workflow history. The service writes both tables in one
transaction for submissions and review decisions.

### Activities

The `activities` table stores the current user-visible activity state.

- `id`: A server-generated 32-character activity identifier.
- `title`: A required title with a maximum length of 120 characters.
- `description`: Required activity details with a maximum length of 2,000
  characters.
- `category`: A required category with a maximum length of 60 characters.
- `location`: A required location with a maximum length of 160 characters.
- `starts_at`: The local activity start date and time.
- `ends_at`: The local activity end date and time. It must be later than
  `starts_at`.
- `capacity`: A positive participant limit no greater than 10,000.
- `organizer_id`: The authenticated creator's user ID. The API never accepts
  this value from the request body.
- `status`: The current activity lifecycle state.
- `review_decision`: The latest review state: `pending`, `approved`, or
  `rejected`.
- `review_reason`: The latest rejection reason. It is required for a rejection
  and cleared when the activity is submitted or approved.
- `reviewed_by`: The latest administrator reviewer, or `NULL` before review.
- `reviewed_at`: The latest review time, or `NULL` before review.
- `created_at`: The submission creation time.
- `updated_at`: The latest current-state update time.

Indexes cover the published browse query, the administrator pending queue, the
organizer's activities, and chronological review history. Foreign-key
constraints aren't introduced in this slice because the existing schema uses
application-level user references consistently.

### Activity reviews

The `activity_reviews` table records every submission and administrator
decision so a later decision never erases the earlier reason or actor.

- `id`: A server-generated 32-character review event identifier.
- `activity_id`: The reviewed activity.
- `actor_id`: The authenticated organizer or administrator who caused the
  event.
- `decision`: `submitted`, `approved`, or `rejected`.
- `reason`: The rejection reason, or `NULL` for submission and approval.
- `created_at`: The event time.

## State machine

The activity lifecycle uses the fixed phase-two states. Review outcome is a
separate field so rejection doesn't introduce an incompatible activity state.

```text
draft --submit--> pending --approve--> published
                         --reject----> draft

published <--> full
published/full --> closed
draft/pending/published/full --> cancelled
```

This slice implements the following transitions:

- Creation acts as the initial submission and produces `pending` with a
  `pending` review decision.
- Approval requires `pending` plus a `pending` review decision and produces
  `published` with an `approved` decision.
- Rejection requires `pending` plus a `pending` review decision and returns the
  activity to `draft` with a `rejected` decision and a required reason.
- A second decision on an already reviewed activity fails and doesn't append a
  review event.

Later slices can add editing and `draft` resubmission, capacity-driven
`published` and `full` transitions, closure, and cancellation.

## Permission matrix

All protected endpoints derive the current account from the bearer token.
Request bodies contain no organizer, reviewer, or current-user ID.

| Capability | Student | Teacher | Club leader | Administrator |
| --- | --- | --- | --- | --- |
| Create and submit an activity | Denied | Allowed | Allowed | Denied |
| Supply a different organizer ID | Not accepted | Not accepted | Not accepted | Not accepted |
| View published activities | Allowed | Allowed | Allowed | Allowed |
| View the pending review queue | Denied | Denied | Denied | Allowed |
| Approve an activity | Denied | Denied | Denied | Allowed |
| Reject an activity with a reason | Denied | Denied | Denied | Allowed |

For the current demo data, a role containing `教师` identifies a teacher, a
role containing `社团负责人` identifies a club leader, and a role containing
`管理员` identifies an administrator. Activity authorization stays in
`ActivityService` instead of broadening the generic authentication service
with domain rules.

## API contract

The first slice exposes four authenticated endpoints.

- `POST /api/activities` creates a `pending` activity. The request contains
  `title`, `description`, `category`, `location`, `startsAt`, `endsAt`, and
  `capacity`.
- `GET /api/activities` returns only `published` activities, including local
  MySQL history when the Java API is available.
- `GET /api/admin/activities/pending` returns only the pending activity review
  queue and requires an administrator.
- `POST /api/admin/activities/{activityId}/reviews` accepts `decision` as
  `approve` or `reject`. A rejection also requires a nonblank `reason`.

The activity response includes server-derived organizer identity, lifecycle
status, latest review decision and reason, reviewer identity, review time, and
creation time. Invalid input returns `400`, missing or invalid authentication
returns `401`, and insufficient role permissions return `403` through the
existing global error format.

## Transaction boundaries

The service owns both cross-table boundaries.

1. Creation inserts the activity and its `submitted` history event in one
   transaction.
2. Review updates current activity state and inserts the `approved` or
   `rejected` history event in one transaction.

Any exception rolls back both writes. The MyBatis integration test disables
`schema.sql` and `data.sql`, uses the local MySQL schema, and wraps each test in
a rollback transaction so historical demo rows remain intact.

## Test design

Tests verify public behavior in small red-green cycles.

### Service rules

- A teacher creates an activity whose organizer comes from authentication and
  whose state is `pending`.
- A club leader can create the same activity type.
- A student and an administrator receive `403` when they attempt creation.
- An end time that isn't later than the start time fails without persistence.
- An administrator approves a pending activity and receives `published`.
- An administrator rejection requires a reason, returns the activity to
  `draft`, and preserves the reason.
- A non-administrator can't review an activity.
- A reviewed or missing activity can't receive another decision.

### HTTP boundaries

- A valid teacher token can submit without an organizer ID in the payload.
- Validation rejects blank fields, invalid capacity, and malformed date/time
  input.
- A student submission returns `403` in the existing `{ "message": ... }`
  format.
- The pending queue and review endpoint require an administrator token.
- Approval returns the published status; rejection without a reason returns
  `400`.
- The published list excludes pending and rejected/draft activities.

### MyBatis and rollback

- Repository mapping round-trips all activity fields through local MySQL.
- Creation stores one activity and one `submitted` history event.
- Approval updates the activity and appends one review event.
- Rejection stores its reason in current state and review history.
- A failed repeated review leaves current state and history unchanged.
- Test transactions roll back and don't alter existing local MySQL history.

## Next steps

Implement the four endpoints with one behavior per red-green cycle. After the
slice passes its targeted tests, add registration and waitlist models as the
next independent activity-domain increment.
