# CampusLink 新对话交接报告

这份报告用于在新对话中快速接手 CampusLink demo。新的 agent 应先阅读
`AGENTS.md` 和本文件，再根据“下一步建议”继续推进。当前项目已经从单文件
demo 演进到“静态前端 + Spring Boot API + MySQL 本地库 + IDEA 运行配置”的
可运行状态。

最后更新：2026 年 7 月 1 日。

补充更新：2026 年 7 月 1 日继续推进了后端 JUnit 测试和第一段 MySQL
持久化迁移。用户、验证码、好友申请、好友关系和在线状态已经改为通过 JDBC
Repository 访问 MySQL；聊天、动态、评论、审核和审计仍保留在内存 demo
Repository，作为下一段迁移对象。

第二次补充更新：继续完成了 Controller 层 MockMvc 测试，并把聊天消息和聊天
附件 metadata 迁移到 MySQL。当前聊天列表、发送消息、附件 metadata 返回和
撤回消息都已经通过真实 API 验证。

第三次补充更新：修复了“周同学已经是林一好友，但仍显示 pending 好友申请”
的数据一致性问题。`data.sql` 现在会在两人已经是好友时清理对应好友申请，不
再每次启动强制恢复 pending。动态和评论也已经迁移到 MySQL，发布动态、发布
评论、点赞、读取动态列表和读取评论都走 JDBC Repository。

第四次补充更新：审核队列和审计记录也已经迁移到 MySQL。后台审核列表、审核
通过/拒绝、后台报表和审计记录接口都已经通过真实 API 验证。当前后端仍使用
`JdbcClient` Repository；后续可以单独把 Repository 平滑迁移到 MyBatis 或
MyBatis-Plus。

第五次补充更新：补齐了 users 和 friends Controller 层 MockMvc 测试。当前
后端测试覆盖 auth、users、friends、chat、feed 和 admin API 边界，Maven
测试总数为 32 个。

第六次补充更新：引入 MyBatis starter，并新增 `mapper` 包。用户、验证码、
好友关系和好友申请已经从 `JdbcClient` Repository 迁移为 MyBatis-backed
Repository；Service 和 Controller 接口不变。真实 API 已验证用户搜索、好友
列表、好友申请列表、验证码生成和登录均正常。

第七次补充更新：继续迁移聊天持久化。聊天消息和聊天附件 metadata 已经从
`JdbcClient` Repository 迁移为 MyBatis-backed Repository；Service、
Controller 和前端接口不变。真实 API 已验证健康检查、发送带附件聊天消息、
撤回消息和再次读取持久化状态均正常。

第八次补充更新：完成当前 demo 的全量 MyBatis 迁移。动态、评论、审核队列、
审计记录和数据库健康检查已经从直接 JDBC 访问迁移为 MyBatis-backed Mapper
和 Repository；旧 `Jdbc*Repository` 和早期内存 `DemoRepository` 已移除。
后端源码中不再直接使用 `JdbcClient` 或 `JdbcTemplate`。真实 API 已验证动态
发布、点赞、评论、审核通过、报表、审计记录和健康检查均正常。

## 接手入口

新对话开始后，建议先确认项目守则和当前运行状态。项目根目录是
`/Users/linus_k/Documents/test`，交接目标是继续把 CampusLink 做成一个
模块化、可维护的校园社交平台 demo。

接手时按这个顺序读文件：

1. 读 `AGENTS.md`，确认模块化和分层规则。
2. 读 `README.md`，确认当前运行方式和 API 清单。
3. 读本文件，确认最近修好的 IDEA、JDK、MySQL、运行脚本和浏览器打开方式。
4. 必要时再读 `docs/report/current-progress-report.md` 和
   `docs/report/new-dialogue-handoff-2026-06-29.md` 了解历史进度。

## 当前项目状态

CampusLink 当前可以在浏览器里打开前端，也可以通过 IDEA 运行后端 API。
前端没有构建系统，仍是 dependency-free 静态页面；后端是 Spring Boot
分层项目，已经接入本地 MySQL。

关键文件如下：

