# CampusLink demo

This repository contains a verifiable full-stack demo for a campus social
application. It combines a Vue browser frontend with a Spring Boot and
MySQL backend for authentication, social interaction, real-time chat, content
moderation, and administrative workflows.

> **Note:** Spring Boot is free and open source. You can run this backend with
> Maven and any compatible JDK. IntelliJ IDEA Ultimate is optional; IntelliJ
> IDEA Community, VS Code, or the command line can run the same project.

## What you can verify now

Open the local demo in a browser:

```bash
./script/run_frontend_demo.sh
```

Then visit `http://127.0.0.1:5180`. The preserved static baseline remains
available through `./script/run_legacy_frontend_demo.sh` at
`http://127.0.0.1:5179/?v=20260715-signed-jwt-logout-v1`.

## Current handoff

The current functional stable point is
`98c2dad Add notification read and target actions` on `main`. The latest
delivery adds activity check-in credentials. Friend-request notifications now
include a safe **处理申请** entry: the server resolves the notification to the
current recipient's pending request before the existing accept or reject
controls are shown. Start from
[`docs/new-chat-handoff-2026-07-08.md`](docs/new-chat-handoff-2026-07-08.md)
for the complete handoff, constraints, and local verification commands.

The Vue 3 migration is complete in the separate `frontend-vue/` application.
The completed slices cover authentication, the application shell, contacts and
chat, the campus feed, activities, and the unified notification desk. The
notification desk merges persisted activity and social summaries, supports
single or bulk read actions, resolves protected post and friend-request
targets, and receives events through the authenticated chat WebSocket. The
final Vue slice adds the administrator dashboard, moderation, activity review,
audit cleanup, and CSV report generation. After the completed parity checks,
Vue is the default demo entry. The root static frontend is preserved intact as
a legacy fallback at `/legacy/` in Compose and through its dedicated local
script.
The Vue feed comment panel keeps its own open state and comment draft state, so
opening a post's comments now displays the loaded comment list and composer.
Teachers and club leaders can also submit activities from the Vue activity page;
the organizer identity remains derived from the bearer token and the activity
stays pending until an administrator approves it.
The workspace no longer repeats a persistent authentication-status banner above
every page, keeping the main area available for the active feature. Page-level
operation feedback remains in the feature that initiated it.
The sidebar uses only functional navigation labels and the active-page marker;
it doesn't repeat migration progress or explanatory subtitles.
Live parity checks now cover chat send, recipient unread counts, read clearing,
withdrawal, and the `message.created` and `message.withdrawn` WebSocket events.
They also cover single notification reads, social notification delivery and
target resolution; the combined read-all behavior is covered by the Vue store
test without changing historical notification state.
Live administrator parity checks cover an activity rejection with its retained
reason, content approval, pending-content and audit deletion, plus daily report
generation through the Vue proxy.
See
[`docs/vue-migration-handoff.md`](docs/vue-migration-handoff.md) for the
completed slice and next boundary.

The demo supports these flows:

- Log in with the student account; the login button gets a demo verification
  code automatically when the code field is empty.
- Create a student account from the Vue login page with a name, phone number,
  and verification code. When the Java API is available, the account is stored
  in MySQL, audited, and signed in immediately. The same phone number can't be
  registered twice. Student registration and activity registration use distinct
  client API actions, so an expired saved session can't send registration to a
  protected activity endpoint.
- Enter the workspace instantly through the quick demo button.
- Use **切换账号** next to the current identity to move directly between the
  student, teacher, club-leader, and administrator demo accounts without first
  leaving the workspace. The client obtains the next demo session, revokes the
  current server-side session, replaces its saved token, and refreshes the
  workspace so no prior account data remains in the page state.
- Log out to return to the login screen. In Java API mode, logging out also
  revokes the server-side session.
- Search demo users and send a friend request. The recipient receives a
  persistent **新的好友申请** notification.
- Switch demo accounts to review incoming friend requests and accept or reject
  them from the recipient account. The requester receives a persistent result
  notification for either decision.
