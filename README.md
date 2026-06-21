# Smart Office — 企业智能办公平台

面向企业内部的协同办公系统，涵盖**组织架构、员工管理、RBAC 权限、审批工作流、考勤打卡、消息通知、文件管理、制度文档检索、AI 办公助手**等核心业务模块。

采用 **Spring Cloud Alibaba 微服务架构** + **Vue 3 前端**，前后端分离，可作为微服务全栈项目的参考实现。

---

## 功能概览

| 模块 | 功能 |
|---|---|
| **用户与权限** | 用户管理（CRUD）、角色管理、菜单树权限分配（父子独立控制）、RBAC 网关拦截、JWT 认证、BCrypt 密码加密 |
| **组织架构** | 公司信息维护、部门树管理、岗位管理、员工入离职状态跟踪 |
| **审批中心** | 请假/加班/报销/通用审批、自定义审批规则（优先级、部门范围、角色过滤、或签、超时自动处理）、审批流转（提交→通过→驳回→撤回）、乐观锁防重 |
| **考勤打卡** | 上班/下班打卡（确认弹窗+备注）、打卡顺序校验、月度考勤统计、部门考勤看板、考勤规则配置（上下班时间/迟到早退阈值） |
| **消息中心** | 通知消息（已读/未读分类分页）、公告发布（全部/部门/主管）、批量操作、待办管理 |
| **文件中心** | 文件上传/下载/预览、MinIO 对象存储、业务关联附件 |
| **制度文档** | 文档浏览与全文检索（Elasticsearch）、管理端 CRUD、文件上传解析（PDF/TXT/MD）、浏览/编辑界面分离 |
| **AI 办公助手** | 制度问答、审批摘要、智能填单、风险提示（可降级，无模型时不阻塞主流程） |
| **工作台** | 待办/未读/考勤状态概览、月度考勤统计卡片、请假感知 |

---

## 技术栈

| 层级 | 技术 |
|---|---|
| **语言** | Java 17, TypeScript |
| **后端框架** | Spring Boot 3.3, Spring Cloud Alibaba, Spring Cloud Gateway |
| **服务治理** | Nacos（注册中心 + 配置中心）, OpenFeign（声明式 RPC）, Sentinel（熔断降级） |
| **ORM** | MyBatis-Plus 3.5（Snowflake 主键、逻辑删除、乐观锁、枚举映射） |
| **安全** | Spring Security + JWT（网关签发、Redis 存活校验、RBAC 路径拦截） |
| **数据库** | MySQL 8.x（按服务独立 database） |
| **缓存** | Redis 7.x（Token 黑名单、分布式锁） |
| **消息队列** | RabbitMQ（异步通知投递 + 同步降级） |
| **检索引擎** | Elasticsearch 8.x（制度文档全文检索） |
| **对象存储** | MinIO（文件上传，S3 兼容） |
| **定时任务** | XXL-JOB（每日考勤结算、月度汇总） |
| **AI 集成** | Spring AI（可降级大模型调用） |
| **前端** | Vue 3 + TypeScript + Vite + Element Plus + Pinia |

---

## 架构图

```
                    ┌──────────────────────────────────────┐
                    │         Vue 3 Frontend (Vite)         │
                    │    Element Plus · Pinia · TypeScript  │
                    └──────────────┬───────────────────────┘
                                   │ HTTP :8000
                    ┌──────────────▼───────────────────────┐
                    │       Spring Cloud Gateway            │
                    │    JWT解析 · RBAC鉴权 · 路由转发       │
                    └──────┬───────┬───────┬──────┬────────┘
                           │       │       │      │
              ┌────────────┼───────┼───────┼──────┼──────────────┐
              │            │       │       │      │              │
              ▼            ▼       ▼       ▼      ▼              ▼
        ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
        │  auth   │ │ system  │ │   org   │ │approval │ │attend.  │
        │  :9101  │ │  :9102  │ │  :9103  │ │  :9104  │ │  :9105  │
        └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
              │            │       │       │      │              │
              ▼            ▼       ▼       ▼      ▼              ▼
        ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
        │ message │ │  file   │ │ search  │ │   ai    │ │  Nacos  │
        │  :9106  │ │  :9107  │ │  :9108  │ │  :9109  │ │  :8951  │
        └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘
              │            │       │       │
              ▼            ▼       ▼       ▼
        ┌──────────────────────────────────────────────────┐
        │          Middleware (Docker Compose)              │
        │  MySQL:3306 · Redis:6379 · RabbitMQ:5673          │
        │  Elasticsearch:9200 · MinIO:9000 · XXL-JOB:8088   │
        └──────────────────────────────────────────────────┘
```

**双轨策略：** `smart-office-server` 为单体基线（仅维护），`smart-office-services` 为微服务主线（持续迭代）。前端统一通过 Gateway 端口 8000 访问。

### 服务端口与数据库

