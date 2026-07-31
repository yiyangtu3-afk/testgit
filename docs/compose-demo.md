# Docker Compose 演示

本说明使用独立容器、独立命名卷和现有健康接口启动 CampusLink。它不会连接、
重置、重种或清理本机 MySQL 历史数据。

## 启动演示

在已安装 Docker Desktop 或其他兼容 Docker Compose 运行时的机器上，从仓库根目录
执行以下命令。

```bash
docker compose up --build
```

Compose 会启动 Kafka、未发布到主机的 MySQL 8.4、内部 Redis、Nacos 3、Spring Boot API、独立
`activity-service` 与 `notification-service`、Spring Cloud Gateway、Vue 前端，以及
Jaeger、Prometheus、Grafana。Nacos 首次使用嵌入式存储初始化可能需要约三分钟；其
readiness 就绪后，`nacos-config` 一次性发布版本化的 `CAMPUSLINK_DEV` 配置，再启动业务
服务。浏览器打开 `http://127.0.0.1:5179`，然后使用 **快速进入** 或演示账号登录。Nginx
为 Vue 构建代理 `/api` 和 `/ws` 到网关，网关再按路径转发，并处理 Vue 路由刷新。
所有需要映射到宿主机的 Compose 端口都绑定 `127.0.0.1`，因此开发模式关闭认证的 Nacos
控制台、Kafka 和观测工具不会对局域网开放。

Redis Exporter 同样只在 Compose 内部网络运行，Prometheus 通过 `redis-exporter:9121` 抓取 Redis
INFO 指标，不映射新的宿主端口。Grafana 会显示 Redis 内存、连接客户端、键空间命中率和命令吞吐，
用于把应用层缓存、限流和幂等指标与 Redis 运行状态关联起来。

Prometheus 还会评估内部 Redis 规则：Exporter 连续 2 分钟不可达、Exporter 可抓取但 Redis
连续 2 分钟不可达，以及缓存、签到限流或报名幂等错误在每个 1 分钟窗口连续出现 5 分钟。
Compose 没有配置 Alertmanager，因此规则仅显示在 Prometheus 或 Grafana 告警视图，
不会向外部系统发送消息。

GitHub Actions 的 Compose 演示会请求 Prometheus Rules API，确认三条 Redis
告警已被实际加载；它还要求六个 Prometheus 抓取目标均为健康状态。

Redis 只在 Compose 内部网络提供给 `activity-service`，不会映射宿主机端口，也不使用命名卷。
活动公开目录会使用带短 TTL 抖动的版本化缓存键；活动审核、报名或取消只有在 MySQL 事务提交
成功后才递增版本，因而不会在回滚时提前失效。Redis 读取、解析或写入失败时会记录 Micrometer
指标并回源 MySQL，不会降级到 Mock。缓存不会保存活动名额、报名、候补、签到、通知或认证事实；
这些状态继续由 MySQL 事务、行锁和既有 Outbox 流程负责。

公开目录缓存失效后，活动服务会用五秒 Redis 租约与双重缓存读取防止多个实例同时加载 MySQL。
未获得租约的请求最多等待 4 次、每次 25 毫秒；若缓存仍未写入则安全回源 MySQL。租约只能由持有者
通过条件 Lua 释放，并且只用于公开目录缓存，绝不参与报名、名额、签到或其他业务事务。Grafana 的
缓存面板会显示租约获取、等待命中和等待超时计数。

同一个内部 Redis 还对已鉴权的签到凭证操作执行 Lua 原子计数限流：学生按“用户 + 活动”领取或
轮换凭证每分钟最多 5 次，组织者按“用户 + 活动”校验凭证每分钟最多 10 次。超限返回真实 HTTP
429，不会回退 Mock。Redis 暂时不可用时，为避免可选基础设施故障阻断现场签到，服务会记录错误
指标后放行请求；凭证摘要和签到状态仍由 MySQL 事务维护。限流开关和阈值由
`campuslink-activity-service.yaml` 的 `campuslink.redis.check-in-rate-limit` 管理。Redis
键只保存“操作、用户和活动”组合的 HMAC 指纹，不保存可逆标识。

网关在主机端口 `18084` 提供公开的 API 和 WebSocket 入口。所有服务向 Nacos 注册，Gateway
从版本化集中配置读取 `lb://` 路由：活动与审核、报名、候补和签到路径转发给
`activity-service`，活动通知路径转发给 `notification-service`，其他 API 与 WebSocket
转发给核心 API。Gateway 校验现有 JWT 的签名与过期时间，并原样转发 bearer token；下游服务
仍会再次校验 MySQL 会话和角色。活动与通知路径还配置了有界熔断；下游不可用时返回明确的
`503` JSON 错误，而不会把失败隐藏为 Mock 成功。

Gateway 还使用 Redis 令牌桶在请求到达下游服务前保护 HTTP API。`/api/auth/**` 对匿名客户端
限制为每分钟 5 次；其他 HTTP API 对每个已验签用户或匿名客户端、每条路由限制为每分钟 30 次。
Gateway 只把 JWT subject 或远端地址的 HMAC-SHA-256 指纹放入 Redis 键，不转发身份信任头。超限由
Gateway 返回真实 `429`；Redis 不可用时入口层会保留真实失败，不会回退 Mock。WebSocket 和
内部 Actuator 管理端口不使用此限流。Nacos 的 `campuslink-gateway.yaml` 提供 Redis 连接和
`RequestRateLimiter` 路由策略。Compose Gateway 宿主端口仅绑定回环，并使用前端 Nginx 传递的
客户端地址，因此不同浏览器不会共享同一个匿名限流桶。Nginx 会覆盖客户端自行提交的
`X-Forwarded-For`，避免 Gateway 从可伪造的地址生成匿名限流键。