- Open a **新的好友申请** notification to locate and highlight the recipient's
  pending request, then use the existing **同意** or **拒绝** action. The API
  never accepts a requester or recipient identity from this notification flow.
- View accepted friends in the contacts list and open chats from that list.
- See the latest message preview for every accepted friend without loading each
  conversation's complete history or changing its read state.
- Open a conversation and load its most recent message page. When history is
  available, load earlier messages without losing the current scroll position.
  The message history scrolls inside the conversation panel, while the composer
  remains available at the bottom of that panel. Its compact two-line default
  keeps more history visible, and you can still resize it for a longer draft.
  Selecting a conversation or sending a message opens the latest content, while
  loading older pages preserves the message currently being read.
  On wider screens, the workspace prioritizes the conversation by using a
  compact application rail and contacts column instead of a fixed chat width.
  The chat page removes its redundant heading and lets the conversation panel
  use the available viewport height for message history. At a 1440 by 900
  viewport, the internal message stream is about 500 pixels high while the
  composer remains visible.
- Keep unread counts in the API database so they survive a page refresh and
  account switch.
- Render user-generated messages, posts, comments, audit events, and review
  content as escaped text rather than executable HTML.
- Keep open chat conversations refreshed through a lightweight WebSocket
  channel when the Java API is running.
- Attach one or more local files to a chat message and verify each attachment
  as a standard file card. The demo doesn't show image bubbles or large image
  previews.
- Withdraw the latest message sent by the current user.
- Filter published activities by an inclusive start-date range and category,
  register while a slot is available, see a waitlist state when full, and
  cancel a current registration.
- Receive persistent in-app notifications for activity state changes, new post
  likes, new comments, and friend-request results. A post author receives a
  comment notification when another user submits a comment for review; that
  comment remains outside the public feed until moderation approves it. The
  notification rail combines unread counts, the unified notification center
  supports marking activity and social items as read, opens the related
  activity or post with a visible target highlight, and connected recipients
  see new social notifications without refreshing.
- Switch online and invisible presence states.
- Publish a campus feed post.
- Like a post once per signed-in user, see the current-user liked state, and
  click the same action again to cancel the like without erasing legacy counts.
- Open personal post management from the campus feed and edit or delete your
  own posts.
- Expand a feed post, view comments, and add a new comment.
- Browse published campus activities from the activity workspace.
- Switch to the teacher or club-leader account, submit an activity, and see
  its pending review state without supplying an organizer ID.
- Open **我的活动运营** as the organizer, inspect the persisted registration
  and waitlist roster, check in registered students, and export the roster as
  CSV.
- As a registered student, display a fresh one-time **签到凭证** from the Vue
  activity card. The organizer can enter that opaque code in **我的活动运营** to
  check in the participant without accepting any client-provided identity.
- Switch to the administrator account, approve or reject pending activities,
  and provide a required reason for rejection.
- See the activity-review workspace immediately when opening the administrator
  console. An empty queue or a failed activity request shows clear feedback
  instead of hiding the workspace.
- Review only pending feed posts and comments from the admin content queue.
  Rejecting content requires an audit comment; completed decisions leave this
  workspace, while the audit event retains the reviewer, review time, and comment.
- Generate a non-binding moderation assistance suggestion for a pending item.
  The local, explainable policy reports matched risk signals and a suggested
  reason, but never approves, rejects, or records a decision automatically.
- Delete one or more moderation records from the admin queue.
- Delete one or more audit records from the admin audit table.
- Filter the admin report by **今日**, **本周**, or **全部**, then print a
  report card with preview rows, a CSV download, and a print preview action.
- View admin metrics and live audit records. The **活动报名** and **活动签到**
  cards read real activity registration rows instead of display constants. The
  **注册用户**、**今日消息**、**动态总数** and **待审内容** cards also read
  current MySQL counts.

