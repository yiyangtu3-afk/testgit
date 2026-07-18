# CampusLink 新对话交接说明（2026-07-15 更新）

本文用于在新 Codex 对话中恢复 CampusLink 当前开发上下文。当前状态保留
**普通用户审核状态反馈**、窄窗口聊天滚动修复、动态可见范围真实过滤和
好友聊天访问控制。

> **Note:** 本文保留可信基线的历史约束。活动阶段的最新完成状态、验证记录
> 和下一项任务以
> [`phase-two-activity-handoff.md`](phase-two-activity-handoff.md) 为准。

## 当前基线

当前前端基线保留管理员审核工作台和普通用户审核状态反馈，不保留后来
试做的聊天图片消息、图片预览、图片缓存、搜索好友直接聊天、动态页强制
刷新、Java API 空态文案、以及 portal 风格登录页。

当前静态资源版本是：

```text
20260715-signed-jwt-logout-v1
```

Vue 默认本地验证地址是：

```text
http://127.0.0.1:5180
```

保留的静态基线地址是 `http://127.0.0.1:5179/?v=20260715-signed-jwt-logout-v1`；使用
`./script/run_legacy_frontend_demo.sh` 启动。

当前功能稳定提交为 `98c2dad Add notification read and target actions`，已推送到
`main`。后续仅文档交接提交可能比该提交更新；开始功能开发时，以此提交的行为和
本文的约束为准。

## Vue 迁移交接

Vue 3 与 Vite 的渐进迁移已经完成，且在逐项 live 等价验收和用户明确确认后成为默认
入口。根目录静态入口和 `frontend/js/` 仍完整保留，作为回退演示和旧版回归基线，不能
删除、移动、替换或重写。独立
`frontend-vue/` 已提供 Vite 代理、Vue Router、Pinia 会话、统一 API/Mock 边界、
验证码登录、演示登录、注销页面，以及应用壳、导航与统一状态提示。Vue 联系人切片
包含好友搜索、申请处理、会话分页、附件、未读数与认证 WebSocket；动态和活动切片也
已迁移。Vue 通知切片按时间合并活动和社交通知，支持已读、受限目标跳转和实时事件去重。
Vue 管理员切片提供指标、活动与内容审核、审计记录和 CSV 报表。Compose 默认服务 Vue，
并在 `/legacy/` 保留旧版。
完整边界、目录设计、验证方式和下一切片见
[`vue-migration-handoff.md`](vue-migration-handoff.md)。

## 已完成内容

当前基线包含以下已完成工作。

- 管理员后台不再只显示 **待审内容** 数量。
- 管理员后台在指标卡片下方、审计记录上方展示 **待审核内容** 工作台。
- **待审核内容** 列表展示标题、提交人、提交时间、内容类型、当前状态
  和操作。
- 管理员可执行 **同意**、**拒绝**、**查看**。
- **待审核内容** 和 **审计记录** 都支持多选后的 **删除所选**。
- 未勾选时 **删除所选** 禁用，勾选后启用。
- 普通用户发布动态后会自动打开 **我的动态**，并看到该动态处于
  **待审核**。
- 待审核动态提示 **通过前不会出现在公共动态流**。
- 普通用户发布评论后会显示 **评论已提交审核，通过后会显示在动态下**。
- 个人动态管理展示 **待审核**、**已发布**、**已拒绝** 状态和审核说明。
- 公共动态和评论只展示 `approved` 内容。
- 动态可见范围持久化到 MySQL：**全校可见** 对所有用户开放，**好友可见**
  对作者和已建立好友关系的用户开放，**仅老师可见** 对作者和教师开放。
- `GET /api/feed` 从 bearer token 解析当前用户，并在 MyBatis 查询中执行
  可见范围过滤；Mock 数据使用同一组规则。
- 聊天消息读取、发送和撤回从 bearer token 解析当前用户，并要求双方已经
  建立好友关系；非好友的任意 `peerId` 请求返回 `403 Forbidden`。
- 会话消息按游标分页加载；打开当前会话时会持久化已读位置，未读数量从
  Java API 或 Mock 的同一接口读取。
- 联系人列表通过独立的会话预览接口读取每位好友的最后一条消息，不会提前
  加载完整历史或改变已读状态。
