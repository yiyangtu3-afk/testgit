# 新对话提示词

下面的提示词用于把管理员审核工作台上下文带到新的 Codex 对话。复制
整段内容到新对话即可。

## 可复制提示词

```text
请先阅读并遵循仓库中的 AGENTS.md：

1. 根目录 AGENTS.md。
2. frontend/AGENTS.md。
3. backend/AGENTS.md。
4. 如果修改 docs/ 或 Markdown 文件，请遵循文档写作要求。

项目路径是 /Users/linus_k/Documents/test。项目是 CampusLink，一个静态
前端加 Spring Boot 后端的校园社交 Demo。请不要重置或清理未跟踪文件，
当前仓库可能整体显示为未跟踪状态。

上一轮已经完成管理员审核工作台修复：

- 管理员后台不再只显示“待审内容”数量。
- 指标卡片下方、审计记录上方已经新增“待审核内容”工作台。
- 待审核内容列表展示标题、提交人、提交时间、内容类型、当前状态和操作。
- 管理员可对待审动态或评论执行“同意”“拒绝”“查看”。
- “查看”会展开审核详情，再次点击可收起。
- 待审核内容支持按“全部”“动态”“评论”筛选。
- “待审核内容”和“审计记录”都已增加多选后的“删除所选”功能。
- 未勾选时“删除所选”禁用，勾选后启用。
- 删除待审核内容或审计记录前会二次确认。
- 审核或删除完成后，工作台会显示成功或失败反馈。
- 普通用户发布动态后会自动进入“我的动态”，并看到“待审核”说明。
- 普通用户发布评论后会看到“评论已提交审核”的反馈。
- 个人动态管理会展示待审核、已发布、已拒绝状态；已拒绝内容会展示原因。
- 登录页已调整为更简洁大气的双栏入口，右侧只保留品牌封面，不展示功能
  演示。
- 当前静态资源版本是 20260707-login-portal-v1。
- 本地验证地址是：
  http://127.0.0.1:5176/?v=20260707-login-portal-v1

关键文件：

- frontend/js/admin/renderers.js
- frontend/js/admin/audit-events.js
- frontend/js/app-events.js
- frontend/js/loaders.js
- frontend/js/api/client.js
- frontend/js/api/mock-api.js
- frontend/js/ui/renderers.js
- styles.css
- index.html
- app.js
- tests/frontend-smoke.test.js
- backend/src/main/java/com/campuslink/service/AdminService.java
- backend/src/main/java/com/campuslink/mapper/ModerationMapper.java
- backend/src/main/java/com/campuslink/mapper/FeedMapper.java
- backend/src/main/java/com/campuslink/dto/DemoDtos.java
- backend/src/main/resources/data.sql
- docs/admin-moderation-content-module-fix.md
- docs/admin-review-workbench-handoff.md

重要实现细节：

- 可见的“待审核内容”工作台由 frontend/js/admin/renderers.js 中的
  renderAuditEvents() 插入在 auditTable 内，具体 markup 来自
  moderationWorkbenchMarkup()。
- renderModerationItems() 当前隐藏旧的 #moderationPanel，避免出现两个
  审核区域。
- 审核队列批量删除走 api.deleteModerationItems(itemIds)。
- 审计记录批量删除走 api.deleteAuditEvents(eventIds)。
- 类型筛选状态是 state.moderationFilter。
- 当前展开详情状态是 state.reviewingModerationId。
- 操作反馈状态是 state.adminNotice。
- 用户侧审核反馈状态是 state.feedNotice。
- 选择状态分别是 state.selectedModerationIds 和
  state.selectedAuditEventIds。
- PostView / PostEntity 新增 moderationReason 字段，用于展示个人动态
  审核原因。
- 不要给 state.js 加版本查询参数，否则可能破坏共享状态单例。

上一轮验证：

- ./script/run_frontend_check.sh 通过。
- 渲染级 smoke 检查通过，确认“待审核内容”位于“审计记录”上方，并能
  展示“审核详情”。
- 浏览器验证通过：切换到教务管理员后，“删除所选”按钮存在，未勾选时
  禁用，勾选后启用。
- 后端相关服务测试 FeedServiceTest,AuthTokenServiceTest 曾通过。
- 完整 mvn test 曾受 Microsoft JDK 21 的 Mockito inline Byte Buddy
  自附加限制影响失败，不是审核业务断言失败。

如果需要继续开发，请优先从 docs/admin-review-workbench-handoff.md 和
docs/admin-moderation-content-module-fix.md 读取上下文。修改后前端至少
运行 ./script/run_frontend_check.sh，并用浏览器或渲染级 smoke 检查确认
管理员后台仍然可用。
```
