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

function runEscapeHtmlCheck() {
  try {
    const output = execFileSync(
      process.execPath,
      [
        "--input-type=module",
        "--eval",
        'import { escapeHtml } from "./frontend/js/utils/format.js"; process.stdout.write(escapeHtml(\'<img src=x onerror=alert(1)>\\\"&\\\'\'));'
      ],
      { cwd: root, encoding: "utf8" }
    );
    if (output !== "&lt;img src=x onerror=alert(1)&gt;&quot;&amp;&#39;") {
      failures.push("HTML escaping: expected user content to be encoded for text and attributes");
    }
  } catch (error) {
    failures.push(`HTML escaping check failed: ${String(error.message || error)}`);
  }
}

function runFeedRendererEscapingCheck() {
  const payload = "<img src=x onerror=alert(1)>";
  const script = [
    'globalThis.elements = { "#feedList": { hidden: false, innerHTML: "" } };',
    'globalThis.document = { querySelector(selector) { return globalThis.elements[selector]; } };',
    'const { state } = await import("./frontend/js/state.js");',
    'state.personalPostManagerOpen = false;',
    'state.expandedPostId = 1;',
    `state.posts = [{ id: 1, author: "林一", body: ${JSON.stringify(payload)}, visibility: "全校可见", moderationStatus: "approved", likes: 0, comments: 1 }];`,
    `state.postComments = { 1: [{ author: "陈老师", body: ${JSON.stringify(payload)}, time: "09:30" }] };`,
    'const { renderFeed } = await import("./frontend/js/posts/renderers.js");',
    'renderFeed();',
    'process.stdout.write(globalThis.elements["#feedList"].innerHTML);'
  ].join("\n");
  try {
    const output = execFileSync(
      process.execPath,
      ["--input-type=module", "--eval", script],
      { cwd: root, encoding: "utf8" }
    );
    if (output.includes(payload) || !output.includes("&lt;img src=x onerror=alert(1)&gt;")) {
      failures.push("feed renderer: expected user post and comment content to render as escaped text");
    }
  } catch (error) {
    failures.push(`Feed renderer escaping check failed: ${String(error.message || error)}`);
  }
}

