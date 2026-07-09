# CampusLink new dialogue handoff

This report helps the next Codex dialogue resume CampusLink work without
reconstructing the project history. Start by reading `AGENTS.md`, then read
this file, then run the baseline checks before adding new features.

Last updated: July 3, 2026, after the realtime chat milestone was completed.

## Current status

CampusLink is now a local campus social platform demo with a static ES module
frontend, a layered Spring Boot backend, MySQL persistence through MyBatis,
demo bearer-token authentication, admin role checks, and realtime one-to-one
chat over WebSocket.

The chat milestone is complete for the current demo scope. It includes
bidirectional conversation reads, realtime message delivery across two browser
windows, heartbeat and reconnect handling, a realtime status badge, unread
conversation refresh, attachment metadata, and realtime message withdrawal.

## Start here

The next dialogue must first confirm the current baseline and avoid restoring
previously reverted work.

Read these files in order:

1. `AGENTS.md`
2. `README.md`
3. `docs/report/new-dialogue-handoff-2026-07-03.md`

Then run these checks:

```bash
cd /Users/linus_k/Documents/test
./script/run_frontend_check.sh
```

```bash
cd /Users/linus_k/Documents/test/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
/Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn test
```

Expected results:

```text
Frontend smoke test passed.
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
```

In the Codex sandbox, Maven can fail because Mockito and Byte Buddy cannot
self-attach to the JVM. This is an environment limitation. Re-run the same
Maven command outside the sandbox when the sandbox error mentions
`Could not initialize inline Byte Buddy mock maker`.

## Running the demo

Run the backend and frontend separately.

Start the backend:

```bash
cd /Users/linus_k/Documents/test
./script/run_backend_idea.sh
```

Start the frontend:

```bash
cd /Users/linus_k/Documents/test
./script/run_frontend_demo.sh
```

Open the frontend:

```text
http://localhost:5178/
```

The frontend also works at:

```text
http://127.0.0.1:5178/
```

Use the backend health check to confirm Java and MySQL are connected:

```text
http://127.0.0.1:8080/api/database/health
```

The frontend tries `http://127.0.0.1:8080/api` first. If the Java API is not
running, it falls back to mock data and shows **Mock** in the sidebar. When the
Java API responds, the sidebar shows **Java API**.

## Completed capabilities

The current demo supports these flows:

- Demo verification-code login.
- Quick demo workspace entry.
- Demo account switching and logout.
- Demo bearer tokens stored in the MySQL `auth_sessions` table.
- Protected live API requests resolved from the `Authorization` header.
- Admin-only APIs guarded by the `管理员` role.
- User search, friend requests, accept and reject actions, and contacts.
- One-to-one chat from the contacts list.
- Chat messages with optional attachment metadata.
- Bidirectional conversation loading based on the bearer-token user.
- WebSocket realtime chat at `/ws/chat?token=...`.
- `message.created` events sent to both sender and recipient sessions.
- `message.withdrawn` events sent to both sender and recipient sessions.
- Heartbeat `heartbeat.ping` and `heartbeat.pong` messages.
- Reconnect handling and a realtime status badge.
- Unread count refresh for conversations that are not currently open.
- Presence switching between online and invisible.
- Feed post publishing, likes, comments, and lightweight moderation status.
- Personal post management from the campus feed, including list, edit, and
  delete for the current token user's own posts.
- Admin moderation queue approval, rejection, single delete, and bulk delete.
- Empty default moderation queue on startup.
- Admin report filtering by today, week, and all.
- CSV download and print preview for the admin report.
- Audit records for key demo operations.
- Audit record single and bulk deletion from the admin audit table.
- Request logging for `/api/**` backend requests.

## Chat milestone details

The chat milestone is now closed for the current demo scope.

Important files:

- `backend/src/main/java/com/campuslink/config/ChatWebSocketConfig.java`
- `backend/src/main/java/com/campuslink/config/ChatWebSocketHandler.java`
- `backend/src/main/java/com/campuslink/service/ChatRealtimeNotifier.java`
- `backend/src/main/java/com/campuslink/service/ChatService.java`
- `frontend/js/chat/realtime.js`
- `frontend/js/chat/renderers.js`
- `frontend/js/auth/workspace.js`
- `frontend/js/ui/status.js`
- `backend/src/test/java/com/campuslink/config/ChatWebSocketHandlerTest.java`
- `backend/src/test/java/com/campuslink/service/ChatServiceTest.java`

Manual chat verification:

1. Open `http://localhost:5178/` in two browser windows.
2. Log in as 林一 in one window and 周同学 in the other.
3. Send a message from 林一 to 周同学.
4. Confirm 周同学 receives it without a manual refresh.
5. Withdraw 林一's latest message.
6. Confirm 周同学 sees the withdrawn state without a manual refresh.

The realtime badge shows WebSocket health, not message activity. **实时** means
the socket is connected and heartbeat responses are arriving. It changes to
**连接中** while reconnecting and **离线** when no live socket is active.

## Frontend structure

The frontend is still a static browser demo, but the JavaScript is split into
ES modules under `frontend/js/`. The root `app.js` is only the entrypoint.

Module layout:

- `api/`: live API adapter and mock fallback.
- `auth/`: login, quick demo entry, logout, and account switching.
- `chat/`: chat realtime connection and chat rendering.
- `posts/`: feed rendering.
- `admin/`: moderation, audit, and report rendering.
- `ui/`: shared shell, status, contacts, and renderer exports.
- `utils/`: DOM helpers, formatting, attachment metadata, and CSV helpers.
- `loaders.js`: data loading and render coordination.
- `app-events.js`: page event binding.

