# CampusLink 当前进度报告

这份报告用于帮助后续 agent 快速接手 CampusLink demo。内容包括当前
项目状态、已完成能力、最近改动、验证结果、已知限制和推荐下一步。

最后更新：2026 年 6 月 29 日。

## 项目概览

CampusLink 当前是一个无前端依赖的静态桌面端 demo，配套一个最小
Spring Boot API 骨架。前端文件位于仓库根目录，后端代码位于
`backend/`。

关键文件如下：

- `index.html`：定义登录页、工作区外壳、聊天、动态和后台面板。
- `styles.css`：定义黑、白、灰为主的 Apple-like 桌面端界面样式。
- `app.js`：定义 mock 数据、API fallback、页面状态和交互逻辑。
- `backend/src/main/java/com/campuslink/controller/`：提供 HTTP Controller 层。
- `backend/src/main/java/com/campuslink/service/`：提供业务 Service 层。
- `backend/src/main/java/com/campuslink/repository/`：提供内存 Repository 层。
- `backend/src/main/java/com/campuslink/entity/`：提供内部实体模型。
- `backend/src/main/java/com/campuslink/dto/`：提供接口 DTO 模型。
- `backend/src/main/java/com/campuslink/config/`：提供 CORS 和全局异常处理。
- `README.md`：记录当前运行方式和手动验证流程。
- `AGENTS.md`：记录仓库约定、验证命令和代码风格要求。

当前仓库没有提交历史，大部分项目文件仍是 Git 未跟踪状态。因此，在
文件被 staged 或 committed 之前，`git diff` 不会显示这些文件内容。

## 已实现前端流程

当前前端支持完整可点击 demo。页面会优先请求
`http://127.0.0.1:8080/api`，如果 Java API 未启动，会自动回退到
内置 mock 数据。侧边栏显示 **Mock** 表示当前使用 fallback，显示
**Java API** 表示 live API 请求成功。

已实现流程包括：

- 获取演示验证码，并以学生账号登录。
- 通过 **快速进入** 按钮直接进入工作区。
- 切换演示账号，或退出登录回到登录页。
- 按姓名或手机号搜索演示用户。
- 从搜索结果发送好友申请。
- 切换演示账号，在接收方账号下处理收到的好友申请。
- 查看已接受好友组成的联系人列表，并从联系人入口打开聊天。
- 打开一对一会话。
- 发送模拟聊天消息。
- 选择一个或多个本地文件，随聊天消息发送附件 metadata，并在消息气泡中
  预览文件名、大小和类型。
- 撤回当前用户最近一条未撤回消息。
- 在在线和隐身状态之间切换。
- 按可见范围发布校园动态。
- 点赞校园动态。
- 展开校园动态评论，查看评论并新增评论。
- 在管理后台查看待审动态和评论，并执行通过或拒绝。
- 在管理后台打印报表卡，预览指标、待审内容和最近审计记录，并下载 CSV。
- 查看后台指标和随操作更新的审计记录。

## 最近改动

最近一次开发把登录页从占位按钮升级为验证码登录流程，同时保留快速
演示入口。

本次开发把后台审计记录从 `index.html` 静态行升级为前后端一致的
内存数据流。前端现在通过 API adapter 加载审计记录；登录、好友申请、
聊天、撤回、在线状态切换、动态发布和点赞都会写入审计事件，并刷新
管理后台。

下一次 demo 切片补齐了动态评论。动态卡片现在可以展开评论区、加载
评论列表并发布新评论；评论数量会同步更新，评论动作也会写入后台审计。

本次 demo 切片补齐了好友申请处理。侧边栏现在展示 **好友申请** 面板和
**演示账号** 切换器；当前账号发出的申请只显示为发出状态，不能自己
同意或拒绝。切换到接收方账号后，接收方才能同意或拒绝收到的申请。同意
后会打开对应会话并插入一条好友关系生效提示，操作会刷新后台指标和
审计记录。

下一次 demo 切片补齐了联系人列表。侧边栏现在展示 **联系人** 面板，
只有已建立好友关系的用户会进入联系人和聊天入口；搜索结果仍可用于查找
全校用户并发送好友申请。同意申请后，双方联系人列表都会出现彼此。