- `index.html`：登录页、工作区、聊天、动态、后台面板。
- `styles.css`：当前桌面式 UI 样式。
- `app.js`：前端 mock 数据、API fallback、交互逻辑。
- `backend/`：Spring Boot 后端。
- `backend/src/main/java/com/campuslink/controller/`：Controller 层。
- `backend/src/main/java/com/campuslink/service/`：Service 层。
- `backend/src/main/java/com/campuslink/mapper/`：MyBatis Mapper 层。
- `backend/src/main/java/com/campuslink/repository/`：Repository 层。
- `backend/src/main/java/com/campuslink/entity/`：实体层。
- `backend/src/main/java/com/campuslink/dto/`：DTO 层。
- `backend/src/main/java/com/campuslink/config/`：配置、拦截器、异常处理。
- `backend/src/main/resources/application.yml`：MySQL 连接和 SQL 初始化配置。
- `backend/src/main/resources/schema.sql`：建表脚本。
- `backend/src/main/resources/data.sql`：演示数据初始化脚本。
- `backend/src/test/java/com/campuslink/service/`：后端服务层 JUnit 测试。
- `backend/src/test/java/com/campuslink/controller/`：后端 Controller 层
  MockMvc 测试。
- `script/run_backend_idea.sh`：IDEA 后端启动脚本。
- `script/run_frontend_demo.sh`：前端静态服务启动脚本。
- `script/run_frontend_check.sh`：前端 smoke 检查脚本。
- `tests/frontend-smoke.test.js`：dependency-free smoke test。

当前仓库文件大多还是 Git 未跟踪状态。不要只看 `git diff` 判断项目内容，
接手时要直接读取文件。

## 当前运行方式

后端和前端要分开启动。后端负责 API，前端负责浏览器页面，两者都启动后，
浏览器页面会优先使用 Java API。

在 IDEA 里启动后端：

1. 打开项目 `/Users/linus_k/Documents/test`。
2. 顶部运行配置选择 **CampusLink API**。
3. 点击绿色运行按钮。
4. 看到 `Tomcat started on port 8080` 表示后端启动成功。

在 IDEA 里启动前端：

1. 顶部运行配置选择 **CampusLink Frontend Demo**。
2. 点击绿色运行按钮。
3. 保持这个运行窗口不要关闭。
4. 在浏览器地址栏输入 `http://127.0.0.1:5178/?v=20260629-report-range`。

也可以用终端启动前端：

```bash
cd /Users/linus_k/Documents/test
./script/run_frontend_demo.sh
```

## 当前浏览器入口

当前前端页面地址是本地静态服务地址。必须先启动前端服务，直接打开
`index.html` 或只点链接都可能不工作。

浏览器地址栏输入：

```text
http://127.0.0.1:5178/?v=20260629-report-range
```

后端健康检查地址是：

```text
http://127.0.0.1:8080/api/database/health
```

最近一次健康检查返回：

```json
{"status":"UP","database":"campuslink","demoUsers":4}
```

## IDEA 和 JDK 状态

IDEA 里的 Java 标红问题已经修好。之前 `java`、`List`、`String` 和 `Long`
无法解析，是因为 Project SDK 配置不正确。

当前状态如下：

- IDEA 项目 SDK 名称是 `ms-21`。
- `ms-21` 是本机已安装的 Microsoft OpenJDK 21.0.11。
- `.idea/misc.xml` 指向 `project-jdk-name="ms-21"`。
- 编译语言级别是 `JDK_21`。
- Maven 测试能通过。
- IDEA Problems 面板已经不再显示 Java 标准库解析错误。

Spring Boot 是免费开源的，不需要付费。IntelliJ IDEA 只是开发工具，当前项目
也可以用 IDEA Community、VS Code 或命令行运行。

## MySQL 状态

MySQL 已经安装并作为本地服务运行。后端启动时会连接本地 `campuslink`
数据库，并自动执行 `schema.sql` 和 `data.sql`。

当前本地数据库配置如下：

- 数据库：`campuslink`
- 用户名：`campuslink`
- 密码：`campuslink123`
- 端口：`127.0.0.1:3306`
- JDBC URL：
  `jdbc:mysql://127.0.0.1:3306/campuslink?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false`

最近已确认：

- MySQL 服务处于 started 状态。
- `127.0.0.1:3306` 正在监听。
- `campuslink` 用户可以连接数据库。
- 后端健康接口返回 `status: UP`。

## 最近修复的问题

最近一轮主要修复的是“IDEA 能打开、代码不标红、运行按钮能启动、浏览器能
打开页面”这条本地开发链路。

已经完成的修复包括：

