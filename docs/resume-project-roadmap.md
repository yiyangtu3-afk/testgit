# CampusLink 简历主项目路线

本文记录 CampusLink 面向简历主项目的长期开发顺序。后续需求优先遵循
本路线；每个阶段在本地验证后单独提交到 GitHub，作为可回退的稳定点。

## 当前基线

Vue 已是默认演示入口，地址为 `http://127.0.0.1:5180`；聊天页为
`http://127.0.0.1:5180/workspace/contacts`。旧版静态基线继续保留在
`http://127.0.0.1:5179/?v=20260715-signed-jwt-logout-v1`，仅用于回退与
回归，根目录 `index.html`、`app.js`、`styles.css` 和 `frontend/js/` 不能
删除、移动、替换或重写。

当前功能稳定行为基线为 `98c2dad Add notification read and target actions`。
最新交付为活动签到凭证：Vue 已报名学生可以展示会轮换的签到凭证，组织者经 JWT、活动归属
和报名状态校验后手动核验签到。凭证在 MySQL
仅保存 SHA-256 摘要，签到继续写入既有事务与追加式事件。聊天图片消息仍支持 PNG、JPEG、
WebP 和 GIF（单张最多 5 MB），图片字节受 JWT 和好友关系保护，旧附件保持普通文件卡片。
详细边界见
[`new-chat-handoff-2026-07-08.md`](new-chat-handoff-2026-07-08.md) 和
[`vue-migration-handoff.md`](vue-migration-handoff.md)。

## 开发原则

每个新阶段都必须让产品行为、数据状态和文档描述保持一致。遇到真实 API
拒绝请求时，界面必须显示失败，不能把操作静默写入 Mock 数据。新功能必须
复用现有认证、审核、审计和实时通信边界，避免形成孤立的页面功能。

完成一个阶段前，必须执行与改动匹配的前端检查、后端测试和浏览器验证。
用户确认功能可用后，再提交并推送到 `main`；提交说明必须表达可感知的
功能或工程变化。

## 阶段一：可信基线

这一阶段先让现有能力经得起演示和追问，再扩展业务范围。

该阶段已完成，稳定提交为 `f06b09d Add transactional workflow safeguards`。
下一阶段的具体交接见
[`phase-two-activity-handoff.md`](phase-two-activity-handoff.md)。

1. [x] 同步 README、运行脚本和当前前端版本。
2. [x] 仅在 Java API 不可达时回退到 Mock；保留真实 API 的 `4xx` 和 `5xx`
   失败结果。
3. [x] 让动态可见范围成为真实数据库字段和查询规则，或者在实现前移除界面
   选项。
4. [x] 校验聊天双方的好友关系，拒绝通过任意 `peerId` 读取、发送或撤回
   非好友会话。
5. [x] 补齐会话分页和持久化已读状态的设计与实现。
6. [x] 统一转义用户生成内容，消除直接写入 `innerHTML` 的 XSS 风险。
7. [x] 为跨表业务流程补充服务层事务和数据库集成测试。

完成标准是：核心操作不会产生 Mock 与 MySQL 数据分裂，界面承诺都有真实
后端实现，且关键失败路径有测试覆盖。

## 阶段二：校园活动报名闭环

这一阶段是简历主功能。它以活动为中心，把已有的权限、审核、通知、审计
和实时能力串成一个完整业务流程。

1. [x] 教师或社团负责人创建活动，管理员审核后发布前后端闭环。
2. [x] 学生按时间和类别浏览活动，并报名、取消报名或进入候补队列。
3. [x] 名额释放时，以事务方式递补第一位候补者，避免超卖和重复报名。
4. [x] 使用站内通知和 WebSocket 推送审核、报名和递补结果。
5. [x] 让组织者管理名单、签到和导出；让管理员查看真实报名和签到指标。

第一项已经覆盖后端 API、权限、状态迁移、MySQL 持久化、活动创建界面、提交
状态和管理员活动待审区。live API 权限、列表和错误边界已经通过验收；本机
浏览器已验证管理员活动审核工作区的显示和待审活动操作。活动待审请求失败时，
该工作区保持可见并显示错误反馈。

