# Smart Office 当前目标与实施步骤

本文档是当前开发路线的唯一入口。`TODO.md` 保留模块细项和历史记录，`docs/microservices-architecture.md` 保留运行结构、端口和联调说明。

## 1. 当前结论

Smart Office 已完成单体模块化基线，当前主线切到 Spring Cloud Alibaba 微服务版联调与功能补齐。

当前约定：

* `smart-office-server` 保留为单体业务基线，只做必要修复，不继续扩展新能力。
* `smart-office-services` 是后续主线，所有新能力优先落到微服务模块。
* `smart-office-web` 通过 gateway 访问后端，不直接连业务服务端口。
* AI 模块已进入独立增强第一阶段，先提供可降级接口，不阻塞审批、考勤、组织、消息等主流程。

## 2. 总目标

先完成一个可演示、可测试、可写进简历的企业办公微服务闭环：

```text
登录认证
-> gateway 鉴权和身份透传
-> 用户、组织基础数据
-> 审批创建和提交
-> 待办生成
-> 审批处理
-> 结果通知
-> 考勤打卡和定时结算
-> 文件上传和下载
-> 制度文档检索
```

非 AI 主流程已完成主链路收尾，AI 办公助手按独立增强方式推进：

```text
制度问答
-> 审批摘要
-> 智能填单
-> 风险提示
```

## 3. 已完成

基础工程：

* Spring Boot 3 + Java 17 后端工程已建立。
* 单体模块化业务基线已完成。
* 微服务父工程、公共模块、API 契约和各业务服务骨架已完成。
* Docker Compose 已包含 MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、MinIO、XXL-JOB Admin。
* Nacos 配置已拆分为共享配置和服务私有配置。
* MySQL 已拆分为各服务独立数据库。
* 前端 `smart-office-web` 已初始化，并切到 gateway 代理模式。

业务与中间件：

* gateway：JWT 校验、路由、身份 Header 透传、外部伪造 Header 清理。
* auth-service：登录、当前用户、退出、JWT 签发、Redis Token 存储和退出失效。
* system-service：用户认证、用户分页、角色和用户角色基础能力。
* org-service：公司、部门、岗位、员工、部门负责人、内部活跃用户列表。
* approval-service：审批草稿、提交、通过、驳回、撤回、关闭、审批节点和记录。
  * 报销金额大于 1000 元时，支持部门负责人审批后继续流转到财务审批。
  * 审批关键状态变更使用数据库乐观锁，重复审批或并发更新会返回明确业务错误。
* message-service：待办、通知、RabbitMQ 异步通知、发送失败同步降级、发送重试、消费幂等。
* attendance-service：考勤规则、打卡、记录、月度统计、XXL-JOB 每日结算和月度统计任务。
  * 每日结算发现缺卡、迟到、早退等异常后，会通过 message-service 通知员工。
* file-service：MinIO 上传、下载、预览 URL、文件记录。
* search-service：制度文档 CRUD、MySQL 模糊检索、Elasticsearch 索引同步和全文检索。
* ai-service：会话/消息落库、降级聊天、制度问答检索、审批摘要、智能填单和风险提示接口。
* smart-office-web：登录、工作台、用户、组织、审批、消息、文件、考勤、制度文档检索、审批详情和考勤记录页面。

已固化脚本：

* `scripts/smoke-p0-approval-message.ps1`：审批和消息网关主链路。
* `scripts/smoke-p1-file-minio.ps1`：文件和 MinIO 链路。
* `scripts/smoke-p1-message-rabbitmq.ps1`：审批通知 RabbitMQ 链路。
* `scripts/smoke-p1-attendance-xxl-job.ps1`：考勤和 XXL-JOB 链路。
* `scripts/smoke-p1-search-elasticsearch.ps1`：制度文档和 Elasticsearch 链路。
* `scripts/smoke-p1-auth-redis-token.ps1`：登录、当前用户、退出和旧 token 拒绝链路。
* `scripts/smoke-p1-approval-rules-concurrency.ps1`：高额报销二级审批和重复审批拒绝链路。
* `scripts/smoke-p1-attendance-message-notice.ps1`：考勤异常通知和重复日结幂等链路。
* `scripts/smoke-p1-ai-service.ps1`：ai-service 聊天、制度问答、审批摘要、智能填单和风险提示链路。
* `smart-office-web` 的 `npm run test:e2e:microservice`：前端 microservice 模式 Playwright 冒烟。

