# 管理员审核工作台交接说明

本文用于把管理员审核工作台相关上下文交接给新的对话或新的开发者。
当前实现已完成管理员待审核内容展示、审核操作和批量删除操作。
普通用户侧也已补齐审核状态反馈：发布动态或评论后会看到待审核提示，
个人动态管理会展示待审核、已发布、已拒绝状态和审核原因。登录页也已
调整为更简洁大气的双栏入口，右侧只保留品牌封面，不展示功能演示。

## 当前状态

管理员后台已经从只显示 **待审内容** 数量，调整为在指标卡片下方展示
可操作的 **待审核内容** 工作台。该工作台位于 **审计记录** 上方，管理员
可以看到待审动态和评论，并执行 **同意**、**拒绝**、**查看** 和
**删除所选** 操作。工作台支持按 **全部**、**动态**、**评论** 筛选，
删除待审内容和审计记录前会二次确认，审核或删除完成后会显示操作反馈。
普通用户发布动态后会自动进入 **我的动态**，看到该内容的 **待审核**
状态；发布评论后会看到 **评论已提交审核** 的反馈。

当前静态资源缓存版本是：

```text
20260707-login-portal-v1
```

本地预览地址是：

```text
http://127.0.0.1:5176/?v=20260707-login-portal-v1
```

## 已完成内容

本轮工作完成了管理员审核链路的前后端补全，并保持了现有 Demo 的静态
前端和 Spring Boot 后端结构。

- 新增管理员 **待审核内容** 工作台。
- 将工作台固定渲染在指标卡片和 **审计记录** 之间。
- 列表展示内容标题、提交人、提交时间、内容类型、当前状态和操作。
- **同意** 和 **拒绝** 复用现有审核 API。
- **查看** 会在当前审核卡片内展开详情，再次点击可收起。
- **待审核内容** 支持按 **全部**、**动态** 和 **评论** 筛选。
- **待审核内容** 支持勾选多条后执行 **删除所选**。
- **审计记录** 支持勾选多条后执行 **删除所选**。
- 删除待审内容或审计记录前会弹出二次确认。
- 审核、删除成功或失败后会在工作台显示操作反馈。
- 普通用户发布动态后自动打开 **我的动态**，并显示 **待审核** 说明。
- 普通用户发布评论后显示待审核反馈，通过前不会出现在评论列表。
- 个人动态管理展示审核说明，已拒绝动态展示拒绝原因。
- `PostView` / `PostEntity` 新增 `moderationReason`，前端用它展示审核
  原因。
- 后端审核列表补充 `title` 和 `submittedAt` 展示字段。
- 公共动态和评论只展示 `approved` 内容。
- Mock API 与 live API 保持同一返回结构。
- 补充 Demo 待审数据，方便进入后台后直接验证。
- 更新前端 smoke test 和修复说明文档。

## 关键文件

以下文件是后续继续维护该功能时最需要先看的入口。

- `frontend/js/admin/renderers.js`
- `frontend/js/admin/audit-events.js`
- `frontend/js/app-events.js`
- `frontend/js/loaders.js`
- `frontend/js/api/client.js`
- `frontend/js/api/mock-api.js`
- `frontend/js/ui/renderers.js`
- `styles.css`
- `index.html`
- `app.js`
- `tests/frontend-smoke.test.js`
- `backend/src/main/java/com/campuslink/service/AdminService.java`
- `backend/src/main/java/com/campuslink/mapper/ModerationMapper.java`
- `backend/src/main/java/com/campuslink/mapper/FeedMapper.java`
- `backend/src/main/java/com/campuslink/dto/DemoDtos.java`
- `backend/src/main/resources/data.sql`
- `docs/admin-moderation-content-module-fix.md`

## 重要实现细节

当前可见的管理员主内容由 `renderAuditEvents()` 统一渲染两块区域：
**待审核内容** 和 **审计记录**。这是为了确保 **待审核内容** 一定出现在
用户指定的位置，也就是指标卡片下方、审计记录上方。

`renderModerationItems()` 当前会隐藏旧的独立 `#moderationPanel` 容器，
避免页面同时出现两个审核区域。真实可见的审核工作台由
`moderationWorkbenchMarkup()` 生成，并插入 `#auditTable` 内。

批量删除复用了已有接口：

- `api.deleteModerationItems(itemIds)` 删除选中的待审内容。
- `api.deleteAuditEvents(eventIds)` 删除选中的审计记录。

选择状态存放在共享状态里：

- `state.moderationFilter`
- `state.selectedModerationIds`
- `state.reviewingModerationId`
- `state.selectedAuditEventIds`
- `state.adminNotice`
- `state.feedNotice`

后台数据刷新由 `loadAdminData()` 统一处理。它会先加载指标，再加载审核
队列，必要时用报表回填待审内容，最后加载审计记录。

## 验证记录

已完成以下验证。

1. 运行前端检查：

   ```bash
   ./script/run_frontend_check.sh
   ```

   结果：通过。

2. 运行渲染级检查：

   ```bash
   node --input-type=module -e "/* renderAuditEvents smoke check */"
   ```

   结果：通过。检查点包括 **待审核内容** 位于 **审计记录** 上方，且
   包含 **同意**、**拒绝**、**查看**、**审核详情**、筛选和
   **删除所选**。

3. 打开浏览器验证：

   ```text
   http://127.0.0.1:5176/?v=20260707-login-portal-v1
   ```

   结果：进入 Demo，切换到 **教务管理员**，打开后台后可以看到
   **待审核内容** 工作台。未勾选时 **删除所选** 禁用；勾选记录后
   **删除所选** 启用，并显示已选择数量。

4. 后端相关服务测试曾通过：

   ```bash
   /Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn \
     -Dtest=FeedServiceTest,AuthTokenServiceTest test
   ```

   结果：通过，9 个测试成功。

5. 完整 Maven 测试曾受本机 Microsoft JDK 21 的 Mockito inline mock
   maker 自附加限制影响失败。失败集中在 Byte Buddy mock maker 初始化，
   不是当前审核业务断言失败。

## 本地运行

如果本地页面打不开，优先确认静态服务是否运行。可使用项目脚本：

```bash
./script/run_frontend_demo.sh
```

也可以使用基础静态服务：

```bash
python3 -m http.server 5176 --bind 127.0.0.1
```

如果端口被占用，先确认占用者是否是旧的 Python 静态服务，再决定复用或
换端口。不要随意终止未知进程。

## 注意事项

后续继续开发时，必须先读取根目录、`frontend/` 和 `backend/` 下的
`AGENTS.md`。如果修改文档，还需要遵循文档写作要求。

当前仓库在 `git status --short` 下显示大量未跟踪文件。这像是整个 Demo
目录未被 Git 跟踪，而不是本轮只新增了少量未跟踪文件。继续工作时不要
用破坏性命令清理这些文件。

不要把 `state.js` 加查询版本参数。多个模块必须共享同一个状态实例。
当前缓存版本只加在入口和业务模块导入上。

## 后续建议

后续可以继续做这些增强：

- 给 **待审核内容** 的 **查看** 增加详情弹窗或右侧预览。
- 给 **待审核内容** 增加按类型、提交人和状态筛选。
- 为批量删除增加二次确认和操作结果 toast。
- 为审核操作增加审核意见、审核人和审核时间。
- 修复本机 JDK 21 下 Mockito inline mock maker 的完整测试运行问题。