- 动态、评论、聊天消息、附件名、审核内容和审计事件通过统一的 HTML
  转义函数渲染，用户输入不会作为标签或属性执行。
- 聊天发送与撤回、好友申请通过、动态和评论发布、管理员审核等跨表流程
  均由服务层事务保护。
- `ChatRepositoryIntegrationTest` 使用本地 MySQL 验证聊天消息与附件的
  写入和分页读取；测试禁用种子脚本并在结束后自动回滚，不会改动历史数据。
- 登录页保持黑色右侧信息面板版本：**简洁的校园沟通空间**，右侧展示
  **身份**、**状态**、**模式** 三个信息块。
- 聊天附件保持普通文件卡片展示，不做图片气泡或大图预览。
- 修复窄窗口下林一与周同学聊天内容不能上下滑动的问题。
- 动态点赞按 bearer token 当前用户写入 `post_likes`，再次点击只取消自己的
  点赞；接口返回 `likedByCurrentUser`，前端显示 **赞** 或 **已赞**。
- 新点赞会通过独立社交通知模块写给动态作者；本人点赞不通知，取消点赞不删除
  已产生的历史通知。
- 原活动通知页升级为 **站内通知**，合并活动与社交通知的列表、未读徽标和
  全部已读操作。
- 好友申请、同意和拒绝会写入独立社交通知历史：收件人看到 **新的好友申请**，
  发起人看到 **好友申请已同意** 或 **好友申请未通过**。这些跨表流程受
  `FriendService` 事务保护，Java API 的错误响应不会回退到 Mock。
- 非作者提交动态评论时，动态作者会收到 **动态收到新评论** 通知；通知只保留
  60 个字符的评论预览。评论本身仍处于 `pending`，审核通过前不显示在公开
  动态下；作者评论自己的动态不会产生通知。
- 点赞、好友申请及其结果和评论通知现在复用已认证的 `/ws/chat` 通道。通知先
  在原业务事务中写入 MySQL，提交后才发送 `social.notification.created`；收件人
  前端按 ID 去重并立即更新统一未读数，离线历史继续由 Java API 加载。
- 站内通知支持单条已读；活动通知会定位到活动页，点赞与评论通知会解析到对应
  动态并高亮。通知目标解析必须按当前收件人约束，历史评论通知仍用评论 ID
  反查所属动态。
- **新的好友申请** 通知提供 **处理申请** 入口。后端仅按当前 bearer token
  收件人解析通知，并同时验证通知类型、申请 ID、申请发送者和 `pending` 状态；
  前端只取得受限的申请 ID，进入并高亮已有的待处理申请卡片，复用原有
  **同意** 与 **拒绝** 操作。
- 管理员后台的 **注册用户**、**今日消息**、**动态总数** 与 **待审内容**
  均按当前 MySQL 数据计算；活动报名和签到继续使用独立活动指标。Mock 使用
  账号与消息历史计算同名字段，不保留展示性固定数字。
- Java API 登录签发 HMAC-SHA256 签名 JWT，默认一小时过期。受保护请求必须同时
  通过签名和过期校验，并在 MySQL `auth_sessions` 找到与 JWT subject 一致的会话；
  后端不会从请求体或查询参数取得当前用户或角色。`POST /api/auth/logout` 仅删除
  当前 bearer token 的会话，前端无论令牌已经失效还是服务端拒绝，都清除本地登录
  状态。`auth_sessions.token` 已通过幂等迁移扩展到 `varchar(512)`，不清理历史数据。
- Spring Security 现在提供无状态 HTTP 安全链：`/api/auth/**` 与数据库健康检查
  公开，其余 `/api/**` 由 JWT 过滤器验证数据库会话；`/api/admin/**` 还要求
  `ROLE_ADMIN`。认证与授权拒绝都保留 JSON `message` 响应，CORS 配置归入安全链，
  支持 `PATCH`，不依赖 Web MVC 自动装配。

## 2026-07-09 基线修复

本次基线修复将动态范围从仅在前端展示的字段，调整为真正参与数据库写入
和读取的访问规则。为兼容已有本地 MySQL 数据，`schema.sql` 会先检查
`posts.visibility` 是否存在，再按需执行迁移；不会重置历史动态或好友关系。