活动状态使用 `draft`、`pending`、`published`、`full`、`closed` 和
`cancelled`。报名状态使用 `registered`、`waitlisted`、`checked_in` 和
`cancelled`。完成标准是同时覆盖状态流转、并发名额控制、审计记录和端到端
演示。

报名与候补的字段、状态机、权限、接口、并发事务边界和测试用例见
[`activity-registration-design.md`](activity-registration-design.md)。当前实现包含
独立报名 Repository/Mapper/Service、MySQL 当前状态与事件历史、活动行锁、
HTTP 接口和学生活动卡片操作；定向 MyBatis 回滚集成测试已覆盖报名、候补和
递补。公开活动列表与前端已支持包含边界的日期范围、精确类别和组合筛选，
筛选或清除后会恢复当前报名状态。

活动通知使用独立的 Repository、Mapper、Service 和 Controller；审核、报名、
候补与递补结果和业务变更处于同一事务，事务提交后再通过已有 `/ws/chat`
连接向收件人推送。前端通知中心展示持久化历史、未读计数和全部已读操作；
离线期间产生的递补结果会在下次登录后从 MySQL 恢复。Java API 的 `4xx` 或
`5xx` 不会回退 Mock。

组织者现在通过 **我的活动运营** 读取自己创建的持久化活动和报名名单。名单
区分待签到、已签到和候补，候补位置由服务端计算；只有活动组织者可以执行
签到。`registered` 到 `checked_in` 的状态变化、`checked_in_at` 时间和追加式
事件处于同一事务。名单 CSV 在前端从服务端名单生成。管理员通过独立活动
指标接口读取真实占位报名数和签到数，活动规则没有进入 `AdminService`。

## 阶段三：通知与社交完整性

这一阶段让用户能看见社交行为的后续结果，并让社交数据具备可靠约束。

1. [x] 新增持久化通知中心，覆盖好友申请、评论、点赞、审核和活动状态变化。
2. [x] 将 WebSocket 扩展为通知推送，并支持未读计数和已读状态。
3. [x] 将点赞改为按用户记录的可取消操作，避免重复累加。
4. [x] 为动态、评论和活动补充真实统计和筛选，不再使用展示性硬编码数据。

点赞、好友申请和评论子项已经完成。`post_likes` 以动态和当前用户组成唯一键，
点赞接口返回 `likedByCurrentUser` 并支持再次点击取消。`SocialNotificationService`
和独立的 MyBatis Repository、Mapper、Controller 保存点赞、评论、好友申请和
处理结果通知；前端 **站内通知** 按时间合并活动和社交通知，并合并未读计数与
全部已读操作。非作者提交评论时，评论、审核记录、审计记录和作者通知处于同一
事务；评论保持 `pending`，仅审核通过后进入公共动态流。好友申请创建、同意和
拒绝也分别在同一事务内写入申请状态、好友关系或聊天系统消息、审计记录和相应
通知。历史 `posts.likes` 作为兼容计数继续保留，种子启动不再覆盖真实点赞总数。
社交通知复用已认证 `/ws/chat` 连接：`SocialNotificationService` 在原业务事务
内持久化后发布领域事件，由提交后监听器投递
`social.notification.created`。前端按通知 ID 去重、立即更新统一未读数；离线历史
仍在下次登录时从 MySQL 加载。

通知中心还支持单条已读。活动通知会打开并高亮对应活动；动态点赞和评论通知通过
收件人受限的后端目标解析回到对应动态并高亮。评论通知保留评论 ID，因此解析器
在 MySQL 查询其所属动态，不把历史通知错误当作动态 ID。

管理员仪表盘现在按 MySQL 当前状态显示注册用户、当天消息、全部动态和待审内容；
活动报名与签到继续由独立活动指标提供。Mock API 按同样的数据源计算这些字段，
不再返回固定展示数字。活动列表已有日期和类别组合筛选，动态点赞、评论和审核
状态也来自当前持久化数据。

