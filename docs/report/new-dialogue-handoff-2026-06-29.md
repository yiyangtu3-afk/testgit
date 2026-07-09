# CampusLink 新对话交接总结

这份总结用于在新对话中快速接手 CampusLink demo。新的 agent 可以先读
`AGENTS.md`，再读本文件，然后从“下一步建议”继续推进。

最后更新：2026 年 6 月 29 日。

## 当前状态

CampusLink 当前是一个无前端依赖的静态桌面端 demo，配套一个最小
Spring Boot API 骨架。前端文件在仓库根目录，后端代码在 `backend/`。

当前前端会优先请求 `http://127.0.0.1:8080/api`。如果 Java API 没有启动，
会自动回退到 `app.js` 内置 mock 数据。侧边栏显示 **Mock** 表示使用
fallback，显示 **Java API** 表示 live API 请求成功。

当前关键文件如下：

- `index.html`：登录页、工作区外壳、侧边栏、聊天、动态和后台结构。
- `styles.css`：黑、白、灰为主的 Apple-like 桌面 UI 样式。
- `app.js`：mock 数据、API fallback、账号切换、好友、聊天、动态、后台
  和报表交互逻辑。
- `backend/src/main/java/com/campuslink/controller/`：HTTP Controller 层。
- `backend/src/main/java/com/campuslink/service/`：业务 Service 层。
- `backend/src/main/java/com/campuslink/repository/`：内存 Repository 层。
- `backend/src/main/java/com/campuslink/entity/`：内部实体模型。
- `backend/src/main/java/com/campuslink/dto/`：接口 DTO 模型。
- `backend/src/main/java/com/campuslink/config/`：CORS 和全局异常处理。
- `tests/frontend-smoke.test.js`：dependency-free 前端 smoke test。
- `README.md`：运行方式和当前可验证流程。
- `docs/report/current-progress-report.md`：较完整的历史进度记录。
- `docs/report/next-thread-handoff.md`：上一版新对话交接文档。

当前仓库没有提交历史，大部分文件仍是 Git 未跟踪状态。不要只依赖
`git diff` 判断项目内容，接手时直接读取文件。

## 当前可用 demo

当前静态服务运行在：

```bash
http://127.0.0.1:5178/
```

浏览器若缓存旧资源，可以打开或刷新：

```bash
http://127.0.0.1:5178/?v=20260629-report-range
```

如果需要重新启动前端服务，在仓库根目录运行：

```bash
python3 -m http.server 5174
```

如果 `5174` 被占用，可以换端口，例如：

```bash
python3 -m http.server 5177
```

## 已完成能力

当前 demo 已经能完整点击验证以下流程：

- 使用演示手机号获取验证码，并登录学生账号。
- 通过 **快速进入** 直接进入工作区。
- 在侧边栏通过 **切换账号** 切换到林一、陈老师、周同学或教务管理员。
- 点击 **退出登录** 返回登录页，并清空当前工作区状态。
- 按姓名或手机号搜索演示用户。
- 从搜索结果发送好友申请。
- 在接收方账号下处理收到的好友申请，同意或拒绝。
- 好友申请文案显示完整方向，例如 **周同学申请林一** 或
  **林一申请陈老师**。
- 查看已接受好友组成的 **联系人** 列表。
- 从联系人入口打开一对一聊天。
- 发送模拟聊天消息。
- 选择一个或多个本地文件，随聊天消息发送附件 metadata，并在消息气泡中
  预览文件名、大小和类型。
- 撤回当前用户最近一条未撤回消息。
- 在在线和隐身状态之间切换。
- 按可见范围发布校园动态。
- 点赞校园动态。
- 展开校园动态评论，查看评论并新增评论。
- 在后台 **内容审核** 队列中通过或拒绝待审动态和评论。
- 在后台选择 **今日**、**本周** 或 **全部** 报表范围，再点击
  **打印报表**，生成 **后台运营报表** 卡片，预览指标、待审内容和审计
  记录。
- 从报表卡点击 **下载 CSV**，下载前端生成的 CSV blob。
- 从报表卡点击 **打印预览**，打开浏览器打印流程。
- 查看后台指标和随操作更新的审计记录。

## 最近完成的重点

最近一轮主要完善了管理后台报表范围筛选能力。后端环境仍缺少 JDK 21 和
Maven，因此本轮按上一版交接文档里的轻量前端增强建议推进。现在已经完成：