function runActivityRendererCheck() {
  const payload = "<img src=x onerror=alert(1)>";
  const script = [
    'globalThis.elements = {',
    '  "#activityCreatorPanel": { hidden: false },',
    '  "#activityNotice": { hidden: true, className: "", textContent: "" },',
    '  "#activitySubmissionPanel": { hidden: true },',
    '  "#activitySubmissionList": { innerHTML: "" },',
    '  "#activityFilterFrom": { value: "" },',
    '  "#activityFilterTo": { value: "" },',
    '  "#activityFilterCategory": { value: "", innerHTML: "" },',
    '  "#activityFilterSummary": { textContent: "" },',
    '  "#publishedActivityCount": { textContent: "" },',
    '  "#activityList": { innerHTML: "" },',
    '  "#activityReviewPanel": { hidden: true, innerHTML: "" }',
    '};',
    'globalThis.document = { querySelector(selector) { return globalThis.elements[selector]; } };',
    'const { state } = await import("./frontend/js/state.js");',
    'state.currentUser = { id: "u-2001", name: "陈老师", role: "教师账号" };',
    `state.activities = [{ id: "activity-xss", title: ${JSON.stringify(payload)}, description: ${JSON.stringify(payload)}, category: "科技", location: "A201", startsAt: "2026-08-01T09:00:00", endsAt: "2026-08-01T11:00:00", capacity: 20, organizerId: "u-2001", organizerName: "陈老师", status: "published", reviewDecision: "approved" }];`,
    'state.activitySubmissions = [];',
    'delete state.activityFilters;',
    'delete state.activityCategories;',
    'const { renderActivities, renderPendingActivities } = await import("./frontend/js/activities/renderers.js");',
    'renderActivities();',
    'const activityHtml = globalThis.elements["#activityList"].innerHTML;',
    'state.currentUser = { id: "u-1001", name: "林一", role: "学生账号" };',
    'state.activityRegistrations = { "activity-xss": { id: "registration-1", activityId: "activity-xss", status: "registered", queuePosition: 0 } };',
    'renderActivities();',
    'const registrationHtml = globalThis.elements["#activityList"].innerHTML;',
    'state.currentUser = { id: "u-2003", name: "教务管理员", role: "管理员账号" };',
    `state.pendingActivities = [{ id: "activity-pending", title: ${JSON.stringify(payload)}, description: ${JSON.stringify(payload)}, category: "社团", location: "活动中心", startsAt: "2026-08-02T09:00:00", endsAt: "2026-08-02T11:00:00", capacity: 30, organizerId: "u-2004", organizerName: "王社长", status: "pending", reviewDecision: "pending" }];`,
    'renderPendingActivities();',
    'process.stdout.write(JSON.stringify({ activityHtml, registrationHtml, reviewHtml: globalThis.elements["#activityReviewPanel"].innerHTML }));'
  ].join("\n");
  try {
    const output = JSON.parse(execFileSync(
      process.execPath,
      ["--input-type=module", "--eval", script],
      { cwd: root, encoding: "utf8" }
    ));
    if (output.activityHtml.includes(payload) || !output.activityHtml.includes("&lt;img")) {
      failures.push("activity renderer: expected activity title and description to be escaped");
    }
    if (!output.registrationHtml.includes("已报名") || !output.registrationHtml.includes("取消报名")) {
      failures.push("activity renderer: expected current student registration actions");
    }
    if (!output.reviewHtml.includes('data-review-activity="activity-pending"')) {
      failures.push("activity renderer: expected pending activity review actions");
    }
    if (output.reviewHtml.includes(payload) || !output.reviewHtml.includes("&lt;img")) {
      failures.push("activity review renderer: expected pending activity content to be escaped");
    }
  } catch (error) {
    failures.push(`Activity renderer check failed: ${String(error.message || error)}`);
  }
}

function runActivityNotificationRendererCheck() {
  const payload = "<img src=x onerror=alert(1)>";
  const script = [
    'globalThis.elements = {',
    '  "#activityNotificationBadge": { hidden: true, textContent: "" },',
    '  "#activityNotificationUnreadCount": { textContent: "" },',
    '  "#activityNotificationList": { innerHTML: "" },',
    '  "#activityNotificationNotice": { hidden: true, className: "", textContent: "" },',
    '  "#markAllActivityNotifications": { disabled: false }',
    '};',
    'globalThis.document = { querySelector(selector) { return globalThis.elements[selector]; } };',
    'const { state } = await import("./frontend/js/state.js");',
    'delete state.activityNotifications;',
    'delete state.activityNotificationUnreadCount;',
    'delete state.activityNotificationNotice;',
    'const { activityNotificationState } = await import("./frontend/js/notifications/state.js");',
    'const notificationState = activityNotificationState();',
    `notificationState.items = [{ id: "notification-xss", activityId: "activity-1", type: "activity.review.rejected", title: ${JSON.stringify(payload)}, body: ${JSON.stringify(payload)}, read: false, createdAt: "2026-07-12T12:00:00" }];`,
    'notificationState.unreadCount = 1;',
    'const { renderActivityNotifications } = await import("./frontend/js/notifications/renderers.js");',
    'renderActivityNotifications();',
    'process.stdout.write(JSON.stringify({ html: globalThis.elements["#activityNotificationList"].innerHTML, badge: globalThis.elements["#activityNotificationBadge"].textContent, hidden: globalThis.elements["#activityNotificationBadge"].hidden }));'
  ].join("\n");
  try {
    const output = JSON.parse(execFileSync(
      process.execPath,
      ["--input-type=module", "--eval", script],
      { cwd: root, encoding: "utf8" }
    ));
    if (output.html.includes(payload) || !output.html.includes("&lt;img")) {
      failures.push("activity notification renderer: expected title and body to be escaped");
    }
    if (!output.html.includes("notification-card--unread") || output.badge !== "1" || output.hidden) {
      failures.push("activity notification renderer: expected visible unread state and navigation badge");
    }
  } catch (error) {
    failures.push(`Activity notification renderer check failed: ${String(error.message || error)}`);
  }
}