## 4. 当前目标

当前阶段目标是先把非 AI 主业务做成可演示、可写简历的闭环，再继续推进 ai-service 第一阶段运行态验收。

优先级：

1. 补操作审计日志，覆盖登录、角色授权、制度维护、文件删除、审批处理等关键动作。
2. 补公告发布、审批超时扫描、制度上传解析的网关冒烟脚本。
3. 补考勤后端聚合接口，减少前端页面为看板重复拉取数据。
4. 非 AI 主链路稳定后，再执行 ai-service 网关冒烟并进入 Spring AI、模型配置、流式输出和前端 AI 助手页面。

## 5. 实施步骤

### Step 1：Elasticsearch 制度文档检索

已实现：

* search-service 已增加 Elasticsearch 依赖和配置。
* 已建立制度文档索引模型 `smart-office-policy-document`。
* 制度文档新增、修改、删除时同步索引。
* 已增加内部重建索引接口，支持从 MySQL 重新刷 ES。
* 带 `keyword` 的分页搜索优先走 ES；ES 不可用时降级到 MySQL LIKE。
* 已新增 smoke 脚本，覆盖登录、创建制度文档、重建索引、检索命中、清理数据。

验收：

* `[x] mvn -pl smart-office-services/smart-office-search-service -am test`
* `[x] mvn test`
* `[x] git diff --check`
* `[x] scripts/smoke-p1-search-elasticsearch.ps1`

### Step 2：Redis Token 与退出失效

已实现：

* auth-service 登录成功后把 token 写入 Redis，TTL 与 JWT 过期时间一致。
* gateway 校验 JWT 后再校验 Redis 中 token 是否仍有效。
* 退出登录时删除 Redis token。
* 已新增登录、当前用户、退出和旧 token 拒绝冒烟脚本。
* 预留 `AUTH_TOKEN_REDIS_ENABLED` 开关和 token key 前缀配置。

验收：

* `[x] 登录后可正常访问 /api/auth/me`
* `[x] 退出后旧 token 再访问 gateway 返回 401`
* `[x] scripts/smoke-p1-auth-redis-token.ps1`

### Step 3：审批并发与规则补强

已实现：

* 审批通过、驳回、撤回、关闭等关键状态变更接入数据库乐观锁。
* 重复审批、非当前审批人审批、已处理审批再次处理会返回明确业务错误。
* 报销金额大于 1000 元时支持部门负责人加财务审批。
* 高额报销审批通过后，会先进入 `PROCESSING` 状态并生成财务待办，财务审批后才变为 `APPROVED`。

待补：

* 补系统操作审计日志和审计查询页。
* 视后续压测结果决定是否再引入 Redisson 分布式锁。

验收：

* `[x] 重复点击审批不会重复流转。`
* `[x] 非审批人、已处理审批、状态不合法时都有明确业务错误。`
* `[x] 单元测试覆盖重复审批和高额报销二级审批核心场景。`
* `[x] scripts/smoke-p1-approval-rules-concurrency.ps1`

### Step 4：消息可靠性与考勤异常通知

已实现：

* attendance-service 每日结算发现缺卡、迟到、早退等异常后，通过 message-service 发送 `ATTENDANCE` 通知。
* message-service RabbitMQ 通知发送支持有限重试，重试后仍失败则同步落库降级。
* message-service 通知消费按用户、业务类型、业务 ID 和标题做幂等落库，避免 MQ 重投或重复命令生成重复消息。
* 已新增 smoke 脚本，覆盖日结生成缺卡记录、员工收到考勤异常通知、重复日结通知仍保持单条。

