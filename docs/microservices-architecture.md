# Smart Office 微服务架构草案

## 当前策略

当前仓库进入微服务演进阶段，但不直接删除已有 `smart-office-server`。现有单体继续作为业务可运行基线，新建微服务模块先确定工程结构、服务边界、网关、注册发现、OpenFeign 契约和公共模块。

后续业务代码按模块逐步从 `smart-office-server` 迁移到对应服务，迁移完成并通过端到端测试后，再移除单体模块。

## Maven 模块

```text
smart-office
├─ smart-office-common              公共返回、错误码、服务名常量
├─ smart-office-api                 跨服务 DTO 与 OpenFeign Client
├─ smart-office-server              现有单体业务基线
.0
└─ smart-office-services
   ├─ smart-office-gateway          Spring Cloud Gateway
   ├─ smart-office-auth-service     认证服务
   ├─ smart-office-system-service   用户、角色、菜单服务
   ├─ smart-office-org-service      公司、部门、岗位、员工服务
   ├─ smart-office-approval-service 审批服务
   ├─ smart-office-attendance-service 考勤服务
   ├─ smart-office-message-service  消息与待办服务
   ├─ smart-office-file-service     文件服务
   ├─ smart-office-search-service   制度文档与搜索服务
   └─ smart-office-ai-service       AI 办公助手服务
```

## 服务端口

| 服务 | 端口 |
|---|---:|
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

## 网关路由

| 路径 | 目标服务 |
|---|---|
| `/api/auth/**` | auth-service |
| `/api/system/**` | system-service |
| `/api/org/**` | org-service |
| `/api/approvals/**` | approval-service |
| `/api/attendance/**` | attendance-service |
| `/api/messages/**` | message-service |
| `/api/files/**` | file-service |
| `/api/policies/**` | search-service |
| `/api/ai/**` | ai-service |

## 本地启动顺序

1. 启动 Docker 依赖：

   ```powershell
   cd docker
   docker compose up -d mysql redis nacos rabbitmq elasticsearch minio xxl-job-admin
   ```

2. 启动网关：

   ```powershell
   mvn -q install -DskipTests
   mvn -pl smart-office-services/smart-office-gateway spring-boot:run
   ```

3. 启动需要调试的业务服务，例如：

   ```powershell
   mvn -q install -DskipTests
   mvn -pl smart-office-services/smart-office-auth-service spring-boot:run
   ```

4. 前端切到网关代理：

   ```powershell
   # 网关默认 http://localhost:8000
   cd smart-office-web
   npm run dev -- --mode microservice
   ```

## 迁移顺序建议

1. 抽公共能力：统一返回、异常、分页、安全上下文、Feign 拦截器。
2. 迁移 system-service：用户、角色、菜单是认证和组织的基础依赖。
3. 迁移 auth-service：登录认证、JWT、Redis Token。
4. 迁移 org-service：公司、部门、岗位、员工。
5. 迁移 approval-service：审批单、审批记录、审批状态机。
6. 迁移 attendance-service：考勤规则、打卡、统计。
7. 迁移 message-service：消息、待办、异步通知入口。
8. 迁移 file/search/ai 等增强服务。

每迁移一个服务，都需要补齐单服务单元测试、Feign 契约测试、网关路由冒烟测试。

## 快速验证命令

编译整个后端工程：

```powershell
mvn -q -DskipTests compile
```

打包整个后端工程：

```powershell
mvn -q -DskipTests package
```

运行现有单体测试：

```powershell
mvn -q -pl smart-office-server test
```

单独启动一个服务骨架并检查健康接口：

```powershell
mvn -q install -DskipTests
$env:NACOS_DISCOVERY_ENABLED='false'
$env:NACOS_REGISTER_ENABLED='false'
$env:SERVICE_REGISTRY_AUTO_REGISTRATION_ENABLED='false'
mvn -pl smart-office-services/smart-office-system-service spring-boot:run
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:9102/internal/health
```

说明：完整联调需要先启动 `docker compose up -d nacos`。如果只是单服务本地健康检查，可以按上面的命令临时关闭注册发现。
## 当前迁移状态

已迁移到微服务版本的能力：