function runMockActivityWorkflowCheck() {
  const script = [
    'const { state } = await import("./frontend/js/state.js");',
    'const { mockApi } = await import("./frontend/js/api/mock-api.js");',
    'state.currentUser = { id: "u-2001", name: "陈老师", role: "教师账号" };',
    'const created = await mockApi.createActivity({ title: "行为测试活动", description: "测试", category: "科技", location: "A201", startsAt: "2026-08-01T09:00", endsAt: "2026-08-01T11:00", capacity: 20 });',
    'const createdOrganizerId = created.organizerId;',
    'const createdStatus = created.status;',
    'state.currentUser = { id: "u-2003", name: "教务管理员", role: "管理员账号" };',
    'const pending = await mockApi.pendingActivities();',
    'const reviewed = await mockApi.reviewActivity(created.id, "approve", null);',
    'const published = await mockApi.activities();',
    'state.currentUser = { id: "u-1001", name: "林一", role: "学生账号" };',
    'const registration = await mockApi.registerActivity(created.id);',
    'const cancelled = await mockApi.cancelActivityRegistration(created.id);',
    'process.stdout.write(JSON.stringify({ createdId: created.id, createdOrganizerId, createdStatus, pendingIds: pending.map((item) => item.id), reviewed, publishedIds: published.map((item) => item.id), registration, cancelled }));'
  ].join("\n");
  try {
    const output = JSON.parse(execFileSync(
      process.execPath,
      ["--input-type=module", "--eval", script],
      { cwd: root, encoding: "utf8" }
    ));
    if (output.createdOrganizerId !== "u-2001" || output.createdStatus !== "pending") {
      failures.push("mock activity workflow: expected authenticated teacher and pending creation");
    }
    if (!output.pendingIds.includes(output.createdId)) {
      failures.push("mock activity workflow: expected created activity in admin pending queue");
    }
    if (output.reviewed.status !== "published" || !output.publishedIds.includes(output.createdId)) {
      failures.push("mock activity workflow: expected approved activity in published list");
    }
    if (output.registration.status !== "registered" || output.cancelled.status !== "cancelled") {
      failures.push("mock activity workflow: expected student registration and cancellation");
    }
  } catch (error) {
    failures.push(`Mock activity workflow check failed: ${String(error.message || error)}`);
  }
}

function runMockActivityFilterCheck() {
  const script = [
    'const { mockStore, state } = await import("./frontend/js/state.js");',
    'const { mockApi } = await import("./frontend/js/api/mock-api.js");',
    'state.currentUser = { id: "u-1001", name: "林一", role: "学生账号" };',
    'mockStore.activities.push({ ...mockStore.activities[0], id: "activity-mock-public-service", title: "公益实践日", category: "公益", startsAt: "2026-09-03T09:00:00", endsAt: "2026-09-03T12:00:00" });',
    'mockStore.activities.push({ ...mockStore.activities[0], id: "activity-mock-technology", title: "科技实践日", category: "科技", startsAt: "2026-09-04T09:00:00", endsAt: "2026-09-04T12:00:00" });',
    'mockStore.activities.push({ ...mockStore.activities[0], id: "activity-mock-october", title: "十月公益日", category: "公益", startsAt: "2026-10-01T09:00:00", endsAt: "2026-10-01T12:00:00" });',
    'const category = await mockApi.activities({ category: "公益" });',
    'const dates = await mockApi.activities({ from: "2026-09-01", to: "2026-09-30" });',
    'const combined = await mockApi.activities({ category: "公益", from: "2026-09-01", to: "2026-09-30" });',
    'process.stdout.write(JSON.stringify({ category: category.map((activity) => activity.id), dates: dates.map((activity) => activity.id), combined: combined.map((activity) => activity.id) }));'
  ].join("\n");
  try {
    const output = JSON.parse(execFileSync(
      process.execPath,
      ["--input-type=module", "--eval", script],
      { cwd: root, encoding: "utf8" }
    ));
    if (!output.category.includes("activity-mock-public-service")
        || !output.category.includes("activity-mock-october")
        || output.category.includes("activity-mock-technology")) {
      failures.push("mock activity filters: expected category filter to match live API semantics");
    }
    if (output.dates.length !== 2
        || !output.dates.includes("activity-mock-public-service")
        || !output.dates.includes("activity-mock-technology")) {
      failures.push("mock activity filters: expected inclusive date range filtering");
    }
    if (output.combined.length !== 1 || output.combined[0] !== "activity-mock-public-service") {
      failures.push("mock activity filters: expected category and date filters to combine");
    }
  } catch (error) {
    failures.push(`Mock activity filter check failed: ${String(error.message || error)}`);
  }
}