同一轮还收紧了 API 降级策略：仅在 Java API 完全不可达时使用 Mock。
Java API 返回 `4xx` 或 `5xx` 时，界面保留失败结果，不会写入浏览器内的
Mock 数据。

聊天访问控制同样在 Service 层统一执行。HTTP 和 WebSocket 推送共用这条
已验证的消息写入链路，因此非好友请求不能读取、发送或撤回消息，也不会
产生实时事件或审计记录。

## 最近一次界面修复

最近一次修复只改了聊天滚动布局。问题原因是窄窗口下触发移动端布局，
`.workspace-view` 变成自适应高度，长聊天会把 `.message-stream` 撑高，
导致消息区自身没有滚动空间。

修复点在 `styles.css`：

- `.message-stream` 使用 `align-content: start`，避免长消息列表被贴底裁切。
- `.message-stream` 保持 `overflow: auto`，并增加 `overscroll-behavior:
  contain`。
- 窄窗口媒体查询中给 `.main-stage` 设置固定视口高度：

  ```css
  .main-stage {
    height: calc(100vh - 66px);
    min-height: 520px;
  }
  ```

浏览器验证中，进入林一与周同学聊天后，消息流数据为：

```text
clientHeight = 300
scrollHeight = 1842
overflowY = auto
scrollTop 从 1541.5 变到 1181.5
```

这确认消息区可以内部滚动。

## 明确回退掉的内容

不要把下面这些后续试做内容当作当前基线的一部分。

- `20260707-chat-image-preview-v1` 到
  `20260707-chat-image-preview-v5` 的图片消息版本。
- `20260707-login-refresh-v1`、`20260707-login-minimal-v1`、
  `20260707-login-symbol-v1`、`20260707-login-portal-v1` 的后续登录页
  迭代。
- 聊天图片缩略图、点击大图预览、`imagePreviewOverlay`。
- `state.attachmentPreviewUrls` 和 `state.imagePreview`。
- `isImageAttachment()`、`previewUrl`、`releaseAttachmentPreview()`。
- 搜索结果里已是好友时显示 **聊天** 的改动。
- 点击 **动态** tab 时强制 `loadFeed()` 的改动。
- **当前 Java API 数据库暂无已通过动态** 的 live 空态文案。

当前仓库里可能还存在 `frontend/js/chat/attachments.js`，它是图片消息试做
期间创建的兼容空模块。当前主代码不引用它。不要重新接入它，除非用户
明确要求重新做图片消息。

## 关键文件

后续继续开发时，优先阅读这些文件。

- `AGENTS.md`
- `frontend/AGENTS.md`
- `backend/AGENTS.md`
- `docs/admin-review-workbench-handoff.md`
- `docs/admin-moderation-content-module-fix.md`
- `docs/new-chat-handoff-2026-07-08.md`
- `index.html`
- `app.js`
- `styles.css`
- `frontend/js/app-events.js`
- `frontend/js/loaders.js`
- `frontend/js/state.js`
- `frontend/js/chat/renderers.js`
- `frontend/js/posts/renderers.js`
- `frontend/js/admin/renderers.js`
- `frontend/js/admin/audit-events.js`
- `frontend/js/api/client.js`
- `frontend/js/api/mock-api.js`
- `backend/src/main/java/com/campuslink/controller/FeedController.java`
- `backend/src/main/java/com/campuslink/service/FeedService.java`
- `backend/src/main/java/com/campuslink/mapper/FeedMapper.java`
- `backend/src/main/resources/schema.sql`
- `tests/frontend-smoke.test.js`

## 当前验证记录

截至 2026-07-15，当前功能稳定点已完成以下验证：

1. `./script/run_frontend_check.sh` 通过。
2. 显式加载 Byte Buddy agent 的完整 Maven 测试通过，`142` 个测试无失败、
   错误或跳过；仅有 JVM class-sharing 兼容性警告。
3. 本地前端地址和 `http://127.0.0.1:8080/api/database/health` 都返回 `200`。
4. 通知单条已读、活动/动态目标定位、好友申请通知的处理入口、管理员后台、
   动态审核反馈和聊天页完成回归检查；Java API 模式读取的是本地 MySQL 历史数据，
   不会回退 Mock。