The frontend tries `http://127.0.0.1:8080/api` first. If the Java API can't be
reached, it falls back to the built-in mock data and shows **Mock** in the
sidebar. When the Java API responds, the sidebar shows **Java API**. A response
from the Java API with an error status doesn't fall back to Mock, so rejected
or failed requests remain visible instead of changing browser-only data.

After login, the Java API returns a signed, time-limited JWT bearer token. The
frontend sends that token in the `Authorization` header, and protected live API
requests resolve the current user from the verified token and matching MySQL
session instead of trusting query parameters or request body user IDs. The
quick demo entry and account switcher use `POST /api/auth/demo-login` to issue
the token for the selected demo account. Student registration uses
`POST /api/auth/register` after the same phone verification-code check, creates
an `online` student record, adds an audit event, and issues a JWT in one
transaction. JWTs expire after one hour by default;
set `CAMPUSLINK_JWT_SECRET` before using a non-demo environment. The matching
MySQL `auth_sessions` row supports server-side revocation: `POST /api/auth/logout`
deletes only the presented session, so that token cannot authorize another
request even before its JWT expiry. Existing legacy random demo tokens no
longer authorize requests after this change. Spring Security now enforces this
as a stateless API boundary: `/api/auth/**` and the database health endpoint
remain public, other `/api/**` routes require a verified JWT session, and
`/api/admin/**` requires `ROLE_ADMIN`. Actuator exposes the status-only
`/actuator/health` endpoint publicly, while `/actuator/info` and
`/actuator/metrics/**` require `ROLE_ADMIN`. Authentication and authorization errors
return the existing JSON `message` shape, so live API failures don't fall back
to Mock. Student and teacher tokens receive `403 Forbidden` for admin-only APIs.

The Java API prints one request log line when each `/api/**` request starts and
one line when it finishes. In IntelliJ IDEA, open the **Run** tool window for
the **CampusLink API** configuration to see entries like
`[HTTP] --> GET /api/admin/metrics` and
`[HTTP] <-- GET /api/admin/metrics status=200 duration=12ms`.

Actuator and Micrometer also expose status and diagnostics without widening the
public API surface. `GET /actuator/health` returns only the overall health
status. An administrator bearer token can query `GET /actuator/metrics` and the
business gauges `campuslink.users.total`, `campuslink.messages.today`,
`campuslink.posts.total`, and `campuslink.moderation.pending`. The
`campuslink.http.requests` timer uses method, route template, and status tags,
so it records API latency without adding user IDs or resource IDs as metric tags.

For the local demo, the verification code is returned by the API response and
filled into the login form so the flow can be tested without SMS delivery.

## Test commands

Run the lightweight frontend checks before handing off a change:

```bash
./script/run_frontend_check.sh
```

Run the Vue slice independently from the repository root:

```bash
cd frontend-vue
npm install
npm test
npm run build
npm run dev
```

The default local demo starts Vite on `http://127.0.0.1:5180` and proxies `/api` and `/ws` to the
local Java API. The Vue client uses relative proxy paths. It shows Java API
mode when the API responds and uses Mock only when the request cannot reach the
API; HTTP `4xx` and `5xx` responses remain visible failures.

Run the backend JUnit suite from the backend directory:

```bash
cd backend
mvn test
```

On this machine, Microsoft JDK 21 can't reliably let Mockito self-attach its
Byte Buddy agent. The verified full run passes an explicit `-javaagent` through
Maven's `argLine`; without it, Mockito-based tests can fail during test setup
rather than on application behavior. The latest verified run completed all 151
tests with the explicit agent and only printed the JVM class-sharing warning:

```bash
/Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn \
  -f backend/pom.xml \
  -DargLine=-javaagent:/Users/linus_k/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.5/byte-buddy-agent-1.17.5.jar \
  test
```

