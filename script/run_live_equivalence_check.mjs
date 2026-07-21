const vueBase = "http://127.0.0.1:5180";
const legacyBase = "http://127.0.0.1:8080";
const legacyPage = "http://127.0.0.1:5179/?v=20260715-signed-jwt-logout-v1";

const results = [];
const tokens = [];

async function request(base, path, { token, method = "GET", body } = {}) {
  const response = await fetch(`${base}${path}`, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body ? { "Content-Type": "application/json" } : {})
    },
    ...(body ? { body: JSON.stringify(body) } : {})
  });
  const data = response.status === 204 ? null : await response.json();
  return { status: response.status, data };
}

function record(name, passed, detail = "") {
  results.push({ name, passed, detail });
}

async function compare(name, path, token, options = {}) {
  const [legacy, vue] = await Promise.all([
    request(legacyBase, path, { token, ...options }),
    request(vueBase, path, { token, ...options })
  ]);
  const same = legacy.status === vue.status
    && JSON.stringify(legacy.data) === JSON.stringify(vue.data);
  record(name, same, same ? `${legacy.status}` : `legacy=${legacy.status}, vue=${vue.status}`);
  return legacy;
}

async function login(userId) {
  const response = await request(legacyBase, "/api/auth/demo-login", {
    method: "POST",
    body: { userId }
  });
  if (response.status !== 200 || !response.data?.token) {
    throw new Error(`演示登录失败：${userId}`);
  }
  tokens.push(response.data.token);
  return response.data.token;
}

async function checkStudent(token) {
  await compare("学生公开动态", "/api/feed", token);
  const activities = await compare("学生活动列表", "/api/activities", token);
  await compare("学生活动通知", "/api/activity-notifications", token);
  const social = await compare("学生站内通知", "/api/social-notifications", token);
  await compare("聊天未读数", "/api/conversations/unread-counts", token);
  await compare("聊天预览", "/api/conversations/previews", token);
  const friends = await compare("好友列表", "/api/friends", token);

  const peerId = friends.data?.[0]?.id;
  if (peerId) {
    await compare("聊天消息历史", `/api/conversations/${encodeURIComponent(peerId)}/messages?limit=1`, token);
    await compare(
      "聊天撤回无效消息的安全拒绝",
      `/api/conversations/${encodeURIComponent(peerId)}/messages/0/withdraw`,
      token,
      { method: "POST" }
    );
  } else {
    record("聊天消息历史", true, "跳过：当前学生没有好友");
  }

  const activityId = activities.data?.[0]?.id;
  if (activityId) {
    await compare("学生当前活动报名", `/api/activities/${encodeURIComponent(activityId)}/registrations/current`, token);
  } else {
    record("学生当前活动报名", true, "跳过：没有已发布活动");
  }

  const postNotification = social.data?.items?.find((item) => item.type?.startsWith("social.post."));
  if (postNotification) {
    await compare("动态通知受限跳转", `/api/social-notifications/${encodeURIComponent(postNotification.id)}/post-target`, token);
  } else {
    await compare("动态通知无效目标的安全拒绝", "/api/social-notifications/missing/post-target", token);
  }
}

async function checkTeacher(token) {
  const managed = await compare("组织者活动运营", "/api/activities/managed", token);
  const activityId = managed.data?.[0]?.id;
  if (activityId) {
    await compare("组织者报名名单", `/api/activities/${encodeURIComponent(activityId)}/registrations/roster`, token);
  } else {
    record("组织者报名名单", true, "跳过：当前组织者没有活动");
  }
}

async function checkAdmin(token) {
  await compare("管理员指标", "/api/admin/metrics", token);
  const moderation = await compare("管理员审核历史", "/api/admin/moderation?includeResolved=true", token);
  await compare("管理员审计记录", "/api/admin/audit-events", token);
  const pending = moderation.data?.find((item) => item.status === "pending");
  if (pending) {
    await compare("管理员审核辅助", `/api/admin/moderation/${encodeURIComponent(pending.id)}/assistance`, token);
  } else {
    record("管理员审核辅助", true, "跳过：当前没有待审核内容");
  }
}

async function checkLegacyPage() {
  const response = await fetch(legacyPage);
  const html = await response.text();
  record("旧版静态入口", response.status === 200 && html.includes("app.js"), `${response.status}`);
}

async function logoutAll() {
  await Promise.all(tokens.map((token) => request(legacyBase, "/api/auth/logout", {
    token,
    method: "POST"
  }).catch(() => null)));
}

try {
  await checkLegacyPage();
  const [student, teacher, admin] = await Promise.all([
    login("u-1001"),
    login("u-2001"),
    login("u-2003")
  ]);
  await checkStudent(student);
  await checkTeacher(teacher);
  await checkAdmin(admin);
} finally {
  await logoutAll();
}

for (const result of results) {
  console.log(`${result.passed ? "PASS" : "FAIL"} ${result.name}${result.detail ? ` (${result.detail})` : ""}`);
}
if (results.some((result) => !result.passed)) {
  process.exitCode = 1;
}
