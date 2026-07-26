# Docker Compose 演示

本说明使用独立容器、独立命名卷和现有健康接口启动 CampusLink。它不会连接、
重置、重种或清理本机 MySQL 历史数据。

## 启动演示

在已安装 Docker Desktop 或其他兼容 Docker Compose 运行时的机器上，从仓库根目录
执行以下命令。

```bash
docker compose up --build
```

Compose 会启动 Kafka、未发布到主机的 MySQL 8.4、Spring Boot API、Spring Cloud
Gateway 和 Vue 前端。API 只在 Kafka 与数据库健康后启动；网关只在 API 健康后启动；
前端只在网关健康后启动。浏览器打开 `http://127.0.0.1:5179`，然后使用 **快速进入**
或演示账号登录。Nginx 为 Vue 构建代理 `/api` 和 `/ws` 到网关，网关再转发到 API，
并处理 Vue 路由刷新。

网关在主机端口 `8081` 提供公开的 API 和 WebSocket 入口。它校验现有 JWT 的签名与
过期时间，并原样转发 bearer token；API 仍校验 MySQL 会话、注销状态和角色，因而不会
把服务端注销失效语义交给网关单独处理。

Compose 会为 API 启用 `eventing` profile。活动报名、候补、取消、递补和签到
在现有 MySQL 事务中额外写入 Outbox 事件；发布器将事件发送到 Kafka 的
`campuslink.activity.events.v1` topic，回执消费者以 `(consumer_name, event_id)`
去重后写入处理记录。报名、候补和递补通知由 Kafka 消费者投影，仍复用当前通知
API、未读数和 WebSocket 内容。消费者在达到三次处理尝试后会把事件发送到
`campuslink.activity.events.v1.DLT`，并在 MySQL 的 `event_dead_letters` 保留失败
原因；Outbox 发布同样在达到上限后进入持久化 `dead_letter` 状态。管理员可通过
`GET /api/admin/eventing/operations` 查看状态，再使用带 `confirm=true` 的重放接口
恢复合格事件。主机可通过 `127.0.0.1:9094` 访问 Kafka；容器间使用 `kafka:9092`。
Compose 使用 `apache/kafka:3.9.0` 的单节点 KRaft 模式。Kafka 监听组件与 Kafka Bean
配置分离，因此 `eventing` profile 不依赖 Spring 的循环注入。

如果本机开发 API 已占用 `8080`，可以在启动 Compose 时使用
`CAMPUSLINK_API_HOST_PORT=18080 docker compose up --build`。这只改变容器 API 的
宿主映射；网关与前端仍通过 Compose 内部的 `api:8080` 通信，公开网关继续使用 `8081`。

根目录的静态前端文件仍完整保留，在 Compose 中可通过
`http://127.0.0.1:5179/legacy/` 打开，供回退演示和旧版回归检查使用。

## 确认健康状态

服务启动后，在另一终端查询公开的数据库健康接口。

```bash
curl -fsS http://127.0.0.1:8081/api/database/health
```

成功响应包含 `"status":"UP"`、当前数据库名和演示用户数量。完整 API 路径清单
见仓库根目录 [`README.md`](../README.md)。

Compose 还公开网关状态摘要 `http://127.0.0.1:8081/actuator/health`，但不会公开
数据库细节。API 的 `/actuator/info` 和 `/actuator/metrics/**` 仍需要管理员 JWT；使用
管理员登录得到的 bearer token 查询这些诊断端点。核心业务指标包括用户、当日消息、动态
和待审核内容总数，`campuslink.http.requests` 按路由模板记录 API 耗时。

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
`/api` 和 `/ws`，由前端 Nginx 转发到 Compose 网关；因此端口 `8081` 和 `5179` 必须可用。