The smoke test is dependency-free. It checks the static page, styles, frontend
modules, adapter, and Java API skeleton for the selectors and routes that keep
the demo flows connected. The backend test suite covers the migrated service
behavior for login, user search, friend requests, accepted friendships, chat
messages, chat attachments, feed posts, comments, moderation, and audit
records. Activity tests cover organizer permissions, review transitions,
date/category filters, registration and waitlist promotion, organizer rosters,
transactional check-in, persistent notifications, user-scoped post likes,
comment and friend-request outcomes, HTTP boundaries, MyBatis mapping,
credential-hash verification, and rollback-safe history.
The suite also includes MockMvc controller tests for the auth, users, friends,
chat, feed, activity notifications, social notifications, and admin API
boundaries, plus direct WebSocket handler tests for chat and recipient-only
activity and social notification events, plus the Spring Security API chain.
The full suite currently contains 156 tests. `TestcontainersMySqlIntegrationTest`
starts an isolated `mysql:8.4` container and initializes it from the existing
`schema.sql` and `data.sql` files. It verifies MyBatis visibility filtering,
the transactional friend-acceptance and notification writes, and the JWT
student/admin authorization boundary. The container is stopped after the test;
it never opens or changes the developer's local MySQL history.
Repository integration tests use transactions that roll back, so test rows
don't remain in existing demo history. Chat repository coverage verifies
that unread counts include only messages addressed to the current user, still
count newly received messages, and clear after the read cursor advances.

With the Vue server, legacy static server, and Java API already running, use
the live equivalence check to compare the Vue proxy with the legacy client's
direct API path. It creates temporary demo sessions and logs out those exact
sessions before it exits. It doesn't create or modify activities, messages,
moderation decisions, notifications, or MySQL history.

```bash
node script/run_live_equivalence_check.mjs
```

The check compares student feed, activities, notifications, chat unread and
history, organizer operations and roster, admin metrics, moderation history,
audit records, and moderation assistance. It also confirms that invalid
notification targets and invalid chat withdrawal requests return the same safe
error through both paths.

## Docker Compose demo

With Docker Compose available, you can start the browser demo, API, and an
isolated MySQL 8.4 database with one command. This database is a Docker named
volume, not your local MySQL server.

```bash
docker compose up --build
```

Open `http://127.0.0.1:5179`, then confirm the API with
`curl -fsS http://127.0.0.1:8080/api/database/health`. Compose serves the Vue
build by default, proxies `/api` and `/ws` to the API service, and retains the
static fallback at `http://127.0.0.1:5179/legacy/`. The Compose guide explains
startup, health checks, persistence, and safe shutdown in
[`docs/compose-demo.md`](docs/compose-demo.md). The repository doesn't publish
a public online demo URL without explicit authorization.

## Continuous integration

GitHub Actions runs [the verification workflow](.github/workflows/verify.yml)
on pushes and pull requests targeting `main`, and you can also start it
manually. The workflow starts a disposable MySQL 8.4 service, runs
`./script/run_frontend_check.sh` through Bash for GitHub's Linux runner, then
imports the UTF-8 `schema.sql` and `data.sql` files into the disposable CI
MySQL service, then runs the complete Maven suite with the explicit Byte Buddy
agent. This makes rollback-safe MyBatis tests reproducible without enabling
initialization against a developer's local history. It also builds and starts
the Compose demo, waits for its health checks, and calls the public health
endpoint. It never connects to, resets, or seeds a developer's local MySQL
history.

## Local live API acceptance

On July 11, 2026, the browser acceptance flow confirmed that a teacher can
submit an activity through the Java API, an administrator can publish it from
the pending-activity workspace, and a student can then see it in the published
list. This check intentionally uses local MySQL history; it doesn't replace
the database with Mock data or reset existing records.

On July 12, 2026, the student activity page was verified against the Java API
and existing MySQL history. Category filtering, inclusive date-range filtering,
combined empty results, clearing filters, and restoring registration state all
worked without falling back to Mock. The administrator console, feed review
feedback, and chat page were also checked after the UI change.

The same day, the complete activity notification path was verified with the
Java API and existing MySQL data. A teacher received the approval result, one
student received a live registration result, and a second student received a
waitlist result followed by a persisted promotion result after returning
online. Unread counts and **mark all read** survived account switches. The
administrator console, moderation feedback, and chat page remained usable,
and the browser console reported no errors.