5. `/actuator/health`、管理员指标授权和 API 请求耗时指标由安全集成测试覆盖。

### 可信基线历史验收

下列内容保留以说明既有约束。

1. 运行前端检查：

   ```bash
   ./script/run_frontend_check.sh
   ```

   结果：通过。

2. 使用浏览器打开当前版本：

   ```text
   http://127.0.0.1:5179/?v=20260710-conversation-previews-v1
   ```

   结果：页面无当前版本模块错误。

3. 点击 **快速进入** 后进入工作台。

   结果：可以进入工作台。

4. 打开林一与周同学聊天。

   结果：消息流 `scrollHeight > clientHeight`，实际滚轮上滑后
   `scrollTop` 变小，确认可上下滑动。

5. 使用修改后的后端副本连接本地 MySQL，分别以学生、好友学生和教师账号
   读取动态流。

   结果：全校动态对学生可见，好友动态对已有好友可见，仅老师动态不显示
   给学生且显示给教师。当前本地历史数据中四个演示账号已互为好友，因此
   非好友拒绝分支由 Mock 行为检查和 SQL 条件检查覆盖，未重置数据库。

## 重要注意事项

继续工作前必须遵守以下约束。

- 不要运行 `git reset --hard`、`git clean`，也不要删除或清理未跟踪文件。
- 不要给 `frontend/js/state.js` 的导入添加版本查询参数。多个模块必须
  共享同一个状态单例。
- 修改前端后至少运行：

  ```bash
  ./script/run_frontend_check.sh
  ```

- 修改前端 UI 后，用浏览器或渲染级 smoke 确认管理员后台、动态发布
  审核反馈和聊天页仍可用。
- Java API 连上时，页面显示的是本地数据库历史数据，不是 Mock 固定
  演示数据。不要误把数据库历史消息当作前端渲染错误。
- 如果用户要求恢复干净演示数据，先确认是否允许重置或重种后端数据库。
  不要擅自清数据库。

## 建议下一步

校园活动报名闭环、按用户点赞、好友申请、评论、实时通知、单条已读、动态目标
跳转、好友申请通知的处理入口、真实仪表盘指标、签名 JWT 注销边界、Spring
Security 安全链、GitHub Actions 验证、Docker Compose 本地演示和 Testcontainers
MySQL 集成测试已经完成。Compose
使用独立 MySQL 命名卷，不发布 MySQL 端口，也不访问本地历史数据；CI 会构建、启动
并检查该演示的健康接口。Testcontainers 使用临时 `mysql:8.4` 容器覆盖 MyBatis、
事务和权限边界，且不会访问本机 MySQL 历史数据。Actuator/Micrometer 已提供公开的
状态摘要、管理员受保护的核心指标和请求诊断。阶段四已完成；Vue 第一认证切片也已
完成，旧入口继续并行保留。Vue 应用壳、联系人与聊天、动态、活动和通知切片也已
完成；Vue 领域切片已经齐备，下一项是逐项等价验收。最新边界见
[`vue-migration-handoff.md`](vue-migration-handoff.md)、
[`phase-two-activity-handoff.md`](phase-two-activity-handoff.md) 和
[`resume-project-roadmap.md`](resume-project-roadmap.md)。

## 可直接复制到下个对话的提示词

复制下面这段到新对话即可。

