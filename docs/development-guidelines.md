# Smart Office 开发规范

本文档用于约束当前 `smart-office` 项目的后续开发。规范参考了 `tjxt-javaai02` 后端项目和 `tj-portal-src` 前端项目的分层习惯，但以本项目当前的 Spring Boot 3 + Spring Cloud Alibaba 双轨架构为准。

## 1. 架构边界

当前采用双轨策略：`smart-office-server` 保留为单体业务基线，`smart-office-services` 作为微服务主线继续联调和完善。

后端根模块为 `smart-office-server`，包根路径为：

```text
com.natsukaze.smartoffice
```

业务模块按领域拆包：

```text
auth        认证授权
user        用户、角色、菜单、权限
org         公司、部门、岗位、员工
approval    审批
attendance  考勤
message     消息与待办
file        文件记录
search      制度文档与搜索
ai          AI 助手，当前可降级，后续可独立拆分
common      公共能力
```

单体内部允许模块间通过 Service/Mapper 直接调用，但禁止出现跨模块循环依赖。微服务内部禁止跨库直查，跨服务数据读取必须通过 `smart-office-api` 中的 Feign 契约。AI 模块不参与主业务强依赖，AI 调用失败时必须可降级。

## 2. 后端分层

每个业务模块优先使用以下结构：

```text
module
├── controller   REST 接口入口
├── service      业务编排与事务
├── mapper       MyBatis-Plus Mapper
├── entity       数据库实体
├── dto          入参对象，Request/Query/Form
└── vo           出参对象
```

约束：

| 类型 | 命名 | 用途 |
|---|---|---|
| Entity | `SysUser`, `ApprovalForm` | 对应数据库表，不直接暴露给前端 |
| DTO | `UserCreateRequest`, `ApprovalPageQuery` | 接收前端入参，必须配合 Validation |
| VO | `UserVO`, `ApprovalFormVO` | 返回前端展示数据 |
| Mapper | `XxxMapper` | 只做数据访问，不写业务规则 |
| Service | `XxxService` | 写业务规则、状态流转、事务边界 |
| Controller | `XxxController` | 只做参数接收、认证用户获取、统一返回 |

Controller 不写业务逻辑；Mapper 不写状态判断；Service 是唯一业务编排层。

## 3. 枚举与状态机

状态、类型、动作禁止散落魔法字符串。新增状态必须先定义枚举，再在业务代码中使用。

枚举统一放在：

```text
common.enums
```

枚举要求：

1. 实现 `BaseCodeEnum`。
2. 使用 `@EnumValue` 标记数据库存储值。
3. 使用 `@JsonValue` 标记接口序列化值。
4. 提供 `of(...)` 或 `ofNullable(...)` 方法解析前端传入编码。

示例：

```java
APPROVED("APPROVED", "Approved")
```

数据库仍存可读编码，例如 `APPROVED`、`PENDING`、`LEAVE`，但 Java 业务代码必须使用枚举常量，例如：

```java
ApprovalStatus.APPROVED
ApprovalType.LEAVE
BusinessType.APPROVAL
```

当前核心枚举：

| 枚举 | 场景 |
|---|---|
| `ApprovalType` | 请假、加班、报销、通用审批 |
| `ApprovalStatus` | 审批状态 |
| `ApprovalAction` | 审批操作记录 |
| `AttendanceStatus` | 考勤状态 |
| `BusinessType` | 消息、待办、文件等业务归属 |
| `TodoStatus` | 待办状态 |
| `DocumentStatus` | 制度文档状态 |
| `AiMessageRole` | AI 消息角色 |

审批状态流转必须集中在 `ApprovalService`，不得在 Controller 或前端自行拼状态。

## 4. 接口规范

