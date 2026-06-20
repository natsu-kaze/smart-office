# Smart Office 微服务架构说明

本文记录 `smart-office-services` 的当前微服务拆分、运行约定和迁移进度。当前主线是微服务联调，`smart-office-server` 继续保留为单体业务基线。

## 1. 模块结构

```text
smart-office
├─ smart-office-common              公共返回、错误码、枚举、MyBatis-Plus 配置
├─ smart-office-api                 跨服务 DTO 与 OpenFeign Client
├─ smart-office-server              单体业务基线
└─ smart-office-services
   ├─ smart-office-gateway          Spring Cloud Gateway
   ├─ smart-office-auth-service     登录、JWT、当前用户
   ├─ smart-office-system-service   用户、角色、用户角色
   ├─ smart-office-org-service      公司、部门、岗位、员工
   ├─ smart-office-approval-service 审批单、审批节点、审批记录
   ├─ smart-office-attendance-service 考勤规则、打卡、统计
   ├─ smart-office-message-service  通知与待办
   ├─ smart-office-file-service     文件记录
   ├─ smart-office-search-service   制度文档检索
   └─ smart-office-ai-service       AI 办公助手，可降级接口与模型增强预留
```

## 2. 服务端口

| 服务 | 端口 |
| --- | ---: |
| gateway | 8000 |
| auth-service | 9101 |
| system-service | 9102 |
| org-service | 9103 |
| approval-service | 9104 |
| attendance-service | 9105 |
| message-service | 9106 |
| file-service | 9107 |
| search-service | 9108 |
| ai-service | 9109 |

## 3. 中间件端口

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

本地启动服务前建议设置：

```powershell
$env:NACOS_SERVER_ADDR='127.0.0.1:8951'
$env:REDIS_PORT='6380'
$env:RABBITMQ_PORT='5673'
```

如命令行默认 `java` 不是 JDK 17，使用本机 JDK 17 启动 Spring Boot 3 服务：

```powershell
C:\Users\14224\.jdks\ms-17.0.18\bin\java.exe
```

## 4. 网关与身份

gateway 负责统一入口和身份透传：

* 放行 `/api/auth/login`、Actuator 健康接口。
* 其它 `/api/**` 请求必须携带 `Authorization: Bearer <token>`。
* gateway 校验 JWT 后写入 `X-User-Id`、`X-Username`、`X-Real-Name`。
* gateway 会清理外部伪造的身份 Header。
* 下游业务服务只信任 gateway 透传的 `X-User-Id`，不接受前端直接传用户 ID。

## 5. 数据库拆分

`docker/mysql/init/04-smart-office-microservices.sql` 会在新数据库卷初始化时创建微服务分库，并从单体库复制基础表结构和种子数据：

* `smart_office_system`
* `smart_office_org`
* `smart_office_approval`
* `smart_office_attendance`
* `smart_office_message`
* `smart_office_file`
* `smart_office_search`
* `smart_office_ai`

跨服务读取禁止直接跨库查询，统一通过 `smart-office-api` 中的 Feign Client 调用内部接口。

## 6. 当前迁移进度

已完成：

* gateway JWT 鉴权和身份 Header 透传。
* auth/system/org 基础登录与用户组织链路。
* auth-service 已接入 Redis Token 存储，gateway 在 JWT 签名校验后再做 Redis 二次校验，退出后旧 token 立即失效。
* attendance/file/search 基础业务迁移。
* approval/message 代码层主链路：
  * 提交审批创建审批节点、待办和通知。
  * 审批通过、驳回、撤回、关闭完成待办并更新审批节点。
  * 报销金额大于 1000 元时，部门负责人审批后继续生成财务审批节点和待办。
  * 审批单关键状态变更使用数据库乐观锁，重复审批或并发更新会返回业务错误。
  * message-service 命令失败时 approval-service 抛出业务异常。
  * `approval_form.content` 已从 `JSON` 调整为 `TEXT`，支持普通表单文本内容。
* 前端审批中心支持保存草稿、保存并提交、提交草稿、通过、驳回。
* 前端新增消息中心，支持待办列表、通知列表、标记完成、标记已读。
* file-service 已接入 MinIO，支持真实上传、预览 URL、服务端下载。
* 前端新增文件中心，支持文件上传、预览、下载。
* message-service 已接入 RabbitMQ，审批结果和考勤异常通知异步投递到通知队列并由消费者落库。
* RabbitMQ 通知链路具备发送重试、投递失败同步降级和消费幂等，避免 MQ 抖动时静默丢通知或重复落库。
* system-service 已补 `/api/system/users` 用户分页接口，前端用户管理页可通过 gateway 访问。
* common 已补 JavaScript 安全整数序列化，避免 Snowflake Long ID 在浏览器侧精度丢失。
* smart-office-web 已补 microservice 模式 Playwright 冒烟，覆盖制度检索、审批详情、考勤记录页面。
* attendance-service 已接入 XXL-JOB 执行器，支持每日缺卡结算、月度统计任务和考勤异常员工通知。
* org-service 已补内部活跃员工 userId 列表接口，供 attendance-service 通过 Feign 获取结算对象。
* search-service 已接入 Elasticsearch，制度文档新增、修改、删除会同步索引，带关键字检索优先走 ES，失败时降级 MySQL LIKE。
* ai-service 已开始独立增强：
  * 使用 `smart_office_ai` 独立库保存 AI 会话和消息。
  * 通过 `smart-office-api` 调 search-service 内部制度文档检索接口。
  * 暴露 `/api/ai/chat`、`/api/ai/policy-qa`、`/api/ai/approval-summary`、`/api/ai/approval-draft`、`/api/ai/approval-risk`。
  * 当前先使用规则与检索降级响应，后续再接入 Spring AI 和真实模型。