验收：

* `[x] mvn -pl smart-office-services/smart-office-message-service,smart-office-services/smart-office-attendance-service -am test`
* `[x] mvn test`
* `[x] scripts/smoke-p1-attendance-message-notice.ps1`

### Step 5：前端体验补齐

已实现：

* 制度文档管理和检索页面。
* 审批详情抽屉、审批时间线和二级审批 `PROCESSING` 状态展示。
* 我的考勤、部门考勤、月度统计和考勤记录页面。
* Playwright microservice 冒烟扩展到制度检索、审批详情、考勤记录。

已验收：

* 页面仍只访问 gateway。
* `npm run build` 通过。
* `npm run test:e2e:microservice` 通过。

验收：

* `[x] 页面能完整演示登录、审批、消息、文件、考勤、制度搜索。`
* `[x] npm run build`
* `[x] npm run test:e2e:microservice`
* `[x] git diff --check`

### Step 6：AI 独立增强

已实现：

* 迁移 AI 会话、消息、提示词模板基础结构到 ai-service，并连接 `smart_office_ai` 独立库。
* 新增 `/api/ai/chat`，当前保存用户消息和助手降级回复。
* 新增 `/api/ai/policy-qa`，通过 search-service 内部检索制度文档并返回引用来源。
* 新增 `/api/ai/approval-summary`、`/api/ai/approval-draft`、`/api/ai/approval-risk`，当前使用规则降级生成摘要、草稿和风险提示。
* ai-service 继续保持独立增强，不作为审批、考勤、组织、消息等主流程的强依赖。

待验收：

* 通过 gateway 登录后调用 ai-service 接口，验证身份透传、落库和降级响应。

下一阶段：

* 接入 Spring AI。
* 配置真实大模型 API。
* 封装 AI Chat Client。
* 实现流式输出接口。
* 增加前端 AI 助手聊天页、制度问答页、智能填单入口和审批辅助信息展示。

验收：

* `[x] mvn -pl smart-office-services/smart-office-ai-service,smart-office-services/smart-office-search-service -am test`
* `[x] scripts/smoke-p1-ai-service.ps1 syntax check`
* `[x] git diff --check`
* `[x] ai-service smoke script 固化`
* `[ ] ai-service gateway smoke`
* `[x] AI 服务关闭时主流程不受影响。`
* `[x] AI 调用失败时可以降级为普通搜索或普通审批流程。`

## 6. 本地运行约定

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

启动中间件：

```powershell
$env:REDIS_HOST_PORT='6380'
$env:RABBITMQ_HOST_PORT='5673'
$env:NACOS_HOST_PORT='8951'
$env:NACOS_GRPC_HOST_PORT='9951'
$env:ELASTICSEARCH_TRANSPORT_PORT='9459'
docker compose -f docker/docker-compose.yml up -d
```

启动业务服务前建议设置：

```powershell
$env:NACOS_SERVER_ADDR='127.0.0.1:8951'
$env:REDIS_PORT='6380'
$env:RABBITMQ_PORT='5673'
```

当前命令行默认 `java` 可能不是 JDK 17。运行 Spring Boot 3 服务时使用 Java 17 或更高版本，例如：

```powershell
C:\Users\14224\.jdks\ms-17.0.18\bin\java.exe
```

## 7. 提交前检查

后端通用检查：

```powershell
git diff --check
mvn test
```

单服务变更优先加一条局部测试，例如：

```powershell
mvn -pl smart-office-services/smart-office-approval-service -am test
```

涉及前端时：

```powershell
cd smart-office-web
npm run build
$env:E2E_BASE_URL='http://127.0.0.1:5173'
npm run test:e2e:microservice
```

每完成一项，需要同步更新：

* `TODO.md`
* `docs/project-roadmap.md`
* `docs/microservices-architecture.md`
* 必要时更新 `README.md`
