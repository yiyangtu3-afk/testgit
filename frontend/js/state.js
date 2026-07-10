export const API_BASE = "http://127.0.0.1:8080/api";

export const state = {
  apiMode: "mock",
  realtimeMode: "offline",
  verificationCode: "",
  token: "",
  currentUser: {
    id: "u-1001",
    name: "林一",
    role: "学生账号",
    phone: "13800000001",
    presence: "online"
  },
  users: [],
  selectedConversation: "u-2001",
  conversations: {},
  unread: {
    "u-2001": 0,
    "u-2002": 2,
    "u-2003": 1
  },
  friendRequests: {},
  friendRequestItems: [],
  friends: [],
  pendingAttachments: [],
  posts: [],
  personalPosts: [],
  personalPostManagerOpen: false,
  editingPersonalPostId: null,
  feedNotice: null,
  postComments: {},
  expandedPostId: null,
  metrics: {},
  moderationItems: [],
  moderationFilter: "all",
  selectedModerationIds: new Set(),
  reviewingModerationId: "",
  selectedAuditEventIds: new Set(),
  adminNotice: null,
  reportRange: "today",
  reportExport: null,
  auditEvents: []
};

export const reportRanges = {
  today: { label: "今日", auditLimit: 4 },
  week: { label: "本周", auditLimit: 8 },
  all: { label: "全部", auditLimit: Number.POSITIVE_INFINITY }
};

export const mockStore = {
  codes: {},
  accounts: [
    { id: "u-1001", name: "林一", role: "学生账号", phone: "13800000001", status: "online" },
    { id: "u-2001", name: "陈老师", role: "教师账号", phone: "13800000002", status: "online" },
    { id: "u-2002", name: "周同学", role: "学生账号", phone: "13800000003", status: "offline" },
    { id: "u-2003", name: "教务管理员", role: "管理员账号", phone: "13800000004", status: "online" }
  ],
  friendRequests: [
    { id: "fr-1001", fromUserId: "u-2002", toUserId: "u-1001", status: "pending" }
  ],
  friendships: [
    ["u-1001", "u-2001"],
    ["u-1001", "u-2003"]
  ],
  conversations: {
    "u-2001": [
      { id: 1, from: "u-2001", text: "毕设题目可以再收敛一点，先把第一版 demo 跑起来。", time: "09:12", deleted: false, attachments: [] },
      { id: 2, from: "u-1001", text: "收到，我先做登录、好友和单聊。", time: "09:14", deleted: false, attachments: [] }
    ],
    "u-2002": [
      { id: 3, from: "u-2002", text: "晚上一起测试聊天功能？", time: "昨天", deleted: false, attachments: [] }
    ],
    "u-2003": [
      { id: 4, from: "u-2003", text: "后台需要能看到基础统计和审计记录。", time: "周一", deleted: false, attachments: [] }
    ]
  },
  posts: [
    {
      id: 9001,
      authorId: "u-2002",
      author: "周同学",
      body: "今天社团招新摊位安排在图书馆门口，欢迎大家下午来看看。",
      visibility: "全校可见",
      likes: 0,
      comments: 0,
      moderationStatus: "pending",
      moderationReason: "校园动态发布审核"
    },
    {
      id: 1,
      authorId: "u-2001",
      author: "陈老师",
      body: "软件工程课程设计进入验收阶段，请各组准备可运行 demo、架构图和数据库设计。",
      visibility: "好友可见",
      likes: 12,
      comments: 2,
      moderationStatus: "approved",
      moderationReason: "内容符合校园动态规范"
    },
    {
      id: 2,
      authorId: "u-1001",
      author: "林一",
      body: "CampusLink 第一条链路：验证码登录、好友搜索、聊天和动态已经串起来。",
      visibility: "全校可见",
      likes: 5,
      comments: 1,
      moderationStatus: "approved",
      moderationReason: "内容符合校园动态规范"
    },
    {
      id: 3,
      authorId: "u-2001",
      author: "陈老师",
      body: "教师研讨会将在周五下午举行，相关老师可在教研室确认材料。",
      visibility: "仅老师可见",
      likes: 3,
      comments: 0,
      moderationStatus: "approved",
      moderationReason: "内容符合校园动态规范"
    }
  ],
  comments: {
    1: [
      { id: 1, author: "周同学", body: "收到，我把验收材料清单也补上。", time: "09:42", moderationStatus: "approved" },
      { id: 2, author: "林一", body: "demo 已经可以本地验证，晚点同步说明。", time: "09:45", moderationStatus: "approved" }
    ],
    2: [
      { id: 3, author: "陈老师", body: "很好，下一步可以补评论和后台审计链路。", time: "09:51", moderationStatus: "approved" }
    ]
  },
  moderationItems: [
    {
      id: "mod-demo-post-9001",
      type: "post",
      targetId: 9001,
      postId: null,
      title: "动态：今天社团招新摊位安排在图书馆门口",
      author: "周同学",
      body: "今天社团招新摊位安排在图书馆门口，欢迎大家下午来看看。",
      status: "pending",
      reason: "校园动态发布审核",
      submittedAt: "2026-07-06 09:20",
      time: "09:20"
    }
  ],
  auditEvents: [
    { id: "a-mock-1", time: "09:18", module: "用户", event: "13800000001 登录成功" },
    { id: "a-mock-2", time: "09:24", module: "聊天", event: "林一向陈老师发送消息" },
    { id: "a-mock-3", time: "09:31", module: "动态", event: "新增一条好友可见动态" }
  ]
};