- `index.html` 在管理后台标题区新增报表范围分段控件。
- `app.js` 新增 `today`、`week` 和 `all` 报表范围模型，并把范围写入报表
  payload、CSV 内容和下载文件名。
- `styles.css` 新增紧凑的范围分段控件样式，并适配窄屏布局。
- 分层后的 `AdminController` 和 `AdminService` 让 `/api/admin/report` 接收
  可选 `range` 查询参数，并返回 `ReportRangeView`。
- `tests/frontend-smoke.test.js` 覆盖报表范围控件、样式、前端 adapter 和后端
  report range 结构。
- `README.md` 已同步新的报表筛选流程和 API 说明。

## 后端 API 状态

后端骨架使用 Spring Boot 3.5.0、Java 21 和 Maven。当前 API 路径如下：

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

当前已通过 IntelliJ IDEA 自带 JBR 和 bundled Maven 跑通过后端。IDEA 顶部
运行配置里可选择 **CampusLink API**，点击绿色运行按钮会执行
`spring-boot:run` 并监听 `8080`。

## 验证方式

当前可靠的前端验证命令如下：

```bash
node --check app.js
node tests/frontend-smoke.test.js
```

静态服务验证命令如下：

```bash
curl -I http://127.0.0.1:5178/
curl -s http://127.0.0.1:5178/ | rg "styles.css\\?v=20260629-report-range|app.js\\?v=20260629-report-range"
```

最近一次验证结果：

- `node --check app.js` 通过。
- `node tests/frontend-smoke.test.js` 通过。
- IDEA **CampusLink API** 运行配置可启动 Spring Boot。
- `GET http://127.0.0.1:8080/api/admin/metrics` 可返回 JSON。
- `GET http://127.0.0.1:8080/api/admin/report?range=week` 可返回报表 JSON。
- `5178` 静态服务能返回带 `20260629-report-range` 版本参数的 CSS 和 JS。

## 已知限制

以下限制属于当前预期状态：

- 前端没有构建系统，在引入构建系统前必须保持 dependency-free。
- 后端数据全部保存在内存中。
- 登录使用 demo token，后端尚未校验 `Authorization` 请求头。
- 短信发送未实现，验证码通过响应体返回用于本地测试。
- 聊天实时能力未实现，前端发送消息后仍会模拟一条回复。
- 聊天附件当前只传递 metadata，不读取、不上传、不持久化本地文件内容。
- 内容审核当前只覆盖动态和评论的内存状态，还没有真实风控规则或审核人
  权限校验。
- 后台报表当前由前端渲染为报表卡并转换为 CSV blob；范围筛选会进入
  payload 和 CSV，但还没有服务端文件存储或打印历史。
- 当前已有 dependency-free 前端 smoke test，但还没有真实浏览器交互测试。
- 当前后端已经按 controller、service、repository、entity、dto、config 分层，
  但还没有 JUnit 测试。

## 下一步建议

建议下一位 agent 从 **后端运行环境和测试** 开始。原因是前端 demo 的
主要业务链路已经很完整，但 Java API 还没有在本机成功启动过，后续再加
业务功能会越来越依赖可运行的后端验证。

推荐下一步按以下顺序执行：

1. 使用 IDEA 顶部 **CampusLink API** 运行配置，或安装本地 JDK 21 和 Maven。
2. 在仓库根目录运行前端检查：

   ```bash
   node --check app.js
   node tests/frontend-smoke.test.js
   ```

3. 启动后端：

   ```bash
   cd backend
   mvn spring-boot:run
   ```

4. 启动静态前端：

   ```bash
   python3 -m http.server 5177
   ```

5. 打开 `http://127.0.0.1:5177/?v=20260629-report-card-2`，验证侧边栏是否
   从 **Mock** 切换为 **Java API**。
6. 在 `backend/src/test/java` 下为 Controller 和 Service 增加 JUnit 测试。
7. 优先覆盖这些后端测试路径：
   - 验证码登录。
   - 用户搜索。
   - 好友申请创建、同意和拒绝。
   - 联系人列表。
   - 消息发送、附件 metadata 和撤回。
   - 动态发布、点赞、评论。
   - 内容审核通过和拒绝。
   - 后台指标、审计记录和报表 payload。
8. 后端跑通后，再考虑把当前 dependency-free smoke test 升级为真实浏览器
   交互测试。

如果暂时无法安装后端环境，建议下一步先拆分前端结构。`app.js` 已超过
1400 行，优先把 API adapter、mock 数据、报表工具函数和渲染函数拆到
`frontend/js/` 下，再继续新增功能。
