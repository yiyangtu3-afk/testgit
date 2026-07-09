# CampusLink 新对话交接报告

这份报告用于让下一次 Codex 对话快速接手 CampusLink。新的 agent 应先读
`AGENTS.md`，再读本文件，然后按“接手检查”和“下一步建议”继续推进。

最后更新：2026 年 7 月 2 日，审核删除回退确认后。

## 当前一句话状态

CampusLink 已经从静态单页 demo 演进为“静态前端 + Spring Boot API +
MySQL + MyBatis + JUnit/MockMvc 测试”的本地可运行校园社交平台 demo。
当前后端 8080、前端 5178、IDEA、JDK 21 和 MySQL 都已经配置过。前端
`app.js` 已拆分为 `frontend/js/` 下的 ES modules，后端已有 demo bearer
token 鉴权基础和后台管理员角色校验。

## 新对话从哪里开始

新对话开始后，先按这个顺序确认上下文：

1. 读 `AGENTS.md`，遵守模块化、分层、测试和文档同步规则。
2. 读 `README.md`，确认运行方式、API 清单和当前技术栈。
3. 读本文件，确认最新进度和下一步任务。
4. 需要历史细节时，再读
   `docs/report/new-dialogue-handoff-2026-07-01.md`。

接手后先执行这两个检查：

```bash
cd /Users/linus_k/Documents/test
./script/run_frontend_check.sh
```

```bash
cd /Users/linus_k/Documents/test/backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH \
/Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn test
```

预期结果如下：

```text
Frontend smoke test passed.
Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
```

## 当前运行入口

后端和前端分开运行。前端会优先请求 Java API，失败时才回退到 mock 数据。

启动后端：

```bash
cd /Users/linus_k/Documents/test
./script/run_backend_idea.sh
```

启动前端：

```bash
cd /Users/linus_k/Documents/test
./script/run_frontend_demo.sh
```

浏览器入口：

```text
http://127.0.0.1:5178/?v=20260702-frontend-modules
```

后端健康检查：

```text
http://127.0.0.1:8080/api/database/health
```

最近一次健康检查返回：

```json
{"status":"UP","database":"campuslink","demoUsers":4}
```

## 当前已完成能力

当前 demo 支持这些主流程：

- 演示手机号验证码登录。
- 快速进入 demo 工作区。
- 切换林一、陈老师、周同学、教务管理员等演示账号。
- 登录、快速进入和演示账号切换会获取 demo bearer token。
- live API 受保护请求通过 `Authorization` 解析当前用户，不再依赖
  `currentUserId` query 参数或请求体里的 `fromUserId`。
- `/api/admin/**` 要求管理员角色；学生和教师 token 会收到 `403`。
- 前端非管理员账号下不会请求后台 live API，切换教务管理员后再加载后台数据。
- 搜索用户、发送好友申请、同意或拒绝好友申请。
- 查看联系人列表，从联系人进入聊天。
- 发送聊天消息，附带文件 metadata，撤回当前用户消息。
- 切换在线和隐身状态。
- 发布校园动态，点赞动态，查看和新增评论。
- 后台审核动态和评论。
- 后台审核队列默认不再预置固定两条待审记录。
- 后台审核记录支持单条删除和批量删除。
- 最近一次对话已撤回“已处理审核记录删除 UI”和“朋友动态删除”方向的错误
  实现。当前代码中没有 `data-delete-post`、`feed/mine`、`my-post`、或
  `DeletePostResponse` 等动态删除功能残留。
- 后台报表按 **今日**、**本周**、**全部** 筛选。
- 报表支持预览、下载 CSV 和打印预览。
- 后台操作写入审计记录。
- API 请求会在 IDEA 或终端运行窗口输出请求日志。

之前的好友申请问题已经修复：周同学和林一已经是好友时，不会再反复显示
周同学申请林一的 pending 好友申请。

## 前端当前结构

前端仍是静态页面和原生 JavaScript，但不再把逻辑继续追加到单个
`app.js`。根目录 `app.js` 只作为 ES module 入口，实际模块在
`frontend/js/`：

- `api/`：Java API adapter 和 mock fallback。
- `auth/`：验证码登录、快速进入、退出登录和演示账号切换。
- `chat/`：聊天渲染和当前会话辅助逻辑。
- `posts/`：校园动态渲染。
- `admin/`：审核、审计和报表渲染。
- `ui/`：联系人渲染、shell 状态、状态徽标和聚合导出。
- `utils/`：DOM、格式化、附件 metadata 和报表 CSV 工具。
- `loaders.js`：页面数据加载和渲染协调。
- `app-events.js`：页面事件绑定。

## 后端当前结构

后端已经按 Spring Boot 分层组织。Controller 只处理 HTTP，Service 处理业务
逻辑，Repository 负责数据访问，Mapper 负责 MyBatis SQL。

关键目录如下：

- `backend/src/main/java/com/campuslink/controller/`：HTTP API。
- `backend/src/main/java/com/campuslink/service/`：业务逻辑。
- `backend/src/main/java/com/campuslink/mapper/`：MyBatis Mapper。
- `backend/src/main/java/com/campuslink/repository/`：Repository 接口和实现。
- `backend/src/main/java/com/campuslink/entity/`：内部实体 record。
- `backend/src/main/java/com/campuslink/dto/`：接口请求和响应 DTO。
- `backend/src/main/java/com/campuslink/config/`：CORS、异常处理和请求日志。
- `backend/src/main/resources/schema.sql`：MySQL 建表脚本。
- `backend/src/main/resources/data.sql`：演示数据初始化脚本。

