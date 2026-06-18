# 企业智能审批与考勤管理系统

## 项目简介

本项目是一个面向企业内部协同办公场景的审批与考勤管理系统，参考钉钉、飞书、企业微信等办公平台中的审批、考勤、组织架构和待办通知能力，设计并实现员工管理、部门管理、角色权限、请假/加班/报销审批、考勤打卡、消息待办、文件附件、制度文档检索与 AI 办公助手等功能。

项目采用后端优先的开发方式，当前已进入 Spring Cloud Alibaba 微服务联调阶段。`smart-office-server` 保留为单体业务基线，`smart-office-services` 作为后续主线，已完成 gateway、auth、system、org、approval、message、attendance、file、search 等核心服务迁移，AI 服务暂作独立增强能力保留占位。

## 项目定位

本项目不是完整复刻钉钉或飞书，而是聚焦企业办公场景中的核心后端业务能力：

* 组织架构管理
* 员工与权限管理
* 审批流程流转
* 考勤打卡与考勤统计
* 消息通知与待办任务
* 文件附件管理
* 制度文档检索
* AI 办公助手能力

## 技术栈

### 后端

* Java 17
* Spring Boot 3.x
* Spring Security / JWT
* MyBatis-Plus
* MySQL 8.x
* Redis
* Redisson
* RabbitMQ
* Elasticsearch
* MinIO
* XXL-JOB
* Spring AI
* Knife4j / Swagger
* Docker / Docker Compose

### 微服务技术

* Spring Cloud Alibaba
* Nacos
* Spring Cloud Gateway
* OpenFeign
* Sentinel

### 前端

* Vue 3
* TypeScript
* Vite
* Element Plus
* Pinia
* Axios

前端优先采用后台管理模板风格，重点完成业务展示，不追求复杂 UI。

## 架构说明

* 当前目标、实施步骤和验收口径见 [docs/project-roadmap.md](docs/project-roadmap.md)。
* 微服务工程结构、端口、网关路由和本地联调说明见 [docs/microservices-architecture.md](docs/microservices-architecture.md)。
* 开发约束和代码风格见 [docs/development-guidelines.md](docs/development-guidelines.md)。
* 当前 `smart-office-server` 仍保留为业务基线，`smart-office-services` 用于承载微服务主线。

## 核心功能

### 1. 用户与权限模块

* 用户登录
* JWT 认证
* 用户信息管理
* 角色管理
* 菜单权限
* 接口权限
* 基于 RBAC 的权限控制

### 2. 组织架构模块

* 公司信息管理
* 部门管理
* 岗位管理
* 员工管理
* 部门负责人配置
* 员工所属部门配置

### 3. 审批中心模块

支持以下审批类型：

* 请假审批
* 加班审批
* 报销审批
* 通用审批

审批状态包括：

* 草稿
* 待审批
* 审批中
* 已通过
* 已驳回
* 已撤回
* 已关闭

核心能力：

* 创建审批单
* 提交审批单
* 审批通过
* 审批驳回
* 撤回审批
* 查看我的申请
* 查看我的待办
* 查看审批详情
* 审批流转记录

### 4. 审批规则模块

第一阶段实现简单审批规则：

* 请假审批：部门负责人审批
* 加班审批：部门负责人审批
* 报销审批：金额小于等于 1000 元由部门负责人审批，金额大于 1000 元需要部门负责人和财务审批

后续可扩展：

* 多级审批
* 条件审批
* 会签
* 或签
* 抄送人
* 审批超时提醒

### 5. 考勤模块

* 上班打卡
* 下班打卡
* 防重复打卡
* 查询个人考勤记录
* 查询部门考勤记录
* 每日考勤结算
* 月度考勤统计

考勤状态包括：

* 正常
* 迟到
* 早退
* 缺卡
* 请假
* 加班
* 异常

默认考勤规则：

* 上班时间：09:00
* 下班时间：18:00
* 迟到判断：上班打卡时间晚于 09:00
* 早退判断：下班打卡时间早于 18:00

### 6. 消息与待办模块

* 审批待办通知
* 审批结果通知
* 考勤异常通知
* 审批超时提醒
* 消息已读 / 未读
* 待办数量统计

消息发送优先使用 RabbitMQ 异步处理。

### 7. 文件模块

* 文件上传
* 文件下载
* 文件预览地址
* 报销凭证上传
* 请假证明上传
* 制度文档上传

文件存储使用 MinIO。

### 8. 搜索模块

使用 Elasticsearch 实现：

* 员工搜索
* 审批单搜索
* 制度文档搜索
* 历史审批搜索

第一阶段可先使用 MySQL 模糊查询，后续接入 Elasticsearch。

### 9. AI 办公助手模块

基于 Spring AI 实现企业办公助手能力。

第一阶段目标：

* 制度问答
* 审批内容摘要
* 请假 / 报销智能填单
* 审批风险提示

示例场景：

用户输入：

> 我明天下午请半天假，原因是去医院复查

AI 生成请假单草稿：

* 审批类型：请假
* 请假时间：明天下午
* 请假原因：医院复查
* 请假时长：0.5 天

审批人查看报销单时，AI 生成摘要：

* 本次报销金额：1280 元
* 报销类型：差旅报销
* 需要财务复核：是
* 风险提示：金额超过 1000 元，需要二级审批

## 项目架构

第一阶段采用单体模块化架构：