Do not rebuild a monolithic `app.js`. New frontend behavior must stay in the
appropriate module.

## Backend structure

The backend uses a layered Spring Boot structure.

Package layout:

- `controller`: HTTP routes and request validation entrypoints.
- `service`: business logic for auth, friends, chat, feed, admin, and audit.
- `mapper`: MyBatis mapper interfaces.
- `repository`: repository interfaces and MyBatis-backed implementations.
- `entity`: internal domain and data records.
- `dto`: request and response records.
- `config`: CORS, global exceptions, request logging, and WebSocket config.

Controller classes must not call repositories directly. Repository classes must
not contain business rules. Use DTO records for API request and response
models.

## Persistence status

The current demo data is persisted through MyBatis and MySQL for these areas:

- Users and verification codes.
- Demo auth sessions.
- Friend requests and friendships.
- Presence updates.
- Chat messages and chat attachment metadata.
- Feed posts, comments, and likes.
- Moderation items and moderation state.
- Audit events.
- Database health checks.

Local database settings:

- Database: `campuslink`
- Username: `campuslink`
- Password: `campuslink123`
- Port: `127.0.0.1:3306`
- JDBC URL:
  `jdbc:mysql://127.0.0.1:3306/campuslink?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false`

Spring Boot initializes `schema.sql` and `data.sql` on startup.

## Important guardrails

Do not restore these previously reverted mistakes:

- Friend feed deletion.
- Processed moderation record deletion UI.
- Dynamic post deletion through `data-delete-post`.
- `feed/mine`, `my-post`, `DeletePostResponse`, or similar personal-feed
  deletion flows.

Personal post management is now an intentional current feature, exposed through
`/api/feed/personal-posts` and limited to the current token user's own posts.
Do not implement friend feed deletion or restore the old reverted
`feed/mine`, `my-post`, `DeletePostResponse`, or `data-delete-post` directions.

The current moderation delete feature is only for records in the pending admin
moderation queue. The audit-table delete feature is only for rows in
`audit_events`. Neither feature must delete feed posts, friend posts, personal
posts, comments, users, or chat messages.

Use this scan before finishing a new task:

```bash
cd /Users/linus_k/Documents/test
rg -n "data-delete-post|feed/mine|my-post|DeletePostResponse|deletePost|删除动态|个人动态" .
```

Expected result: matches only in historical handoff documentation, not in
source code.

## Known limits

These are current demo boundaries, not regressions:

- Frontend has no bundler and remains static HTML, CSS, and ES modules.
- Verification codes are local demo codes, not SMS.
- Demo bearer tokens are not production JWTs.
- Chat attachments store metadata only; file bytes are not uploaded.
- Chat has no read receipts, typing indicator, message pagination, or full file
  transfer.
- Admin report data can be previewed, downloaded as CSV, and printed, but the
  backend does not archive generated report files.
- Mapper and repository tests do not yet run against an isolated disposable
  MySQL test database.

## Suggested next tasks

Wait for the user's next product direction before implementing anything. If
the user wants a quality or infrastructure task, good next options are:

- Add MyBatis Mapper or Repository integration tests against a disposable
  MySQL test database.
- Add real file upload for chat attachments.
- Add read receipts or typing indicators for chat.
- Add message pagination for long conversations.
- Improve admin report persistence or export history.
- Add a small backend test profile that avoids the Mockito self-attach issue in
  sandboxed runs.

Every new task must keep the frontend modular, keep the backend layered, pass
tests, and update `README.md` or `docs/` when behavior, APIs, structure, or run
commands change.

## Copy prompt for the next dialogue

Use this prompt to start the next Codex conversation:

```text
请阅读 AGENTS.md、README.md 和
docs/report/new-dialogue-handoff-2026-07-03.md，同步 CampusLink 当前进度。

当前项目已经完成前端 ES modules 拆分、后端 Spring Boot + MySQL + MyBatis
分层、demo bearer token 鉴权并持久化到 auth_sessions、后台管理员权限校验、
默认空审核队列、待审核记录单条/批量删除、审计记录删除、个人动态管理，以及对话大任务。

对话功能当前已经闭环：双账号/双窗口实时收发消息、WebSocket 推送、心跳检测、
断线重连、实时状态徽标、会话双向读取、未读会话刷新、附件 metadata、以及撤回
消息实时同步都已完成。后端有 ChatWebSocketHandlerTest 和 ChatServiceTest
覆盖心跳、广播和撤回事件。

请不要恢复之前已经撤回的误改：朋友动态删除、已处理审核记录删除 UI、
data-delete-post、feed/mine、my-post、DeletePostResponse 等方向都不要恢复。
当前个人动态管理只允许当前 token 用户管理自己的动态。

接手后请先运行：

cd /Users/linus_k/Documents/test
./script/run_frontend_check.sh

然后运行后端测试：

cd /Users/linus_k/Documents/test/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
/Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn test

预期结果是前端 smoke 通过，后端 Tests run: 45, Failures: 0, Errors: 0。
如果 Maven 在 Codex 沙箱里因为 Mockito/Byte Buddy self-attach 失败，请用非沙箱
环境重跑同一条 Maven 命令确认。

完成基线检查后，等待我的新提示词再继续开发。所有新增功能必须保持模块化、
测试通过，并在涉及 API、结构、配置、运行方式或用户可见行为变化时同步更新
README 或 docs。
```