核心 API 还对认证接口执行 Redis Lua 限流。每个 HMAC 指纹手机号每分钟最多请求 3 次验证码；
连续 5 次登录失败会在 5 分钟窗口内阻止后续登录。成功登录会清除失败计数。认证 Redis 不可用时
返回真实 `503`，不会放行未受保护请求或回退 Mock。Grafana 会按 `verification_code` 和
`login_failure` 显示认证限流的放行、拒绝和 Redis 异常指标。

活动报名 `POST /api/activities/{activityId}/registrations` 可以带可选 `Idempotency-Key`。
同一用户、活动和有效键的首次成功响应会在 Redis 保留 24 小时，重试会重放相同的 `201` 报名结果；
仍在执行的重复请求返回真实 `409`。Redis 连接异常时服务保留已有 MySQL 行锁和冲突语义，不回退
Mock。Redis 中只保存该组合的 HMAC 指纹和响应，不保存原始用户 ID 或客户端键。

Compose 会为 API 启用 `eventing` profile。活动报名、候补、取消、递补和签到
在现有 MySQL 事务中额外写入 Outbox 事件；发布器将事件发送到 Kafka 的
`campuslink.activity.events.v1` topic，回执消费者以 `(consumer_name, event_id)`
去重后写入处理记录。API 在 Compose 中关闭旧的本地通知投影；独立通知服务以同一稳定
消费者组消费报名、候补和递补事件，并以 `(consumer_name, event_id)` 去重后写入活动通知。
通知服务持久化后发布投递事件，API 只作为既有认证 WebSocket 的桥接层发送
`activity.notification.created`。因此通知服务暂时不可用不会回滚活动报名事务，恢复后会从
Kafka 补齐通知。消费者在达到三次处理尝试后会把事件发送到
`campuslink.activity.events.v1.DLT`，并在 MySQL 的 `event_dead_letters` 保留失败
原因；Outbox 发布同样在达到上限后进入持久化 `dead_letter` 状态。管理员可通过
`GET /api/admin/eventing/operations` 查看状态，再使用带 `confirm=true` 的重放接口
恢复合格事件。主机可通过 `127.0.0.1:9094` 访问 Kafka；容器间使用 `kafka:9092`。
Compose 使用 `apache/kafka:3.9.0` 的单节点 KRaft 模式。Kafka 监听组件与 Kafka Bean
配置分离，因此 `eventing` profile 不依赖 Spring 的循环注入。

Compose 默认把 API 与 Gateway 分别映射到 `18080`、`18084`，以避免占用本机原生开发的
`8080`、`8081`。如有需要，可在启动时用 `CAMPUSLINK_API_HOST_PORT` 和
`CAMPUSLINK_GATEWAY_HOST_PORT` 覆盖这两个宿主端口；服务之间仍通过 Compose 网络和 Nacos
通信。

根目录的静态前端文件仍完整保留，在 Compose 中可通过
`http://127.0.0.1:5179/legacy/` 打开，供回退演示和旧版回归检查使用。

## 确认健康状态

服务启动后，在另一终端查询公开的数据库健康接口。

```bash
curl -fsS http://127.0.0.1:18084/api/database/health
```

成功响应包含 `"status":"UP"`、当前数据库名和演示用户数量。完整 API 路径清单
见仓库根目录 [`README.md`](../README.md)。

Compose 不会把任一服务的 Actuator 管理端口映射到宿主机。Gateway、活动和通知服务的
管理端口默认绑定本机回环，Compose 仅在内部网络为 Prometheus 开放它们。API 的
`/actuator/info` 和 `/actuator/metrics/**` 仍需要管理员 JWT；使用管理员登录得到的 bearer
token 查询这些诊断端点。核心业务指标包括用户、当日消息、动态和待审核内容总数，
`campuslink.http.requests` 按路由模板记录 API 耗时。活动服务还发布
`campuslink.redis.activity_catalog.cache` 的命中、未命中、异常和失效计数。
Grafana 还展示 `campuslink.redis.check_in_rate_limit` 按操作维度的放行、拒绝和 Redis 异常计数。
Gateway 面板额外按路由展示 Redis 限流 `429` 与下游 `5xx` 数量。
Grafana 还展示报名幂等键的首次声明、响应重放、处理中冲突和 Redis 异常。
Prometheus 还会显示 `redis` 目标，用于确认 Redis Exporter 抓取成功。

Nacos 状态页在 `http://127.0.0.1:8088`，Prometheus 在 `http://127.0.0.1:9090`，
Grafana 在 `http://127.0.0.1:3000`（本地演示账号 `admin` / `campuslink-dev-only`），
Jaeger 在 `http://127.0.0.1:16686`。Prometheus 抓取 Gateway、API、活动和通知的隔离
管理端口 `8084` 至 `8087`，这些端口都没有映射到宿主机；Nacos 的 Prometheus 端点也只由
Compose 网络访问。

## 停止演示

以下命令停止容器，但保留 `campuslink-mysql` 和 `campuslink-kafka` 命名卷中的
容器演示数据，下一次启动会继续使用它。

```bash
docker compose down
```

> **Warning:** 不要在未明确确认数据处理范围时删除卷。本 Compose 卷与本机 MySQL
> 历史数据相互独立，但两者都可能包含需要保留的演示记录和 Kafka 事件。

## 浏览器演示边界

此仓库提供本地浏览器演示，不会在未获授权时发布公共在线地址。Vue 默认使用同源的
`/api` 和 `/ws`，由前端 Nginx 转发到 Compose 网关；因此端口 `18084` 和 `5179` 必须可用。