function runMockActivityNotificationWorkflowCheck() {
  const script = [
    'const { state } = await import("./frontend/js/state.js");',
    'const { mockApi } = await import("./frontend/js/api/mock-api.js");',
    'state.currentUser = { id: "u-2001", name: "陈老师", role: "教师账号" };',
    'const created = await mockApi.createActivity({ title: "通知行为测试", description: "测试", category: "科技", location: "A201", startsAt: "2026-09-01T09:00", endsAt: "2026-09-01T11:00", capacity: 1 });',
    'state.currentUser = { id: "u-2003", name: "教务管理员", role: "管理员账号" };',
    'await mockApi.reviewActivity(created.id, "approve", null);',
    'state.currentUser = { id: "u-2001", name: "陈老师", role: "教师账号" };',
    'const organizerSummary = await mockApi.activityNotifications();',
    'state.currentUser = { id: "u-1001", name: "林一", role: "学生账号" };',
    'await mockApi.registerActivity(created.id);',
    'state.currentUser = { id: "u-2002", name: "周同学", role: "学生账号" };',
    'await mockApi.registerActivity(created.id);',
    'state.currentUser = { id: "u-1001", name: "林一", role: "学生账号" };',
    'await mockApi.cancelActivityRegistration(created.id);',
    'state.currentUser = { id: "u-2002", name: "周同学", role: "学生账号" };',
    'const studentSummary = await mockApi.activityNotifications();',
    'const readSummary = await mockApi.markAllActivityNotificationsRead();',
    'process.stdout.write(JSON.stringify({ organizerSummary, studentSummary, readSummary }));'
  ].join("\n");
  try {
    const output = JSON.parse(execFileSync(
      process.execPath,
      ["--input-type=module", "--eval", script],
      { cwd: root, encoding: "utf8" }
    ));
    if (output.organizerSummary.items[0]?.type !== "activity.review.approved") {
      failures.push("mock activity notifications: expected organizer approval result");
    }
    const studentTypes = output.studentSummary.items.map((item) => item.type);
    if (!studentTypes.includes("activity.registration.waitlisted")
        || !studentTypes.includes("activity.registration.promoted")) {
      failures.push("mock activity notifications: expected waitlist and promotion results");
    }
    if (output.studentSummary.unreadCount !== 2 || output.readSummary.unreadCount !== 0
        || output.readSummary.items.some((item) => !item.read)) {
      failures.push("mock activity notifications: expected persisted unread and read-all state");
    }
  } catch (error) {
    failures.push(`Mock activity notification workflow check failed: ${String(error.message || error)}`);
  }
}