## 阶段四：安全、测试与交付

这一阶段把项目整理为可复现、可观察、可持续迭代的工程作品。

1. [x] 使用 Spring Security 和签名 JWT，补齐令牌过期、MySQL 会话校验、
   服务端注销和角色授权。无状态安全链统一保护 `/api/**`，登录与健康检查除外，
   管理员路径要求 `ROLE_ADMIN`。
2. [x] 使用 Testcontainers MySQL 编写 MyBatis、事务和权限集成测试。
3. [x] 添加 GitHub Actions，在推送、拉取请求和手动触发时使用临时 MySQL 8.4
   服务运行前端检查和带显式 Byte Buddy agent 的完整后端测试。
4. [x] 提供 Docker Compose 一键启动、健康检查、API 文档和浏览器演示说明。
   Compose 使用独立 MySQL 命名卷，GitHub Actions 在 Docker runner 构建、启动并
   请求健康接口，不会影响开发者本机 MySQL 历史数据。
5. [x] 使用 Actuator 和 Micrometer 展示健康状态、核心指标和请求诊断信息。

## 阶段五：Vue 前端渐进迁移

这一阶段把已验证的原生 ES Modules 前端迁移为可维护的 Vue 3 前端，但不改变任何
后端 API、MySQL 历史数据或已完成业务规则。迁移从独立 `frontend-vue/` 目录开始，
旧版继续作为可演示基线；只有每个领域完成等价验证后才能考虑切换入口。

1. [x] 初始化 Vue 3、Vite、Vue Router 和 Pinia，并建立 HTTP、Mock 与认证边界。
2. [x] 迁移应用壳、导航和统一状态提示，不加载未迁移领域数据。
3. [x] 迁移联系人、好友申请和聊天，保留分页、未读、附件和 WebSocket 语义。
4. [x] 迁移动态、个人动态、点赞、评论和审核状态反馈。
5. [x] 迁移管理员模块；活动和通知模块已经迁移。
6. [x] 保持 Java API 不可达才回退 Mock、JWT 身份边界、实时通知和 XSS 转义语义。
7. [x] 新旧版本逐项验收后，经明确确认切换默认入口；Vue 成为本地与 Compose 默认演示，
   旧静态前端保留为 `/legacy/` 回退入口和独立旧版启动脚本。

详细阶段边界见 [`vue-migration-handoff.md`](vue-migration-handoff.md)。

## 后续选择

Vue 前端迁移完成后，审核辅助首个切片已经落地。它使用本地可解释规则提示风险等级、
命中信号和建议意见；管理员必须主动请求建议，并继续人工填写理由、同意或拒绝，系统
不会自动改变内容状态或写入审计记录。任何外部模型接入必须另行授权，并继续保持既有
人工审核闭环。管理员审核工作台支持按内容类型、提交人和状态组合筛选；拒绝内容必须
填写审核意见，并保存审核人、时间和意见至审核历史与审计记录。

用户已授权仅在 Vue 默认入口实现受认证图片消息。不要把这一实现回迁到旧版静态入口；
也不要恢复旧的图片消息试做版本、后续登录页试做版本或动态页强制刷新。

## 阶段六：现场签到凭证

本阶段在既有报名与组织者签到闭环上增加了可展示、可轮换的签到凭证。学生只有在
`registered` 状态下才能读取自己的当前凭证；服务端只保存凭证的 SHA-256 摘要，重新展示会
轮换并立即使旧码失效。组织者提交凭证时，后端同时校验 JWT 组织者身份、活动归属、凭证对应
的报名记录和 `registered` 状态，再复用既有事务将其更新为 `checked_in` 并追加事件历史。

