# CampusLink 新对话交接说明（2026-07-09 更新）

本文用于在新 Codex 对话中恢复 CampusLink 当前开发上下文。当前状态保留
**普通用户审核状态反馈**、窄窗口聊天滚动修复、动态可见范围真实过滤和
好友聊天访问控制。

## 当前基线

当前前端基线保留管理员审核工作台和普通用户审核状态反馈，不保留后来
试做的聊天图片消息、图片预览、图片缓存、搜索好友直接聊天、动态页强制
刷新、Java API 空态文案、以及 portal 风格登录页。

当前静态资源版本是：

```text
20260710-conversation-previews-v1
```

当前本地验证地址是：

```text
http://127.0.0.1:5179/?v=20260710-conversation-previews-v1
```

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

已完成以下验证。

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

- 当前仓库 `git status --short` 显示大量未跟踪文件。这是已知状态。
  不要运行 `git reset --hard`、`git clean`，也不要删除未跟踪文件。
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

可信基线已完成。下一步进入校园活动报名闭环，先完成活动数据模型、状态机、
权限矩阵和测试用例设计，再实现教师或社团负责人创建活动、管理员审核发布的
第一条可演示链路。详细边界见
[`phase-two-activity-handoff.md`](phase-two-activity-handoff.md)。

## 可直接复制到下个对话的提示词

复制下面这段到新对话即可。

```text
请先阅读并遵循仓库中的 AGENTS.md：根目录 AGENTS.md、frontend/AGENTS.md、
backend/AGENTS.md。项目路径是 /Users/linus_k/Documents/test。不要重置或
清理未跟踪文件，当前仓库可能整体显示为未跟踪状态。

请优先阅读：
- docs/new-chat-handoff-2026-07-08.md
- docs/admin-review-workbench-handoff.md
- docs/admin-moderation-content-module-fix.md
- docs/phase-two-activity-handoff.md
- docs/resume-project-roadmap.md

可信基线已完成，当前稳定提交为 f06b09d Add transactional workflow
safeguards，已推送到 GitHub main。下一阶段是“校园活动报名闭环”；请先完成
活动数据模型、状态机、权限矩阵和测试用例设计，再实现“创建活动到管理员
审核发布”的第一条后端垂直切片。活动逻辑必须放在独立领域模块，不能塞进
FeedService 或 AdminService。

当前基线已经回退到“普通用户审核状态反馈完成”之后，并额外修复了窄窗口
下林一与周同学聊天内容不能上下滑动的问题。当前静态资源版本是
20260710-conversation-previews-v1，本地验证地址是：
http://127.0.0.1:5179/?v=20260710-conversation-previews-v1

当前保留的功能：
- 管理员后台有“待审核内容”工作台，位于指标卡片下方、审计记录上方。
- 管理员可对待审核内容执行“同意”“拒绝”“查看”。
- “待审核内容”和“审计记录”支持多选后的“删除所选”。
- 普通用户发布动态后自动打开“我的动态”，看到待审核状态。
- 普通用户发布评论后看到“评论已提交审核，通过后会显示在动态下”。
- 个人动态管理展示待审核、已发布、已拒绝状态和审核原因。
- 公共动态和评论只展示 approved 内容。
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
- 完整 mvn test 在本机 Microsoft JDK 21 下仍受 Mockito/Byte Buddy 动态挂载
  限制影响；需要报告受影响测试与本次相关测试的实际结果。
```