* common：统一返回、分页、业务异常、全局异常、BaseEntity、MyBatis-Plus 配置。
* api：system-service 用户认证 Feign 契约。
* system-service：用户认证所需的用户、角色、用户角色实体、Mapper、内部查询与最后登录时间更新。
* auth-service：登录、JWT、Spring Security、当前用户、退出接口，通过 Feign 调用 system-service。
* service runtime：所有启用 OpenFeign 的业务服务已补充 Spring Cloud LoadBalancer。

本地验证结果：

* `mvn -DskipTests compile` 通过。
* `mvn test` 通过。
* auth-service 使用 JDK 21 临时启动，`/actuator/health` 返回 `UP`。
* system-service 可启动，当前因 Docker/MySQL 在本终端不可达，`/actuator/health` 返回 `DOWN`，待数据库联调。
* org-service 已迁移公司、部门、岗位、员工基础接口，保留 `/api/org/**` 路径，通过 Feign 调 system-service 获取用户信息。
* org-service 使用 JDK 21 临时启动，Spring 容器可启动；当前因 Docker/MySQL 在本终端不可达，`/actuator/health` 返回 `DOWN`。

当前本地环境待处理：

* 当前终端无法连接 Docker Desktop pipe，`docker version` 报 `dockerDesktopLinuxEngine` pipe 不存在。
* 当前终端无权限启动 `com.docker.service`。
* `5672` 被本机 `erl.exe` 占用，后续 RabbitMQ 容器启动前需要释放。
* PATH 中默认 `java` 为 JDK 11，运行 Spring Boot 3 服务需使用 JDK 17/21。

## Nacos 配置与独立数据库

微服务配置参照 `D:\my_project\tjxt\tjxt-javaai02` 的结构拆分：

* `application.yml`：只保留端口、服务名、服务私有库名。
* `application-local.yml`：只保留 Nacos 地址、账号和 `spring.config.import`。
* `docker/nacos/config/*.yaml`：保存共享配置与服务私有配置，由 Docker Compose 的 `nacos-config-importer` 自动发布。

共享配置：

* `shared-spring.yaml`：注册发现自动注册、Actuator 暴露。
* `shared-mybatis.yaml`：MySQL 数据源、MyBatis-Plus 公共配置，数据库名来自 `${smart-office.jdbc.database}`。
* `shared-feign.yaml`：OpenFeign 超时配置。
* `shared-redis.yaml`：Redis 连接配置。
* `shared-mq.yaml`：RabbitMQ 连接配置。
* `shared-logs.yaml`：日志级别。

微服务数据库：

| 服务 | 数据库 |
| --- | --- |
| system-service | `smart_office_system` |
| org-service | `smart_office_org` |
| approval-service | `smart_office_approval` |
| attendance-service | `smart_office_attendance` |
| message-service | `smart_office_message` |
| file-service | `smart_office_file` |
| search-service | `smart_office_search` |
| ai-service | `smart_office_ai` |

Docker 当前验证：

* `docker compose -f docker/docker-compose.yml config` 通过。
* MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、MinIO、XXL-JOB Admin 已启动。
* 已手动验证 Nacos 配置发布成功，可读取 `shared-mybatis.yaml`。
* 已验证 system-service 从 Nacos 读取共享 MyBatis 配置，连接 `smart_office_system`，`/actuator/health` 返回 `UP`。

当前 Windows 端口说明：

* `8751-8950` 被 Windows 排除，Nacos 默认 `8848` 无法绑定，因此本机使用 `8951`。
* Nacos 2.x 客户端需要 gRPC 端口等于 HTTP 端口 + 1000，因此本机使用 `9951 -> 9848`。
* 宿主 Redis/RabbitMQ 服务未被当前 shell 停止，Docker 版临时映射为 Redis `6380`、RabbitMQ `5673`。
* Elasticsearch transport `9300` 在 Windows 排除段内，本机映射为 `9459 -> 9300`。

本机启动业务服务时可使用：

```powershell
$env:NACOS_SERVER_ADDR='127.0.0.1:8951'
$env:REDIS_PORT='6380'
$env:RABBITMQ_PORT='5673'
```

下一步：

1. 补 gateway JWT 校验与 `X-User-Id` 透传。
2. 迁移 attendance-service 到 `smart_office_attendance`。
3. 继续迁移 file/search/ai，并补完整网关联调。