The same Java API acceptance also verified the contact unread-count boundary.
When signed in as the teacher, contacts with no direct conversation show
**暂无消息** without an unread badge, even when those friends have sent messages
to another user. Opening an empty conversation keeps the message list empty.
This check used existing MySQL history and didn't reset or replace it.

The activity operations acceptance used the same live Java API and existing
MySQL history. The teacher account loaded three persisted activities, opened a
roster with one registered and one waitlisted student, and generated the CSV
export. The administrator console displayed five real occupied registrations
and zero check-ins. The activity review workspace, feed moderation detail, and
chat composer remained correctly laid out.

On July 13, 2026, the social-like slice was verified with the Java API and
existing MySQL history. The student account liked a teacher post, saw the
button change from **赞 12** to **已赞 13**, and cancelled it back to
**赞 12**. The teacher then received one persisted **动态点赞** notification
from the student. The unified read-all action cleared both activity and social
unread counts. The administrator workspaces, moderation detail, chat page,
and browser console remained healthy.

## Frontend structure

The browser demo is still served as static files, but the JavaScript is split
into ES modules under `frontend/js/`. The root `app.js` is only a small module
entrypoint.

The frontend uses this module layout:

- `api`: Live API adapter and mock API fallback.
- `auth`: Login, quick demo entry, logout, and demo account switching.
- `chat`: Chat-specific rendering.
- `activities`: Activity filters, list, registration, submission, organizer
  roster, check-in, CSV export, status, and admin review UI.
- `notifications`: Unified activity and social notification rendering,
  combined unread state, individual read and target actions, and activity/social
  WebSocket events.
- `contacts`: Friend and contact workflows are wired through shared loaders and
  contact renderers.
- `posts`: Campus feed rendering.
- `admin`: Moderation, audit, and report rendering.
- `ui`: Shared shell, status, and renderer exports.
- `utils`: DOM helpers, formatting, file attachment metadata, and report CSV
  helpers.

## Backend structure

The `backend/` directory contains a layered Spring Boot service. It exposes
demo APIs for authentication, user search, chat messages, feed posts, campus
activities, and admin workflows while keeping HTTP, business logic, data
access, interface models, and configuration in separate packages.

The backend uses this package layout:

- `controller`: HTTP routes and request validation entrypoints.
- `service`: Business logic for auth, friends, chat, feed, activities, activity
  notifications, social notifications, admin, and audit.
- `mapper`: MyBatis mapper interfaces for mapper-based persistence.
- `repository`: Repository interfaces and MyBatis-backed implementations for
  users, verification codes, friends, chat, feed, activities, moderation,
  audit, and health checks.
- `entity`: Internal domain/data records.
- `dto`: Request and response models used by the API.
- `config`: CORS and global exception handling.

When JDK 21 and Maven are installed, run the backend with:

```bash
cd backend
mvn spring-boot:run
```

The IntelliJ IDEA project is configured for OpenJDK 21. Use the
**CampusLink API** run configuration to start the backend from IDEA. That
configuration runs `script/run_backend_idea.sh`, which sets `JAVA_HOME` to the
Homebrew OpenJDK 21 installation and uses IDEA's bundled Maven.

The backend connects to a local MySQL database when it starts. Use these local
demo settings:

- Database: `campuslink`
- Username: `campuslink`
- Password: `campuslink123`
- JDBC URL:
  `jdbc:mysql://127.0.0.1:3306/campuslink?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false`

The Spring Boot startup initializes `schema.sql` and `data.sql` automatically.
After the API starts, open `http://127.0.0.1:8080/api/database/health` to verify
that Java can reach MySQL.

The MySQL persistence migration is complete for the current demo. Users,
verification codes, friend requests, friendships, presence updates, chat
messages, chat attachment metadata, feed posts, comments, moderation items,
activities, activity review history, activity registrations, activity
registration events, activity notifications, post likes, social notifications,
audit events, demo auth sessions, and the health check all use MyBatis
mapper-backed data access. The seed data
also removes stale pending friend requests when the same users are already
friends.

