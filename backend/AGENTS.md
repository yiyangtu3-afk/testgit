# backend/AGENTS.md

# CampusLink 后端开发指南

本文只描述 `backend/` 目录的 Spring Boot 开发约定。通用项目原则见根
目录 `AGENTS.md`。

---

# 技术栈

后端是 Spring Boot Demo API：

* Java 21。
* Maven。
* Spring Boot。
* MyBatis mapper-backed persistence。
* MySQL 本地 Demo 数据库。
* JUnit 和 MockMvc 测试。

---

# 项目结构

主要包结构如下：

```text
backend/src/main/java/com/campuslink/
├── config/
├── controller/
├── dto/
├── entity/
├── mapper/
├── repository/
└── service/
```

测试代码放在 `backend/src/test/java/com/campuslink/` 下，并按被测层级放入
对应包。

---

# 分层架构

后端必须保持清晰分层：

* `controller/` 只处理 HTTP 请求、响应和边界校验入口。
* `service/` 编写业务逻辑和事务性流程。
* `repository/` 定义数据访问接口，并封装持久化实现。
* `mapper/` 只定义 MyBatis 数据映射。
* `entity/` 存放内部领域或数据记录。
* `dto/` 存放 API 请求和响应模型。
* `config/` 存放 CORS、WebSocket、拦截器和异常处理等配置。

禁止：

* Controller 直接访问 Repository 或 Mapper。
* Repository 编写业务逻辑。
* Mapper 承载业务判断。
* 一个类同时承担多个业务领域。

优先使用构造器注入。

---

# API 规范

API 设计应保持当前 Demo 边界一致：

* 路由使用 `/api/...` 前缀。
* 受保护接口从 bearer token 解析当前用户。
* 不信任客户端提交的当前用户 ID。
* 使用 DTO 作为接口模型，不直接暴露内部 Entity。
* 请求失败时返回一致的 HTTP 状态码和错误结构。
* 管理端接口必须校验管理员角色。

修改 API 路径、请求字段或响应字段时，同步更新前端适配、测试和文档。

---

# 数据库规范

数据访问必须通过 Repository 和 MyBatis Mapper 完成：

* `schema.sql` 负责表结构。
* `data.sql` 负责 Demo 种子数据。
* Repository 接口表达业务需要的数据访问能力。
* MyBatis 实现负责 SQL 和实体映射。
* 不在 Service 中拼接 SQL。
* 跨表业务写入必须由 Service 层 `@Transactional` 保护。
* 修改 MyBatis 查询、写入或表结构时，补充真实 MySQL 的集成测试；测试使用
  `@Transactional` 与 `@Rollback`，不得遗留或清理本地历史数据。

修改表结构或种子数据时，必须确认本地 Demo 启动和相关测试仍然通过。

---

# 异常和日志

异常与日志应统一、可诊断：

* 业务拒绝使用明确异常或返回明确错误。
* 全局异常处理放在 `config/`。
* 请求日志保持在拦截器或统一配置中。
* 不在业务代码中输出临时调试日志。
* 日志不得包含敏感令牌或不必要的个人信息。

---

# 测试要求

修改后端后，运行：

```bash
cd backend
mvn test  
```

测试覆盖应匹配改动范围：

* Service 业务规则使用单元测试。
* Controller API 边界使用 MockMvc 测试。
* WebSocket 行为使用 handler 级测试。
* Repository 或数据库行为变化需补充合适的集成验证或现有测试更新。

本机 Microsoft JDK 21 的 Mockito/Byte Buddy 不能可靠地自附加 agent。完整
测试应优先使用显式 agent，并如实报告结果与 JVM 警告：

```bash
/Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn \
  -f backend/pom.xml \
  -DargLine=-javaagent:/Users/linus_k/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.5/byte-buddy-agent-1.17.5.jar \
  test
```

---

# 编码规范

代码应保持小而清晰：

* Java 类接近或超过约 400 行时，优先拆分。
* 方法只表达一个层级的业务意图。
* 命名使用领域语言，避免模糊缩写。
* 新增依赖前先确认标准库或现有项目依赖是否足够。
* 不提交 `target/` 等构建产物。

新增功能时，优先扩展已有领域服务和 DTO；当现有类职责变宽时，先拆分
再实现。
