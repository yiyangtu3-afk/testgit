# 管理员待审核内容模块修复说明

本文记录管理员后台待审核内容工作台缺失的修复原因、实现范围、验证
方式和后续建议。修复后，管理员不再只能看到 **待审内容** 数量，也
可以在管理后台查看别人提交的待审动态或评论，并执行 **同意** 或
**拒绝** 操作。普通用户发布动态或评论后，也能看到内容处于待审核状态；
个人动态管理会展示待审核、已发布、已拒绝状态和审核原因。

## 问题原因

管理后台已有 **待审内容** 指标和 `/api/admin/moderation` 审核接口，
但管理员首屏主要显示的是 **审计记录**。审计记录只是后台行为日志，
不能承担内容审核职责。管理员看到 **待审内容 7** 时，缺少一个明确的
工作台来查看具体待审动态或评论，也无法直接对这些内容执行同意或拒绝。

发布链路也存在边界不清的问题。动态和评论会进入 `pending` 状态，但
公共动态流之前只排除 `rejected`，导致待审内容可能在管理员同意前就被
普通用户看到。正确流程必须是：用户发布内容，内容进入待审队列；管理员
在后台审核；只有 `approved` 内容进入公共动态流。

## 修改内容

本次修复复用现有管理员审核接口，没有新增外部依赖，也没有修改数据库
表结构。后端从现有审核记录、动态、评论和 `created_at` 生成列表展示
字段，前端将审核队列渲染为独立的 **待审核内容** 工作台。

主要调整包括：

- 扩展审核项 DTO 和内部实体，新增 `title` 和 `submittedAt` 字段。
- 调整 MyBatis 审核查询，从动态或评论正文生成内容标题，并格式化提交
  时间。
- 保持 `/api/admin/moderation`、`POST
  /api/admin/moderation/{itemId}/{decision}` 和 `DELETE
  /api/admin/moderation` 路径不变。
- 让 `/api/admin/moderation` 直接返回 `pending` 内容，避免前端混入已
  处理记录。
- 将管理后台审核区域改为 **待审核内容** 工作台，放在指标卡下方和
  **审计记录** 上方。当前可见区域由 `renderAuditEvents()` 统一渲染
  **待审核内容** 与 **审计记录** 两个卡片，避免旧的审计列表占据指标卡
  下方位置。框内以卡片式列表展示内容摘要、提交人、提交时间、内容类型、
  当前状态和操作。
- 将审核按钮文案改为 **同意** 和 **拒绝**，其中 **同意** 继续调用
  `approve` 决策，**拒绝** 继续调用 `reject` 决策。
- 为 **查看** 操作补充卡片内详情面板，展示内容类型、当前状态、提交人、
  提交时间、审核原因、来源和完整正文。
- 为 **待审核内容** 增加按 **全部**、**动态** 和 **评论** 的类型筛选。
  切换筛选会清空当前勾选，避免误删当前视图不可见的记录。
- 为 **待审核内容** 和 **审计记录** 工具条补充 **删除所选** 操作。
  管理员勾选多条记录后，可以一次删除选中的待审项或审计记录。
- 删除待审内容或审计记录前会二次确认，审核或删除完成后会在工作台显示
  成功或失败反馈。
- 将公共动态和评论查询改为只展示 `approved` 内容。待审内容只出现在
  管理员审核工作台和发布者自己的动态管理视图中。
- 为普通用户侧补充审核状态反馈。发布动态后自动进入 **我的动态** 并
  显示 **待审核**，发布评论后显示 **评论已提交审核**。
- 在个人动态管理中展示审核状态说明；已拒绝动态展示拒绝原因，待审核
  动态说明通过前不会进入公共动态流。
- 扩展 `PostView` 和 `PostEntity`，新增 `moderationReason` 字段，并从
  后端审核记录回填审核原因。
- 保留管理员角色校验。前端非管理员显示受限提示，后端管理员 API 继续
  通过 `AuthTokenService.requireAdmin()` 校验。
- 同步 Mock API 的审核项字段，保证 Java API 不可用时前端 Demo 仍能
  展示相同结构。
