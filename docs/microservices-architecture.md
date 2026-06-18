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
   └─ smart-office-ai-service       AI 占位，后续独立增强
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
* attendance/file/search 基础业务迁移。
* approval/message 代码层主链路：
  * 提交审批创建审批节点、待办和通知。
  * 审批通过、驳回、撤回、关闭完成待办并更新审批节点。
  * message-service 命令失败时 approval-service 抛出业务异常。
* 前端审批中心支持保存草稿、保存并提交、提交草稿、通过、驳回。
* 前端新增消息中心，支持待办列表、通知列表、标记完成、标记已读。

已验证：

* `mvn -pl smart-office-services/smart-office-approval-service -am test`
* `mvn test`
* `npm run build`（smart-office-web）
* `git diff --check`
* gateway 基础冒烟已覆盖登录、鉴权、当前用户、考勤、文件、制度文档。

待验证：

* gateway 运行时审批/消息闭环：
  * `employee/123456` 创建并提交审批。
  * `manager/123456` 查看审批待办并审批。
  * manager 待办完成，employee 收到结果通知。
* `smart-office-web` 使用 `npm run dev -- --mode microservice` 连接 gateway 后的 Playwright 冒烟。

## 7. 推荐启动顺序

```powershell
docker compose -f docker/docker-compose.yml up -d

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
npm run dev -- --mode microservice
```

## 8. 后续顺序

1. 完成 approval/message gateway 运行时联调。
2. 将 Playwright 冒烟切到 microservice 模式。
3. file-service 接入 MinIO 真实上传、下载、预览。
4. message-service 接入 RabbitMQ 异步通知。
5. attendance-service 接入 XXL-JOB 每日结算和月度统计。
6. search-service 接入 Elasticsearch 索引同步和全文检索。
7. auth-service 补 Redis Token 存储和退出失效。
8. ai-service 独立迁移。
