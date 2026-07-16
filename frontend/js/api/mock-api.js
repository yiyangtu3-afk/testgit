import { mockStore, reportRanges, state } from "../state.js";
import { nowTime } from "../utils/dom.js?v=20260715-comment-notifications-v1";

let mockAuditId = Date.now();
let mockActivityId = Date.now();
let mockActivityNotificationId = Date.now();
let mockSocialNotificationId = Date.now();

export function accountById(userId) {
  return mockStore.accounts.find((account) => account.id === userId);
}

function normalizeFriendRequest(request) {
  const incoming = request.toUserId === state.currentUser.id;
  const userId = incoming ? request.fromUserId : request.toUserId;
  return {
    id: request.id,
    fromUserId: request.fromUserId,
    toUserId: request.toUserId,
    userId,
    direction: incoming ? "incoming" : "outgoing",
    status: request.status,
    user: accountById(userId)
  };
}

function areFriends(firstUserId, secondUserId) {
  return mockStore.friendships.some((pair) => {
    return pair.includes(firstUserId) && pair.includes(secondUserId);
  });
}

function canViewPost(post) {
  if (post.moderationStatus !== "approved") {
    return false;
  }
  if (post.visibility === "全校可见" || post.authorId === state.currentUser.id) {
    return true;
  }
  if (post.visibility === "好友可见") {
    return areFriends(post.authorId, state.currentUser.id);
  }
  return post.visibility === "仅老师可见" && state.currentUser.role.includes("教师");
}

function requireFriendship(peerId) {
  if (!areFriends(state.currentUser.id, peerId)) {
    throw new Error("仅能与已建立好友关系的用户聊天。");
  }
}

function conversationReadKey(peerId) {
  return `${state.currentUser.id}:${peerId}`;
}

function addFriendship(firstUserId, secondUserId) {
  if (!areFriends(firstUserId, secondUserId)) {
    mockStore.friendships.push([firstUserId, secondUserId]);
  }
}

export function pushMockAudit(module, event) {
  mockStore.auditEvents.unshift({ id: `a-mock-${++mockAuditId}`, time: nowTime(), module, event });
}

function moderationTitle(type, body) {
  const label = type === "post" ? "动态" : "评论";
  return `${label}：${String(body || "").slice(0, 24) || "待审核内容"}`;
}

function submittedAt() {
  return new Date()
    .toLocaleString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false
    })
    .replaceAll("/", "-");
}

function pushMockActivityNotification(recipientId, activity, type, title, body) {
  mockStore.activityNotifications.unshift({
    id: `notification-mock-${++mockActivityNotificationId}`,
    recipientId,
    activityId: activity.id,
    type,
    title,
    body,
    read: false,
    createdAt: new Date().toISOString()
  });
}

function postLikeKey(postId, userId = state.currentUser.id) {
  return `${postId}:${userId}`;
}

function withCurrentUserLike(post) {
  return {
    ...post,
    likedByCurrentUser: mockStore.postLikes.includes(postLikeKey(post.id))
  };
}

function pushMockPostLikeNotification(post) {
  mockStore.socialNotifications.unshift({
    id: `social-notification-mock-${++mockSocialNotificationId}`,
    recipientId: post.authorId,
    targetId: String(post.id),
    type: "social.post.liked",
    title: "动态收到新点赞",
    body: `${state.currentUser.name}赞了你的动态。`,
    read: false,
    createdAt: new Date().toISOString()
  });
}

function pushMockPostCommentNotification(post, comment) {
  const preview = String(comment.body || "").replace(/\s+/g, " ").trim();
  mockStore.socialNotifications.unshift({
    id: `social-notification-mock-${++mockSocialNotificationId}`,
    recipientId: post.authorId,
    targetId: String(comment.id),
    type: "social.post.commented",
    title: "动态收到新评论",
    body: `${comment.author}评论了你的动态：${preview.length <= 60 ? preview : `${preview.slice(0, 60)}…`}`,
    read: false,
    createdAt: new Date().toISOString()
  });
}

