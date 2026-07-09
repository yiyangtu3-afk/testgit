#!/usr/bin/env node
/* eslint-env node */
// noinspection JSUnresolvedReference,SqlNoDataSourceInspection,SqlDialectInspection,SpellCheckingInspection

const { execFileSync } = require("node:child_process");
const { readdirSync, readFileSync, statSync } = require("node:fs");
const { join } = require("node:path");

const root = join(__dirname, "..");
const files = {
  html: read("index.html"),
  css: read("styles.css"),
  js: `${read("app.js")}\n${readTree("frontend/js")}`,
  apiClient: read("frontend/js/api/client.js"),
  appEntry: read("app.js"),
  backend: readTree("backend/src/main/java"),
  resources: readTree("backend/src/main/resources"),
  pom: read("backend/pom.xml")
};
const failures = [];

function read(path) {
  return readFileSync(join(root, path), "utf8");
}

function readTree(path) {
  const absolutePath = join(root, path);
  return readdirSync(absolutePath)
    .flatMap((entry) => {
      const child = join(path, entry);
      const childAbsolutePath = join(root, child);
      return statSync(childAbsolutePath).isDirectory() ? readTree(child) : read(child);
    })
    .join("\n");
}

function expectIncludes(fileName, needle, label) {
  if (!files[fileName].includes(needle)) {
    failures.push(`${label}: expected ${fileName} to include ${needle}`);
  }
}

function expectMatch(fileName, pattern, label) {
  if (!pattern.test(files[fileName])) {
    failures.push(`${label}: expected ${fileName} to match ${pattern}`);
  }
}

function runSyntaxCheck() {
  ["app.js", ...listFiles("frontend/js").filter((file) => file.endsWith(".js"))].forEach((file) => {
    try {
      execFileSync(process.execPath, ["--check", join(root, file)], { stdio: "pipe" });
    } catch (error) {
      failures.push(`JavaScript syntax check failed for ${file}: ${String(error.message || error)}`);
    }
  });
}

function listFiles(path) {
  const absolutePath = join(root, path);
  return readdirSync(absolutePath).flatMap((entry) => {
    const child = join(path, entry);
    const childAbsolutePath = join(root, child);
    return statSync(childAbsolutePath).isDirectory() ? listFiles(child) : child;
  });
}

runSyntaxCheck();

if (files.appEntry.split("\n").length > 20) {
  failures.push("frontend entry: expected root app.js to stay as a small module entry");
}

[
  ["loginForm", "verification login form"],
  ["sendCodeButton", "send code button"],
  ["demoLoginButton", "quick demo login button"],
  ["accountSelect", "demo account switcher"],
  ["friendRequestList", "friend request panel"],
  ["conversationList", "contacts list"],
  ["realtimeMode", "chat realtime status badge"],
  ["attachmentInput", "chat attachment input"],
  ["feedList", "campus feed list"],
  ["managePersonalPosts", "personal post manager button"],
  ["personalPostPanel", "personal post manager panel"],
  ["exportReportButton", "admin report export button"],
  ["reportRange", "admin report range control"],
  ["exportPanel", "admin report export panel"],
  ["moderationPanel", "admin moderation panel"],
  ["auditTable", "admin audit table"]
].forEach(([id, label]) => expectIncludes("html", `id="${id}"`, label));

expectIncludes("html", "简洁的校园沟通空间", "login hero copy");
expectIncludes("html", "class=\"insight-grid\"", "login insight grid markup");
expectIncludes("html", "class=\"insight-card\"", "login insight card markup");

if (files.html.indexOf('id="moderationPanel"') > files.html.indexOf('id="auditTable"')) {
  failures.push("admin panel order: expected moderationPanel to appear before auditTable");
}

[
  ['type="module"', "ES module app loader"],
  ['src="app.js', "root module app loader"]
].forEach(([needle, label]) => expectIncludes("html", needle, label));