已验证：

* `mvn -pl smart-office-services/smart-office-approval-service -am test`
* `mvn test`
* `npm run build`（smart-office-web）
* `git diff --check`
* gateway 基础冒烟已覆盖登录、鉴权、当前用户、考勤、文件、制度文档。
* `scripts/smoke-p0-approval-message.ps1` 已覆盖 approval/message 网关主链路：
  * `employee/123456` 创建并提交审批。
  * `manager/123456` 查看审批待办并审批。
  * manager 待办完成，employee 收到结果通知。
* `scripts/smoke-p1-file-minio.ps1` 已覆盖 file/MinIO 网关链路：
  * `employee/123456` 上传文件。
  * 文件记录写入 MySQL。
  * 预签名 URL 可读取 MinIO 对象。
  * `/api/files/{id}/download` 可下载原始内容。
* `scripts/smoke-p1-message-rabbitmq.ps1` 已覆盖 message/RabbitMQ 网关链路：
  * approval/message P0 主链路通过。
  * RabbitMQ 通知队列为 durable。
  * 通知消费者已连接，消息消费后无积压。
* `smart-office-web` 使用 `npm run dev:microservice` 连接 gateway 后的 Playwright 冒烟已通过：
  * `admin/123456` 登录后访问工作台、用户管理、组织架构、审批、消息、文件、考勤页面。
  * `employee/123456` 新建并提交审批。
  * `manager/123456` 审批通过。
  * employee 在消息中心看到审批通过通知。
  * employee 在文件中心上传文件并看到记录。
* `scripts/smoke-p1-attendance-xxl-job.ps1` 已覆盖 attendance/XXL-JOB 网关链路：
  * XXL-JOB Admin 可访问。
  * 内部任务接口触发每日缺卡结算和月度统计。
  * employee 通过 gateway 查询到缺卡记录和月度 `missingCount`。
* `scripts/smoke-p1-attendance-message-notice.ps1` 覆盖 attendance/message 网关链路：
  * 内部任务接口触发每日缺卡结算。
  * employee 通过 gateway 查询到 `ATTENDANCE` 考勤异常通知。
  * 重复触发同一天日结时，考勤异常通知仍保持单条。
* `scripts/smoke-p1-search-elasticsearch.ps1` 覆盖 search/Elasticsearch 网关链路：
  * Elasticsearch 集群健康为 `green` 或 `yellow`。
  * employee 通过 gateway 创建已发布制度文档。
  * search-service 内部接口重建制度文档索引。
  * employee 通过 gateway 使用唯一关键字检索到制度文档。
* `scripts/smoke-p1-auth-redis-token.ps1` 覆盖 auth/Redis Token 网关链路：
  * employee 通过 gateway 登录并访问 `/api/auth/me`。
  * employee 调用 `/api/auth/logout` 删除 Redis token。
  * 旧 token 再访问 gateway 返回 `401`。
* `scripts/smoke-p1-approval-rules-concurrency.ps1` 覆盖 approval 规则和重复审批链路：
  * employee 创建并提交高额报销审批。
  * manager 审批后审批单进入 `PROCESSING`，并生成 finance 财务待办。
  * manager 重复审批同一审批单会被拒绝。
  * finance 审批后审批单最终变为 `APPROVED`。
* `scripts/smoke-p1-ai-service.ps1` 已固化 ai-service 网关冒烟脚本，待当前代码的 search-service 与 ai-service 同时启动后执行。

## 7. 推荐启动顺序

先启动 Docker 中间件并确认 Nacos 配置导入完成：

```powershell
docker compose -f docker/docker-compose.yml up -d
```

再启动当前主链路服务：

```powershell
mvn -pl smart-office-services/smart-office-system-service spring-boot:run
mvn -pl smart-office-services/smart-office-org-service spring-boot:run
mvn -pl smart-office-services/smart-office-auth-service spring-boot:run
mvn -pl smart-office-services/smart-office-approval-service spring-boot:run
mvn -pl smart-office-services/smart-office-message-service spring-boot:run
mvn -pl smart-office-services/smart-office-gateway spring-boot:run
```

前端连接 gateway：

```powershell
cd smart-office-web
npm run dev:microservice
```

前端 Playwright microservice 冒烟：

```powershell
cd smart-office-web
$env:E2E_BASE_URL='http://127.0.0.1:5174'
npm run test:e2e:microservice
```

## 8. 后续顺序

1. 启动当前代码的 search-service 与 ai-service，执行 `scripts/smoke-p1-ai-service.ps1`。
2. 接入 Spring AI、模型配置、Chat Client 和流式输出。
3. 补前端 AI 助手页面：聊天、制度问答、智能填单、审批辅助信息展示。
4. 后续增强审批超时扫描、Redisson 防重复审批、文件上传限制、角色/菜单管理等非主链路能力。