function pushMockFriendRequestNotification(recipientId, actor, request, type) {
  const accepted = type === "social.friend.accepted";
  const requested = type === "social.friend.requested";
  mockStore.socialNotifications.unshift({
    id: `social-notification-mock-${++mockSocialNotificationId}`,
    recipientId,
    targetId: request.id,
    type,
    title: requested ? "新的好友申请" : accepted ? "好友申请已同意" : "好友申请未通过",
    body: requested
      ? `${actor.name}向你发送了好友申请。`
      : `${actor.name}${accepted ? "已同意" : "拒绝"}你的好友申请。`,
    read: false,
    createdAt: new Date().toISOString()
  });
}

function requireOwnedMockActivity(activityId) {
  const role = state.currentUser.role || "";
  if (!role.includes("教师") && !role.includes("社团负责人")) {
    throw new Error("只有教师或社团负责人可以管理活动");
  }
  const activity = mockStore.activities.find((item) => item.id === activityId);
  if (!activity) throw new Error("活动不存在");
  if (activity.organizerId !== state.currentUser.id) throw new Error("只能管理自己创建的活动");
  return activity;
}

export const mockApi = {
  async createCode(phone) {
    const code = String(Math.floor(100000 + Math.random() * 900000));
    mockStore.codes[phone] = code;
    return { phone, code };
  },
  async login(phone, code) {
    if (mockStore.codes[phone] !== code) {
      throw new Error("验证码不正确");
    }
    pushMockAudit("用户", `${phone} 登录成功`);
    return {
      token: "mock-jwt-token",
      user: { id: "u-1001", name: "林一", role: "学生账号", phone }
    };
  },
  async loginAsDemo(userId = "u-1001") {
    const account = accountById(userId) || accountById("u-1001");
    pushMockAudit("用户", `${account.phone} 快速进入演示`);
    return {
      token: `demo-placeholder-token-${account.id}`,
      user: { id: account.id, name: account.name, role: account.role, phone: account.phone }
    };
  },
  async users(keyword = "") {
    const normalized = keyword.trim().toLowerCase();
    return mockStore.accounts.filter((user) => {
      return (
        user.id !== state.currentUser.id &&
        normalized === "" ||
        (user.id !== state.currentUser.id && user.name.toLowerCase().includes(normalized)) ||
        (user.id !== state.currentUser.id && user.phone.includes(normalized))
      );
    });
  },
  async sendFriendRequest(userId) {
    if (areFriends(state.currentUser.id, userId)) {
      return { userId, status: "accepted" };
    }
    state.friendRequests[userId] = "pending";
    const existing = mockStore.friendRequests.find((item) => {
      return item.fromUserId === state.currentUser.id && item.toUserId === userId;
    });
    const request = existing || {
      id: `fr-${Date.now()}`,
      fromUserId: state.currentUser.id,
      toUserId: userId,
      status: "pending"
    };
    if (existing) {
      existing.status = "pending";
    } else {
      mockStore.friendRequests.unshift(request);
    }
    const user = accountById(userId);
    pushMockFriendRequestNotification(userId, state.currentUser, request, "social.friend.requested");
    pushMockAudit("好友", `${state.currentUser.name}向${user ? user.name : userId}发送好友申请`);
    return { userId, status: "pending" };
  },
  async friendRequests() {
    return mockStore.friendRequests
      .filter((request) => {
        return request.fromUserId === state.currentUser.id || request.toUserId === state.currentUser.id;
      })
      .map(normalizeFriendRequest);
  },
  async resolveFriendRequest(requestId, decision) {
    const request = mockStore.friendRequests.find((item) => item.id === requestId);
    if (!request) {
      throw new Error("好友申请不存在");
    }
    if (request.toUserId !== state.currentUser.id) {
      throw new Error("只能处理发给当前账号的好友申请");
    }
    request.status = decision === "accept" ? "accepted" : "rejected";
    const requester = accountById(request.fromUserId);
    state.friendRequests[request.fromUserId] = request.status;
    if (request.status === "accepted") {
      addFriendship(request.fromUserId, request.toUserId);
      const acceptedMessage = {
        id: Date.now(),
        from: state.currentUser.id,
        text: "我们已经是好友了，之后可以直接在这里沟通。",
        time: nowTime(),
        deleted: false,
        attachments: []
      };
      mockStore.conversations[request.fromUserId] = mockStore.conversations[request.fromUserId] || [];
      mockStore.conversations[request.toUserId] = mockStore.conversations[request.toUserId] || [];
      mockStore.conversations[request.fromUserId].push(acceptedMessage);
      mockStore.conversations[request.toUserId].push(acceptedMessage);
    }
    pushMockFriendRequestNotification(
      request.fromUserId,
      state.currentUser,
      request,
      request.status === "accepted" ? "social.friend.accepted" : "social.friend.rejected"
    );
    pushMockAudit("好友", `${state.currentUser.name}${request.status === "accepted" ? "同意" : "拒绝"}${requester ? requester.name : request.fromUserId}的好友申请`);
    return normalizeFriendRequest(request);
  },
  async friends() {
    return mockStore.friendships
      .filter((pair) => pair.includes(state.currentUser.id))
      .map((pair) => {
        const friendId = pair.find((userId) => userId !== state.currentUser.id);
        return accountById(friendId);
      })
      .filter(Boolean);
  },
  async messages(peerId, beforeId = null, limit = 30) {
    requireFriendship(peerId);
    const pageSize = Math.max(1, Math.min(limit, 50));
    const newestFirst = [...(mockStore.conversations[peerId] || [])]
      .filter((message) => beforeId === null || message.id < beforeId)
      .sort((first, second) => second.id - first.id);
    const page = newestFirst.slice(0, pageSize + 1);
    const hasMore = page.length > pageSize;
    const messages = page.slice(0, pageSize);
    const nextBeforeId = hasMore ? messages.at(-1).id : null;
    if (beforeId === null && messages[0]) {
      mockStore.conversationReads[conversationReadKey(peerId)] = messages[0].id;
    }
    return {
      messages: messages.reverse(),
      hasMore,
      nextBeforeId
    };
  },
  async unreadCounts() {
    const counts = {};
    const friends = await this.friends();
    friends.forEach((friend) => {
      const lastReadMessageId = mockStore.conversationReads[conversationReadKey(friend.id)] || 0;
      const count = (mockStore.conversations[friend.id] || []).filter((message) => {
        return message.from === friend.id && !message.deleted && message.id > lastReadMessageId;
      }).length;
      if (count > 0) {
        counts[friend.id] = count;
      }
    });
    return { counts };
  },
  async conversationPreviews() {
    const previews = {};
    const friends = await this.friends();
    friends.forEach((friend) => {
      const messages = mockStore.conversations[friend.id] || [];
      if (messages.length > 0) {
        previews[friend.id] = messages.at(-1);
      }
    });
    return { previews };
  },
  async sendMessage(peerId, text, attachments = []) {
    requireFriendship(peerId);
    const message = {
      id: Date.now(),
      from: state.currentUser.id,
      text,
      time: nowTime(),
      deleted: false,
      attachments
    };
    mockStore.conversations[peerId].push(message);
    const peer = accountById(peerId);
    const attachmentCopy = attachments.length ? `，包含 ${attachments.length} 个附件` : "";
    pushMockAudit("聊天", `${state.currentUser.name}向${peer ? peer.name : peerId}发送消息${attachmentCopy}`);
    return message;
  },
  async withdrawMessage(peerId, messageId) {
    requireFriendship(peerId);
    const message = (mockStore.conversations[peerId] || []).find((item) => item.id === messageId);
    if (message) {
      message.deleted = true;
      pushMockAudit("聊天", `林一撤回消息 ${messageId}`);
    }
    return message;
  },
  async updatePresence(presence) {
    state.currentUser.presence = presence;
    pushMockAudit("用户", `${state.currentUser.name}切换为${presence === "invisible" ? "隐身" : "在线"}`);
    return { presence };
  },
  async feed() {
    return mockStore.posts.filter(canViewPost).map(withCurrentUserLike);
  },
  async publishPost(body, visibility) {
    const post = {
      id: Date.now(),
      authorId: state.currentUser.id,
      author: state.currentUser.name,
      body,
      visibility,
      likes: 0,
      comments: 0,
      moderationStatus: "pending",
      moderationReason: "校园动态发布审核"
    };
    mockStore.posts.unshift(post);
    mockStore.moderationItems.unshift({
      id: `mod-${Date.now()}`,
      type: "post",
      targetId: post.id,
      title: moderationTitle("post", post.body),
      author: post.author,
      body: post.body,
      status: "pending",
      reason: "校园动态发布审核",
      submittedAt: submittedAt(),
      time: nowTime()
    });
    pushMockAudit("动态", `${state.currentUser.name}发布一条${visibility}动态`);
    return post;
  },
  async personalPosts() {
    return mockStore.posts.filter((post) => {
      return post.authorId === state.currentUser.id || (!post.authorId && post.author === state.currentUser.name);
    }).map(withCurrentUserLike);
  },
  async updatePersonalPost(postId, body) {
    const post = mockStore.posts.find((item) => {
      return item.id === postId && (item.authorId === state.currentUser.id || item.author === state.currentUser.name);
    });
    if (!post) {
      throw new Error("只能编辑自己的动态");
    }
    post.body = body;
    post.moderationStatus = "pending";
    post.moderationReason = "个人动态编辑审核";
    mockStore.moderationItems.unshift({
      id: `mod-${Date.now()}`,
      type: "post",
      targetId: post.id,
      title: moderationTitle("post", post.body),
      author: post.author,
      body: post.body,
      status: "pending",
      reason: "个人动态编辑审核",
      submittedAt: submittedAt(),
      time: nowTime()
    });
    pushMockAudit("动态", `${state.currentUser.name}编辑自己的动态`);
    return post;
  },
  async deletePersonalPost(postId) {
    const before = mockStore.posts.length;
    mockStore.posts = mockStore.posts.filter((post) => {
      const ownPost = post.id === postId && (post.authorId === state.currentUser.id || post.author === state.currentUser.name);
      return !ownPost;
    });
    const deleted = before !== mockStore.posts.length;
    if (!deleted) {
      throw new Error("只能删除自己的动态");
    }
    delete mockStore.comments[postId];
    mockStore.moderationItems = mockStore.moderationItems.filter((item) => {
      return !(item.type === "post" && item.targetId === postId) && item.postId !== postId;
    });
    pushMockAudit("动态", `${state.currentUser.name}删除自己的动态`);
    return { deleted };
  },
  async likePost(postId) {
    const post = mockStore.posts.find((item) => item.id === postId);
    if (post) {
      const key = postLikeKey(postId);
      const existingIndex = mockStore.postLikes.indexOf(key);
      const liked = existingIndex === -1;
      if (liked) {
        mockStore.postLikes.push(key);
        post.likes += 1;
        if (post.authorId && post.authorId !== state.currentUser.id) {
          pushMockPostLikeNotification(post);
        }
      } else {
        mockStore.postLikes.splice(existingIndex, 1);
        post.likes = Math.max(0, post.likes - 1);
      }
      pushMockAudit("动态", `${state.currentUser.name}${liked ? "点赞" : "取消点赞"}${post.author}的动态`);
    }
    return post ? withCurrentUserLike(post) : null;
  },
  async comments(postId) {
    return (mockStore.comments[postId] || []).filter((comment) => {
      return comment.moderationStatus === "approved";
    });
  },
  async publishComment(postId, body) {
    const post = mockStore.posts.find((item) => item.id === postId);
    if (!post) {
      throw new Error("动态不存在");
    }
    const comment = {
      id: Date.now(),
      author: state.currentUser.name,
      body,
      time: nowTime(),
      moderationStatus: "pending"
    };
    mockStore.comments[postId] = [comment, ...(mockStore.comments[postId] || [])];
    mockStore.moderationItems.unshift({
      id: `mod-${Date.now()}`,
      type: "comment",
      targetId: comment.id,
      postId,
      title: moderationTitle("comment", comment.body),
      author: comment.author,
      body: comment.body,
      status: "pending",
      reason: "动态评论发布审核",
      submittedAt: submittedAt(),
      time: comment.time
    });
    if (post.authorId && post.authorId !== state.currentUser.id) {
      pushMockPostCommentNotification(post, comment);
    }
    pushMockAudit("动态", `${state.currentUser.name}评论${post.author}的动态`);
    return comment;
  },
  async activities(filters = {}) {
    const category = String(filters.category || "").trim();
    const from = String(filters.from || "").trim();
    const to = String(filters.to || "").trim();
    if (from && to && to < from) {
      throw new Error("活动筛选结束日期不能早于开始日期");
    }
    return mockStore.activities.filter((activity) => {
      const startsOn = String(activity.startsAt || "").slice(0, 10);
      return ["published", "full"].includes(activity.status)
        && (!category || activity.category === category)
        && (!from || startsOn >= from)
        && (!to || startsOn <= to);
    });
  },
  async createActivity(activity) {
    const role = state.currentUser.role || "";
    if (!role.includes("教师") && !role.includes("社团负责人")) {
      throw new Error("只有教师或社团负责人可以创建活动");
    }
    const created = {
      id: `activity-mock-${++mockActivityId}`,
      ...activity,
      capacity: Number(activity.capacity),
      organizerId: state.currentUser.id,
      organizerName: state.currentUser.name,
      status: "pending",
      reviewDecision: "pending",
      reviewReason: null,
      reviewerId: null,
      reviewerName: null,
      reviewedAt: null,
      createdAt: new Date().toISOString()
    };
    mockStore.activities.unshift(created);
    pushMockAudit("活动", `${state.currentUser.name}提交活动“${created.title}”`);
    return created;
  },
  async managedActivities() {
    const role = state.currentUser.role || "";
    if (!role.includes("教师") && !role.includes("社团负责人")) {
      throw new Error("只有教师或社团负责人可以管理活动");
    }
    return mockStore.activities.filter((activity) => activity.organizerId === state.currentUser.id);
  },
  async activityRoster(activityId) {
    const activity = requireOwnedMockActivity(activityId);
    let waitlistPosition = 0;
    const entries = Object.values(mockStore.activityRegistrations)
      .filter((item) => item.activityId === activityId && item.status !== "cancelled")
      .sort((first, second) => {
        if (first.status === "waitlisted" && second.status !== "waitlisted") return 1;
        if (first.status !== "waitlisted" && second.status === "waitlisted") return -1;
        return String(first.waitlistedAt || first.registeredAt).localeCompare(
          String(second.waitlistedAt || second.registeredAt)
        );
      })
      .map((item) => {
        const attendee = accountById(item.attendeeId);
        return {
          registrationId: item.id,
          attendeeId: item.attendeeId,
          attendeeName: attendee?.name || item.attendeeId,
          status: item.status,
          queuePosition: item.status === "waitlisted" ? ++waitlistPosition : 0,
          registeredAt: item.registeredAt || null,
          waitlistedAt: item.waitlistedAt || null,
          checkedInAt: item.checkedInAt || null
        };
      });
    return {
      activityId,
      title: activity.title,
      capacity: Number(activity.capacity),
      registeredCount: entries.filter((item) => item.status === "registered").length,
      waitlistedCount: entries.filter((item) => item.status === "waitlisted").length,
      checkedInCount: entries.filter((item) => item.status === "checked_in").length,
      entries
    };
  },
  async checkInActivityRegistration(activityId, registrationId) {
    requireOwnedMockActivity(activityId);
    const registration = Object.values(mockStore.activityRegistrations)
      .find((item) => item.activityId === activityId && item.id === registrationId);
    if (!registration) throw new Error("报名记录不存在");
    if (registration.status === "checked_in") throw new Error("该参与者已签到");
    if (registration.status !== "registered") throw new Error("只有已报名参与者可以签到");
    registration.status = "checked_in";
    registration.checkedInAt = new Date().toISOString();
    pushMockAudit("活动", `${state.currentUser.name}完成活动现场签到`);
    const roster = await this.activityRoster(activityId);
    return roster.entries.find((item) => item.registrationId === registrationId);
  },
  async activityRegistration(activityId) {
    return mockStore.activityRegistrations[`${activityId}:${state.currentUser.id}`] || null;
  },
  async registerActivity(activityId) {
    if (!(state.currentUser.role || "").includes("学生")) throw new Error("只有学生可以报名活动");
    const activity = mockStore.activities.find((item) => item.id === activityId);
    if (!activity || !["published", "full"].includes(activity.status)) throw new Error("当前活动暂不接受报名");
    const key = `${activityId}:${state.currentUser.id}`;
    const existing = mockStore.activityRegistrations[key];
    if (existing && ["registered", "waitlisted"].includes(existing.status)) throw new Error("你已报名该活动");
    const active = Object.values(mockStore.activityRegistrations).filter((item) => {
      return item.activityId === activityId && ["registered", "checked_in"].includes(item.status);
    });
    const status = active.length < Number(activity.capacity) ? "registered" : "waitlisted";
    const registration = { id: existing?.id || `mock-registration-${Date.now()}`, activityId,
      attendeeId: state.currentUser.id,
      status, queuePosition: status === "waitlisted" ? 1 : 0,
      registeredAt: status === "registered" ? new Date().toISOString() : null,
      waitlistedAt: status === "waitlisted" ? new Date().toISOString() : null };
    mockStore.activityRegistrations[key] = registration;
    if (status === "registered" && active.length + 1 >= Number(activity.capacity)) activity.status = "full";
    pushMockActivityNotification(
      state.currentUser.id,
      activity,
      `activity.registration.${status}`,
      status === "registered" ? "活动报名成功" : "已加入活动候补",
      status === "registered"
        ? `你已成功报名“${activity.title}”。`
        : `“${activity.title}”当前已满，你位于候补第 ${registration.queuePosition} 位。`
    );
    return { ...registration };
  },
  async cancelActivityRegistration(activityId) {
    const key = `${activityId}:${state.currentUser.id}`;
    const registration = mockStore.activityRegistrations[key];
    if (!registration || registration.status === "cancelled") throw new Error("没有可取消的报名记录");
    if (registration.status === "checked_in") throw new Error("已签到的报名不能取消");
    const previous = registration.status;
    registration.status = "cancelled";
    if (previous === "registered") {
      const next = Object.values(mockStore.activityRegistrations).find((item) => {
        return item.activityId === activityId && item.status === "waitlisted";
      });
      if (next) {
        next.status = "registered";
        next.queuePosition = 0;
        const activity = mockStore.activities.find((item) => item.id === activityId);
        if (activity) {
          pushMockActivityNotification(
            next.attendeeId,
            activity,
            "activity.registration.promoted",
            "候补已递补",
            `“${activity.title}”已释放名额，你已获得活动名额。`
          );
        }
      }
      else {
        const activity = mockStore.activities.find((item) => item.id === activityId);
        if (activity) activity.status = "published";
      }
    }
    return { ...registration };
  },
  async pendingActivities() {
    if (!(state.currentUser.role || "").includes("管理员")) {
      throw new Error("需要管理员账号审核活动");
    }
    return mockStore.activities.filter((activity) => {
      return activity.status === "pending" && activity.reviewDecision === "pending";
    });
  },
  async reviewActivity(activityId, decision, reason = null) {
    if (!(state.currentUser.role || "").includes("管理员")) {
      throw new Error("需要管理员账号审核活动");
    }
    const activity = mockStore.activities.find((item) => item.id === activityId);
    if (!activity || activity.status !== "pending" || activity.reviewDecision !== "pending") {
      throw new Error("活动已完成审核");
    }
    if (decision === "reject" && !String(reason || "").trim()) {
      throw new Error("拒绝活动时必须填写原因");
    }
    activity.status = decision === "approve" ? "published" : "draft";
    activity.reviewDecision = decision === "approve" ? "approved" : "rejected";
    activity.reviewReason = decision === "reject" ? String(reason).trim() : null;
    activity.reviewerId = state.currentUser.id;
    activity.reviewerName = state.currentUser.name;
    activity.reviewedAt = new Date().toISOString();
    pushMockActivityNotification(
      activity.organizerId,
      activity,
      decision === "approve" ? "activity.review.approved" : "activity.review.rejected",
      decision === "approve" ? "活动已发布" : "活动审核未通过",
      decision === "approve"
        ? `你提交的“${activity.title}”已通过审核并公开发布。`
        : `你提交的“${activity.title}”未通过审核。原因：${activity.reviewReason}`
    );
    pushMockAudit(
      "活动",
      `${state.currentUser.name}${decision === "approve" ? "通过" : "拒绝"}活动“${activity.title}”`
    );
    return { ...activity };
  },
  async activityNotifications() {
    const items = mockStore.activityNotifications
      .filter((notification) => notification.recipientId === state.currentUser.id)
      .map(({ recipientId, ...notification }) => ({ ...notification }));
    return {
      items,
      unreadCount: items.filter((notification) => !notification.read).length
    };
  },
  async markAllActivityNotificationsRead() {
    mockStore.activityNotifications.forEach((notification) => {
      if (notification.recipientId === state.currentUser.id) notification.read = true;
    });
    return this.activityNotifications();
  },
  async socialNotifications() {
    const items = mockStore.socialNotifications
      .filter((notification) => notification.recipientId === state.currentUser.id)
      .map(({ recipientId, ...notification }) => ({ ...notification }));
    return {
      items,
      unreadCount: items.filter((notification) => !notification.read).length
    };
  },
  async markAllSocialNotificationsRead() {
    mockStore.socialNotifications.forEach((notification) => {
      if (notification.recipientId === state.currentUser.id) notification.read = true;
    });
    return this.socialNotifications();
  },
  async metrics() {
    const pendingModeration = mockStore.moderationItems.filter((item) => item.status === "pending").length;
    return {
      注册用户: "128",
      今日消息: "436",
      动态总数: String(mockStore.posts.length),
      待审内容: String(pendingModeration)
    };
  },
  async activityMetrics() {
    if (!(state.currentUser.role || "").includes("管理员")) {
      throw new Error("需要管理员账号查看活动指标");
    }
    const registrations = Object.values(mockStore.activityRegistrations);
    return {
      registrationCount: registrations.filter((item) => ["registered", "checked_in"].includes(item.status)).length,
      checkedInCount: registrations.filter((item) => item.status === "checked_in").length
    };
  },
  async moderationItems() {
    return mockStore.moderationItems.filter((item) => item.status === "pending");
  },
  async resolveModeration(itemId, decision) {
    const item = mockStore.moderationItems.find((entry) => entry.id === itemId);
    if (!item) {
      throw new Error("审核记录不存在");
    }
    item.status = decision === "approve" ? "approved" : "rejected";
    if (item.type === "post") {
      const post = mockStore.posts.find((entry) => entry.id === item.targetId);
      if (post) {
        post.moderationStatus = item.status;
        post.moderationReason = item.status === "approved" ? "内容符合校园动态规范" : item.reason || "内容不符合校园动态规范";
      }
    } else {
      const comments = mockStore.comments[item.postId] || [];
      const comment = comments.find((entry) => entry.id === item.targetId);
      if (comment) comment.moderationStatus = item.status;
      if (item.status === "approved") {
        const post = mockStore.posts.find((entry) => entry.id === item.postId);
        if (post) post.comments += 1;
      }
    }
    pushMockAudit("审核", `${state.currentUser.name}${item.status === "approved" ? "通过" : "拒绝"}${item.author}的${item.type === "post" ? "动态" : "评论"}`);
    return item;
  },
  async deleteModerationItems(itemIds = []) {
    const selected = new Set(itemIds);
    const before = mockStore.moderationItems.length;
    mockStore.moderationItems = mockStore.moderationItems.filter((item) => !selected.has(item.id));
    const deleted = before - mockStore.moderationItems.length;
    if (deleted > 0) {
      pushMockAudit("审核", `${state.currentUser.name}删除${deleted}条审核记录`);
    }
    return { deleted };
  },
  async adminReport() {
    const range = reportRanges[state.reportRange] ? state.reportRange : "today";
    const rangeConfig = reportRanges[range];
    const metrics = await this.metrics();
    const pendingModeration = mockStore.moderationItems.filter((item) => item.status === "pending");
    pushMockAudit("后台", `${state.currentUser.name}导出${rangeConfig.label}后台报表`);
    return {
      generatedAt: nowTime(),
      range: {
        key: range,
        label: rangeConfig.label
      },
      fileName: `campuslink-admin-report-${range}-${new Date().toISOString().slice(0, 10)}.csv`,
      metrics,
      moderation: pendingModeration,
      auditEvents: mockStore.auditEvents.slice(0, rangeConfig.auditLimit)
    };
  },
  async auditEvents() {
    return mockStore.auditEvents;
  },
  async deleteAuditEvents(eventIds = []) {
    const selected = new Set(eventIds);
    const before = mockStore.auditEvents.length;
    mockStore.auditEvents = mockStore.auditEvents.filter((item) => !selected.has(item.id));
    return { deleted: before - mockStore.auditEvents.length };
  }
};