当前所有后端持久化访问都已经统一为 MyBatis：

- 用户、验证码、在线状态。
- 好友申请和好友关系。
- 聊天消息和聊天附件 metadata。
- 动态、评论、点赞状态。
- 审核队列和审核状态。
- 审计记录。
- 数据库健康检查。

旧的 `Jdbc*Repository` 已经移除，早期内存 `DemoRepository` 也已经移除。
源码中不再直接使用 `JdbcClient` 或 `JdbcTemplate`。

## MySQL 和 MyBatis 状态

MySQL 本地库已经配置好，后端启动时会自动执行 `schema.sql` 和 `data.sql`。

当前数据库配置如下：

- 数据库：`campuslink`
- 用户名：`campuslink`
- 密码：`campuslink123`
- 端口：`127.0.0.1:3306`
- JDBC URL：
  `jdbc:mysql://127.0.0.1:3306/campuslink?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false`

`backend/pom.xml` 使用 `mybatis-spring-boot-starter` 和 `mysql-connector-j`。
`application.yml` 开启了 MyBatis 下划线到驼峰映射。

## 当前测试状态

后端测试覆盖服务层和主要 Controller API 边界。最近一次 Maven 测试通过：

```text
Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
```

前端 smoke test 仍是 dependency-free，用于检查静态页面、样式、前端 API
adapter、后端路由和 MyBatis 分层断言。最近一次结果：

```text
Frontend smoke test passed.
```

真实 API 最近已验证这些链路：

```text
GET /api/database/health -> UP
POST /api/auth/code + POST /api/auth/login -> 登录成功
POST /api/auth/demo-login -> 演示账号 token 成功
GET /api/users -> token 用户搜索成功
GET /api/friends -> token 用户好友列表成功
GET /api/friends/requests -> token 用户无重复 pending 申请
GET/POST /api/conversations/{peerId}/messages -> 聊天和附件 metadata 成功
POST /api/conversations/{peerId}/messages/{messageId}/withdraw -> token 用户撤回成功
POST /api/feed -> token 用户动态持久化成功
POST /api/feed/{postId}/likes -> token 用户点赞成功
POST /api/feed/{postId}/comments -> token 用户评论持久化成功
GET/POST /api/admin/moderation -> 管理员 token 审核队列和审核状态更新成功
DELETE /api/admin/moderation -> 管理员 token 批量删除审核记录成功
GET /api/admin/audit-events -> 管理员 token 审计记录成功
GET /api/admin/report?range=today -> 管理员 token 报表数据成功
GET /api/admin/metrics -> 学生 token 返回 403
```

## 已知限制

这些是当前 demo 的真实边界，不一定是 bug。

- 后台审核删除目前只在待审核队列 UI 中暴露；`DELETE /api/admin/moderation`
  后端接口存在，但不要把它误实现为删除动态、删除朋友动态、或新增个人动态
  CRUD。
- 前端没有构建系统，当前仍是静态页面和原生 JS。
- 登录 token 仍是内存 demo token，服务重启后会失效，尚未持久化。
- 验证码只用于本地演示，没有接入真实短信。
- 聊天附件只保存 metadata，没有上传或保存文件内容。
- 聊天还不是 WebSocket 实时通信。
- 后台审核规则仍是轻量 demo 逻辑。
- 报表可预览、下载 CSV 和打印预览，但没有服务端报表文件存档。
- 后端还没有连接独立 MySQL 测试库的 Mapper 集成测试。

## 下一步建议

下一位 agent 可以等待用户的新提示词再继续。全量 MyBatis 迁移、前端基础
模块拆分、demo bearer token 鉴权基础、后台管理员角色校验、默认空审核
队列、以及待审核记录删除已经完成，继续做数据库迁移或重复拆分的收益不高。

推荐顺序如下：

1. 再次运行 `./script/run_frontend_check.sh` 和后端 Maven 测试，确认基线。
2. 等待用户明确下一步需求；如果没有新需求，再考虑 token 持久化或会话表。
3. 前端新增功能继续放入 `frontend/js/` 对应模块，不要重新扩张根目录
   `app.js`。
4. 如果用户再次提到“审核记录删除”，先确认是后台审核记录，不是动态删除或
   朋友动态 CRUD。
5. 如果优先做质量任务，补一组连接 MySQL 测试库的 Mapper/Repository 集成
   测试。

## 给新对话的第一句话

可以直接把下面这段发给新对话：

```text
请阅读 AGENTS.md 和 docs/report/new-dialogue-handoff-2026-07-02.md，
同步 CampusLink 当前进度。当前项目已经完成前端 ES modules 拆分、后端
Spring Boot + MySQL + MyBatis 分层、demo bearer token 鉴权、后台管理员权限
校验、默认空审核队列、以及待审核记录单条/批量删除。刚才曾误做过朋友动态
删除、个人动态 CRUD、已处理审核记录删除 UI，但这些都已经撤回；不要恢复这些
误改。请先运行 ./script/run_frontend_check.sh 和后端 Maven 测试确认基线，
然后等待我的新提示词再继续开发。所有新增功能必须保持模块化、测试通过，并
同步更新 README 或 docs。
```