Vue 活动卡显示紧凑的 **签到凭证** 通行证，组织者的 **我的活动运营** 提供手动核验输入。
组织者始终能在标题区看到 **现场签到** 入口；若当前账号没有自己创建的活动，运营区会明确提示
切换到活动创建者账号，而不是隐藏入口。旧版静态入口没有改动；Mock API 仅在 Java API 不可达时
提供相同的凭证和核验结构。该切片还修正了 Vue 名单手动签到使用真实 `registrationId` 的字段边界。
组织者和管理员不再请求学生专属的当前报名接口，因而不会在权限拒绝后跳过“我的活动运营”加载；
公开活动卡也直接显示活动发起人。报名入口也只对尚未报名的学生显示；其他角色看到
“仅学生可报名”提示，已签到的学生显示“已签到”。
组织者的运营区只为已发布或满额活动显示名单和核验控件；待审核或未通过活动会说明当前
不可签到。前端对加载、报名、取消、名单和手动签到的 API 失败统一显示服务端真实错误。
后端也拒绝未发布活动的核验和手动签到，不能仅靠前端隐藏控件绕过边界。

新增接口为：

- `POST /api/activities/{activityId}/registrations/current/check-in-credential`
- `POST /api/activities/{activityId}/registrations/check-in-credential`

定向 Service/MockMvc 测试、MySQL `@Transactional`/`@Rollback` 集成测试、Vue 单元测试
和生产构建均已覆盖；后续新增扫码能力必须是浏览器兼容的渐进增强，不能绕过上述服务端核验。

## 阶段七：事件驱动与微服务演进

本阶段按 `plans/spring-cloud-kafka-microservices-upgrade.md` 渐进引入 Spring
Cloud、Kafka 与服务边界，先保持模块化单体的 API 与数据行为，再独立部署领域服务。
当前八个切片均已完成：Spring Cloud 2025.0.3 BOM、Kafka KRaft Compose 配置和事务
Outbox；活动报名、候补、递补通知的 Kafka 幂等投影；有界重试、死信与管理员重放；独立
Spring Cloud Gateway；以及独立 `notification-service`。普通本地启动不启用
`eventing`，因此仍保留同步通知和 `pending` Outbox 记录；启用 `eventing` 后，活动事务
只保存状态与事件，由 `campuslink-activity-notification-v1` 消费组在独立事务内写入通知，
并以 `(consumer_name, event_id)` 防止重复投递产生重复通知。Outbox 和消费者都采用
三次上限重试；失败后分别保留 `dead_letter` 状态或写入 `event_dead_letters`，管理员在
Vue 控制台确认后才能重放，且每次操作写入审计。Vue API、未读数、WebSocket、JWT 边界
与旧版静态回归均不变。网关运行在 `8081`，Vue Vite 和 Compose Nginx 的 `/api`、`/ws`
都先到达网关；网关验证 JWT 签名和过期时间，再原样转发 token 到仍在 `8080` 的 MVC API。
活动通知路径由网关路由到 `notification-service:8082`，该服务再次验证 JWT 和 MySQL
会话，并只消费事件中携带的活动标题、候补位次和收件人，不读取活动或报名表。API 保留
认证 WebSocket 桥接，以投递事件发送原有 `activity.notification.created`。下游继续验证
MySQL 会话、注销状态和角色，因此不会因为网关迁移削弱既有权限边界。
Compose 的 KRaft broker 已切换到可用的 `apache/kafka:3.9.0` 官方镜像；同时拆分 Kafka
监听组件与配置类，保证 `eventing` profile 可以独立启动。

活动领域已进一步完成报名、候补、递补、签到和 Outbox 的独立服务迁移。第八切片引入
Spring Cloud Alibaba 2025.0.0.0 与 Nacos 3.0.3：核心 API、活动、通知和 Gateway 使用
`CAMPUSLINK_DEV` 集中配置并注册发现；Gateway 使用 `lb://` 路由和 Resilience4j 有界熔断。
Compose 同时提供 Jaeger、Prometheus 和 Grafana，Micrometer 输出 HTTP、Kafka、Outbox、重试和
死信指标；核心 API 的 Prometheus 端点仅在 Compose 内部 `8085` 管理端口匿名暴露，同端口本地
启动时仍要求管理员 JWT；Gateway、活动和通知服务的管理端口也分别隔离为 `8084`、`8086`、
`8087`，默认仅监听本机回环。HTTP 直方图用于 Grafana 的 P95 延迟面板。完整 Maven 168 项、
Vue 48 项、三个独立服务测试、Vue 构建和旧版回归检查均已通过。