| 服务 | 端口 | 数据库 |
|---|---|---|
| Gateway | 8000 | — |
| Auth | 9101 | 委派 system-service |
| System | 9102 | `smart_office_system` |
| Org | 9103 | `smart_office_org` |
| Approval | 9104 | `smart_office_approval` |
| Attendance | 9105 | `smart_office_attendance` |
| Message | 9106 | `smart_office_message` |
| File | 9107 | `smart_office_file` |
| Search | 9108 | `smart_office_search` |
| AI | 9109 | `smart_office_ai` |

---

## 快速开始

### 前置条件

- JDK 17+, Maven 3.8+, Node.js 18+
- Docker Desktop（MySQL, Redis, RabbitMQ, Nacos, Elasticsearch, MinIO, XXL-JOB）

### 1. 启动中间件

```powershell
cd docker
docker compose up -d
```

### 2. 构建后端

```powershell
mvn -DskipTests package
```

### 3. 启动服务（推荐顺序）

```powershell
# 设置 Nacos 地址（Windows 下端口映射为 8951）
$env:NACOS_SERVER_ADDR='127.0.0.1:8951'

# 按顺序启动 JAR 包
java -jar smart-office-services/smart-office-system-service/target/*.jar
java -jar smart-office-services/smart-office-org-service/target/*.jar
java -jar smart-office-services/smart-office-message-service/target/*.jar
java -jar smart-office-services/smart-office-auth-service/target/*.jar
java -jar smart-office-services/smart-office-approval-service/target/*.jar
java -jar smart-office-services/smart-office-attendance-service/target/*.jar
java -jar smart-office-services/smart-office-file-service/target/*.jar
java -jar smart-office-services/smart-office-search-service/target/*.jar
java -jar smart-office-services/smart-office-gateway/target/*.jar

# 或使用一键脚本
powershell -File scripts/start-non-ai-local.ps1
```

### 4. 启动前端

```powershell
cd smart-office-web
npm install
npm run dev:microservice
```

浏览器访问 `http://localhost:5173`，默认管理员账号 `admin / 123456`。

---

## 核心业务设计

### RBAC 权限模型

```
用户 ──多对多──► 角色 ──多对多──► 菜单/权限
```

- 菜单控制侧边栏入口可见性，按钮权限控制操作（发布公告、删除文件、维护制度等）
- 父子权限**独立勾选**（`check-strictly`），可单独关闭按钮权限而不影响菜单入口
- 权限由数据库驱动，无硬编码覆盖，保存后新登录生效
- 网关层拦截 `/api/**` 请求，校验 JWT + Redis Token + RBAC 权限码

### 审批状态机

```
DRAFT → PENDING → PROCESSING → APPROVED
                    ↘ REJECTED
                    ↘ WITHDRAWN
                    ↘ CLOSED
```

规则引擎支持：**优先级排序、部门范围限定、申请人角色过滤、金额门槛、或签（同步骤多人任一审批）、超时自动处理（通过/驳回/升级）**。乐观锁（`version`）防止重复审批。

### 微服务通信

- Feign 接口统一定义在 `smart-office-api` 模块，各服务通过 `@FeignClient` 调用
- 内部接口使用 `/internal/` 前缀，服务间不跨库查询
- Sentinel 熔断降级保底，AI 服务故障不阻塞审批/考勤/消息主流程

---

## 模块说明

| 模块 | 说明 |
|---|---|
| `smart-office-common` | 通用实体基类、统一返回体 `Result<T>`、分页、异常处理、枚举（审批/考勤/业务类型） |
| `smart-office-api` | 共享 DTO + Feign 契约（跨服务调用通过此模块定义接口） |
| `smart-office-gateway` | Spring Cloud Gateway：JWT 验证、`X-User-*` Header 注入、Redis Token 校验、RBAC 权限拦截 |
| `smart-office-auth-service` | 登录认证、JWT 签发（委派 system-service 查询用户） |
| `smart-office-system-service` | 用户管理、角色管理、菜单权限、RBAC 权限解析 |
| `smart-office-org-service` | 公司信息、部门树、岗位管理、员工管理 |
| `smart-office-approval-service` | 审批表单、审批规则、流程流转、待办/通知联动 |
| `smart-office-attendance-service` | 打卡、考勤规则、月度汇总、部门看板、每日结算 |
| `smart-office-message-service` | 通知公告、待办任务、批量操作、MQ 异步投递 |
| `smart-office-file-service` | MinIO 文件上传/下载/预览 |
| `smart-office-search-service` | 制度文档 CRUD、Elasticsearch 全文检索、文件上传解析 |
| `smart-office-ai-service` | AI 会话、制度问答、审批摘要、智能填单、风险提示（可降级） |
| `smart-office-server` | 单体基线（bugfix 维护，不新增功能） |
| `smart-office-web` | Vue 3 前端 |

---

## Git 提交规范

- `feat:` 新功能
- `fix:` 修复
- `docs:` 文档
- `refactor:` 重构
- `style:` 格式
- `test:` 测试
- `chore:` 构建/工具变更