接口统一返回 `Result<T>`：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": "2026-06-15T23:00:00"
}
```

分页统一返回 `PageResult<T>`：

```json
{
  "records": [],
  "total": 0,
  "current": 1,
  "size": 10
}
```

REST 路径约定：

| 操作 | 方法 | 示例 |
|---|---|---|
| 分页查询 | `GET` | `/api/system/users` |
| 详情查询 | `GET` | `/api/approvals/{id}` |
| 新增 | `POST` | `/api/org/departments` |
| 修改 | `PUT` | `/api/org/departments/{id}` |
| 局部状态修改 | `PATCH` | `/api/system/users/{id}/status` |
| 删除 | `DELETE` | `/api/org/departments/{id}` |
| 动作接口 | `POST` | `/api/approvals/{id}/submit` |

要求：

1. 入参对象必须使用 `@Valid`。
2. 单体内当前登录用户使用 `@AuthenticationPrincipal UserPrincipal` 获取；微服务业务接口通过 gateway 透传的 `X-User-Id` 获取当前用户。
3. 需要分页的接口继承 `PageQuery`。
4. 删除接口默认逻辑删除，关系表确实需要重建关系时才允许物理删除。
5. 接口返回前端 VO，不返回 Entity。

## 5. 异常与校验

业务异常统一抛 `BusinessException`，由 `GlobalExceptionHandler` 统一转换响应。

禁止：

1. 在 Controller 中手写 try/catch 包业务异常。
2. 直接返回异常堆栈给前端。
3. 用 `RuntimeException` 表达可预期业务失败。

常见业务校验：

1. 数据不存在：`xxx not found`。
2. 状态不可流转：明确说明当前状态不可操作。
3. 非本人/非审批人操作：明确提示权限不匹配。
4. 重复操作：例如重复打卡、重复提交、重复审批。

## 6. 事务与并发

写操作默认在 Service 层加 `@Transactional`。

必须使用事务的场景：

1. 审批提交：更新审批单、生成记录、生成待办。
2. 审批处理：更新审批单、完成待办、生成通知、写记录。
3. 用户/角色分配关系。
4. 删除主表并清理关系表。

当前阶段以数据库约束和乐观锁为主。后续接 Redis/Redisson 后，再补分布式锁，不能提前把中间件细节散落到业务层。

## 7. 数据库与 MyBatis-Plus

实体统一继承 `BaseEntity`，包含：

```text
id
createTime
updateTime
createdBy
updatedBy
deleted
version
```

约束：

1. 表名使用模块前缀，例如 `sys_`、`org_`、`approval_`、`attendance_`。
2. 字段使用下划线命名，Java 使用 camelCase。
3. 状态/类型字段优先使用 varchar 编码，Java 使用枚举。
4. 逻辑删除字段统一为 `deleted`。
5. 乐观锁字段统一为 `version`。
6. SQL 初始化脚本放在 `src/main/resources/db`。

## 8. 前端协作规范

前端后续建议参考 `tj-portal-src` 的分层方式，但使用 Vue 3 + TypeScript。

推荐结构：

```text
smart-office-web
├── src/api        接口封装
├── src/router     路由
├── src/store      Pinia 状态
├── src/views      页面
├── src/components 通用组件
├── src/utils      request、token、日期等工具
└── src/types      DTO/VO 类型定义
```

约束：

1. 所有 HTTP 请求必须经过统一 `request` 实例。
2. token 统一放在请求拦截器中处理。
3. API 按业务模块拆文件，例如 `auth.ts`、`user.ts`、`approval.ts`。
4. 前端状态选项必须复用后端枚举编码，例如 `APPROVED`、`PENDING`。
5. 页面不直接拼接后端 URL，必须调用 `src/api`。
6. 表格分页字段统一映射 `current`、`size`、`total`、`records`。

## 9. 测试规范

每轮改动至少执行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
mvn test
```

涉及接口行为时，需要启动本地服务做 HTTP 冒烟测试：

1. 登录获取 token。
2. 调用本次新增/修改接口。
3. 验证成功路径。
4. 至少验证一个失败路径，例如未授权、重复操作、状态不合法。
5. 测试数据必须清理，或使用可重复导入的初始化 SQL。

## 10. Git 规范

提交应按功能边界拆分：

```text
Initialize Spring Boot backend
Add user and role management APIs
Add monolith business modules
Refactor business states to enums
```

提交前必须确认：

```powershell
git status --short
git diff --check
mvn test
```

禁止把以下内容提交：

```text
.vscode/
target/
logs/
.env
本地密钥、数据库密码、真实 token
```

## 11. 当前项目优先级

后续开发优先级：

1. 保持单体模块可运行、可回退。
2. 跑通微服务版审批/消息主链路。
3. 前端代理切到 gateway，并补 Playwright 网关冒烟。
4. 按阶段补 MinIO、RabbitMQ、XXL-JOB、Elasticsearch、Redis Token。
5. 最后迁移 AI 独立能力。

AI 模块当前允许降级实现，不阻塞审批、考勤、组织、消息等主流程。
