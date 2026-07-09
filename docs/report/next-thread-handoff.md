# CampusLink 新对话交接报告

这份报告用于在新对话中快速接手 CampusLink demo。新的 agent 应先阅读
`AGENTS.md`、`README.md` 和本文件，然后从“下一步建议”继续开发。

最后更新：2026 年 6 月 29 日。

## 当前项目状态

CampusLink 当前是一个无前端依赖的静态桌面端 demo，配套一个最小
Spring Boot API 骨架。前端文件在仓库根目录，后端文件在 `backend/`。

核心文件如下：

- `index.html`：登录页、桌面工作区、侧边栏、聊天、动态和后台结构。
- `styles.css`：黑、白、灰为主的 Apple-like 桌面 UI 样式。
- `app.js`：mock 数据、API fallback、账号切换、好友、聊天、动态和后台
  交互逻辑。
- `backend/src/main/java/com/campuslink/controller/`：HTTP Controller 层。
- `backend/src/main/java/com/campuslink/service/`：业务 Service 层。
- `backend/src/main/java/com/campuslink/repository/`：内存 Repository 层。
- `backend/src/main/java/com/campuslink/entity/`：内部实体模型。
- `backend/src/main/java/com/campuslink/dto/`：接口 DTO 模型。
- `backend/src/main/java/com/campuslink/config/`：CORS 和全局异常处理。
- `README.md`：运行方式和当前可验证流程。
- `tests/frontend-smoke.test.js`：dependency-free 前端 smoke test。
- `AGENTS.md`：仓库协作规则、代码风格和验证要求。
- `docs/report/current-progress-report.md`：较完整的历史进度记录。

当前仓库没有提交历史，主要文件仍是 Git 未跟踪状态。不要依赖 `git diff`
判断全部内容，接手时直接读取文件。

## 当前可用 demo

前端服务当前运行在：

```bash
http://127.0.0.1:5177/
```

如果需要重新启动静态服务，可在仓库根目录运行：

```bash
python3 -m http.server 5174
```

如果 `5174` 被占用，可以改用其他端口，例如：

```bash
python3 -m http.server 5176
```

前端会优先请求 `http://127.0.0.1:8080/api`。如果 Java API 未启动，会自动
回退到 `app.js` 内置 mock 数据。侧边栏显示 **Mock** 表示当前使用
fallback，显示 **Java API** 表示 live API 请求成功。

## 已完成能力

当前 demo 已经能完整点击验证以下流程：

- 使用演示手机号获取验证码，并登录学生账号。
- 通过 **快速进入** 直接进入工作区。
- 在工作区侧边栏通过 **切换账号** 切换到林一、陈老师、周同学或教务
  管理员。
- 点击 **退出登录** 返回登录页，并清空当前工作区状态。
- 按姓名或手机号搜索演示用户。
- 发送好友申请。
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
- 在后台点击 **打印报表**，生成 **后台运营报表** 卡片，预览指标、待审
  内容和审计记录，并提供 **下载 CSV** 和 **打印预览**。
- 查看后台指标和随操作更新的审计记录。

## 重要业务约定

好友申请逻辑已经按真实账号归属修正：

- 当前账号只能发送好友申请，不能替对方处理自己发出的申请。
- 当前账号只能处理发给自己的好友申请。
- 例如林一申请陈老师后，林一侧只能看到“林一申请陈老师 / 待处理 /
  等待对方”。
- 切换到陈老师账号后，陈老师侧才能看到“林一申请陈老师 / 待处理”，并
  执行 **同意** 或 **拒绝**。
- 同意后，双方都会进入彼此的联系人列表。

联系人逻辑也已经独立出来：

- 搜索结果展示可申请的全校用户。
- **联系人** 面板只展示已经成为好友的用户。
- 聊天入口从 **联系人** 面板进入，不再把全部演示用户都默认当成可聊
  对象。

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
- `GET /api/admin/report`
- `GET /api/admin/audit-events`

聊天消息支持可选附件 metadata。动态和评论支持轻量审核状态，拒绝后的内容
会从 feed 或 comment 响应中隐藏。后台报表接口返回结构化 payload，前端会
把它渲染为报表卡，并转换为 CSV 下载。

当前已通过 IntelliJ IDEA 自带 JBR 和 bundled Maven 成功启动后端。IDEA
顶部运行配置中可以选择 **CampusLink API** 并点击绿色运行按钮。

## 已执行验证

前端语法检查已通过：

```bash
node --check app.js
node tests/frontend-smoke.test.js
```

当前静态服务资源检查已通过：

```bash
curl -I http://127.0.0.1:5176/
curl -I http://127.0.0.1:5176/app.js
```

两者都返回 `HTTP/1.0 200 OK`。

最新静态服务也已在 `5177` 端口验证：

```bash
curl -I http://127.0.0.1:5177/
```

新增的 dependency-free smoke test 已通过：

```bash
node tests/frontend-smoke.test.js
```

最新静态资源已确认包含后台报表打印入口：

```bash
curl -s http://127.0.0.1:5177/ | rg "exportReportButton|exportPanel"
```

后端验证尚未执行成功，原因是当前环境缺少 JDK 和 Maven。

## 已知限制

以下限制是当前预期状态，不代表新 bug：

- 前端没有构建系统，在引入构建系统前必须保持 dependency-free。
- 后端数据全部保存在内存中。
- 登录使用 demo token，后端尚未校验 `Authorization` 请求头。
- 短信发送未实现，验证码通过响应体返回用于本地测试。
- 聊天实时能力未实现，前端发送消息后仍会模拟一条回复。
- 聊天附件当前只传递 metadata，不读取、不上传、不持久化本地文件内容。
- 内容审核当前只覆盖动态和评论的内存状态，还没有真实风控规则或审核人
  权限校验。
- 后台报表当前由前端渲染为报表卡并转换为 CSV blob，还没有服务端文件
  存储或打印历史。
- 当前已有一个 dependency-free 前端 smoke test，但还没有真实浏览器交互
  测试或后端 JUnit 测试。
- 浏览器自动化验证不稳定，之前 Playwright bundled Chromium 缺失，本机
  Chrome headless 也受权限限制失败。

## 下一步建议

建议下一位 agent 先补后端运行环境，或者把当前 dependency-free smoke
test 升级为真实浏览器交互测试。当前自动浏览器环境不稳定，Playwright 的
bundled Chromium 曾缺失，本机 Chrome headless 也曾受权限限制失败。

更大的后续方向仍是补齐后端运行环境：

1. 安装 JDK 21 和 Maven。
2. 运行后端：

   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. 启动前端静态服务。
4. 验证侧边栏从 **Mock** 切换为 **Java API**。
5. 在 `backend/src/test/java` 下为 Controller 和 Service 增加 JUnit 测试。

## 接手 checklist

新对话开始后建议按以下顺序执行：

1. 阅读 `AGENTS.md`。
2. 阅读本文件和 `docs/report/current-progress-report.md`。
3. 运行：

   ```bash
   node --check app.js
   node tests/frontend-smoke.test.js
   ```

4. 打开或刷新当前 demo：

   ```bash
   http://127.0.0.1:5177/
   ```

5. 手动验证关键路径：
   登录或快速进入、切换账号、好友申请、联系人列表、聊天、动态评论和
   后台内容审核。
6. 从后端运行环境或浏览器交互测试开始下一步开发。
