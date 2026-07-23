# CampusLink 阶段二活动报名交接

本文交接 CampusLink 的稳定基线和下一阶段工作。活动创建与审核、学生报名与
候补、时间与类别筛选、持久化通知、组织者名单、签到、CSV 导出，以及真实
活动管理指标已经完成。阶段二闭环已全部交付；阶段三的按用户点赞、作者通知、
好友申请通知、评论通知、社交通知实时推送和通知操作切片也已完成。好友申请类
通知的可追溯操作入口已经完成，不要重做已完成的聊天、动态、内容审核和活动
报名链路。

## 稳定基线

本轮实现继承 `f296c13 Complete activity roster and check-in workflow`。当前功能
稳定提交是 `98c2dad Add notification read and target actions`。远程仓库是
`https://github.com/yiyangtu3-afk/testgit.git`，使用 `main` 分支；后续仅文档
交接提交可能更新，不应被误判为新的功能基线。

静态前端版本为 `20260715-signed-jwt-logout-v1`，本地地址为：

```text
http://127.0.0.1:5179/?v=20260715-signed-jwt-logout-v1
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
- 聊天未读统计同时限定发送者和收件人，好友发给第三人的消息不会串入当前
  账号的联系人未读数。
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
- 独立通知中心展示审核、报名、候补和递补结果，支持未读计数与全部已读。
- 通知先与活动业务结果一同写入 MySQL，事务成功提交后才向收件人发送
  `activity.notification.created` WebSocket 事件。
- WebSocket 复用已认证的 `/ws/chat` 传输，但通知持久化领域不依赖聊天、动态
  或管理员服务。
- 用户离线时产生的通知会在下次登录后从 Java API 恢复；live API 错误不会
  回退到 Mock 通知。

## 组织者活动运营闭环

阶段二最后一项使用现有活动领域边界完成，没有把名单、签到或指标加入动态和
通用管理员服务。

- `GET /api/activities/managed` 只返回当前教师或社团负责人创建的活动。
- `GET /api/activities/{activityId}/registrations/roster` 校验当前账号是活动
  组织者，并返回参与者姓名、状态、候补位置和时间记录。
- `POST /api/activities/{activityId}/registrations/{registrationId}/check-in`
  只允许组织者把 `registered` 变为 `checked_in`，重复签到或候补签到返回
  `409`。
- `checked_in_at` 通过幂等 `schema.sql` 迁移加入现有 MySQL 表；签到状态和
  追加式事件在同一 `@Transactional` 方法中写入。
- `GET /api/admin/activity-metrics` 只允许管理员读取真实占位报名数和签到数，
  并由独立 `ActivityOperationsAdminController` 暴露。
- 前端 **我的活动运营** 从 Java API 恢复历史活动和名单，支持现场签到与
  CSV 导出。Mock API 使用相同权限、状态和响应结构。

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
17. 2026 年 7 月 12 日完成活动通知闭环：新增 `activity_notifications` 表、
    当前用户通知 API、全部已读操作和提交后 WebSocket 推送。审核同意与拒绝、
    报名成功、加入候补和递补都在原业务事务内写通知；1 个真实 MySQL
    `@Transactional`/`@Rollback` 集成测试覆盖完整候补递补与已读流程。完整
    Maven 测试 97 个全部通过，无失败、错误或跳过，仅输出 Microsoft JDK 21
    的 Mockito/Byte Buddy 动态代理兼容性警告。前端检查通过，浏览器以真实
    Java API 验证教师审核通知、学生实时报名与候补通知、离线递补恢复和全部
    已读，并回归管理员后台、动态审核反馈与聊天页；MySQL 历史数据未重置，
    浏览器控制台无错误。前端版本升级为
    `20260712-activity-notifications-v1`，共享 `state.js` 导入仍无查询参数。
18. 2026 年 7 月 12 日修复联系人未读数串号：未读 MyBatis 查询新增当前
    收件人约束，不再把好友发给第三人的消息计入当前账号。测试先复现陈老师
    错误看到周同学 `4` 条、教务管理员 `2` 条未读的行为，再用最小 SQL 修复
    转绿。`ChatRepositoryIntegrationTest` 现有 3 个真实 MySQL
    `@Transactional`/`@Rollback` 场景，覆盖跨会话排除、真实收件计数和已读
    游标清零；聊天 Repository、Service 和 Controller 定向测试 16 个通过，
    完整 Maven 测试 99 个全部通过。浏览器以 Java API 验证陈老师的两个空
    会话都显示 **暂无消息** 且没有未读徽标，打开空会话后消息数为 `0`，控制台
    无错误。测试和验收都保留本地 MySQL 历史数据；本轮没有前端代码改动，
    前端版本保持不变。
19. 2026 年 7 月 12 日完成活动运营闭环：教师和社团负责人可以读取自己创建的
    持久化活动，展开报名、签到与候补名单，确认现场签到并导出 CSV。签到将
    `registered`、`checked_in_at` 和追加式 `checked_in` 事件放在同一事务；
    MyBatis 真实数据库测试使用 `@Transactional` 与 `@Rollback`。管理员通过
    独立 `/api/admin/activity-metrics` 读取真实报名和签到数量，活动逻辑没有
    进入 `AdminService`。完整 Maven 测试 107 个全部通过，无失败、错误或
    跳过；Mockito/Byte Buddy 仅输出未来 JDK 动态代理兼容性警告。前端检查
    通过，浏览器在 Java API 模式读取本地 MySQL 的 3 个教师活动，打开包含
    1 个待签到和 1 个候补学生的名单并生成 CSV；管理员指标显示 5 个占位报名、
    0 个签到。管理员活动审核、动态审核详情和聊天页均完成回归，页面无覆盖。
    前端版本升级为 `20260712-activity-operations-v1`，共享 `state.js` 导入仍
    无查询参数。
20. 2026 年 7 月 13 日完成按用户点赞与作者通知：`post_likes` 以动态和用户
    组成唯一键，再次点击取消当前用户的点赞；历史 `posts.likes` 作为兼容计数
    保留，种子启动不再覆盖历史总数。新点赞通过独立社交通知 Repository、
    Mapper、Service 和 Controller 写给动态作者，本人点赞不通知，取消点赞
    保留历史通知。点赞关系、计数、审计和通知在同一事务中写入。真实 MySQL
    `@Transactional`/`@Rollback` 集成测试通过；显式加载 Byte Buddy agent 后
    完整 Maven 测试 114 个全部通过。前端检查通过，浏览器以 Java API 验证
    **赞 12**、**已赞 13**、取消恢复、作者未读通知和统一全部已读，并回归
    管理员后台、动态审核详情和聊天页；控制台无错误。前端版本升级为
    `20260713-social-like-notifications-v1`，共享 `state.js` 导入仍无查询参数。
21. 2026 年 7 月 15 日完成好友申请持久化通知：发送申请时收件人获得
    `social.friend.requested`，同意或拒绝时原申请人分别获得
    `social.friend.accepted` 或 `social.friend.rejected`。`FriendService` 的创建、
    同意和拒绝流程都以事务写入申请状态、好友关系或聊天系统消息、审计记录和
    通知；通知仍由独立 `SocialNotificationService` 负责，未塞入好友仓储或
    控制器。新 MySQL `@Transactional`/`@Rollback` 集成测试覆盖申请、同意和
    两端通知；相关测试 13 个和显式 Byte Buddy agent 下完整 Maven 117 个测试
    均通过。前端 Mock 保持同一通知语义，站内通知显示好友申请、已添加和申请
    结果标签；前端检查通过。前端版本升级为
    `20260715-friend-notifications-v1`，共享 `state.js` 导入仍无查询参数。
22. 2026 年 7 月 15 日完成评论作者通知：非作者提交评论时，动态作者获得
    `social.post.commented`，通知目标使用评论 ID，内容显示评论者和至多 60 个
    字的评论预览；作者评论自己动态不会产生自通知。评论仍然以 `pending` 写入，
    审核通过前不进入公开列表；评论、审核记录、审计和通知受 `FeedService`
    事务保护。新增 MySQL `@Transactional`/`@Rollback` 集成测试确认待审核
    评论与作者通知一同回滚；显式 Byte Buddy agent 下完整 Maven 119 个测试
    均通过。前端 Mock 和渲染级 smoke 保持同一语义，站内通知显示 **动态评论**
    标签。前端版本升级为 `20260715-comment-notifications-v1`，共享 `state.js`
    导入仍无查询参数。
23. 2026 年 7 月 15 日完成社交通知实时推送：新增独立
    `SocialNotificationCreatedEvent`、提交后监听器和实时发布契约；
    `ChatWebSocketHandler` 仅作为已认证传输，实现收件人定向的
    `social.notification.created` 事件。点赞、好友申请及处理结果、评论通知仍由
    `SocialNotificationService` 在原业务事务中持久化，只有事务提交后才推送。
    前端按通知 ID 去重并立即重绘统一未读数，离线历史仍从 MySQL 恢复。新增
    Service、提交后监听器和 WebSocket handler 测试；显式 Byte Buddy agent 下
    完整 Maven 测试 122 个通过，前端 smoke 通过。前端版本升级为
    `20260715-social-realtime-v1`，共享 `state.js` 导入仍无查询参数。
24. 2026 年 7 月 15 日完成通知操作闭环：活动和社交通知分别新增按
    `notificationId + recipientId` 条件更新的单条已读接口，无法修改其他收件人
    的记录。活动项打开并高亮对应活动；点赞和评论项通过独立
    `SocialNotificationTargetService` 在当前收件人范围内解析到动态。评论历史
    仍以评论 ID 反查所属动态，避免误把评论 ID 当作动态 ID。新的 MyBatis 条件
    更新和评论目标解析由 `@Transactional`/`@Rollback` MySQL 集成测试覆盖，
    MockMvc、前端 Mock 行为和渲染 smoke 均通过；显式 Byte Buddy agent 下完整
    Maven 测试 127 个通过。前端版本升级为
    `20260715-notification-actions-v1`，共享 `state.js` 导入仍无查询参数。
25. 2026 年 7 月 15 日完成好友申请通知处理入口：通知中心的
    **处理申请** 只向 `social.friend.requested` 显示。新目标接口先以 bearer
    token 锁定当前收件人，再同时校验通知归属、发送者、申请 ID 和 `pending`
    状态；前端只使用服务端返回的申请 ID 高亮已有待处理申请并复用 **同意**、
    **拒绝**。Mock 行为、MockMvc、`@Transactional`/`@Rollback` MySQL 集成测试
    和前端 smoke 均通过；显式 Byte Buddy agent 下完整 Maven 测试 128 个通过。
    前端版本升级为 `20260715-friend-request-actions-v1`，共享 `state.js` 导入
    仍无查询参数。
26. 2026 年 7 月 15 日完成真实仪表盘指标：新增独立管理员指标 Repository 和
    Mapper，**注册用户** 与 **今日消息** 分别按 `users` 和当天 `messages`
    计算；**动态总数** 改为统计全部 `posts`，不再只统计公开动态；**待审内容**
    与活动报名、签到保留原有真实查询。Mock 以账户和消息历史计算同名字段。
    新的 `@Transactional`/`@Rollback` MySQL 集成测试确认当前日消息会在事务内
    写入后计入指标并自动回滚；前端 smoke 通过。显式 Byte Buddy agent 下完整
    Maven 测试 130 个通过。前端版本升级为 `20260715-real-dashboard-metrics-v1`，
    共享 `state.js` 导入仍无查询参数。
27. 2026 年 7 月 15 日完成签名 JWT 与注销边界：登录为当前 MySQL 用户签发
    HMAC-SHA256 JWT，默认一小时过期；受保护请求先验证签名和过期时间，再校验
    `auth_sessions` 中的同 subject 会话，不能伪造当前用户或管理员角色。新增
    `POST /api/auth/logout`，只删除当前 bearer token 的会话，前端在 live API 和
    Mock 模式维持同一退出体验；API 的 `4xx`/`5xx` 不会触发 Mock 写入。
    `auth_sessions.token` 以幂等迁移扩展至 `varchar(512)`，仅扩展列、不重置或清理
    本地历史数据。MyBatis 删除由新的 `@Transactional`/`@Rollback` MySQL 集成测试
    覆盖，另有 JWT 过期、注销和 HTTP 边界测试；前端检查通过，`state.js` 导入仍
    无版本参数。显式 Byte Buddy agent 下完整 Maven `133` 个测试通过，仅输出
    JVM class-sharing 警告。
28. 2026 年 7 月 15 日完成 Spring Security HTTP 安全链：JWT 过滤器把已验证且
    未撤销的数据库用户放入无状态安全上下文，控制器可继续复用已有身份边界；
    `/api/auth/**` 与 `/api/database/health` 公开，其他 `/api/**` 必须认证，
    `/api/admin/**` 统一要求 `ROLE_ADMIN`。认证和授权失败返回既有 JSON `message`
    结构，避免前端把 `401` 或 `403` 误回退到 Mock。CORS 移入安全配置并明确允许
    `PATCH`，兼容非 Web MySQL 测试上下文。新的 `@Transactional`/`@Rollback`
    安全集成测试覆盖公开登录、无令牌、无效令牌、有效 JWT 和学生管理员拒绝；
    显式 Byte Buddy agent 下完整 Maven `137` 个测试通过，仅输出 JVM
    class-sharing 警告。
29. 2026 年 7 月 15 日完成 GitHub Actions 验证：`.github/workflows/verify.yml`
    在推送到 `main`、面向 `main` 的拉取请求和手动触发时启动临时 MySQL 8.4 服务，
    再由 Linux runner 的 Bash 运行 `./script/run_frontend_check.sh`，并运行带显式
    Byte Buddy agent 的完整 Maven 测试。CI 在测试前只向该临时服务导入
    UTF-8 `schema.sql` 和 `data.sql`，使关闭启动期初始化的回滚 MyBatis 测试可复现；
    聊天集成测试也会在其回滚事务内显式设置好友关系并隔离既有演示会话未读状态。该服务
    不会连接、重置、重种或清理开发者本地 MySQL 历史数据。
30. 2026 年 7 月 15 日完成 Docker Compose 演示：`compose.yml` 启动未发布到主机的
    MySQL 8.4、API 和静态前端；容器数据库使用独立命名卷，保留容器演示数据而不触碰
    本地 MySQL。API 和前端按健康顺序启动，公开
    `/api/database/health` 同时作为 API 健康检查。GitHub Actions 新增 Compose
    构建、启动等待和健康接口请求，提供有 Docker runner 的运行级验证。
31. 2026 年 7 月 15 日完成 Testcontainers MySQL 集成测试：`mysql:8.4` 临时容器在
    Spring 上下文创建前启动，并复用现有 `schema.sql`、`data.sql` 初始化。新的
    `@Transactional`、`@Rollback` 测试验证 MyBatis 动态可见范围查询、好友申请同意
    后的跨表关系与通知写入，以及 JWT 学生账号访问普通 API、拒绝管理员 API 的边界。
    Testcontainers 升级到 `1.21.4`，兼容 Docker Engine 29；临时容器会在测试结束后
    停止，绝不连接、重置、重种或清理开发者本机 MySQL 历史数据。显式 Byte Buddy
    agent 下完整 Maven `140` 个测试通过，仅输出 JVM class-sharing 警告。
32. 2026 年 7 月 15 日完成 Actuator 与 Micrometer 可观察性：公开
    `/actuator/health` 仅返回总体状态，不泄露数据库详情；`/actuator/info` 与
    `/actuator/metrics/**` 要求已有 JWT 管理员角色。新增用户、当日消息、动态、待审
    内容 Gauge，以及按 HTTP 方法、路由模板和状态聚合的 API 耗时 Timer，不使用用户
    或资源 ID 作为指标标签。安全集成测试验证公开健康、学生指标拒绝和管理员指标读取；
    显式 Byte Buddy agent 下完整 Maven `142` 个测试通过，仅输出 JVM
    class-sharing 警告。
33. 2026 年 7 月 15 日确认下一阶段为 Vue 前端渐进迁移：当前原生 ES Modules
    静态前端仍是功能基线，迁移将从独立 `frontend-vue/` 目录开始，并行保留旧入口，
    先建立 Vite、Vue Router、Pinia、认证与 API/Mock 边界。此交接不改变运行代码、
    前端版本、后端或本机 MySQL 数据；详见 `docs/vue-migration-handoff.md`。
34. 2026 年 7 月 15 日完成 Vue 第一认证切片：新增独立 `frontend-vue/`，使用
    Vue 3、Vite、Vue Router 和 Pinia。Vite 在 `5179` 旧版之外使用 `5180`，以
    相对路径代理 `/api` 和 `/ws` 到 Java API。统一 HTTP 边界只把网络层不可达映射
    为 `ApiUnavailableError`；`401`、`403` 和 `500` 保留为真实 HTTP 失败，不会
    回退 Mock。Pinia 会话保存 bearer token 和用户，覆盖验证码登录、演示登录和
    注销。新 Vue 单元测试 6 项、生产构建和旧版前端检查通过；通过 Vite 代理验证
    本地 MySQL API 的验证码登录、演示登录和注销均返回 `200`；未重置、重种或清理
    本机 MySQL 历史数据。
35. 2026 年 7 月 17 日完成 Vue 第二应用壳切片：新增左侧导航、账号身份与退出操作、
    统一 Java API/Mock 状态提示和独立 Pinia shell store。只有工作台框架可用；聊天、
    动态、活动、通知和管理员项都明确标记为待迁移，不读取领域 API 或伪造业务内容。
    新 Vue 单元测试 7 项、生产构建和旧版前端检查通过；旧静态入口与本机 MySQL 数据
    均未修改。
36. 2026 年 7 月 17 日完成 Vue 联系人与聊天切片：联系人搜索、好友申请处理、好友
    会话、游标分页、未读数、普通附件和撤回全部经统一 API/Mock 边界；认证 WebSocket
    收到新消息或撤回事件后刷新会话和未读数。未迁移领域没有进入 Vue。新 Vue 测试、
    生产构建和旧版前端检查通过。
37. 2026 年 7 月 17 日完成 Vue 动态切片：公开动态、我的动态、点赞、评论和审核
    状态反馈复用既有 JWT API 与 Mock 边界。公开流只显示已审核内容，评论提交后提示
    待审核；个人动态显示待审、通过或拒绝原因。Vue 测试、生产构建和旧版前端检查通过。
38. 2026 年 7 月 17 日完成 Vue 活动与通知切片：活动页复用筛选、报名、候补、取消、
    组织者名单和签到 API，学生不请求组织者专属接口。通知页按时间合并活动和社交通知，
    支持单条或全部已读、活动/动态/好友申请的受限目标跳转。`AppShell` 复用单条已认证
    `/ws/chat` 连接分发聊天和通知事件，通知按 ID 去重。新 Vue 测试 14 项、生产构建和
    旧版前端检查通过；HTTP `401`、`403`、`500` 测试确认不会回退 Mock。8080 在本轮
    验证中未运行，因此未把 Mock 结果误记为 MySQL live 验收，也未修改本机 MySQL 数据。
39. 2026 年 7 月 17 日完成 Vue 管理员切片：管理员工作台通过已有 JWT API 读取指标、
    待审核活动、待审核内容、审计记录和报表；非管理员不会请求后台数据。活动和内容审核、
    审计或审核记录删除、报表 CSV 下载保持原有后端权限与数据边界。新 Vue 测试 20 项、
    生产构建和旧版前端检查通过；管理员 API 的 `401`、`403`、`500` 不会回退 Mock。
40. 2026 年 7 月 17 日修复 Vue 动态评论面板：加载评论后页面错误地用草稿对象判断是否
    展开，导致评论列表和输入框始终不可见。展开状态与草稿现已独立管理，并由纯状态单元
    测试覆盖。Vue 测试 21 项、生产构建和旧版前端检查通过。live 验收通过 Vue 代理提交
    一条带日期标识的评论，确认其进入 MySQL 待审队列、经管理员通过后出现在公开评论列表；
    该评论和审计记录按验证需要保留，未重置或清理本机 MySQL 数据。
41. 2026 年 7 月 17 日补齐 Vue 活动创建入口：教师和社团负责人可提交标题、类别、名额、
    地点、开始/结束时间和详情，组织者继续由 JWT 身份决定。store 在客户端拦截结束时间
    不晚于开始时间的无效表单，成功后显示待审核反馈。Vue 测试 23 项、生产构建和旧版
    前端检查通过。live 验收经 Vue 代理创建一条带唯一标识的活动，管理员发布后学生报名、
    收到通知并取消报名；对应活动、审核、报名、通知和审计历史按验收要求保留。
42. 2026 年 7 月 17 日完成 Vue 聊天 live 验收：通过代理发送带唯一标识的消息后，接收方
    未读数从 `0` 增至 `1`，读取会话后归零；发送方撤回后接收方看到撤回状态。接收方的
    `/ws/chat` 连接收到 `message.created` 与 `message.withdrawn`。测试消息已撤回，
    消息与审计历史按现有业务规则保留。
43. 2026 年 7 月 17 日完成 Vue 通知 live 验收：本轮活动报名通知成功单条已读。一次点赞
    并取消点赞生成持久化社交通知，收件方收到 `social.notification.created`，受限动态目标
    解析和单条已读正确。`markAll` 的 store 测试确认活动与社交通知摘要同时更新，不为
    验收改写本机历史通知的全部已读状态。Vue 测试 24 项、生产构建和旧版前端检查通过。
44. 2026 年 7 月 17 日完成 Vue 管理员 live 验收：教师经代理创建唯一待审活动，管理员
    拒绝后组织者端保留拒绝原因；学生创建待审动态并由管理员审核通过后，公开动态流可见。
    随后创建一条待审评论，管理员从内容审核队列删除该记录；本轮新生成的一条审计记录也已
    删除，并确认 `today` 报表返回正确范围、CSV 文件名与指标。除这些业务动作外，未重置、
    重种或清理本机 MySQL 历史数据。
45. 2026 年 7 月 17 日在逐项等价验收和用户确认后切换 Vue 默认入口：
    `run_frontend_demo.sh` 启动 5180 上的 Vue Vite 服务；Compose 在 5179 构建并服务 Vue
    生产包，以 Nginx 代理相对 `/api`、`/ws` 并支持 History 路由刷新。根目录
    `index.html`、`app.js`、`styles.css` 与 `frontend/js/` 保持不变；旧版仍可由
    `run_legacy_frontend_demo.sh` 在 5179 提供，并在 Compose 的 `/legacy/` 使用。
46. 2026 年 7 月 17 日新增 Vue 工作台内的演示账号切换：账号区域不再要求先退出，
    可直接切换学生、教师与管理员。会话 store 先取得目标 demo 会话、撤销旧 token 的服务端
    会话，再持久化新 token 并刷新工作台，避免上一账号的 Pinia 领域状态残留；撤销失败时
    当前账号保持不变。Vue 测试 26 项、生产构建和旧版前端检查通过。
47. 2026 年 7 月 17 日新增 Vue 学生自助注册：登录页收集姓名、手机号和验证码，
    `POST /api/auth/register` 在同一事务内写入 MySQL 用户、用户审计和 JWT 会话，并自动
    进入工作台。手机号已存在时返回 `409`，不会回退 Mock；Mock 仅在 API 不可达时提供本次
    浏览器会话的等价流程。Vue 测试 29 项、生产构建和旧版前端检查通过；回滚式 MySQL
    注册集成测试、Controller 和 Service 测试通过。live 验收经 Vue 代理注册带唯一标识的
    用户，重复手机号返回 `409`，只读 MySQL 查询确认用户记录持久化；该正常注册用户和审计
    历史保留，未重置、重种或清理本机 MySQL 数据。
48. 2026 年 7 月 17 日修复 API 重启后的注册失败：浏览器保存的旧 bearer token 不再附加到
    验证码、登录、注册和演示登录等公开认证请求。注销和受保护领域请求仍携带 token；新增
    回归测试确认四类匿名认证动作均不会携带旧 token。Vue 测试 30 项、生产构建和旧版前端
    检查通过。
49. 2026 年 7 月 18 日修复 Vue 学生注册错误进入活动报名的问题：`createApi()` 合并适配器时，
    活动 `register(id)` 覆盖了认证 `register(name, phone, code)`，导致注册提交到受保护的
    `/api/activities/{name}/registrations`，旧 token 会得到 `401`“请先登录”。认证动作改为
    明确的 `registerStudent`，活动报名继续使用 `register`；组合 API 测试同时覆盖两条路径的
    URL 和 bearer-token 边界。
50. 2026 年 7 月 18 日补齐 Vue 工作台的演示身份菜单：新增现有 MySQL 种子账号
    **王社长**（`u-2004`、社团负责人），并同步补齐 Mock 账号。该身份可直接进入活动创建与
    运营流程；Mock API 测试确认其 `demo-login` 返回正确的社团负责人角色。
51. 2026 年 7 月 18 日修复 Vue 长聊天记录布局：对话面板使用固定高度的 Flex 三段布局，
    只有中部消息流可上下滚动；对话标题与底部发送输入框、待发送附件区保持可见。窄屏下
    联系人和选定对话仍各自保留可用滚动区，不需要滚过整段历史才能发送新消息。
52. 2026 年 7 月 18 日移除 Vue 工作台各页面重复的认证状态横幅：登录来源仍由会话 store
    保存，领域页面继续显示自己的操作反馈；工作台不再为同一条“认证会话已就绪”消息占用
    每个页面的内容高度，聊天面板因此获得更大的首屏可视区。
53. 2026 年 7 月 18 日继续压缩 Vue 聊天面板：对话身份栏、附件操作与发送按钮采用紧凑
    尺寸，输入框默认显示两行但仍支持用户拖拽扩展。释放出的固定面板高度全部分配给消息
    流，因此首屏可同时读取更多记录，长消息仍只在消息区滚动。
54. 2026 年 7 月 18 日完善 Vue 聊天阅读位置：切换会话和发送后定位到最新消息，加载更早
    分页消息后按新增高度补偿滚动位置，避免阅读内容跳动。窄屏新增联系人栏开关，选择会话
    后可收起联系人列表，将对话区作为主要阅读区域。
55. 2026 年 7 月 18 日清理 Vue 侧边栏的开发迁移说明：移除“迁移进度 07/07”、完成计数和
    “已迁移 / 状态与互动 / 审核与报表”等导航副标题。侧边栏只保留功能名称和当前路由高亮，
    让工作台界面专注于实际功能。
56. 2026 年 7 月 18 日扩大 Vue 宽屏聊天对话区：应用侧栏、页面留白和联系人栏改为紧凑宽度，
    聊天工作区取消固定最大宽度，让额外空间优先分配给消息面板。窄屏单列布局和联系人栏
    开关保持不变。
57. 2026 年 7 月 18 日扩大 Vue 聊天垂直可视区：移除重复的聊天页标题和连接说明，对话面板
    直接使用工作台页头以下的可用视口高度；窄屏联系人开关移入对话标题栏。更多高度分配给
    消息流，输入框仍固定在面板底部。
58. 2026 年 7 月 18 日完成 Vue 界面交接验收：最新提交为
    `6a87873 Increase Vue chat height`，已推送到 `main`。在 `1440 × 900` 浏览器视口中，
    Vue 聊天消息流约为 `500px` 高，长历史只在消息区内部滚动，标题与输入区保持可见；窄屏
    联系人开关位于对话标题栏。`frontend-vue` 的 `npm test`（34 项测试、13 个文件）、
    `npm run build` 与旧版 `./script/run_frontend_check.sh` 均通过。该轮没有后端修改，也没有
    重置、重种或清理本机 MySQL 历史数据。
59. 2026 年 7 月 20 日完成内容审核反馈闭环：拒绝内容必须填写审核意见。审核决定持久化
    审核人、审核时间和意见，并在 Vue 审核历史与审计事件中显示；默认待审队列仍保持只返回
    `pending` 内容，旧版不受影响。MySQL 集成测试以事务和回滚验证新增字段写入；条件迁移只
    在缺列时添加字段，不重置或重种本机历史数据。
60. 2026 年 7 月 21 日完成审核辅助首个切片：管理员可针对待审内容主动请求本地、可解释的
    风险提示。接口仅返回风险等级、命中信号和建议意见；不会改变内容状态、预填审核意见、
    写入审核记录或审计记录。Vue 与 Mock 复用同一返回形状；`401`、`403`、`500` 仍显示
    真实 API 失败，不回退 Mock。完整 Maven（含 Docker/Testcontainers MySQL 8.4）151 项、
    Vue 单测 38 项、生产构建和旧版前端检查均通过。
61. 2026 年 7 月 21 日完成可重复的 live 等价验收：新增
    `node script/run_live_equivalence_check.mjs`，比较 Vue `5180` 代理和旧版 `5179`
    直连 API。学生动态、活动、通知、聊天未读与历史，组织者运营与名单，管理员指标、
    审核历史、审计记录和审核辅助均返回相同 JSON；无效通知目标和无效撤回也一致返回
    `400`。脚本仅创建并注销本次演示会话，不写入业务数据或清理 MySQL 历史。
62. 2026 年 7 月 21 日补充审计记录的全选删除入口：管理员可一次选中当前全部审计记录，
    再复用原有 **删除所选** 和二次确认执行批量删除；再次点击会取消全选。该选择仅存在于
    Vue 页面状态，实际删除仍经既有管理员 JWT API。
63. 2026 年 7 月 21 日调整 Vue 内容审核工作台：它仅请求并显示 `pending` 内容，审核通过或
    拒绝后会从功能框移除，不再作为历史记录占用审核操作区。审核人、时间和意见仍保留在
    审计记录与持久化审核数据中。
64. 2026 年 7 月 23 日完成 Vue 活动签到凭证切片：已报名学生可主动展示当前不透明凭证，
    组织者在 **我的活动运营** 输入凭证后完成签到。`activity_check_in_credentials` 只保存
    SHA-256 摘要；再次展示会轮换凭证并使旧码失效。`POST`
    `/api/activities/{activityId}/registrations/current/check-in-credential` 从 JWT 确认学生和
    `registered` 状态；`POST /api/activities/{activityId}/registrations/check-in-credential`
    从 JWT 确认组织者、活动归属、凭证和报名状态，再复用同一事务写入 `checked_in` 与追加式
    事件。Vue 与 Mock 保持同一返回结构，真实 API 的失败不会回退 Mock；旧静态入口没有改动。
    本轮还修正了 Vue 名单手动签到使用 `registrationId` 的真实响应字段。完整 Maven
    测试 156 项、Vue 测试 43 项（16 个文件）、生产构建和旧版前端回归检查均通过；新增
    MySQL 集成测试使用 `@Transactional` 与 `@Rollback`，没有清理本机历史数据。
65. 2026 年 7 月 23 日修复组织者现场签到加载：活动页原本会让教师、社团负责人和管理员
    逐项调用学生专属的“当前报名”接口。服务端正确拒绝该请求后，前端没有继续加载
    **我的活动运营**，使真实活动发起人看不到核验框。现在只有学生读取当前报名，组织者会加载
    自己的活动；活动卡直接显示发起人。浏览器真实 API 验证“揍康鹏”由王社长发起，林一展示
    凭证后，王社长成功核验，名单显示 `checked_in`。Vue 测试更新为 44 项，生产构建和旧版
    回归检查通过；该验证保留正常的 MySQL 签到和事件历史。
66. 2026 年 7 月 23 日收紧 Vue 活动报名入口：只有尚未报名的学生显示 **立即报名**。教师、
    社团负责人和管理员改为看到 **仅学生可报名**，已签到学生显示 **已签到**，避免展示后端会
    拒绝的操作。真实 API 再次用“揍康鹏”验收：林一重新报名、展示凭证，王社长核验后返回
    `checked_in`；学生活动卡不再显示该活动的报名按钮。Vue 测试 45 项、生产构建和旧版
    回归检查通过，正常 MySQL 报名、签到和事件历史均保留。
67. 2026 年 7 月 23 日完成现场签到全链路验收并补齐边界：组织者运营区只对 `published` 与
    `full` 活动显示名单、凭证核验和手动签到；待审核或未通过活动说明不可签到。服务端同步拒绝
    未发布活动的核验和手动签到，避免绕过 Vue 边界。Vue store 现在将加载、报名、取消、名单和
    手动签到的真实 API 失败显示在页面。受控 MySQL 验收活动覆盖学生报名、满额候补、取消递补、
    凭证轮换、失效凭证、非发起人越权、凭证签到、手动签到、重复签到、已签到后取消/领码，以及
    pending 活动拒绝；浏览器确认学生、组织者名单和错误提示均正确。完整 Maven 157 项、Vue
    46 项、生产构建和旧版回归检查通过；验收期间产生的正常活动、报名和事件历史按约定保留。

## 下一项工作

阶段二和阶段三点赞、好友申请、评论通知、实时推送和通知操作切片，以及 Vue 现场签到凭证
切片已经完成。
`GET /api/social-notifications/{notificationId}/friend-request-target` 从 bearer
token 确认当前收件人，并验证 `social.friend.requested`、通知发送者、申请目标
与 `pending` 状态；前端只用返回的申请 ID 定位已有待处理申请卡片，复用同意、
拒绝流程。真实仪表盘指标已不含展示性固定数字，签名 JWT、过期、服务端注销和
Spring Security 安全链、GitHub Actions 验证、Docker Compose 演示与 Testcontainers
MySQL 集成测试以及 Actuator/Micrometer 可观察性已经落地。CI
在临时 MySQL 8.4 服务上运行完整测试，并在 Docker runner 构建和启动 Compose
演示；本机 MySQL 历史数据不受影响。Vue 已完成认证、应用壳、联系人与聊天、动态、活动、
通知和管理员切片，并在用户确认后成为默认入口；旧静态前端继续作为可演示回退与回归基线。
审核辅助首个本地规则切片已经完成；任何未来外部模型接入仍需单独授权和评估，并继续保持
人工最终审核、真实 API 错误不回退 Mock、跨表写入事务和可回滚 MyBatis 集成测试。

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
