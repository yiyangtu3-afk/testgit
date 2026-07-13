# CampusLink 阶段二活动报名交接

本文交接 CampusLink 的稳定基线和下一阶段工作。活动创建与审核、学生报名与
候补，以及时间和类别筛选已经完成；下一项进入持久化通知与 WebSocket 推送，
不要重做已完成的聊天、动态、内容审核和活动报名链路。

## 稳定基线

本轮实现继承 `92c6146 Add activity registration and waitlist workflow`。远程
仓库是 `https://github.com/yiyangtu3-afk/testgit.git`，使用 `main` 分支；最新
活动实现提交以 `main` 的最新提交为准。

静态前端版本为 `20260712-activity-filters-v1`，本地地址为：

```text
http://127.0.0.1:5179/?v=20260712-activity-filters-v1
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

## 活动领域设计

第一条后端垂直切片的字段、状态机、权限矩阵、API、事务边界和测试场景已
整理到 [`activity-domain-design.md`](activity-domain-design.md)。拒绝审核时，
活动返回 `draft`，最新审核决策记录为 `rejected`，拒绝原因同时保留在活动
当前状态和追加式审核历史中。这一处理保持了既定活动状态集合，同时让拒绝
结果可查询、可追溯。

## 第一条后端垂直切片

“教师或社团负责人创建活动到管理员审核发布”的后端实现已经完成并通过完整
回归。活动逻辑使用独立的 `ActivityService`、DTO、Entity、Mapper、Repository
和 Controller，没有进入 `FeedService` 或 `AdminService`。

- `activities` 保存当前活动状态，`activity_reviews` 保存追加式审核历史。
- `POST /api/activities` 从 bearer token 获取组织者，不接受客户端组织者 ID。
- `GET /api/activities` 返回本地 MySQL 中的 `published` 和 `full` 活动，并
  支持可选的 `from`、`to` 和 `category` 筛选。
- `GET /api/admin/activities/pending` 只允许管理员查看待审活动。
- `POST /api/admin/activities/{activityId}/reviews` 支持 `approve` 和 `reject`。
- 教师和社团负责人可以创建；学生和仅管理员角色不能创建。
- 管理员批准后活动变为 `published`；拒绝后返回 `draft`，并保留拒绝原因。
- 创建和审核都通过服务层事务写入活动与审核历史。
- 条件更新只允许处理 `pending` 活动，避免重复审核覆盖原结果。

## 活动前端闭环

静态前端新增独立 `activities/` 模块和 **校园活动** 导航，不把活动流程加入
动态或通用管理员事件模块。

- 所有登录账号可以查看 Java API 或 Mock 返回的已发布活动。
- 教师和社团负责人可以填写标题、类别、地点、时间、容量和详情后提交。
- 表单不包含组织者字段；创建结果使用服务端返回的当前登录身份。
- 创建成功后在当前会话显示 **待审核**，并说明通过前不进入公开列表。
- 管理员后台在内容审核区前展示独立的 **待审核活动** 区域。
- 管理员可以同意发布，或填写原因后拒绝；拒绝结果保留原因。
- live API 返回 `4xx` 或 `5xx` 时直接显示错误，不写入 Mock 活动。
- Mock API 使用相同字段、权限、状态迁移和错误反馈。
- Mock 与 MySQL 种子账号都增加 `u-2004` 社团负责人 **王社长**。
- 学生活动页支持包含边界日期的时间范围和精确类别筛选；空结果、清除筛选和
  当前报名或候补状态都有明确反馈。

## 当前验证记录

后端稳定点和本轮前端改动已完成以下自动检查。

1. 活动 Service 和 HTTP 定向测试共 15 个通过。
2. MySQL 集成测试共 2 个通过，覆盖批准、拒绝和审核历史映射。
3. 集成测试使用 `@Transactional` 与 `@Rollback`，测试后查询确认匹配的测试
   活动行数为 `0`。
4. `./script/run_frontend_check.sh` 通过，覆盖静态结构、JavaScript 语法、活动
   渲染转义、Mock 创建到审核发布流程，以及原有管理员、动态和聊天 smoke。
5. 本轮活动相关后端 Service 和 Controller 测试 15 个通过。
6. 后端稳定点使用 Microsoft JDK 21 运行完整 `mvn test`，79 个测试全部通过，
   无失败、错误或跳过。
7. Mockito/Byte Buddy 本次成功动态加载 Java agent，只输出未来 JDK 禁止动态
   加载的兼容性警告，没有造成测试失败。
8. 8080 已使用当前代码重启并连接 MySQL。教师、社团负责人、管理员和学生
   demo 登录均返回 `200`；活动公开列表与管理员待审队列返回 `200`。
9. 学生创建活动返回 `403`，教师提交容量 `0` 返回 `400`，错误文案正确且
   两次请求都没有写入活动。
10. 已在本机浏览器以教务管理员身份重新加载并验证管理员后台：活动审核工作区
    显示在内容审核工作区之前，且能显示一条待审活动与同意、拒绝操作。
11. 修复了活动待审请求在初次加载时失败会使容器保持空白的问题。管理员进入
    后台会先看到活动审核工作区；请求失败时显示错误反馈而非隐藏该工作区。
12. 修复了管理员页沿用三行网格造成活动审核区被后续工作区遮挡的问题。后台
    现使用普通文档流依次显示指标、活动审核、内容审核与审计记录，避免弹性
    子项收缩后内容溢出；前端 smoke 对此布局结构保留回归断言。
13. 前端缓存版本升级为 `20260712-activity-filters-v1`，确保浏览器重新
    获取 `styles.css` 和相关模块。所有 `state.js` 导入继续保持无查询参数。
14. 2026 年 7 月 11 日以 Java API 模式完成浏览器验收：陈老师提交
    **活动闭环验收 20260711** 后，教务管理员在独立的待审核活动工作区发布；
    林一随后在公开列表看到该活动。管理员内容审核工作区和聊天页面也可正常
    打开。此次验收写入本地 MySQL 历史数据，未回退 Mock 或重置数据库。
15. 2026 年 7 月 12 日完成报名、取消与候补第一条前后端切片：活动卡片提供
    **立即报名**、**加入候补** 和 **取消报名** 操作；后端以活动行锁、当前报名
    表和追加式事件表保证容量判断与递补同属一个事务。前端 smoke、21 个定向
    测试，以及 1 个 `@Transactional`/`@Rollback` MySQL
    集成测试均通过。集成测试仅输出 Microsoft JDK 21 的 Mockito/Byte Buddy
    动态挂载警告。
16. 2026 年 7 月 12 日完成时间与类别筛选：公开列表 API 支持包含边界的
    `from`、`to` 和精确 `category` 组合查询；前端提供日期、类别、空结果与
    清除筛选交互，并在结果中恢复学生报名状态。MyBatis 筛选集成测试使用
    `@Transactional` 与 `@Rollback`；完整 Maven 测试 91 个全部通过。浏览器以
    Java API 和历史 MySQL 数据验证活动页，并回归管理员后台、动态和聊天页。
    `state.js` 继续保持无版本参数，同时新增旧缓存缺少筛选状态的兼容测试。

## 下一项工作

下一项实现审核、报名、候补和递补的持久化通知与 WebSocket 推送。通知必须在
业务事务提交后可追溯，前端显示未读状态和明确结果；不要在这一切片加入组织者
名单、签到或导出。

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
会输出 Microsoft JDK 21 的 Mockito/Byte Buddy 动态挂载兼容性警告，需如实
报告警告和测试的实际结果。

本地 Java API 连上时展示的是历史 MySQL 数据，不能把它误认为 Mock 数据。
不要使用 `git reset --hard`、`git clean` 或任何清理未跟踪文件的命令。每个
验证完成的阶段单独提交并推送到 `main`，同步更新 README 与相关交接文档。