```text
请先阅读并遵循仓库中的 AGENTS.md：根目录 AGENTS.md、frontend/AGENTS.md、
backend/AGENTS.md。项目路径是 /Users/linus_k/Documents/test。不要执行
git reset --hard、git clean，也不要删除或清理未跟踪文件。

请优先阅读：
- docs/new-chat-handoff-2026-07-08.md
- docs/admin-review-workbench-handoff.md
- docs/admin-moderation-content-module-fix.md
- docs/phase-two-activity-handoff.md
- docs/resume-project-roadmap.md
- docs/vue-migration-handoff.md

当前功能稳定提交是 `98c2dad Add notification read and target actions`，已推送到
GitHub `main`。可信基线、校园活动报名闭环、按用户点赞、好友申请、评论通知、
社交通知实时推送、单条已读、动态目标跳转、好友申请通知处理入口、真实仪表盘
指标、签名 JWT 注销边界、Spring Security 安全链、GitHub Actions 验证和 Docker
Compose 演示、Testcontainers MySQL 集成测试和 Actuator/Micrometer 可观察性已经
完成。Compose 使用独立 MySQL 命名卷，健康接口为 `/api/database/health`；
Testcontainers 使用临时 `mysql:8.4` 容器覆盖 MyBatis、事务和权限边界；
`/actuator/health` 公开，指标端点只允许管理员 JWT。阶段四已完成。下一项为 Vue 3
与 Vite 的渐进前端迁移：保留当前根目录静态入口和 `frontend/js/` 作为功能基线，先在
独立 `frontend-vue/` 建立 Vite、Vue Router、Pinia、认证与 API/Mock 边界，再按领域
切片迁移。详细计划见 `docs/vue-migration-handoff.md`。不要重做已完成链路。
活动逻辑必须继续保留在独立领域模块，
不能塞进 `FeedService` 或 `AdminService`。

当前静态资源版本是 `20260715-signed-jwt-logout-v1`，本地验证地址是：
http://127.0.0.1:5179/?v=20260715-signed-jwt-logout-v1

当前保留的功能：
- 管理员后台有“待审核内容”工作台，位于指标卡片下方、审计记录上方。
- 管理员可对待审核内容执行“同意”“拒绝”“查看”。
- “待审核内容”和“审计记录”支持多选后的“删除所选”。
- 普通用户发布动态后自动打开“我的动态”，看到待审核状态。
- 普通用户发布评论后看到“评论已提交审核，通过后会显示在动态下”。
- 个人动态管理展示待审核、已发布、已拒绝状态和审核原因。
- 公共动态和评论只展示 approved 内容。
- 动态点赞按当前用户持久化，可再次点击取消；动态作者能在站内通知看到点赞。
- 好友申请及其同意、拒绝结果写入持久化站内通知，并按收件人严格隔离。
- 非作者提交评论后，动态作者会收到持久化评论通知；待审核评论不会提前公开。
- 已连接的收件人会立即收到点赞、好友申请和评论通知，统一未读数无需刷新。
- 每条活动和动态通知均可单独标记为已读；点击后会打开并高亮对应目标。
- 聊天附件保持普通文件卡片展示，不保留图片气泡或大图预览。
- 登录页是黑色右侧信息面板版本，文案为“简洁的校园沟通空间”。

明确不要把这些后续试做内容当作当前基线：
- chat-image-preview v1-v5 图片消息版本。
- 后续 login-refresh/login-minimal/login-symbol/login-portal 登录页迭代。
- imagePreviewOverlay、state.imagePreview、state.attachmentPreviewUrls。
- 搜索好友按钮改成“聊天”的改动。
- 点击动态 tab 强制 loadFeed() 的改动。
- Java API 空态文案“当前 Java API 数据库暂无已通过动态”。

注意：
- 不要给 state.js 加版本查询参数，否则可能破坏共享状态单例。
- 修改前端后至少运行 ./script/run_frontend_check.sh。
- 修改 UI 后用浏览器或渲染级 smoke 检查管理员后台、动态审核反馈和聊天页。
- Java API 连上时显示本地数据库历史数据，不是 Mock 固定演示数据。
- 不要使用 git reset --hard、git clean 或任何清理未跟踪文件的命令。
- 后端修改后运行相关 Maven 测试。完整测试使用
  `/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -f backend/pom.xml -DargLine=-javaagent:/Users/linus_k/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.5/byte-buddy-agent-1.17.5.jar test`；
  本机 Microsoft JDK 21 仍会输出 Byte Buddy 动态挂载兼容性警告，必须如实报告。
- 每个验证完成的小阶段单独提交并推送 `main`，同时更新 README 和交接文档。
- Vue 已完成脚手架、认证、应用壳、联系人与聊天、动态、活动、通知和管理员切片。
  管理员界面继续受服务端角色校验保护，且 API `4xx`、`5xx` 仍不回退 Mock。不要替换
  根目录入口或删除、移动旧前端；下一步是新旧逐项等价验收。新旧应用必须继续并行启动
  和验证。
```