本次修正把好友申请文案改为完整方向，例如 **周同学申请林一** 和
**林一申请陈老师**，避免把接收方和发起方混淆。侧边栏也新增
**退出登录**，用于清空当前工作区状态并回到登录页。

本次 demo 切片补齐了聊天附件入口。聊天输入栏的 **添加文件** 按钮现在
会打开本地文件选择器；选中文件后先显示附件托盘，可以移除单个附件。
发送消息后，消息气泡展示附件文件名、大小和类型。Mock API 和 Java API
消息结构都新增 `attachments` metadata 字段，聊天审计记录会标注本次消息
包含的附件数量。

本次 demo 切片把管理后台从只读指标升级为可操作的内容审核队列。动态和
评论现在带有轻量 `moderationStatus`；后台会列出待审动态和评论，并支持
**通过** 或 **拒绝**。拒绝动态会从动态流隐藏，拒绝评论会从评论列表隐藏
并同步扣减评论数，审核动作会写入审计记录并刷新 **待审内容** 指标。

本次 demo 切片接通并美化了管理后台 **打印报表**。点击按钮后，前端会
立即显示生成中状态，再调用 `/api/admin/report`。页面不再把 `.csv`
文件名作为标题，而是渲染 **后台运营报表** 卡片，展示生成时间、摘要
chip、报表预览行、**下载 CSV** 主按钮和 **打印预览** 按钮。打印动作也会
写入审计记录。

涉及文件如下：

- `index.html`：把登录占位区域替换为手机号、验证码、**获取验证码**、
  **登录** 和 **快速进入** 控件；后台审计表改为动态容器；侧边栏新增
  **演示账号** 选择器、**退出登录**、**好友申请** 面板和 **联系人**
  面板；聊天输入栏新增隐藏文件输入和附件按钮。
- `app.js`：新增手机号校验、登录 busy 状态、获取验证码、验证码登录，
  共享的 `enterWorkspace()` 入口，审计记录 mock/API 加载和渲染，以及
  动态评论加载、展开、发布、好友申请处理、联系人列表、退出登录逻辑；
  聊天消息新增附件 metadata 选择、预览、移除、发送和渲染；管理后台新增
  内容审核队列加载、渲染、通过和拒绝逻辑；新增后台报表打印、生成中状态、
  报表卡预览、CSV 生成、下载按钮和打印预览逻辑。
- `styles.css`：新增验证码输入行、登录按钮行、好友申请面板和动态评论区
  布局规则；新增附件托盘、附件标签、消息附件卡片、内容审核队列和报表
  卡片、摘要 chip、预览行、下载按钮、打印按钮样式。
- `backend/src/main/java/com/campuslink/`：后端已拆分为 controller、
  service、repository、entity、dto 和 config 包；保留
  `/api/admin/audit-events`、动态评论接口、好友申请处理接口、联系人接口、
  聊天附件 metadata、内容审核和 `/api/admin/report` 报表接口。
- `README.md`：同步更新可验证流程，说明验证码登录、快速进入和动态
  评论、好友申请处理、联系人列表、聊天附件、内容审核、报表打印、审计
  记录。
- `tests/frontend-smoke.test.js`：新增 dependency-free 前端 smoke test，
  检查静态页面、样式、前端 API adapter 和 Java API 骨架是否仍覆盖当前
  demo 主路径；同步覆盖后台报表打印入口、报表卡和 `/api/admin/report`。

验证码会从 API 响应中返回，并自动填入登录表单。这样本地 demo 不依赖
短信服务也能完整验证登录链路。

## 后端 API 状态

后端骨架已经存在，并提供与前端边界一致的内存版接口。当前使用
Spring Boot 3.5.0、Java 21 和 Maven。

当前 API 路径如下：

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

聊天消息 payload 当前支持可选 `attachments` metadata 数组。每个附件只记录
本地 demo 需要展示的文件名、大小、MIME type 和类型标签，尚未上传或保存
文件二进制内容。