- 修好 IDEA Project SDK，使用 `ms-21` OpenJDK 21。
- 修好 IDEA 中 Java 标准库无法解析的问题。
- 修好 `tests/frontend-smoke.test.js` 中 IDEA 对 Node 脚本的静态检查提示。
- 确认后端 `mvn test` 可以通过，当前后端测试覆盖服务层和主要 Controller
  边界。
- 确认前端 `script/run_frontend_check.sh` 可以通过。
- 配置 **CampusLink API** 运行项，执行 `script/run_backend_idea.sh`。
- 配置 **CampusLink Frontend Demo** 运行项，执行 `script/run_frontend_demo.sh`。
- 修复重复点击后端运行导致的 `Port 8080 was already in use`。
- `run_backend_idea.sh` 现在会先停止旧的 CampusLink 后端进程，再启动新的。
- 启动前端 `5178` 静态服务，并验证浏览器页面返回 `HTTP/1.0 200 OK`。

## 当前已完成能力

当前 demo 已经可以验证校园社交平台的核心流程。前端会优先请求
`http://127.0.0.1:8080/api`，如果 API 不可用，会回退到 mock 数据。

当前可用能力包括：

- 演示手机号验证码登录。
- 快速进入 demo 工作区。
- 切换林一、陈老师、周同学、教务管理员等演示账号。
- 搜索用户。
- 发送好友申请。
- 接收方同意或拒绝好友申请。
- 查看联系人列表。
- 从联系人进入聊天。
- 发送聊天消息。
- 附带文件 metadata 发送聊天附件。
- 撤回当前用户最近一条消息。
- 切换在线和隐身状态。
- 发布校园动态。
- 点赞动态。
- 查看和新增动态评论。
- 后台内容审核通过或拒绝待审内容。
- 后台报表按 **今日**、**本周**、**全部** 筛选。
- 报表卡支持预览、下载 CSV 和打印预览。
- 后台操作会产生审计记录。
- API 请求会在 IDEA 运行窗口输出日志。

## 后端 API 状态

后端 API 已经按 Controller、Service、Repository、Entity、DTO 和 Config
分层。Controller 只处理 HTTP，Service 处理业务逻辑，Repository 负责数据
访问。

当前 API 路径包括：

- `POST /api/auth/code`
- `POST /api/auth/login`
- `GET /api/users`
- `GET /api/friends`
- `POST /api/friends/requests`
- `GET /api/friends/requests`
- `POST /api/friends/requests/{requestId}/accept`
- `POST /api/friends/requests/{requestId}/reject`
- `GET /api/conversations/{peerId}/messages`
- `POST /api/conversations/{peerId}/messages`
- `POST /api/conversations/{peerId}/messages/{messageId}/withdraw`
- `POST /api/presence`
- `GET /api/feed`
- `POST /api/feed`
- `POST /api/feed/{postId}/likes`
- `GET /api/feed/{postId}/comments`
- `POST /api/feed/{postId}/comments`
- `GET /api/admin/metrics`
- `GET /api/admin/moderation`
- `POST /api/admin/moderation/{itemId}/{decision}`
- `GET /api/admin/report?range=today`
- `GET /api/admin/audit-events`
- `GET /api/database/health`

当前后端已经能连接 MySQL。用户、验证码、好友申请、好友关系、在线状态、
聊天消息、聊天附件 metadata、动态、评论、审核队列和审计已经迁移到 MySQL。
用户、验证码、好友关系、好友申请、聊天消息、聊天附件 metadata、动态、
评论、审核、审计和数据库健康检查都已经使用 MyBatis-backed Mapper 和
Repository。下一步可以继续做真实鉴权、WebSocket 聊天、前端模块拆分或
面向 MySQL 的集成测试等产品和质量任务。

## 最近验证结果

以下检查已在 2026 年 7 月 1 日执行。结果可以作为新对话接手时的基线。

前端检查通过：

```bash
cd /Users/linus_k/Documents/test
./script/run_frontend_check.sh
```

输出：

```text
Frontend smoke test passed.
```

后端 Maven 测试通过：

```bash
cd /Users/linus_k/Documents/test/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
/Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn test
```

输出结果包含：

```text
BUILD SUCCESS
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
```

本轮重启后端后，真实 API 冒烟通过：