- 为 Mock 和 MySQL Demo 种子数据补充一条待审核动态，方便管理员进入
  后台后直接验证审核工作台。
- 更新静态入口模块版本，避免浏览器继续使用旧的审核渲染模块缓存。
- 更新前端 smoke test 和后端控制器测试断言，覆盖新增字段。

## 涉及文件

本次修复涉及以下文件：

- `backend/src/main/java/com/campuslink/dto/DemoDtos.java`
- `backend/src/main/java/com/campuslink/entity/DemoEntities.java`
- `backend/src/main/java/com/campuslink/mapper/ModerationMapper.java`
- `backend/src/main/java/com/campuslink/mapper/FeedMapper.java`
- `backend/src/main/java/com/campuslink/service/AdminService.java`
- `backend/src/main/java/com/campuslink/service/DemoMapper.java`
- `backend/src/main/resources/data.sql`
- `backend/src/test/java/com/campuslink/controller/AdminControllerTest.java`
- `backend/src/test/java/com/campuslink/service/FeedServiceTest.java`
- `frontend/js/admin/renderers.js`
- `frontend/js/admin/audit-events.js`
- `frontend/js/api/mock-api.js`
- `frontend/js/api/client.js`
- `frontend/js/app-events.js`
- `frontend/js/app.js`
- `frontend/js/auth/session.js`
- `frontend/js/auth/workspace.js`
- `frontend/js/loaders.js`
- `frontend/js/posts/renderers.js`
- `frontend/js/state.js`
- `frontend/js/ui/renderers.js`
- `frontend/js/utils/format.js`
- `backend/src/main/java/com/campuslink/entity/DemoEntities.java`
- `app.js`
- `index.html`
- `styles.css`
- `tests/frontend-smoke.test.js`

## 验证方式

已完成以下验证：

1. 运行前端检查：

    ```bash
    ./script/run_frontend_check.sh
    ```

    结果：通过。

2. 运行审核框渲染验证：

    ```bash
    node --input-type=module -e "/* renderAuditEvents smoke check */"
    ```

    结果：通过。验证内容包括 **待审核内容** 位于 **审计记录** 上方，
    并包含审核内容、提交人、提交时间、正文摘要、详情面板、筛选按钮，
    以及 **同意**、**拒绝**、**查看** 和 **删除所选** 操作按钮。

3. 使用 IntelliJ IDEA 内置 Maven 运行后端相关服务测试：

    ```bash
    /Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn \
      -Dtest=FeedServiceTest,AuthTokenServiceTest test
    ```

    结果：通过，9 个测试成功。

4. 运行完整后端测试：

    ```bash
    /Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn test
    ```

    结果：编译通过，服务层测试通过；控制器测试受当前 Microsoft JDK 21
    的 Mockito inline mock maker 自附加限制影响失败，错误集中在
    `Could not initialize inline Byte Buddy mock maker`，不是业务断言失败。

5. 启动本地静态服务：

    ```bash
    python3 -m http.server 5176 --bind 127.0.0.1
    ```

    结果：服务可启动在 `http://127.0.0.1:5176/`。通过 `curl` 确认服务
    返回新版 `app.js` 和 `frontend/js/admin/renderers.js`，其中包含
    `20260707-login-portal-v1`、**Content Review** 和 **同意**
    文案。

6. 使用 Chrome 打开本地页面并切换到教务管理员账号：

    ```text
    http://127.0.0.1:5176/?v=20260707-login-portal-v1
    ```

    结果：管理员账号可进入后台。Computer Use 的截图输出存在旧帧和
    可访问性树不一致的问题，因此最终以本地服务返回内容、渲染 smoke
    test 和前端检查作为本次验证依据。

## 后续建议

后续可以继续增强以下方向：

- 为 Mockito 测试配置显式 Java agent，避免依赖运行时自附加。
- 为待审核内容增加按提交人和状态筛选。
- 将审核规则从 Demo 级状态流扩展为可配置风控规则。
- 为管理员审核操作增加更细粒度的审计字段，例如审核人 ID、审核时间和
  审核意见。