[
  ['./frontend/js/app.js', "root app entry imports frontend module"],
  ['export const state', "shared frontend state module"],
  ['export const mockApi', "mock API module"],
  ['export const api', "live API adapter module"],
  ['export function bindAppEvents', "event binding module"],
  ['export async function enterWorkspace', "auth workspace module"],
  ['export function renderMessages', "chat renderer module"],
  ['export function connectChatRealtime', "chat realtime module"]
].forEach(([needle, label]) => expectIncludes("js", needle, label));

[
  ["friend-request-panel", "friend request styles"],
  ["contact-panel", "contact styles"],
  ["insight-grid", "login insight grid styles"],
  ["insight-card", "login insight card styles"],
  ["attachment-tray", "attachment tray styles"],
  ["message-attachment", "message attachment card styles"],
  ["comment-panel", "feed comment styles"],
  ["feed-feedback", "feed moderation feedback styles"],
  ["review-state-guide", "personal post moderation guide styles"],
  ["personal-post-review-note", "personal post moderation note styles"],
  ["personal-post-status--rejected", "personal post rejected status styles"],
  ["export-panel", "report export styles"],
  ["export-state", "report export status styles"],
  ["report-card", "report card styles"],
  ["range-segment", "report range styles"],
  ["report-preview-row", "report preview row styles"],
  ["print-link", "report print button styles"],
  ["download-link", "report download link styles"],
  ["moderation-panel", "moderation panel styles"],
  ["review-workbench", "admin review workbench styles"],
  ["audit-log-panel", "admin audit log panel styles"],
  ["admin-feedback", "admin operation feedback styles"],
  ["admin-feedback--success", "admin operation success feedback styles"],
  ["admin-feedback--error", "admin operation error feedback styles"],
  ["review-card", "moderation review card styles"],
  ["review-detail-panel", "moderation review detail styles"],
  ["review-field-head", "moderation review field header styles"],
  ["review-filter", "moderation review filter styles"],
  ["moderation-status--pending", "moderation pending status styles"],
  ["moderation-bulk-actions", "moderation bulk action styles"],
  ["moderation-check", "moderation selection styles"],
  ["danger-action", "bulk delete action styles"],
  ["audit-table", "audit table styles"]
].forEach(([className, label]) => expectIncludes("css", `.${className}`, label));

expectIncludes("css", ".realtime-mode", "chat realtime status styles");