function checkVersionedModuleImports() {
  const bareImports = [...files.js.matchAll(/(?:from|import)\s*["']([^"']+\.js)["']/g)]
    .map((match) => match[1])
    .filter((path) => !path.endsWith("state.js"));
  if (bareImports.length > 0) {
    failures.push(`module cache versioning: expected only state.js imports without a version, found ${bareImports.join(", ")}`);
  }
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
runEscapeHtmlCheck();
runFeedRendererEscapingCheck();
runActivityRendererCheck();
runActivityNotificationRendererCheck();
runMockActivityWorkflowCheck();
runMockActivityFilterCheck();
runMockActivityNotificationWorkflowCheck();
checkVersionedModuleImports();

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
  ["activitiesPanel", "campus activity panel"],
  ["activityForm", "activity submission form"],
  ["activityFilterForm", "activity filter form"],
  ["activityFilterFrom", "activity filter start date"],
  ["activityFilterTo", "activity filter end date"],
  ["activityFilterCategory", "activity category filter"],
  ["clearActivityFilters", "activity filter reset action"],
  ["activityFilterSummary", "activity filter result summary"],
  ["activityList", "published activity list"],
  ["activitySubmissionList", "activity submission status list"],
  ["activityReviewPanel", "admin activity review panel"],
  ["notificationsPanel", "activity notification center"],
  ["activityNotificationBadge", "activity notification navigation badge"],
  ["activityNotificationUnreadCount", "activity notification unread count"],
  ["activityNotificationList", "activity notification list"],
  ["markAllActivityNotifications", "activity notification read-all action"],
  ["exportReportButton", "admin report export button"],
  ["reportRange", "admin report range control"],
  ["exportPanel", "admin report export panel"],
  ["moderationPanel", "admin moderation panel"],
  ["auditTable", "admin audit table"]
].forEach(([id, label]) => expectIncludes("html", `id="${id}"`, label));

expectIncludes("html", "简洁的校园沟通空间", "login hero copy");
expectIncludes("html", "Content-Security-Policy", "browser content security policy");
expectIncludes("html", "script-src 'self'", "content security policy blocks inline scripts");
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
  ['export function renderActivities', "activity renderer module"],
  ['export function bindActivityEvents', "activity event module"],
  ['export function renderActivityNotifications', "activity notification renderer module"],
  ['export function bindActivityNotificationEvents', "activity notification event module"],
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
  ["activity-intro", "activity editorial introduction styles"],
  ["activity-filter-bar", "activity filter toolbar styles"],
  ["activity-form", "activity submission form styles"],
  ["activity-card", "activity card styles"],
  ["activity-review-panel", "activity admin review styles"],
  ["notification-stage", "activity notification stage styles"],
  ["notification-card", "activity notification card styles"],
  ["notification-card--unread", "activity notification unread styles"],
  ["activity-status--pending", "activity pending status styles"],
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
  ["createActivity(activity)", "activity submission adapter"],
  ["pendingActivities()", "pending activity adapter"],
  ["reviewActivity(activityId, decision, reason", "activity review adapter"],
  ["registerActivity(activityId)", "activity registration adapter"],
  ["cancelActivityRegistration(activityId)", "activity cancellation adapter"],
  ["activityNotifications()", "activity notification list adapter"],
  ["markAllActivityNotificationsRead()", "activity notification read-all adapter"],
  ["活动通知暂时无法加载", "activity notification load failure feedback"],
  ["activityFilters", "activity filter state"],
  ["filterActivities", "activity filter event"],
  ["clearActivityFilters", "activity filter reset event"],
  ["活动已提交审核", "activity pending feedback"],
  ["data-review-activity", "activity review action binding"],
  ["data-activity-registration", "activity registration action binding"],
  ["拒绝活动时必须填写原因", "activity rejection reason guard"],
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

expectMatch(
  "css",
  /\.is-admin\s*\{\s*display:\s*block;/,
  "admin layout: expected independent review workspaces to use normal document flow"
);

expectIncludes("html", "20260712-activity-notifications-v1", "HTML escaping cache-busting version");
expectIncludes("appEntry", "20260712-activity-notifications-v1", "root app imports current HTML escaping module version");

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
  ["activitySubmissions", "activity submission state"],
  ["pendingActivities", "pending activity state"],
  ["activityNotifications", "activity notification state"],
  ["activityNotificationUnreadCount", "activity notification unread state"],
  ["withApi", "live API fallback helper"],
  ["setApiMode(\"mock\")", "default mock mode"]
].forEach(([needle, label]) => expectIncludes("js", needle, label));