The current backend exposes these API paths:

- `POST /api/auth/code`
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/demo-login`
- `GET /api/users`
- `GET /api/friends`
- `POST /api/friends/requests`
- `GET /api/friends/requests`
- `POST /api/friends/requests/{requestId}/accept`
- `POST /api/friends/requests/{requestId}/reject`
- `GET /api/conversations/{peerId}/messages`
- `POST /api/conversations/{peerId}/messages`
- `POST /api/conversations/{peerId}/messages/{messageId}/withdraw`
- `POST /api/presence`
- `GET /api/feed`
- `POST /api/feed`
- `GET /api/feed/personal-posts`
- `PATCH /api/feed/personal-posts/{postId}`
- `DELETE /api/feed/personal-posts/{postId}`
- `POST /api/feed/{postId}/likes`
- `GET /api/feed/{postId}/comments`
- `POST /api/feed/{postId}/comments`
- `GET /api/activities`
- `POST /api/activities`
- `GET /api/activities/managed`
- `GET /api/activities/{activityId}/registrations/current`
- `POST /api/activities/{activityId}/registrations`
- `DELETE /api/activities/{activityId}/registrations/current`
- `GET /api/activities/{activityId}/registrations/roster`
- `POST /api/activities/{activityId}/registrations/{registrationId}/check-in`
- `GET /api/activity-notifications`
- `POST /api/activity-notifications/read-all`
- `POST /api/activity-notifications/{notificationId}/read`
- `GET /api/social-notifications`
- `POST /api/social-notifications/read-all`
- `POST /api/social-notifications/{notificationId}/read`
- `GET /api/social-notifications/{notificationId}/post-target`
- `GET /api/social-notifications/{notificationId}/friend-request-target`
- `GET /api/admin/activities/pending`
- `POST /api/admin/activities/{activityId}/reviews`
- `GET /api/admin/metrics`
- `GET /api/admin/activity-metrics`
- `GET /api/admin/moderation`
- `POST /api/admin/moderation/{itemId}/{decision}`
- `DELETE /api/admin/moderation`
- `GET /api/admin/report?range=today`
- `GET /api/admin/audit-events`
- `DELETE /api/admin/audit-events`
- `GET /api/database/health`

Activity creation accepts title, description, category, location, start and
end times, and capacity. It never accepts an organizer ID. The bearer token
supplies the organizer, and only a role containing `教师` or `社团负责人` can
create an activity. New activities enter `pending`. Only an administrator can
approve them into `published` or reject them back to `draft`; rejection keeps
the reason in current activity state and append-only review history.

Student registration uses the authenticated bearer token and never accepts an
attendee ID. The service locks an activity while it assigns capacity, writes a
current registration and an append-only registration event in one transaction,
and promotes the oldest waitlisted attendee when a registered student cancels.

Organizer operations also use the bearer token. `GET /api/activities/managed`
returns only activities created by the current teacher or club leader. Roster
and check-in endpoints verify that the current user owns the activity. A
check-in changes only `registered` to `checked_in`, stores `checked_in_at`, and
appends the check-in event in the same transaction. The independent
`GET /api/admin/activity-metrics` endpoint counts occupied and checked-in rows
without adding activity rules to `AdminService`.

The published activity list accepts optional `from`, `to`, and `category`
query parameters. Dates use `YYYY-MM-DD`; both dates are inclusive and match
the activity start time. Category matching is exact after surrounding
whitespace is removed. Invalid dates and reversed ranges return `400` and do
not fall back to Mock data.

Chat messages accept optional attachment metadata in the message payload. The
current demo stores file name, size, MIME type, and display kind only; it
doesn't upload or persist the local file bytes.

Conversation reads use the bearer token to resolve the current user, then load
messages in both directions between that user and the selected peer. The API
requires the two accounts to be accepted friends before it reads, sends, or
withdraws a message. This keeps two browser windows on different demo accounts
in the same conversation without exposing arbitrary peer IDs.

When the Java API is available, the frontend also opens
`ws://127.0.0.1:8080/ws/chat?token=...` after login. Chat messages still use
the HTTP `POST /api/conversations/{peerId}/messages` endpoint for persistence
and audit logging. The WebSocket channel pushes `message.created` events to
the sender and recipient so open conversations can refresh without the older
mock receipt timer. Message withdrawal publishes `message.withdrawn` on the
same channel, so the other open browser window can refresh the conversation
without a manual reload. If the incoming message belongs to a conversation
that isn't currently open, the frontend fetches that conversation and marks it
as unread for later. The sidebar shows a realtime status badge, and incoming
messages also refresh the contacts list so a window opened before a friendship
change can still show the new conversation. The WebSocket sends lightweight
heartbeat messages and reconnects when the heartbeat times out.

