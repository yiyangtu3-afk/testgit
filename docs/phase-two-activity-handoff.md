# CampusLink 阶段二活动报名交接

本文交接 CampusLink 的稳定基线和下一阶段工作。可信基线已完成并推送到
GitHub `main`；下一个对话从校园活动报名闭环的设计与第一条后端链路开始，
不要重做已完成的聊天、动态和审核功能。

## 稳定基线

当前 Git 提交为 `f06b09d Add transactional workflow safeguards`。工作区在
交接时干净，远程仓库是 `https://github.com/yiyangtu3-afk/testgit.git`，使用
`main` 分支。

静态前端版本为 `20260710-conversation-previews-v1`，本地地址为：

```text
http://127.0.0.1:5179/?v=20260710-conversation-previews-v1
```

后端本地地址为：

```text
http://127.0.0.1:8080
```

## 已完成能力

当前系统已有验证码登录、好友申请、受好友关系限制的单聊、动态与评论审核、
管理员审核工作台、审计记录、MySQL 持久化和 WebSocket 聊天通知。

- 真实 Java API 返回 `4xx` 或 `5xx` 时，前端不会错误写入 Mock 数据。
- 动态可见范围已在 MySQL 查询中执行，公共动态和评论只显示 `approved`。
- 聊天读取、发送与撤回必须是好友双方，支持游标分页、持久化已读状态和
  联系人最新消息预览。
- 用户生成内容通过 HTML 转义与 CSP 防护，不能作为脚本执行。
- 跨表业务写入由服务层 `@Transactional` 保护。
- `ChatRepositoryIntegrationTest` 使用真实 MySQL，并禁用 `schema.sql` 与
  `data.sql`；测试数据会自动回滚。

完整功能约束见
[`new-chat-handoff-2026-07-08.md`](new-chat-handoff-2026-07-08.md)。管理员
审核工作台细节见
[`admin-review-workbench-handoff.md`](admin-review-workbench-handoff.md)。

## 不要恢复的试做内容

以下内容明确不属于当前产品基线：聊天图片预览、图片消息气泡、登录页后续
试做版本、搜索结果的直接聊天按钮、动态 Tab 强制刷新、`state.imagePreview`
和 `state.attachmentPreviewUrls`。聊天附件继续保持普通文件卡片。

不要给 `frontend/js/state.js` 的导入添加版本参数，否则会破坏共享状态单例。

## 阶段二目标

阶段二要交付校园活动报名闭环，而不是单独增加一个活动列表页面。完整闭环为：

1. 教师或社团负责人创建活动。
2. 管理员审核后发布活动。
3. 学生浏览、报名、取消报名，满员后进入候补。
4. 名额释放时，事务化递补第一位候补者。
5. 用户收到状态变化通知，组织者可管理名单和签到，管理员可查看真实指标。

活动状态固定为 `draft`、`pending`、`published`、`full`、`closed` 和
`cancelled`。报名状态固定为 `registered`、`waitlisted`、`checked_in` 和
`cancelled`。

## 下一个任务的边界

先完成设计和第一条后端垂直切片，不要一次实现完整报名系统。第一个提交要
覆盖“创建活动到管理员审核发布”：

- 在 `schema.sql` 中新增活动和活动审核所需的表、索引与兼容迁移。
- 增加活动领域的 DTO、Entity、Mapper、Repository、Service 和 Controller，
  不把活动逻辑塞进 `FeedService` 或 `AdminService`。
- 只有教师或社团负责人可以创建；学生不能伪造组织者身份；管理员才能审核。
- 创建活动后进入 `pending`，审核同意后变为 `published`，拒绝后保留原因。
- 写入与现有审核、审计边界保持一致，并以事务保护跨表写入。
- 为服务规则和 HTTP 边界写测试；涉及 MyBatis 时增加可回滚的数据库集成测试。

在动手前，先把字段清单、状态迁移、权限矩阵和测试场景写入或更新为可审阅的
设计文档。建议字段至少包含标题、详情、类别、地点、开始与结束时间、容量、
组织者、状态、审核原因和创建时间。不要把客户端传来的用户 ID 当作组织者。

## 必读文件

下一个对话开始时，依次阅读：

1. 根目录 `AGENTS.md`。
2. `frontend/AGENTS.md` 和 `backend/AGENTS.md`。
3. `docs/new-chat-handoff-2026-07-08.md`。
4. `docs/admin-review-workbench-handoff.md`。
5. `docs/admin-moderation-content-module-fix.md`。
6. 本文与 `docs/resume-project-roadmap.md`。
7. `backend/src/main/resources/schema.sql`、`data.sql`，以及现有
   `FeedService`、`AdminService`、Repository 和 Mapper 实现。

## 验证与 Git

前端修改后至少运行 `./script/run_frontend_check.sh`，并用浏览器检查管理员
后台、动态审核反馈和聊天页。后端修改后运行 Maven 测试；本机完整 `mvn test`
目前会受 Microsoft JDK 21 的 Mockito/Byte Buddy 动态挂载限制影响，需同时
报告受影响的测试和本次相关测试的实际结果。

本地 Java API 连上时展示的是历史 MySQL 数据，不能把它误认为 Mock 数据。
不要使用 `git reset --hard`、`git clean` 或任何清理未跟踪文件的命令。每个
验证完成的阶段单独提交并推送到 `main`，同步更新 README 与相关交接文档。