expectIncludes("apiClient", "class ApiUnavailableError", "live API availability error type");
expectIncludes("js", "escapeHtml", "shared HTML escaping helper");
expectMatch("js", /escapeHtml\(post\.body\)/, "feed post body is escaped");
expectMatch("js", /escapeHtml\(comment\.body\)/, "feed comment body is escaped");
expectMatch("js", /escapeHtml\(messageText\)/, "chat message text is escaped");
expectMatch("js", /escapeHtml\(item\.event\)/, "audit event text is escaped");
expectMatch(
  "apiClient",
  /if \(!\(error instanceof ApiUnavailableError\)\) \{\s*throw error;\s*\}/,
  "live API errors do not mutate mock data"
);

expectMatch("js", /request\("\/admin\/moderation"\)/, "moderation list endpoint");
expectMatch("apiClient", /request\(`\/activities\$\{query/, "filtered published activity endpoint");
expectMatch("apiClient", /request\("\/admin\/activities\/pending"\)/, "pending activity endpoint");
expectMatch("apiClient", /\/admin\/activities\/\$\{activityId}\/reviews/, "activity review endpoint");
expectMatch("apiClient", /request\("\/activity-notifications"\)/, "activity notification list endpoint");
expectMatch("apiClient", /request\("\/activity-notifications\/read-all", \{ method: "POST" \}\)/, "activity notification read-all endpoint");
expectMatch("js", /escapeHtml\(activity\.title\)/, "activity title is escaped");
expectMatch("js", /escapeHtml\(activity\.description\)/, "activity description is escaped");
expectMatch("js", /\/admin\/moderation\/\$\{itemId}\/\$\{decision}/, "moderation decision endpoint");
expectMatch("js", /request\("\/admin\/moderation", \{\s*method: "DELETE"/, "moderation delete endpoint");
expectMatch("js", /request\(`\/admin\/report\?range=\$\{encodeURIComponent\(range\)}`\)/, "admin report range endpoint");
expectMatch("js", /await loadModerationItems\(\);\s*await backfillModerationItemsWhenMetricsDisagree\(\);\s*await loadAuditEvents\(\);/, "admin loads moderation before audit events");
expectMatch("js", /post\.moderationStatus === "approved"/, "campus feed only renders approved posts");
expectMatch("js", /comment\.moderationStatus === "approved"/, "campus feed only renders approved comments");
expectMatch("js", /function canViewPost\(post\)/, "mock feed visibility guard");
expectMatch("js", /return mockStore\.posts\.filter\(canViewPost\);/, "mock feed applies visibility guard");
expectMatch("js", /function requireFriendship\(peerId\)/, "mock chat friendship guard");
expectMatch("js", /async messages\(peerId, beforeId = null, limit = 30\) \{\s*requireFriendship\(peerId\);/s, "mock message reads require friendship");
expectMatch("js", /async sendMessage\(peerId, text, attachments = \[\]\) \{\s*requireFriendship\(peerId\);/s, "mock sends require friendship");
expectIncludes("js", "conversationPaging", "conversation page state");
expectIncludes("js", "unreadCounts", "persisted unread count API");
expectIncludes("js", "conversationPreviews", "conversation preview API");
expectMatch("js", /request\("\/conversations\/previews"\)/, "conversation preview endpoint");
expectMatch("js", /await loadConversationPreviews\(\);\s*await loadUnreadCounts\(\);/, "workspace loads previews before unread counts");
expectIncludes("js", "data-load-older-messages", "load earlier messages action");
expectMatch("js", /await loadMessages\(peerId, paging\.nextBeforeId\);/, "load earlier messages uses cursor");
expectMatch("js", /await loadUnreadCounts\(\);\s*await loadMessages\(\);/, "workspace only loads the selected conversation");
expectMatch("backend", /@GetMapping\("\/conversations\/unread-counts"\)/, "backend unread count route");
expectMatch("backend", /@GetMapping\("\/conversations\/previews"\)/, "backend conversation preview route");
expectMatch("backend", /findMessagePage\(peerId, currentUserId, beforeId, pageSize \+ 1\)/, "backend retrieves one extra message for cursor pagination");
expectMatch("js", /state\.personalPostManagerOpen = true;/, "post publish opens personal post manager");
expectMatch("js", /state\.feedNotice = successNotice\("评论已提交审核，通过后会显示在动态下。"\);/, "comment publish shows pending feedback");
expectMatch("js", /moderationItems\(\) \{\s*return mockStore\.moderationItems\.filter\(\(item\) => item\.status === "pending"\);/s, "mock moderation list returns pending queue");
expectMatch("js", /data-comment-form="\$\{escapeHtml\(post\.id\)\}"/, "comment form binding");
expectMatch("js", /data-edit-personal-post/, "personal post edit binding");
expectMatch("js", /data-delete-personal-post/, "personal post delete binding");
expectMatch("js", /data-remove-attachment="\$\{escapeHtml\(attachment\.id\)\}"/, "attachment removal binding");
expectMatch("js", /data-toggle-all-moderation/, "moderation select-all toggle binding");
expectMatch("js", /state\.moderationFilter === "all" \|\| item\.type === state\.moderationFilter/, "moderation select-all respects active filter");
expectMatch("js", /state\.reviewingModerationId = state\.reviewingModerationId === itemId \? "" : itemId;/, "moderation detail toggle behavior");
expectMatch("js", /state\.moderationFilter = moderationFilter\.dataset\.moderationFilter;/, "moderation filter toggle behavior");
expectMatch("js", /window\.confirm\(message\)/, "admin delete confirmation dialog");
expectMatch("js", /data-select-audit-event/, "audit event selection binding");
expectMatch("js", /data-toggle-all-audit-events/, "audit event select-all toggle binding");
expectMatch("js", /new WebSocket\(`\$\{chatSocketUrl\(\)\}\?token=/, "chat websocket connection");
expectIncludes("js", "activity.notification.created", "activity notification realtime event");
expectMatch("js", /handleActivityNotificationEvent\(payload\)/, "activity notification realtime routing");
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
  ['@RequestMapping("/api/activities")', "backend activity route group"],
  ['@RequestMapping("/api/admin/activities")', "backend activity admin route group"],
  ['@RequestMapping("/api/activity-notifications")', "backend activity notification route group"],
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
  ["ActivityNotificationRealtimePublisher", "backend activity notification realtime publisher"],
  ["activity.notification.created", "backend activity notification realtime event"],
  ["TransactionPhase.AFTER_COMMIT", "backend activity notification post-commit delivery"],
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
expectMatch("backend", /findPostsVisibleTo\(String viewerId\)/, "backend feed repository has viewer-specific visibility query");
expectMatch("backend", /p\.visibility = '好友可见'/, "backend feed applies friend visibility rule");
expectMatch("backend", /p\.visibility = '仅老师可见'/, "backend feed applies teacher visibility rule");
expectMatch("backend", /feed\(authTokenService\.requireUserId\(authorization\)\)/, "backend feed resolves the token user");
expectMatch("backend", /requireFriendship\(currentUserId, peerId\);/, "backend chat service requires friendship");
expectMatch("backend", /friendRepository\.areFriends\(currentUserId, peerId\)/, "backend chat checks friendship repository");

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
  ["visibility varchar(20)", "MySQL post visibility schema"],
  ["insert into users", "MySQL demo seed data"],
  ["m-demo-post-9001", "MySQL demo pending moderation seed"],
  ["u-2004", "MySQL demo club leader account"],
  ["create table if not exists activity_notifications", "MySQL activity notification schema"],
  ["on duplicate key update", "idempotent MySQL demo seed data"]
].forEach(([needle, label]) => expectIncludes("resources", needle, label));

if (failures.length > 0) {
  console.error("Frontend smoke test failed:");
  failures.forEach((failure) => console.error(`- ${failure}`));
  process.exit(1);
}

console.log("Frontend smoke test passed.");