[
  ["if (!code)", "login auto-fetches missing verification code"],
  ["createCode(phone)", "verification code API adapter"],
  ["login(phone, code)", "login API adapter"],
  ["loginAsDemo(userId", "demo login API adapter"],
  ["sendFriendRequest(userId)", "friend request API adapter"],
  ["resolveFriendRequest(requestId, decision)", "friend request resolution adapter"],
  ["sendMessage(peerId, text, attachments = [])", "attachment-aware message adapter"],
  ["publishComment(postId, body)", "feed comment adapter"],
  ["personalPosts()", "personal posts adapter"],
  ["filter(isCurrentUserPost)", "personal post manager filters to current user"],
  ["#feedList\").hidden = true", "personal post manager hides campus feed list"],
  ["personalPostStatus(post.moderationStatus)", "personal post status label mapping"],
  ["moderationReason", "personal post moderation reason field"],
  ["feedNotice", "feed moderation notice state"],
  ["动态已提交审核", "post submission moderation feedback"],
  ["评论已提交审核", "comment submission moderation feedback"],
  ["通过前不会出现在公共动态流", "pending post visibility explanation"],
  ["拒绝原因", "rejected post reason explanation"],
  ["已拒绝", "personal post rejected status label"],
  ["updatePersonalPost(postId, body)", "personal post update adapter"],
  ["deletePersonalPost(postId)", "personal post delete adapter"],
  ["moderationItems()", "moderation queue adapter"],
  ["resolveModeration(itemId, decision)", "moderation resolution adapter"],
  ["deleteModerationItems(itemIds)", "moderation delete adapter"],
  ["deleteAuditEvents(eventIds)", "audit event delete adapter"],
  ["data-delete-selected-moderation", "moderation selected delete action"],
  ["data-delete-selected-audit-events", "audit selected delete action"],
  ["删除所选", "admin selected delete label"],
  ["data-moderation-filter", "moderation type filter action"],
  ["admin-feedback", "admin operation feedback wrapper"],
  ["confirmAdminAction", "moderation delete confirmation helper"],
  ["确认删除", "admin delete confirmation copy"],
  ["已删除", "admin delete success feedback"],
  ["审核操作失败", "admin moderation error feedback"],
  ["adminReport()", "admin report adapter"],
  ["reportToCsv(report)", "admin report CSV converter"],
  ["reportPreviewRows(report)", "admin report preview rows"],
  ["renderExportPanel()", "admin report export renderer"],
  ["renderAdminAccessDenied()", "admin access denied renderer"],
  ["待审核内容", "admin pending content module title"],
  ["审核内容", "admin pending content review card heading"],
  ["Content Review", "admin content review kicker"],
  ["提交人：", "admin pending content submitter field"],
  ["提交时间：", "admin pending content submitted time field"],
  ["内容类型", "admin pending content type field"],
  ["当前状态", "admin pending content status field"],
  ["同意", "admin moderation approve action label"],
  ["拒绝", "admin moderation reject action label"],
  ["查看", "admin moderation inspect action label"],
  ["data-review-moderation", "admin moderation inspect action binding"],
  ["reviewingModerationId", "admin moderation inspect state"],
  ["review-detail-panel", "admin moderation inspect detail panel"],
  ["审核详情", "admin moderation inspect detail title"],
  ["收起", "admin moderation collapse action label"],
  ["review-workbench", "admin pending content workbench wrapper"],
  ["audit-log-panel", "admin audit log wrapper"],
  ["submittedAt", "moderation submitted time field"],
  ["isAdminUser", "frontend admin role guard"],
  ["reportRanges", "admin report range model"],
  ["data-report-range", "admin report range action"],
  ["后台运营报表", "admin report title"],
  ["data-print-report", "admin report print action"],
  ["Preparing Report", "admin report loading state"],
  ["Export Failed", "admin report error state"],
  ["loadAdminData()", "admin data loader"],
  ["renderModerationItems()", "moderation renderer"]
].forEach(([needle, label]) => expectIncludes("js", needle, label));

if (files.js.indexOf("moderationWorkbenchMarkup()") > files.js.indexOf("Audit Log")) {
  failures.push("admin layout: expected pending content workbench to render before audit log");
}

expectIncludes("html", "20260708-user-moderation-scroll-v2", "admin review cache-busting version");
expectIncludes("appEntry", "20260708-user-moderation-scroll-v2", "root app imports current admin review module version");

expectIncludes("js", "setRealtimeMode", "chat realtime status updater");
expectIncludes("js", "HEARTBEAT_INTERVAL_MS", "chat realtime heartbeat interval");
expectIncludes("js", "heartbeat.ping", "chat realtime heartbeat ping");
expectIncludes("js", "heartbeat.pong", "chat realtime heartbeat pong");

[
  ["currentUserId=", "live API should not pass current user through query strings"],
  ["fromUserId: state.currentUser.id", "live API should not trust request body user ids"],
  ["demo-jwt-token", "live API should not use a fixed demo token"]
].forEach(([needle, label]) => {
  if (files.apiClient.includes(needle)) {
    failures.push(`${label}: expected frontend modules not to include ${needle}`);
  }
});

[
  ["postComments", "comment state"],
  ["feedNotice", "feed moderation notice state"],
  ["pendingAttachments", "attachment state"],
  ["moderationItems", "moderation state"],
  ["moderationFilter", "moderation filter state"],
  ["reviewingModerationId", "moderation detail state"],
  ["adminNotice", "admin operation notice state"],
  ["reportExport", "report export state"],
  ["withApi", "live API fallback helper"],
  ["setApiMode(\"mock\")", "default mock mode"]
].forEach(([needle, label]) => expectIncludes("js", needle, label));