```text
smart-office
├── smart-office-server
│   ├── auth        # 认证授权模块
│   ├── user        # 用户模块
│   ├── org         # 组织架构模块
│   ├── approval    # 审批模块
│   ├── attendance  # 考勤模块
│   ├── message     # 消息待办模块
│   ├── file        # 文件模块
│   ├── search      # 搜索模块
│   ├── ai          # AI 办公助手模块
│   └── common      # 公共模块
├── smart-office-web
└── docker
```

当前微服务架构：

```text
smart-office
├── smart-office-common
├── smart-office-api
├── smart-office-server                 # 单体业务基线
├── smart-office-services
│   ├── smart-office-gateway
│   ├── smart-office-auth-service
│   ├── smart-office-system-service
│   ├── smart-office-org-service
│   ├── smart-office-approval-service
│   ├── smart-office-attendance-service
│   ├── smart-office-message-service
│   ├── smart-office-file-service
│   ├── smart-office-search-service
│   └── smart-office-ai-service
├── smart-office-web
└── docker
```

## 推荐数据库表

### 用户权限相关

* sys_user
* sys_role
* sys_menu
* sys_user_role
* sys_role_menu

### 组织架构相关

* org_company
* org_department
* org_position
* org_employee

### 审批相关

* approval_form
* approval_record
* approval_process
* approval_rule
* approval_attachment

### 考勤相关

* attendance_record
* attendance_rule
* attendance_summary

### 消息相关

* message_notice
* message_todo

### 文件相关

* file_record

### AI 相关

* ai_conversation
* ai_message
* ai_prompt_template

### 制度文档相关

* policy_document
* policy_document_chunk

## 核心业务流程

### 审批流程

```text
员工创建审批单
→ 保存为草稿
→ 提交审批
→ 系统根据审批类型和规则计算审批人
→ 生成审批待办
→ 审批人审批
→ 记录审批流转日志
→ 审批通过 / 驳回
→ 发送审批结果通知
```

### 考勤流程

```text
员工上班打卡
→ 系统判断是否迟到
→ 员工下班打卡
→ 系统判断是否早退
→ XXL-JOB 每日结算考勤
→ 生成考勤统计
→ 发现异常后发送通知
```

### AI 智能填单流程

```text
用户输入自然语言
→ AI 识别审批类型
→ AI 提取时间、金额、原因等字段
→ 生成审批单草稿
→ 用户确认
→ 提交审批
```

## 开发原则

1. 优先完成核心业务闭环，不一开始追求完整微服务。
2. 后端接口优先，前端以能展示业务为主。
3. 所有核心状态变更必须记录操作日志。
4. 审批状态流转必须做合法性校验。
5. 涉及并发的操作使用 Redis / Redisson 控制。
6. 消息通知、统计任务、AI 分析等耗时操作尽量异步化。
7. AI 模块作为增强能力，不能影响主业务流程的稳定性。
8. AI 调用失败时，系统应降级为普通审批 / 普通查询流程。

## 启动方式

启动 Docker 中间件：

```powershell
$env:REDIS_HOST_PORT='6380'
$env:RABBITMQ_HOST_PORT='5673'
$env:NACOS_HOST_PORT='8951'
$env:NACOS_GRPC_HOST_PORT='9951'
$env:ELASTICSEARCH_TRANSPORT_PORT='9459'
docker compose -f docker/docker-compose.yml up -d
```

本机启动业务服务前设置：

```powershell
$env:NACOS_SERVER_ADDR='127.0.0.1:8951'
$env:REDIS_PORT='6380'
$env:RABBITMQ_PORT='5673'
```

当前中间件：

* MySQL
* Redis
* Nacos
* RabbitMQ
* Elasticsearch
* MinIO
* XXL-JOB Admin

## 项目亮点

* 基于 RBAC 实现企业内部权限控制。
* 基于状态机思想实现审批流程流转。
* 使用 Redis / Redisson 解决重复审批、重复打卡等并发问题。
* 使用 RabbitMQ 异步处理审批通知和考勤异常通知。
* 使用 XXL-JOB 实现每日考勤结算和审批超时扫描。
* 使用 Elasticsearch 实现制度文档、员工和审批单搜索。
* 使用 MinIO 管理审批附件和制度文档。
* 基于 Spring AI 实现制度问答、审批摘要和智能填单能力。
* 已进入 Spring Cloud Alibaba 微服务联调阶段。

## 当前开发目标

当前项目已从单体模块化进入微服务联调和中间件补齐阶段：

1. 已完成 gateway 登录、鉴权、身份透传和 approval/message 主链路冒烟。
2. 已完成 file-service MinIO 上传、预览、下载和前端文件中心。
3. 已完成 message-service RabbitMQ 审批通知异步投递、消费落库、失败同步降级和冒烟脚本。
4. 已完成 smart-office-web microservice 模式 Playwright 冒烟，覆盖登录、页面访问、审批、消息、文件上传。
5. 已完成 attendance-service XXL-JOB 每日考勤结算、月度统计和缺卡补记。
6. 已完成 search-service Elasticsearch 制度文档索引同步与全文检索，并通过网关 smoke 验收。
7. 已完成 auth-service Redis Token 存储、gateway 二次校验与退出失效，并通过网关 smoke 验收。
8. 之后补审批并发控制、消息可靠性、前端体验和 ai-service 独立增强。
9. AI 模块最后独立迁移，且不阻塞审批、考勤、组织、消息等主流程。

更细的目标、步骤和验收命令见 [docs/project-roadmap.md](docs/project-roadmap.md) 与 [TODO.md](TODO.md)。
