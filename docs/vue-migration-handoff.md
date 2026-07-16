# CampusLink Vue 前端渐进迁移交接

本文定义 CampusLink 从原生 ES Modules 静态前端迁移到 Vue 3 的方式。迁移从
独立目录开始，保留当前可演示版本，避免在尚未验证等价行为前替换入口。

## 当前状态

当前前端不是 Vue、React 或其他 SPA 框架，而是根目录 `index.html`、`app.js`、
`styles.css` 与 `frontend/js/` 中按职责拆分的原生 ES Modules。它已有 API、认证、
聊天、动态、活动、通知和管理员模块；`frontend/js/state.js` 是所有旧模块共享的
状态单例。当前可用版本和业务基线见
[`new-chat-handoff-2026-07-08.md`](new-chat-handoff-2026-07-08.md)。

当前迁移尚未开始。不要把本交接文档当作“已使用 Vue”的声明，也不要删除、移动或
重写旧静态入口。

## 目标架构

新前端使用 Vue 3、Vite、Vue Router 和 Pinia，初始放在仓库根目录的
`frontend-vue/`。选择独立目录可以让旧版继续在 `5179` 演示，并让新旧版本在功能
完全等价前并行验证。

建议的初始结构如下：

```text
frontend-vue/
├── package.json
├── vite.config.js
└── src/
    ├── main.js
    ├── App.vue
    ├── router/
    ├── stores/
    ├── services/
    │   ├── api/
    │   └── realtime/
    ├── features/
    │   ├── auth/
    │   ├── chat/
    │   ├── feed/
    │   ├── activities/
    │   ├── notifications/
    │   └── admin/
    └── shared/
```

`services/api/` 必须成为新应用唯一的 HTTP 边界；`stores/` 只保存跨页面状态；
`features/` 只组合本领域组件、事件与加载流程。不要把旧的单一 `state.js` 原样复制到
Pinia，也不要让组件直接散落 `fetch` 调用。

## 不可改变的行为

迁移只替换前端表现和组织方式，不能改变已验证的后端、权限或数据语义。

- Java API 可达时，显示本地 MySQL 历史数据；仅在网络层完全不可达时才可使用 Mock。
  API 返回 `4xx` 或 `5xx` 时必须展示真实失败，绝不能写入或回退 Mock。
- 所有受保护请求继续发送当前 bearer token。前端不得提交或信任当前用户、管理员、
  申请人或收件人的身份字段来代替服务端 JWT 身份。
- 保留现有 WebSocket 聊天和社交通知的收件人定向、断线处理和历史加载语义。
- 保留 XSS 转义、好友聊天边界、通知目标受限解析、审核状态和管理员角色限制。
- `frontend/js/state.js` 的旧版导入不能添加版本查询参数。迁移期间旧代码仍要共享同一
  状态单例。
- 不重置、重种、清空或清理本地 MySQL 历史数据；不运行 `git reset --hard`、
  `git clean`，也不删除未跟踪文件。

## 第一阶段：脚手架、认证与 API 边界

第一阶段只建立 Vue 工程和最小可验证闭环，不替换根目录 `index.html`，不迁移聊天、
动态、活动、通知或管理员页面。

1. 在 `frontend-vue/` 初始化 Vue 3、Vite、Vue Router 和 Pinia。保留旧前端所有
   文件及其运行脚本。
2. 配置 Vite 开发服务器使用独立端口，并将 `/api` 和 `/ws` 代理到
   `http://127.0.0.1:8080`。新应用通过相对路径访问代理，不需要为此修改后端 CORS。
3. 实现统一 HTTP 客户端、`ApiUnavailableError` 等价边界、当前 token 注入和 API/Mock
   模式状态。网络异常与 HTTP 失败必须在这里明确区分。
4. 实现会话 Pinia store 和 Vue 登录/快速进入最小页面，覆盖验证码登录、演示登录、
   登出、令牌保存与 API/Mock 状态反馈。
5. 为新 API 适配和会话 store 编写单元测试；构建通过后，以 Java API 和 Mock 两种
   模式分别验证登录。额外验证 Java API 的 `401`、`403` 和 `500` 不会触发 Mock。
6. 提交并推送这一切片。只有在该阶段验证完成后，才开始迁移下一个领域。

## 后续迁移顺序

按以下顺序迁移，且每一项都必须完成独立验证、README/交接更新、提交和推送。

1. 应用壳、导航和统一状态提示。
2. 联系人、好友申请和聊天，包括分页、未读、附件与 WebSocket。
3. 动态、个人动态、点赞、评论和审核状态反馈。
4. 活动浏览、筛选、报名、运营、签到和通知跳转。
5. 站内通知和实时社交通知。
6. 管理员指标、内容审核、审计记录和报表。
7. 在新旧页面逐项等价验证后，再决定是否切换默认入口和移除旧前端；这一步需要用户
   明确授权，不能作为前面任何阶段的隐含操作。

## 验证与交付

迁移期间保留 `./script/run_frontend_check.sh`，确保旧版回归不受影响。新 Vue 切片还必须
运行其自身的依赖安装、单元测试和生产构建命令；新增 UI 必须检查管理员后台、动态审核
反馈和聊天页。后端未变更时不需要为 Vue 重跑 Maven；若为代理或接口契约修改后端，按
`backend/AGENTS.md` 运行相关测试，并使用显式 Byte Buddy agent 跑完整测试。

每个验证完成的切片单独提交并推送 `main`。README、
[`new-chat-handoff-2026-07-08.md`](new-chat-handoff-2026-07-08.md)、
[`phase-two-activity-handoff.md`](phase-two-activity-handoff.md) 和本文件必须反映
“正在迁移”还是“已切换”的真实状态。
