# Smart Office 当前目标与实施步骤

本文档是当前开发路径的入口。`TODO.md` 保留细项和历史记录，本文只描述接下来怎么推进。

## 1. 当前定位

Smart Office 当前已从单体模块化阶段进入 Spring Cloud Alibaba 微服务联调阶段。

后端保留两条线：

* `smart-office-server`：单体业务基线，保留可运行和可回退能力。
* `smart-office-services`：微服务版本，作为后续主线推进。

当前重点不是继续扩展单体，而是把微服务版本核心办公闭环跑通：

```text
登录认证
-> 网关鉴权
-> 组织与用户信息
-> 审批提交
-> 待办生成
-> 审批处理
-> 待办完成
-> 通知生成
-> 考勤、文件、制度文档等业务服务经 gateway 访问
```

AI 模块暂不阻塞主线，后续作为独立增强能力迁移。

## 2. 已完成基础

工程与基础设施：

* Spring Boot 3 + Java 17 后端工程已建立。
* 单体模块化业务基线已完成。
* 微服务父工程和服务模块已建立。
* Docker Compose 已包含 MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、MinIO、XXL-JOB Admin。
* Nacos 配置已拆分为共享配置和服务私有配置。
* MySQL 已拆分为各服务独立数据库。

已迁移微服务：

* `gateway`：路由、JWT 校验、身份 Header 透传。
* `auth-service`：登录、JWT、当前用户、退出。
* `system-service`：用户认证相关用户、角色、用户角色能力。
* `org-service`：公司、部门、岗位、员工能力。
* `approval-service`：审批单、审批记录、审批节点、审批操作基础链路。
* `message-service`：消息、待办、内部命令接口。
* `attendance-service`：考勤规则、打卡、记录、统计。
* `file-service`：文件记录能力。
* `search-service`：制度文档 MySQL 版检索能力。
* `ai-service`：当前仅保留占位。

已验证：

* `mvn -DskipTests compile` 通过。
* `mvn -DskipTests package` 通过。
* `mvn test` 通过。
* Docker 中间件可启动并保持健康。
* gateway 基础冒烟通过：未带 token 返回 `401`，登录成功，带 token 可访问当前用户、考勤、文件记录、制度文档接口。

## 3. 本地约定

Docker 端口当前按 Windows 本机可用端口做了调整：

| 中间件 | 宿主端口 |
| --- | ---: |
| MySQL | 3306 |
| Redis | 6380 |
| RabbitMQ | 5673 |
| RabbitMQ Management | 15672 |
| Nacos HTTP | 8951 |
| Nacos gRPC | 9951 |
| Elasticsearch HTTP | 9200 |
| Elasticsearch transport | 9459 |
| MinIO API | 9000 |
| MinIO Console | 9001 |
| XXL-JOB Admin | 8088 |

本地启动业务服务前建议设置：

```powershell
$env:NACOS_SERVER_ADDR='127.0.0.1:8951'
$env:REDIS_PORT='6380'
$env:RABBITMQ_PORT='5673'
```

当前命令行默认 `java` 可能不是 JDK 17。运行 Spring Boot 3 服务时使用 Java 17 或更高版本，例如：

```powershell
C:\Users\14224\.jdks\ms-17.0.18\bin\java.exe
```

## 4. 2026-06-18 进展快照

P0 的代码层闭环和 gateway 运行时联调已通过，下一步进入 P1：MinIO 文件上传。

本轮完成：

* approval-service 提交审批时创建审批节点、调用 message-service 生成待办和通知。
* approval-service 审批通过、驳回、撤回、关闭时完成待办并更新审批节点。
* approval-service 对 message-service Feign 命令返回值做成功校验，消息链路失败时抛出业务异常。
* smart-office-web 审批中心支持保存草稿、保存并提交、提交草稿、通过、驳回。
* smart-office-web 新增消息中心，支持待办列表、通知列表、标记完成、标记已读。
* 新增 approval-service 单元测试覆盖提交、审批通过和消息命令失败。
* 修复审批内容字段类型：`approval_form.content` 从 `JSON` 调整为 `TEXT`。
* 新增 `scripts/smoke-p0-approval-message.ps1`，固化审批/消息网关冒烟。

本轮已验证：

* `mvn -pl smart-office-services/smart-office-approval-service -am test`
* `mvn test`
* `npm run build`（smart-office-web）
* `git diff --check`
* `powershell -ExecutionPolicy Bypass -File scripts/smoke-p0-approval-message.ps1`

## 5. 下一阶段目标

### P0：微服务主链路联调

已验收：

1. Docker 中间件健康。
2. gateway、auth、system、org、approval、message 健康。
3. 通过 gateway 使用 `employee/123456` 登录并创建审批草稿。
4. 提交审批单，approval-service 调用 message-service 生成审批待办。
5. 通过 gateway 使用 `manager/123456` 登录并处理审批。
6. manager 待办变为 `DONE`，employee 收到 `Approval passed` 通知。
7. 冒烟脚本 `scripts/smoke-p0-approval-message.ps1` 通过。

验收标准：

* 所有调用从 `http://127.0.0.1:8000` 进入。
* 业务服务不依赖前端传入用户 ID。
* 下游服务只信任 gateway 透传的 `X-User-Id`。
* `mvn test`、前端构建和核心 HTTP 冒烟都通过。

### P1：中间件能力补齐

1. file-service 接入 MinIO 真实上传、下载、预览。
2. message-service 接 RabbitMQ 异步通知。
3. attendance-service 接 XXL-JOB 每日结算和月度统计。
4. search-service 接 Elasticsearch 索引同步和全文检索。
5. auth-service 补 Redis Token 存储和退出失效。

验收标准：

* 每个中间件至少有一条可演示业务链路。
* 中间件不可用时，主业务能给出明确错误或降级。
* 文档记录本机启动方式、配置项和验证命令。

### P2：前端切换到微服务

1. 使用 `smart-office-web/.env.microservice` 将 API 代理指向 gateway `8000`。
2. 调整前端接口路径，优先走微服务已迁移接口。
3. 补审批详情、消息中心、文件与制度文档页面。
4. 用 Playwright 做登录、审批、考勤、文件、制度文档冒烟。

验收标准：

* 前端不用直接访问各业务服务端口。
* 登录 token 统一由请求拦截器携带。
* 页面能完整演示核心办公流程。

### P3：AI 独立增强

1. 迁移 AI 会话、消息、提示词模板到 ai-service。
2. 接入 Spring AI。
3. 对制度问答使用 search-service 检索结果作为上下文。
4. 审批摘要、智能填单、风险提示通过独立接口提供。

验收标准：

* AI 调用失败不影响审批、考勤、组织、消息主流程。
* AI 能力可以单独关闭或降级。

## 6. 推荐执行顺序

```text
1. approval/message gateway 运行时联调
2. 前端 microservice 模式 Playwright 冒烟
3. MinIO 文件上传
4. RabbitMQ 消息异步化
5. XXL-JOB 考勤结算
6. Elasticsearch 制度文档检索
7. Redis Token 与退出失效
8. ai-service 独立迁移
```

每完成一项，都需要同步更新：

* `TODO.md`
* `docs/microservices-architecture.md`
* 必要时更新 `README.md`

## 7. 提交前检查

```powershell
git diff --check
mvn -DskipTests compile
mvn test
```

涉及前端或网关链路时，再执行：

```powershell
cd smart-office-web
npm run build
npm run test:e2e
```