动态和评论当前支持轻量审核状态。后台审核接口可以通过或拒绝待审内容；
拒绝后的动态和评论不会出现在对应 feed 或 comment 响应中。

后台报表接口当前返回结构化报表 payload。前端在浏览器内渲染报表卡，并
转换为 CSV blob 提供本地下载；后端尚未生成真实文件对象。

当前已通过 IntelliJ IDEA 自带 JBR 和 bundled Maven 成功运行后端。
IDEA 顶部 **CampusLink API** 运行配置可启动 Spring Boot。

## 已执行验证

前端 JavaScript 语法检查已通过。当前可靠的本地验证命令是：

```bash
node --check app.js
node tests/frontend-smoke.test.js
```

静态前端也曾使用临时端口启动验证，因为 `5174` 当时已被占用：

```bash
python3 -m http.server 5175
curl -I http://127.0.0.1:5175/
```

`curl` 返回 `HTTP/1.0 200 OK`。验证完成后，临时 `5175` 服务已停止。
最新好友申请 demo 使用 `5176` 端口继续服务给浏览器查看，`curl -I
http://127.0.0.1:5176/app.js` 返回 `HTTP/1.0 200 OK`。

聊天附件和内容审核 demo 使用 `5177` 端口继续服务给浏览器查看，`curl -I
http://127.0.0.1:5177/` 返回 `HTTP/1.0 200 OK`。

最新轻量前端 smoke test 已通过：

```bash
node tests/frontend-smoke.test.js
```

最新静态资源也已确认包含报表打印入口：

```bash
curl -s http://127.0.0.1:5177/ | rg "exportReportButton|exportPanel"
```

浏览器自动化验证曾尝试执行，但当前环境不稳定：Playwright 的 bundled
Chromium 可执行文件缺失，本机 Chrome headless 启动也受权限限制失败。
通过桌面辅助能力观察到页面可以渲染，但最终自动点击流程没有可靠完成，
因此不能算作完整浏览器自动化验证。

## 已知限制

当前 demo 仍是小型可验证原型。除非后续任务明确要求修改，否则以下内容
都属于当前预期状态。

- 前端没有构建系统，在引入构建系统前需要保持 dependency-free。
- 后端数据全部保存在内存中。
- 登录使用 demo token，后端尚未校验 `Authorization` 请求头。
- 短信发送未实现，验证码通过响应体返回用于本地测试。
- 聊天实时能力未实现，前端发送消息后会模拟一条回复。
- 聊天附件当前只传递 metadata，不读取、不上传、不持久化本地文件内容。
- 内容审核当前只覆盖动态和评论的内存状态，还没有真实风控规则或审核人
  权限校验。
- 后台报表当前由前端把 API payload 转为 CSV blob，还没有服务端文件存储
  或打印历史。
- 当前没有已提交的前端或后端测试。

## 推荐下一步

建议先补齐后端运行和验证环境，再添加更大的业务能力。这样后续开发可以
建立在可运行链路上。

1. 安装 JDK 21 和 Maven。
2. 启动后端：

   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. 启动前端静态服务：

   ```bash
   python3 -m http.server 5174
   ```

4. 验证 live API 成功后，侧边栏是否从 **Mock** 切换到
   **Java API**。
5. 在 `backend/src/test/java` 下为 Controller 和 Service 增加后端测试。
6. 选择下一个持久化切片，例如用户、好友申请、会话、消息、动态或
   文件上传。
7. 在选定前端测试工具后，把当前 dependency-free smoke test 升级为更完整
   的浏览器交互测试 harness。

## 交接提示

新增前端功能时，优先沿用 `app.js` 中现有 API adapter 模式。当前每个
功能都会通过 `request()` 请求 live API，并通过 `withApi()` 回退到对应
的 `mockApi` 方法。

界面改动需要保持当前黑、白、灰为主的视觉风格。这个应用当前是紧凑的
桌面工作区，不是 landing page。

进行较大改动前，先阅读 `AGENTS.md`，并至少运行：

```bash
node --check app.js
node tests/frontend-smoke.test.js
```

后端开发需要在 Java 和 Maven 可用后运行：

```bash
cd backend
mvn test
```