expectMatch("js", /request\("\/admin\/moderation"\)/, "moderation list endpoint");
expectMatch("js", /\/admin\/moderation\/\$\{itemId}\/\$\{decision}/, "moderation decision endpoint");
expectMatch("js", /request\("\/admin\/moderation", \{\s*method: "DELETE"/, "moderation delete endpoint");
expectMatch("js", /request\(`\/admin\/report\?range=\$\{encodeURIComponent\(range\)}`\)/, "admin report range endpoint");
expectMatch("js", /await loadModerationItems\(\);\s*await backfillModerationItemsWhenMetricsDisagree\(\);\s*await loadAuditEvents\(\);/, "admin loads moderation before audit events");
expectMatch("js", /post\.moderationStatus === "approved"/, "campus feed only renders approved posts");
expectMatch("js", /comment\.moderationStatus === "approved"/, "campus feed only renders approved comments");
expectMatch("js", /state\.personalPostManagerOpen = true;/, "post publish opens personal post manager");
expectMatch("js", /state\.feedNotice = successNotice\("评论已提交审核，通过后会显示在动态下。"\);/, "comment publish shows pending feedback");
expectMatch("js", /moderationItems\(\) \{\s*return mockStore\.moderationItems\.filter\(\(item\) => item\.status === "pending"\);/s, "mock moderation list returns pending queue");
expectMatch("js", /data-comment-form="\$\{post\.id}"/, "comment form binding");
expectMatch("js", /data-edit-personal-post/, "personal post edit binding");
expectMatch("js", /data-delete-personal-post/, "personal post delete binding");
expectMatch("js", /data-remove-attachment="\$\{attachment\.id}"/, "attachment removal binding");
expectMatch("js", /data-toggle-all-moderation/, "moderation select-all toggle binding");
expectMatch("js", /state\.moderationFilter === "all" \|\| item\.type === state\.moderationFilter/, "moderation select-all respects active filter");
expectMatch("js", /state\.reviewingModerationId = state\.reviewingModerationId === itemId \? "" : itemId;/, "moderation detail toggle behavior");
expectMatch("js", /state\.moderationFilter = moderationFilter\.dataset\.moderationFilter;/, "moderation filter toggle behavior");
expectMatch("js", /window\.confirm\(message\)/, "admin delete confirmation dialog");
expectMatch("js", /data-select-audit-event/, "audit event selection binding");
expectMatch("js", /data-toggle-all-audit-events/, "audit event select-all toggle binding");
expectMatch("js", /new WebSocket\(`\$\{chatSocketUrl\(\)\}\?token=/, "chat websocket connection");
expectIncludes("js", "message.withdrawn", "chat websocket withdraw event");
expectMatch("js", /payload\.type === "message\.created" \|\| payload\.type === "message\.withdrawn"/, "chat websocket message event guard");
expectMatch("js", /await refreshConversation\(payload\.peerId\)/, "chat websocket eager message refresh");
expectMatch("js", /await loadMessages\(peerId\)/, "chat websocket peer message refresh");
expectMatch("js", /await loadFriends\(\)/, "chat websocket friend refresh");

[
  ["package com.campuslink.controller", "backend controller layer"],
  ["package com.campuslink.service", "backend service layer"],
  ["package com.campuslink.mapper", "backend MyBatis mapper layer"],
  ["package com.campuslink.repository", "backend repository layer"],
  ["package com.campuslink.entity", "backend entity layer"],
  ["package com.campuslink.dto", "backend dto layer"],
  ["package com.campuslink.config", "backend config layer"],
  ['@RequestMapping("/api/auth")', "backend auth route group"],
  ['@PostMapping("/code")', "backend verification code route"],
  ['@PostMapping("/login")', "backend login route"],
  ['@PostMapping("/demo-login")', "backend demo login route"],
  ["AuthTokenService", "backend bearer token service"],
  ["requireAdmin", "backend admin role guard"],
  ["ForbiddenException", "backend forbidden exception"],
  ["HttpStatus.FORBIDDEN", "backend forbidden status"],
  ['@RequestMapping("/api/friends")', "backend friends route group"],
  ['@GetMapping("/requests")', "backend friend requests route"],
  ['@PostMapping("/conversations/{peerId}/messages")', "backend message route"],
  ['@RequestMapping("/api/feed")', "backend feed route group"],
  ['@GetMapping("/personal-posts")', "backend personal post list route"],
  ['@PatchMapping("/personal-posts/{postId}")', "backend personal post update route"],
  ['@DeleteMapping("/personal-posts/{postId}")', "backend personal post delete route"],
  ['@PostMapping("/{postId}/comments")', "backend comment route"],
  ['@RequestMapping("/api/admin")', "backend admin route group"],
  ['@GetMapping("/moderation")', "backend moderation list route"],
  ['@PostMapping("/moderation/{itemId}/{decision}")', "backend moderation decision route"],
  ['@DeleteMapping("/moderation")', "backend moderation delete route"],
  ["record DeleteModerationResponse", "backend moderation delete response"],
  ['@DeleteMapping("/audit-events")', "backend audit event delete route"],
  ["record DeleteAuditEventsResponse", "backend audit event delete response"],
  ['@GetMapping("/report")', "backend admin report route"],
  ['@RequestMapping("/api/database")', "backend database route group"],
  ['@GetMapping("/health")', "backend database health route"],
  ["RequestLogInterceptor", "backend request logging interceptor"],
  ["addInterceptors", "backend request logging registration"],
  ["ChatWebSocketConfig", "backend chat WebSocket configuration"],
  ["ChatWebSocketHandler", "backend chat WebSocket handler"],
  ['"/ws/chat"', "backend chat WebSocket route"],
  ['setAllowedOriginPatterns("http://127.0.0.1:*", "http://localhost:*")', "backend chat WebSocket local origins"],
  ["ChatRealtimeNotifier", "backend chat realtime notifier"],
  ["heartbeat.ping", "backend chat heartbeat ping handler"],
  ["heartbeat.pong", "backend chat heartbeat pong response"],
  ["publishMessageWithdrawn", "backend chat withdraw realtime notifier"],
  ["record AttachmentView", "backend attachment message shape"],
  ["String moderationReason", "backend personal post moderation reason shape"],
  ["record ModerationItemView", "backend moderation shape"],
  ["record AdminReportView", "backend report shape"],
  ["record ReportRangeView", "backend report range shape"]
].forEach(([needle, label]) => expectIncludes("backend", needle, label));

expectMatch("backend", /findPending\(\)\.stream\(\)\s*\.map\(DemoMapper::toModerationItemView\)/, "backend admin moderation list returns pending content");
expectMatch("backend", /as moderationReason/, "backend personal post query returns moderation reason");
expectMatch("backend", /p\.moderation_status = 'approved'/, "backend campus feed only returns approved posts");
expectMatch("backend", /c\.moderation_status = 'approved'/, "backend campus feed only returns approved comments");

[
  ["mybatis-spring-boot-starter", "MyBatis starter dependency"],
  ["mysql-connector-j", "MySQL driver dependency"],
  ["spring-boot-starter-websocket", "Spring WebSocket dependency"]
].forEach(([needle, label]) => expectIncludes("pom", needle, label));

[
  ["jdbc:mysql://127.0.0.1:3306/campuslink", "MySQL datasource URL"],
  ["username: campuslink", "MySQL demo username"],
  ["schema.sql", "schema init configuration"],
  ["create table if not exists users", "MySQL users schema"],
  ["insert into users", "MySQL demo seed data"],
  ["m-demo-post-9001", "MySQL demo pending moderation seed"],
  ["on duplicate key update", "idempotent MySQL demo seed data"]
].forEach(([needle, label]) => expectIncludes("resources", needle, label));

if (failures.length > 0) {
  console.error("Frontend smoke test failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Frontend smoke test passed.");
