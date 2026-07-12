# CampusLink demo

This repository contains a verifiable full-stack demo for a campus social
application. It combines a static browser frontend with a Spring Boot and
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

Then visit `http://127.0.0.1:5179/?v=20260710-activity-review-ui-v1`.

The demo supports these flows:

- Log in with the student account; the login button gets a demo verification
  code automatically when the code field is empty.
- Enter the workspace instantly through the quick demo button.
- Switch demo accounts or log out to return to the login screen.
- Search demo users and send a friend request.
- Switch demo accounts to review incoming friend requests and accept or reject
  them from the recipient account.
- View accepted friends in the contacts list and open chats from that list.
- See the latest message preview for every accepted friend without loading each
  conversation's complete history or changing its read state.
- Open a conversation and load its most recent message page. When history is
  available, load earlier messages without losing the current scroll position.
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
- Switch online and invisible presence states.
- Publish a campus feed post.
- Open personal post management from the campus feed and edit or delete your
  own posts.
- Expand a feed post, view comments, and add a new comment.
- Browse published campus activities from the activity workspace.
- Switch to the teacher or club-leader account, submit an activity, and see
  its pending review state without supplying an organizer ID.
- Switch to the administrator account, approve or reject pending activities,
  and provide a required reason for rejection.
- See the activity-review workspace immediately when opening the administrator
  console. An empty queue or a failed activity request shows clear feedback
  instead of hiding the workspace.
- Review pending feed posts and comments from the admin content queue.
- Delete one or more moderation records from the admin queue.
- Delete one or more audit records from the admin audit table.
- Filter the admin report by **今日**, **本周**, or **全部**, then print a
  report card with preview rows, a CSV download, and a print preview action.
- View admin metrics and live audit records.

The frontend tries `http://127.0.0.1:8080/api` first. If the Java API can't be
reached, it falls back to the built-in mock data and shows **Mock** in the
sidebar. When the Java API responds, the sidebar shows **Java API**. A response
from the Java API with an error status doesn't fall back to Mock, so rejected
or failed requests remain visible instead of changing browser-only data.

After login, the Java API returns a demo bearer token. The frontend sends that
token in the `Authorization` header, and protected live API requests resolve
the current user from the token instead of trusting query parameters or request
body user IDs. The quick demo entry and account switcher use
`POST /api/auth/demo-login` to issue a token for the selected demo account.
Issued demo tokens are stored in the MySQL `auth_sessions` table, so the Java
API can continue resolving tokens after an application restart as long as the
database keeps the session row. These tokens are still local demo credentials,
not production JWTs.
Admin routes require a token for an account whose role contains `管理员`.
Student and teacher tokens receive `403 Forbidden` for admin-only APIs.

The Java API prints one request log line when each `/api/**` request starts and
one line when it finishes. In IntelliJ IDEA, open the **Run** tool window for
the **CampusLink API** configuration to see entries like
`[HTTP] --> GET /api/admin/metrics` and
`[HTTP] <-- GET /api/admin/metrics status=200 duration=12ms`.

For the local demo, the verification code is returned by the API response and
filled into the login form so the flow can be tested without SMS delivery.

## Test commands

Run the lightweight frontend checks before handing off a change:

```bash
./script/run_frontend_check.sh
```

Run the backend JUnit suite from the backend directory:

```bash
cd backend
mvn test
```

The smoke test is dependency-free. It checks the static page, styles, frontend
modules, adapter, and Java API skeleton for the selectors and routes that keep
the demo flows connected. The backend test suite covers the migrated service
behavior for login, user search, friend requests, accepted friendships, chat
messages, chat attachments, feed posts, comments, moderation, and audit
records. Activity tests cover organizer permissions, review transitions, HTTP
boundaries, MyBatis mapping, and rollback-safe review history. The suite also
includes MockMvc controller tests for the auth, users, friends, chat, feed, and
admin API boundary, plus direct WebSocket handler tests for chat heartbeat and
broadcast behavior. Repository integration tests connect to the local MySQL
database without running `schema.sql` or `data.sql`; each test transaction
rolls back, so existing demo history stays unchanged.

## Frontend structure

The browser demo is still served as static files, but the JavaScript is split
into ES modules under `frontend/js/`. The root `app.js` is only a small module
entrypoint.

The frontend uses this module layout:

- `api`: Live API adapter and mock API fallback.
- `auth`: Login, quick demo entry, logout, and demo account switching.
- `chat`: Chat-specific rendering.
- `activities`: Activity list, submission, status, and admin review UI.
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
- `service`: Business logic for auth, friends, chat, feed, activities, admin,
  and audit.
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
activities, activity review history, audit events, demo auth sessions, and the
health check all use MyBatis mapper-backed data access. The seed data also
removes stale pending friend requests when the same users are already friends.

The current backend exposes these API paths:

- `POST /api/auth/code`
- `POST /api/auth/login`
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
- `GET /api/admin/activities/pending`
- `POST /api/admin/activities/{activityId}/reviews`
- `GET /api/admin/metrics`
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