## 下一步

阶段一至阶段四已经完成；CI 在临时 MySQL 8.4 服务上运行完整测试，并在 Docker
runner 验证 Compose 健康接口，本机 MySQL 历史数据不受影响。阶段五已完成认证、
应用壳、联系人与聊天、动态、活动、通知和管理员七个 Vue 切片。管理员模块复用已有
指标、审核、审计和报表 API，非管理员不会请求后台数据；HTTP `4xx`、`5xx` 不回退
Mock。全部等价验收完成并已获得切换授权：Vue 是默认本地与 Compose 演示入口，旧静态版
保留为 `/legacy/` 回退入口和旧版回归基线。

截至 2026 年 7 月 26 日，Nacos 与可观测性阶段的后端完整 Maven 测试（显式 Byte Buddy
agent）168 项通过；Vue 测试 48 项（16 个测试文件）、生产构建和旧版前端回归检查通过。
独立活动、通知和 Gateway 测试分别通过 4、3、6 项；隔离 Compose 已确认 Nacos 四服务健康注册、
集中配置、Gateway 路由、Jaeger 追踪和五个 Prometheus 目标。Docker 命名卷与本机 MySQL 的
正常图片消息、活动、报名、签到和审计历史均已保留，未清理数据。

新对话应先阅读三份 `AGENTS.md`，再阅读本文件、Vue 与聊天交接、活动阶段交接，以及
两份管理员审核文档。开始实际功能前先执行 `git status --short --branch`。签到凭证阶段完成
后，当前没有已授权的下一项功能；应先向用户确认新的产品优先级，再开展下一阶段。

## 阶段八：Redis 活动目录缓存

用户在 2026 年 7 月 27 日授权此独立工程切片。Redis 仅缓存活动服务的公开目录查询，不能替代
MySQL 对活动名额、报名、候补、签到或认证会话的事实来源。缓存键按筛选条件和目录版本生成，使用
短 TTL 与抖动；活动审核、报名与取消只会在对应 MySQL 事务提交后使版本失效。Redis 不可用时服务
继续查询 MySQL，并通过 Micrometer 记录命中、未命中、异常与失效指标，绝不将真实 API 失败降级为
Mock。Compose Redis 仅在内部网络运行且不使用命名卷；本机原生开发默认关闭此缓存。活动服务
测试同时覆盖模拟 Redis 的命中与降级，以及临时 `redis:7.4.2-alpine` 容器的真实缓存读写。

## 阶段九：Redis 核销凭证限流

在缓存切片完成后，活动服务为已经鉴权的核销凭证请求增加 Redis Lua 原子计数器。学生领取或轮换
签到凭证按“用户 + 活动”每分钟限制 5 次，组织者校验凭证按同一范围每分钟限制 10 次；超限会
保留真实 HTTP 429 和中文错误信息。限流键不包含明文凭证或 JWT，Grafana 和 Micrometer 分别按
`credential_issue`、`credential_verification` 记录放行、拒绝和 Redis 异常。Redis 不可用时采用
显式的 fail-open 可用性策略并记录错误，以免可选 Redis 阻断现场签到；MySQL 中的凭证摘要、行锁
与签到事务仍是最终事实来源。本机原生运行默认关闭，Compose/Nacos 启用并提供 1 分钟窗口和上述
阈值。活动服务测试覆盖模拟 Redis 的 429、故障放行以及 `redis:7.4.2-alpine` Testcontainers
的真实原子计数。

## 阶段十：Gateway Redis 入口限流