Activity and social notifications reuse this authenticated channel. Their
domain services persist the notification in the surrounding business
transaction, then publish `activity.notification.created` or
`social.notification.created` only after the transaction commits. The frontend
deduplicates the received item, refreshes the unified unread count immediately,
and still reloads MySQL history at login; a Java API error never substitutes
Mock notification data.

Feed posts and comments also include a lightweight moderation status. The admin
queue can approve or reject pending demo content; rejected posts and comments
are hidden from the feed response. The seed data starts with an empty
moderation queue, so the admin view doesn't show the same two default review
items on every launch. New posts and comments still create pending moderation
records, and admins can select and delete one or more queue records without
changing the underlying post or comment status.

The campus feed includes personal post management. A signed-in user can open
**管理我的动态** to view, edit, and delete only posts authored by the current
token user. Editing a post moves it back to pending moderation. Deleting a
personal post removes that post, its comments, and related moderation records;
it doesn't delete friends' posts or other users' content.

The public feed resolves the signed-in user from the bearer token. Approved
posts marked **全校可见** are visible to all users, **好友可见** posts are visible
to the author and accepted friends, and **仅老师可见** posts are visible to the
author and teacher accounts. The same rules apply when the frontend uses Mock
data while the Java API is unavailable.

Post likes now use the `(post_id, user_id)` key in `post_likes`. The like API
toggles only the authenticated user's row and returns `likedByCurrentUser`.
The existing `posts.likes` value remains a denormalized compatibility count so
historical MySQL totals are not discarded. A new like by another user writes
the count, relationship, audit event, and author notification in one service
transaction. Cancelling doesn't delete the earlier notification, and liking
your own post doesn't create one. Seed startup no longer overwrites existing
post-like totals.

The admin report endpoint accepts `range=today`, `range=week`, or `range=all`.
It returns report metadata, the selected range, metrics, pending moderation
items, and recent audit events. The frontend renders that payload as a compact
report card, with a CSV download and a print preview action for the local demo.
When a non-admin account opens the admin tab, the frontend shows the restricted
state and waits until you switch to the demo admin account.

Admins can also delete one or more rows from the audit table. This only removes
the audit event rows from `audit_events`; it doesn't delete users, posts,
comments, moderation records, or chat messages.

## Development roadmap

The project follows the staged plan in
[`docs/resume-project-roadmap.md`](docs/resume-project-roadmap.md). The next
milestones focus on a trustworthy baseline, a campus activity registration
workflow, notifications, production-style security, and repeatable delivery.

Phase-two activity implementation follows the reviewed data model, state
machine, permission matrix, transaction boundaries, and test scenarios in the
[`activity domain design`](docs/activity-domain-design.md). The first
end-to-end slice now covers teacher or club-leader submission, pending status,
administrator approval or rejection, and the published activity list while
keeping activity rules outside the feed and generic admin services.

The reviewed
[`activity registration and waitlist design`](docs/activity-registration-design.md)
records the completed registration, cancellation, waitlist, organizer roster,
transactional check-in, CSV export, and real activity-metric boundaries.
Persistent social notifications now also use the existing authenticated
WebSocket for immediate delivery.
