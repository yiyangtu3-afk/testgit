# Docker Compose 演示

本说明使用独立容器、独立命名卷和现有健康接口启动 CampusLink。它不会连接、
重置、重种或清理本机 MySQL 历史数据。

## 启动演示

在已安装 Docker Desktop 或其他兼容 Docker Compose 运行时的机器上，从仓库根目录
执行以下命令。

```bash
docker compose up --build
```

Compose 会启动未发布到主机的 MySQL 8.4、Spring Boot API 和 Vue 前端。API 只在
数据库健康后启动；前端只在 API 健康后启动。浏览器打开
`http://127.0.0.1:5179`，然后使用 **快速进入** 或演示账号登录。Nginx 为 Vue
构建代理 `/api` 和 `/ws` 到 API 服务，并处理 Vue 路由刷新。

根目录的静态前端文件仍完整保留，在 Compose 中可通过
`http://127.0.0.1:5179/legacy/` 打开，供回退演示和旧版回归检查使用。

## 确认健康状态

服务启动后，在另一终端查询公开的数据库健康接口。

```bash
curl -fsS http://127.0.0.1:8080/api/database/health
```

成功响应包含 `"status":"UP"`、当前数据库名和演示用户数量。完整 API 路径清单
见仓库根目录 [`README.md`](../README.md)。

Compose 还公开状态摘要 `http://127.0.0.1:8080/actuator/health`，但不会公开数据库
细节。`/actuator/info` 和 `/actuator/metrics/**` 需要管理员 JWT；使用管理员登录得到
的 bearer token 查询这些诊断端点。核心业务指标包括用户、当日消息、动态和待审核
内容总数，`campuslink.http.requests` 按路由模板记录 API 耗时。

## 停止演示

以下命令停止容器，但保留 `campuslink-mysql` 命名卷中的容器演示数据，下一次启动会
继续使用它。

```bash
docker compose down
```

> **Warning:** 不要在未明确确认数据处理范围时删除卷。本 Compose 卷与本机 MySQL
> 历史数据相互独立，但两者都可能包含需要保留的演示记录。

## 浏览器演示边界

此仓库提供本地浏览器演示，不会在未获授权时发布公共在线地址。Vue 默认使用同源的
`/api` 和 `/ws`，由前端 Nginx 转发到 Compose API；因此端口 `8080` 和 `5179` 必须可用。