```text
GET /api/database/health -> {"status":"UP","database":"campuslink","demoUsers":4}
GET /api/users?keyword=老师&currentUserId=u-1001 -> 陈老师
GET /api/friends?currentUserId=u-1001 -> 陈老师、教务管理员、周同学
GET /api/friends/requests?currentUserId=u-1001 -> []
POST /api/auth/code + POST /api/auth/login -> u-1001 林一
POST /api/presence -> invisible
GET /api/conversations/u-2001/messages -> 种子聊天和附件 metadata
POST /api/conversations/u-2001/messages -> MySQL 持久化聊天消息
POST /api/conversations/u-2001/messages/{messageId}/withdraw -> deleted=true
GET /api/conversations/u-2001/messages -> MyBatis 读回 deleted=true 和附件 metadata
GET /api/friends + GET /api/friends/requests -> 周同学是好友时无 pending 申请
POST /api/feed -> MySQL 持久化动态
POST /api/feed/{postId}/comments -> MySQL 持久化评论
GET /api/admin/moderation -> MySQL 审核队列
POST /api/admin/moderation/{itemId}/approve -> MySQL 审核状态更新
GET /api/admin/audit-events -> MySQL 审计记录
GET /api/admin/report?range=today -> MySQL 报表数据
GET /api/users + GET /api/friends + POST /api/auth/code -> MyBatis-backed Repository
GET/POST /api/conversations/u-2001/messages -> MyBatis-backed Repository
POST /api/feed + POST /api/feed/{postId}/comments -> MyBatis-backed Repository
GET/POST /api/admin/moderation -> MyBatis-backed Repository
GET /api/admin/audit-events + GET /api/admin/report?range=today -> MyBatis-backed Repository
```

前端页面可访问：

```bash
curl -I 'http://127.0.0.1:5178/?v=20260629-report-range'
```

输出结果包含：

```text
HTTP/1.0 200 OK
```

后端健康接口可访问：

```bash
curl -sS http://127.0.0.1:8080/api/database/health
```

输出：

```json
{"status":"UP","database":"campuslink","demoUsers":4}
```

## 已知限制

这些限制是当前项目状态，不一定是 bug。新对话继续开发时要优先按模块化
方式解决，不要继续把所有逻辑追加到单个大文件。

当前限制包括：

- `app.js` 已经偏大，后续新增前端功能前应优先拆分到 `frontend/js/`。
- 前端没有构建系统，当前 smoke test 仍保持 dependency-free。
- 后端已经有服务层 JUnit 测试和主要 Controller 层 MockMvc 测试。
- 后端业务 Repository 已经覆盖当前 demo 的 MySQL 持久化链路，并统一使用
  MyBatis-backed 实现。
- 登录 token 仍是 demo token，后端尚未做真实鉴权。
- 验证码仍用于本地测试，没有接入真实短信。
- 聊天附件只保存 metadata，没有上传或持久化文件内容。
- 聊天还没有 WebSocket 实时通信。
- 后台审核规则仍是轻量 demo 逻辑。
- 报表可以预览、CSV 下载和打印预览，但没有服务端报表文件存档。

## 下一步建议

建议下一位 agent 从“后端测试和数据库持久化”开始。现在本地运行链路已经
打通，继续做功能前应先把后端质量基线补起来。

推荐下一步按这个顺序执行：

1. 再次运行 `./script/run_frontend_check.sh` 和后端 Maven 测试。
2. 如果继续做业务功能，优先补真实鉴权、短信验证码替身或 WebSocket 聊天。
3. 如果继续提高质量，补一组连接 MySQL 测试库的 Repository/Mapper 集成测试。
4. 如果继续做前端功能，先拆分 `app.js`，建议建立：
   `frontend/js/api/`、`frontend/js/auth/`、`frontend/js/chat/`、
   `frontend/js/contacts/`、`frontend/js/posts/`、`frontend/js/admin/` 和
   `frontend/js/utils/`。
5. 每迁移一个模块，同步更新 `README.md` 和本目录下的交接文档。

## 新对话第一句话建议

如果要让新对话快速接上，可以直接发送下面这段话。

```text
请阅读 AGENTS.md 和 docs/report/new-dialogue-handoff-2026-07-01.md，
同步 CampusLink 当前进度。现在 IDEA、JDK、MySQL、后端 8080 和前端
5178 都已经配置过。请先验证前后端能运行，然后从后端 JUnit 测试和
MySQL 持久化迁移开始继续做。
```
