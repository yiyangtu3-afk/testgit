# frontend/AGENTS.md

# CampusLink 前端开发指南

本文只描述 `frontend/` 目录及根目录静态前端入口的开发约定。通用项目
原则见根目录 `AGENTS.md`。

---

# 技术栈

前端是无需打包的静态浏览器 Demo：

* 使用原生 HTML、CSS 和 ES modules。
* 根目录 `index.html`、`styles.css`、`app.js` 是静态入口。
* 业务 JavaScript 模块放在 `frontend/js/` 下。
* Java API 可用时调用 `http://127.0.0.1:8080/api`；不可用时使用 Mock
  数据。

---

# 目录结构

`frontend/js/` 按职责拆分：

```text
frontend/
└── js/
    ├── admin/
    ├── api/
    ├── auth/
    ├── chat/
    ├── posts/
    ├── ui/
    ├── utils/
    ├── app.js
    ├── app-events.js
    ├── loaders.js
    └── state.js
```

新增代码应放入最贴近职责的目录。不要继续把业务逻辑追加到根目录
`app.js` 或单个大型模块中。

---

# 模块职责

前端代码必须保持职责分离：

* `api/` 负责 live API 适配和 Mock API。
* `auth/` 负责登录、快速进入、退出和账号切换。
* `chat/` 负责聊天渲染和实时消息。
* `posts/` 负责动态和个人动态管理。
* `admin/` 负责管理端渲染和事件。
* `ui/` 负责通用界面、状态和渲染器。
* `utils/` 负责 DOM、鉴权、格式化等纯工具。
* `state.js` 只存放共享状态，不承载业务流程。

公共逻辑必须提取到共享模块，避免复制代码。

---

# UI 和交互规范

界面应保持安静、清晰、适合桌面端 Demo：

* 复用已有 CSS class 和视觉语言。
* 保持登录、侧边栏、聊天、动态和管理视图的交互一致。
* 新增按钮、状态和空态时，必须提供清晰的反馈文本。
* 不让文本溢出、遮挡或破坏现有布局。
* 修改根目录 `index.html` 或 `styles.css` 时，同步检查主要 Demo 流程。

---

# 状态和 API 规范

前端状态和 API 调用必须可替换、可测试：

* API 调用集中在 `api/`，界面模块不得直接散落 `fetch` 调用。
* 受保护 live API 请求必须携带当前 bearer token。
* Mock API 与 live API 的返回结构应保持一致。
* 状态更新集中通过共享状态和 loader 流程完成。
* 不使用不必要的全局变量。

---

# 命名和代码风格

代码应保持简洁、可读、便于搜索：

* 使用清晰的 camelCase 变量和函数名。
* 渲染函数使用 `render...` 命名。
* 事件绑定函数使用 `bind...` 或领域动作命名。
* 查询 DOM 时优先复用 `utils/dom.js` 中的工具。
* JavaScript 文件接近或超过约 300 行时，优先拆分模块。

---

# 构建与测试

修改前端后，优先运行：

```bash
./script/run_frontend_check.sh
```

本地预览 Demo：

```bash
./script/run_frontend_demo.sh
```

也可以使用基础静态服务：

```bash
python3 -m http.server 5174
```

至少保证 `node --check app.js` 通过，并根据改动范围运行前端 smoke test。