Gateway 使用 Spring Cloud Gateway 的 Redis 令牌桶在 HTTP 请求进入微服务前实施分布式限流。
`/api/auth/**` 对匿名客户端每分钟最多 5 次，其余 HTTP API 按“已验签用户或匿名客户端 + 路由”
每分钟最多 30 次。Gateway 把已验证 JWT subject 或远端地址做 HMAC-SHA-256 指纹后才作为 Redis 键，
不会新增可被下游信任的身份头，也不对 WebSocket 或内部管理端口限流。超限返回真实 HTTP 429；
入口 Redis 异常保留真实 Gateway 错误，不会伪造成功或回退 Mock。Nacos 集中配置路由限流参数，
Compose 让 Gateway 等待内部 Redis 健康、只绑定回环并应用 Nginx 转发的客户端地址，Grafana 按
Gateway 路由展示 429 和 5xx。Gateway 测试
覆盖了主体与匿名客户端的无明文键解析，并继续验证 JWT 鉴权边界。

## 阶段十一：Redis 目录缓存击穿防护

活动公开目录在缓存键失效后使用短期 Redis 租约和双重读取，避免多实例并发请求同时回源 MySQL。
第一个请求以 5 秒租约成为加载者；其他请求最多等待 4 次、每次 25 毫秒以读取新缓存，超时后安全
回源 MySQL，从而不会让 Redis 故障或长等待影响可用性。释放操作使用“仅持有者可删”的 Lua 脚本，
防止过期租约误删其他实例的新锁。该机制严格只用于无用户状态的公开目录缓存，不替代 MySQL 对
名额、报名、签到或认证的事实与事务。Micrometer/Grafana 记录租约获取、等待命中和超时；真实
`redis:7.4.2-alpine` 并发测试确认 6 个并发读取只执行一次数据库加载。

## 阶段十二：Redis 报名幂等响应重放

活动报名 POST 支持可选 `Idempotency-Key`。服务以“用户、活动、客户端键”的 HMAC 指纹作为 Redis
键，先写入 30 秒处理中标记；业务 MySQL 事务成功后保存 24 小时 `RegistrationView` 响应。同一键的
重试直接返回原始 `201` 结果，处理中重复请求返回真实 `409`，不会重复执行报名 Outbox 事务。Redis
不可用时显式回退到既有 MySQL 行锁和冲突语义，保证数据正确性而不伪造响应。Micrometer/Grafana
记录声明、重放、处理中和异常；真实 `redis:7.4.2-alpine` 测试确认重放不会二次执行报名动作。

## 阶段十三：Redis 基础设施可观测性

Compose 在内部网络运行固定版本的 Redis Exporter，Prometheus 抓取其 `:9121` 指标而不映射新的
宿主端口。Grafana 同时展示 Redis 内存、连接客户端、键空间命中率与命令吞吐，关联已有的缓存、
限流和幂等业务指标。该切片不增加 Redis 数据卷、不暴露 Redis 或 Exporter，也不改变 MySQL
事实来源。

## 阶段十四：Redis Prometheus 告警规则

Prometheus 使用版本化规则文件记录 5 分钟内的应用 Redis 错误，并在 Redis Exporter 连续 2 分钟不可达
或应用错误持续 5 分钟时产生 warning 告警。该 Compose 演示未引入 Alertmanager，告警只在 Prometheus
和 Grafana 视图中评估，不会对外发送消息，也不会改变 Redis、MySQL 或 Kafka 数据。

## July 25, 2026 handoff update

The current repository and local-runtime snapshot is recorded in
[`new-chat-handoff-2026-07-25.md`](new-chat-handoff-2026-07-25.md). At that
handoff, the `main` worktree was clean at `df19538`; MySQL was running, but the
Vue and Java services were not left running. The preceding Java startup had
successfully connected to MySQL and returned an `UP` database health response.
If a sandbox blocks Java loopback access to MySQL, use a host-capable runtime
instead of changing, resetting, or reseeding the local database.

2026 年 7 月 21 日补充了可重复的 live 等价验收。启动 Vue、旧版静态服务和 Java API 后，
运行 `node script/run_live_equivalence_check.mjs` 可对比 Vue 代理与旧版直连 API。该检查
覆盖学生、组织者和管理员的核心只读路径，以及无效通知目标和无效聊天撤回的安全拒绝；
它会注销临时演示会话，不会写入业务数据或更改 MySQL 历史。
