# TODO

## 当前修正记录（2026-06-22）

* [x] 审批规则增强：部门范围、优先级排序、申请人角色过滤、超时自动处理（AUTO_APPROVE/AUTO_REJECT/ESCALATE）、或签支持（| 分隔同步骤审批人）
* [x] 审批规则新增 TimeoutAction 枚举，审批表单新增 name/priority/applicantRoleCode/deptId/timeoutHours/timeoutAction 字段
* [x] 考勤打卡顺序校验：checkOut 检查 checkIn 先完成，checkIn 检查 checkOut 未完成
* [x] 考勤打卡确认弹窗：显示当前时间、可选备注，不再单纯点一下
* [x] 制度文档视图分离：浏览模式 `/policies`（所有用户）+ 管理模式 `/policies/manage`（需 policy:manage 权限）
* [x] 消息/公告优化：公告发布者可见自己公告、senderName 显示发布者、时间格式化（去 T）、未读/已读分类分页、待办时间格式化
* [x] 公告发布后端去 ADMIN/MANAGER 硬编码角色校验，改由网关 RBAC 控制 message:announcement:send 权限
* [x] 权限管理修复：el-tree check-strictly 父子独立控制、SystemPermissionService 去硬编码 DEFAULT_ROLE_PERMISSIONS 覆盖
* [x] 新增用户创建功能：POST /api/system/users、UserCreateRequest DTO、前端创建用户弹窗
* [x] 仪表盘增强：请假感知（显示"请假中"而非"未打卡"）、月度考勤概览卡片、状态颜色区分、加载状态
* [x] 单体 ApprovalService fillForm 补遗漏的 leaveStartDate/leaveEndDate 赋值
* [x] message_notice 表新增 sender_name 列
* [x] .gitignore 新增 .claude/ 忽略

已验证：
* [x] `mvn test`（14/14 模块通过）
* [x] `npm run build`（前端构建成功）
* [x] curl API 冒烟：创建用户、权限独立控制、公告发布者可见、senderName 显示、员工发公告

## 当前修正记录（2026-06-20）

* [x] 文件上传补充大小和类型限制：默认 20MB，白名单覆盖 PDF、图片、文本、Markdown、docx。
* [x] 制度文档新增上传解析入口：支持 PDF / TXT / Markdown 上传，解析正文后生成制度文档并同步检索索引。
* [x] smart-office-web 制度文档页重写为干净中文页面，补上传制度、新建、编辑、发布、转草稿、归档、删除和详情。
* [x] 考勤页重做为「今日考勤 + 月度概览 + 状态分布 + 部门出勤看板」，部门模式可直观看到应到、实到、未到、异常和未到人员。
* [x] 普通员工不再展示部门记录入口；后端 `/api/attendance/department-records` 增加 ADMIN / MANAGER 权限校验，直接调接口也会拒绝。
* [x] 消息中心公告栏独立展示，公告卡片支持已读和删除，不再被大量普通通知盖住。
* [x] 普通通知列表支持多选、批量已读、批量删除；后端新增批量已读和批量删除接口。
* [x] 文件中心去掉「对象 Key」和「业务类型」展示，保留文件名、文件类型、大小、预览、下载、删除和关键词搜索。
* [x] 消息中心补充「发布公告」，支持全部员工、指定部门、部门及下级、主管四类接收范围。
* [x] 消息中心把「业务类型」调整为更易理解的「来源」，不再展示业务 ID。
* [x] 制度文档维护按钮按权限展示；后端限制制度创建、编辑、删除仅 ADMIN / MANAGER 可操作。
* [x] 文件删除补充后端权限校验：上传者或 ADMIN 可删除。
* [x] org-service 补充部门子树用户、部门子树主管、全部主管内部接口，供公告接收人计算复用。
* [x] system-service 补充个人中心接口：查询资料、更新姓名/手机号/邮箱/头像、校验旧密码后修改密码。
* [x] smart-office-web 补充个人中心页面，支持基础资料维护和账号密码修改。
* [x] approval-service 补充审批超时扫描：支持内部接口触发和 XXL-JOB `approvalTimeoutScanJob` 调度，默认扫描超过 24 小时仍待处理的审批。
* [x] system-service 补齐 RBAC：角色分页/新增/修改/删除、菜单树、用户分配角色、角色分配菜单权限。
* [x] auth-service 登录态补充 `permissions`，JWT 写入角色与权限声明。
* [x] gateway 增加路径级权限拦截，覆盖用户/角色、公告、文件删除、制度维护、部门考勤等接口。
* [x] smart-office-web 新增角色权限页，侧边栏与路由守卫改为按角色/菜单权限展示和拦截。
* [x] system-service 修复用户角色/角色菜单重新分配时逻辑删除导致的唯一键冲突，角色保存不再只返回 `system error`。
* [x] system-service 为 `ADMIN / MANAGER / EMPLOYEE / FINANCE` 补充默认权限兜底，避免角色菜单历史数据漂移导致非 admin 登录后 403。
* [x] gateway 调整文件删除权限为基础文件访问，具体是否可删继续交给 file-service 按上传者或管理员校验。
* [x] system-service 新增 `/api/system/users/options` 用户选项接口，供组织架构部门主管下拉使用；gateway 按 `org:manage` 放行，避免 manager 访问组织架构时被用户管理权限拦截。
* [x] attendance-service 部门考勤主管部门判断改为从 org-service 获取员工部门，主管未传部门时默认查询本人部门。
* [x] common 全局异常补充唯一约束冲突提示，避免数据库唯一键异常被吞成模糊系统错误。
* [x] smart-office-web 个人中心头像支持 PNG / JPG 上传，上传后自动写入个人资料。
* [x] file-service 头像上传改为 `AVATAR` 业务类型，文件中心默认列表隐藏头像文件，并兼容隐藏历史无业务类型的图片头像记录。
* [x] smart-office-web 角色权限页补充菜单权限/按钮权限说明，并对旧英文菜单名做中文展示映射。
* [x] smart-office-web 审批草稿支持编辑；请假单补充请假日期，草稿可保存或保存并提交。
* [x] approval-service 请假审批通过后调用 attendance-service，把请假日期范围写入考勤记录。
* [x] attendance-service 补充内部请假标记接口，请假日期按 `LEAVE` 状态写入记录并刷新月度汇总。
* [x] OpenFeign 接入 Sentinel fallbackFactory 和 Nacos 共享 Sentinel 配置，核心服务编译通过。
* [x] smart-office-web 组织架构部门主管从手填 ID 改为用户下拉选择，并在用户管理表格展示用户 ID。
* [x] smart-office-web 考勤状态分布改为按状态总数计算占比，部门记录切换后图表不再被单个状态撑满。

已验证：

* [x] `mvn -pl smart-office-services/smart-office-message-service,smart-office-services/smart-office-file-service,smart-office-services/smart-office-search-service,smart-office-services/smart-office-org-service -am test`
* [x] `mvn -pl smart-office-services/smart-office-message-service,smart-office-services/smart-office-attendance-service -am test`
* [x] `mvn -pl smart-office-services/smart-office-file-service,smart-office-services/smart-office-search-service -am test`
* [x] `mvn -pl smart-office-services/smart-office-org-service,smart-office-services/smart-office-message-service,smart-office-services/smart-office-file-service,smart-office-services/smart-office-search-service -am -DskipTests package`
* [x] `mvn -pl smart-office-services/smart-office-message-service -am -DskipTests package`
* [x] `mvn -pl smart-office-services/smart-office-file-service,smart-office-services/smart-office-search-service -am -DskipTests package`
* [x] `mvn -pl smart-office-services/smart-office-system-service,smart-office-services/smart-office-approval-service -am test`
* [x] `mvn -pl smart-office-services/smart-office-gateway,smart-office-services/smart-office-auth-service,smart-office-services/smart-office-system-service -am test`
* [x] `mvn -pl smart-office-services/smart-office-approval-service,smart-office-services/smart-office-attendance-service,smart-office-services/smart-office-gateway,smart-office-services/smart-office-auth-service,smart-office-services/smart-office-system-service -am test`
* [x] `mvn test`
* [x] `npm run build`（smart-office-web）
* [x] `mvn -pl smart-office-services/smart-office-file-service -am test`
* [x] `mvn -pl smart-office-services/smart-office-file-service -am -DskipTests package`
* [x] `mvn -pl smart-office-services/smart-office-system-service,smart-office-services/smart-office-gateway -am test`
* [x] `mvn -pl smart-office-services/smart-office-system-service,smart-office-services/smart-office-gateway -am -DskipTests package`
* [x] `mvn -pl smart-office-services/smart-office-attendance-service -am test`
* [x] `mvn -pl smart-office-services/smart-office-attendance-service -am -DskipTests package`
* [x] 网关接口验证：头像上传返回 `businessType=AVATAR`，默认文件中心列表不再显示新头像和历史无类型图片头像。
* [x] 网关接口验证：`employee/123456` 可访问工作台依赖的审批、消息、文件、制度、考勤接口；`manager/123456` 可访问组织架构、公告、部门考勤接口。
* [x] Edge headless Playwright 验证：employee 访问工作台、审批、消息、文件、制度、考勤、个人中心不再 403；manager 访问组织架构和主管功能页面不再 403。
* [x] Edge headless Playwright 验证：用户管理显示用户 ID，组织架构部门主管可下拉选择，部门考勤状态分布比例稳定。
* [x] Edge headless Playwright 冒烟：登录、文件中心隐藏对象 Key / 业务类型、消息中心公告入口与接收范围、制度维护入口可见性。
* [x] Edge headless Playwright 冒烟：管理员可见部门考勤看板和消息批量按钮；普通员工不可见部门记录入口。
* [x] employee token 直接请求部门考勤接口返回 `permission denied`。
* [ ] Docker Desktop / Nacos / MySQL / Redis / ES / MinIO 恢复后，补制度上传解析的网关联调和 Playwright 冒烟。

下一步建议：

1. 补操作审计日志：记录登录、角色授权、制度维护、文件删除、审批处理等关键动作。
2. 给公告发布、草稿编辑、头像上传、请假同步考勤补独立冒烟脚本。
3. 给考勤补后端聚合接口，减少前端为看板额外拉取多次列表。
4. 做一次 Docker 中间件恢复后的完整网关联调和 Playwright 冒烟。

## 当前执行快照（2026-06-19）

当前开发口径以 [docs/project-roadmap.md](docs/project-roadmap.md) 为准。

当前结论：

* 单体 `smart-office-server` 已作为业务基线保留，后续只做必要修复。
* 微服务 `smart-office-services` 是主线，新增能力优先落到对应微服务。
* 非 AI 主流程已完成主链路收尾，当前进入 ai-service 独立增强第一阶段。
* 当前已完成 smart-office-web 制度文档检索、审批详情、考勤记录等页面体验，并通过构建与 Playwright microservice 冒烟。
* ai-service 先提供不依赖真实大模型的降级接口，确保 AI 不阻塞审批、考勤、组织、消息等主流程。

已完成：
* [x] approval-service 提交审批时创建审批节点、审批待办和审批通知。
* [x] approval-service 审批通过 / 驳回 / 撤回 / 关闭时同步完成待办并更新审批节点。
* [x] approval-service 对 message-service Feign 命令返回值做成功校验，避免消息链路失败后静默提交。
* [x] smart-office-web 审批中心支持保存草稿、保存并提交、提交草稿、审批通过、审批驳回。
* [x] smart-office-web 新增消息中心，支持查看待办、查看通知、标记待办完成、标记通知已读。
* [x] 新增 approval-service 单元测试覆盖提交、审批通过、消息命令失败三条链路。
* [x] 修复审批内容字段类型：`approval_form.content` 从 `JSON` 调整为 `TEXT`。
* [x] 新增 `scripts/smoke-p0-approval-message.ps1`，固化 P0 网关冒烟链路。
* [x] file-service 接入 MinIO 真实上传、预览、下载。
* [x] smart-office-web 新增文件中心，支持文件上传、预览、下载。
* [x] 新增 `scripts/smoke-p1-file-minio.ps1`，固化文件/MinIO 网关冒烟链路。
* [x] message-service 接入 RabbitMQ，审批结果通知改为异步投递和消费落库。
* [x] 新增 RabbitMQ 通知交换机、队列、消费者配置和投递失败同步降级策略。
* [x] 新增 `scripts/smoke-p1-message-rabbitmq.ps1`，固化审批通知 RabbitMQ 网关冒烟链路。
* [x] system-service 新增 `/api/system/users` 用户分页接口，支持前端用户管理页从 gateway 访问。
* [x] common 新增 JavaScript 安全整数序列化策略，避免 Snowflake Long ID 在前端精度丢失。
* [x] smart-office-web microservice 模式补齐 Playwright 冒烟，覆盖登录、页面访问、审批、消息、文件上传。
* [x] attendance-service 接入 XXL-JOB 执行器，新增每日缺卡结算和月度统计任务。
* [x] org-service 新增内部活跃员工 userId 列表接口，供考勤结算按员工维度处理。
* [x] 新增 `scripts/smoke-p1-attendance-xxl-job.ps1`，固化考勤/XXL-JOB 网关冒烟链路。
* [x] search-service 接入 Elasticsearch，支持制度文档索引同步、重建和全文检索降级。
* [x] 新增 `scripts/smoke-p1-search-elasticsearch.ps1`，固化制度文档 ES 网关冒烟链路。
* [x] auth-service 接入 Redis Token 存储，gateway 完成 Redis 二次校验，退出登录后旧 token 立即失效。
* [x] 新增 `scripts/smoke-p1-auth-redis-token.ps1`，固化登录、当前用户、退出、旧 token 拒绝链路。
* [x] approval-service 支持报销金额大于 1000 元时部门负责人审批后流转到财务审批。
* [x] approval-service 通过状态校验和数据库乐观锁防止重复审批、并发审批重复流转。
* [x] 新增 `scripts/smoke-p1-approval-rules-concurrency.ps1`，固化高额报销二级审批和重复审批拒绝链路。
* [x] attendance-service 每日结算发现缺卡、迟到、早退等异常后通过 message-service 通知员工。
* [x] message-service RabbitMQ 通知发送支持有限重试，重试后仍失败则同步落库降级。
* [x] message-service 通知消费按用户、业务类型、业务 ID 和标题做幂等落库。
* [x] 新增 `scripts/smoke-p1-attendance-message-notice.ps1`，固化考勤异常通知和重复日结幂等链路。
* [x] smart-office-web 新增制度文档页面，支持创建、编辑、删除、关键词检索和详情查看。
* [x] smart-office-web 审批中心新增审批详情抽屉、流转记录展示和 `PROCESSING` 状态处理。
* [x] smart-office-web 考勤页新增月度统计、个人记录和部门记录查询。
* [x] smart-office-web Playwright microservice 冒烟扩展到制度检索、审批详情和考勤记录。
* [x] ai-service 新增会话、消息、提示词模板基础实体与 Mapper，切到 `smart_office_ai` 独立库。
* [x] ai-service 新增聊天、制度问答、审批摘要、智能填单、审批风险提示接口，当前先走规则与检索降级。
* [x] search-service 暴露制度文档内部检索契约，供 ai-service 制度问答复用。
* [x] 新增 `scripts/smoke-p1-ai-service.ps1`，固化 ai-service 网关冒烟脚本。

已验证：
* [x] `mvn -pl smart-office-services/smart-office-approval-service -am test`
* [x] `mvn test`
* [x] `npm run build`（smart-office-web）
* [x] `git diff --check`
* [x] `powershell -ExecutionPolicy Bypass -File scripts/smoke-p0-approval-message.ps1`
* [x] `powershell -ExecutionPolicy Bypass -File scripts/smoke-p1-file-minio.ps1`
* [x] `powershell -ExecutionPolicy Bypass -File scripts/smoke-p1-message-rabbitmq.ps1`
* [x] `npm run test:e2e:microservice`（`E2E_BASE_URL=http://127.0.0.1:5175`）
* [x] `mvn -pl smart-office-services/smart-office-attendance-service -am test`
* [x] `powershell -ExecutionPolicy Bypass -File scripts/smoke-p1-attendance-xxl-job.ps1`
* [x] `mvn -pl smart-office-services/smart-office-search-service -am test`
* [x] `powershell -ExecutionPolicy Bypass -File scripts/smoke-p1-search-elasticsearch.ps1`
* [x] `mvn -pl smart-office-services/smart-office-auth-service,smart-office-services/smart-office-gateway -am test`
* [x] `powershell -ExecutionPolicy Bypass -File scripts/smoke-p1-auth-redis-token.ps1`
* [x] `powershell -ExecutionPolicy Bypass -File scripts/smoke-p1-approval-rules-concurrency.ps1`
* [x] `mvn -pl smart-office-services/smart-office-message-service,smart-office-services/smart-office-attendance-service -am test`
* [x] `powershell -ExecutionPolicy Bypass -File scripts/smoke-p1-attendance-message-notice.ps1`
* [x] `npm run build`（smart-office-web）
* [x] `npm run test:e2e:microservice`（`E2E_BASE_URL=http://127.0.0.1:5173`，前端 dev server 已启动）
* [x] `mvn -pl smart-office-services/smart-office-ai-service,smart-office-services/smart-office-search-service -am test`
* [x] `powershell -NoProfile -ExecutionPolicy Bypass -Command "[scriptblock]::Create((Get-Content -Raw -Encoding UTF8 scripts\smoke-p1-ai-service.ps1)) | Out-Null"`
* [x] `git diff --check`

当前下一步：
1. 手动从 IDE 或 VSCode 启动当前代码的 `search-service` 与 `ai-service`。
2. 执行 `powershell -ExecutionPolicy Bypass -File scripts/smoke-p1-ai-service.ps1`，验证 `/api/ai/chat`、`/api/ai/policy-qa`、`/api/ai/approval-draft`、`/api/ai/approval-risk`。
3. ai-service 冒烟通过后提交当前后端和文档改动。
4. 下一阶段接入 Spring AI、模型配置、流式输出和前端 AI 助手页面。

## 当前目标与步骤

当前主线目标：

* [x] 跑通微服务版审批主链路：gateway -> auth/system/org/approval/message。
* [x] 验证审批提交后生成待办，审批处理后完成待办并生成通知。
* [x] 补 file-service 真实 MinIO 上传、下载、预览。
* [x] 补 message-service RabbitMQ 异步通知。
* [x] 将前端代理稳定切到 gateway，并补 Playwright microservice 冒烟。
* [x] 补 attendance-service XXL-JOB 考勤结算与月度统计。
* [x] 补 search-service Elasticsearch 索引同步与全文检索。
* [x] 补 auth-service Redis Token 存储、gateway 二次校验与退出失效。
* [x] 补 approval-service 重复审批/并发审批控制和报销二级审批。
* [x] 补 message-service 考勤异常通知、消费幂等和失败重试。
* [x] 收尾 smart-office-web 制度文档检索、审批详情、考勤记录等页面并通过 e2e。
* [x] ai-service 第一阶段独立迁移：会话/消息落库、降级聊天、制度问答检索、审批摘要、智能填单、风险提示接口。
* [x] 固化 ai-service 网关冒烟脚本。
* [ ] 执行 ai-service 网关冒烟。
* [ ] 接入 Spring AI、模型配置、流式输出和前端 AI 助手页面。

推荐执行顺序：

1. 启动当前代码的 search-service 与 ai-service，执行 `scripts/smoke-p1-ai-service.ps1`。
2. 接入 Spring AI 与模型配置，封装 Chat Client。
3. 补 AI 前端入口：聊天页、制度问答页、智能填单入口、审批辅助信息展示。
4. 视时间补审批超时扫描、Redisson 防重复审批、文件上传限制等增强项。

当前前端收尾验收命令：

* [x] `npm run build`（smart-office-web）
* [x] `npm run test:e2e:microservice`（`E2E_BASE_URL=http://127.0.0.1:5173`，前端 dev server 已启动）
* [x] `git diff --check`

下面保留详细模块清单和历史进度，后续每完成一项同步勾选。

## 0. 项目初始化

* [x] 创建后端项目 `smart-office-server`
* [x] 创建前端项目 `smart-office-web`
* [x] 创建 `docker` 目录
* [x] 创建 `docs` 目录
* [x] 添加 `.gitignore`
* [x] 添加 `README.md`
* [x] 添加 `TODO.md`
* [x] 统一 Java 版本为 Java 17
* [x] 统一后端编码为 UTF-8
* [x] 统一接口返回结构
* [x] 统一异常处理
* [x] 统一分页返回结构

## 1. 后端基础工程

* [x] 引入 Spring Boot
* [x] 引入 Spring Web
* [x] 引入 MyBatis-Plus
* [x] 引入 MySQL Driver
* [x] 引入 Lombok
* [x] 引入 Validation
* [x] 引入 Knife4j / Swagger
* [x] 配置 dev 环境
* [x] 配置 test 环境
* [x] 配置 prod 环境
* [x] 创建通用返回类 `Result`
* [x] 创建通用分页类 `PageResult`
* [x] 创建全局异常处理器
* [x] 创建业务异常类
* [x] 创建基础实体类 `BaseEntity`
* [x] 创建 MyBatis-Plus 自动填充配置
* [x] 创建逻辑删除配置
* [x] 创建接口文档配置

## 2. Docker 依赖环境

* [x] 编写 MySQL docker-compose 配置
* [x] 编写 Redis docker-compose 配置
* [x] 编写 RabbitMQ docker-compose 配置
* [x] 编写 Elasticsearch docker-compose 配置
* [x] 编写 MinIO docker-compose 配置
* [x] 编写 XXL-JOB Admin docker-compose 配置
* [x] 准备数据库初始化 SQL
* [x] 准备测试数据 SQL

第一阶段可以只启动：

* [x] MySQL
* [x] Redis

后续再接入：

* [x] RabbitMQ
* [x] Elasticsearch
* [x] MinIO
* [x] XXL-JOB

## 3. 用户与认证模块

### 数据库

* [x] 创建 `sys_user` 表
* [x] 创建 `sys_role` 表
* [x] 创建 `sys_menu` 表
* [x] 创建 `sys_user_role` 表
* [x] 创建 `sys_role_menu` 表

### 后端接口

* [x] 用户登录接口
* [x] 用户退出接口
* [x] 获取当前用户信息接口
* [x] 用户分页查询接口
* [x] 新增用户接口
* [x] 修改用户接口
* [x] 禁用用户接口
* [x] 删除用户接口
* [x] 角色分页查询接口
* [x] 新增角色接口
* [x] 修改角色接口
* [x] 删除角色接口
* [x] 菜单树查询接口
* [x] 分配用户角色接口
* [x] 分配角色菜单接口

### 权限能力

* [x] 接入 JWT
* [x] 接入 Spring Security
* [x] 实现登录认证过滤器
* [x] 实现接口权限校验
* [x] 实现密码加密
* [x] Redis 存储登录 Token
* [x] 用户退出时删除 Token

## 4. 组织架构模块

### 数据库

* [x] 创建 `org_company` 表
* [x] 创建 `org_department` 表
* [x] 创建 `org_position` 表
* [x] 创建 `org_employee` 表

### 后端接口

* [x] 公司信息查询接口
* [x] 修改公司信息接口
* [x] 部门树查询接口
* [x] 新增部门接口
* [x] 修改部门接口
* [x] 删除部门接口
* [x] 岗位分页查询接口
* [x] 新增岗位接口
* [x] 修改岗位接口
* [x] 删除岗位接口
* [x] 员工分页查询接口
* [x] 新增员工接口
* [x] 修改员工接口
* [x] 删除员工接口
* [x] 配置部门负责人接口
* [x] 查询部门员工接口

## 5. 审批模块

### 数据库

* [x] 创建 `approval_form` 表
* [x] 创建 `approval_record` 表
* [x] 创建 `approval_rule` 表
* [x] 创建 `approval_attachment` 表

### 审批类型

* [x] 请假审批
* [x] 加班审批
* [x] 报销审批
* [x] 通用审批

### 审批状态

* [x] 草稿
* [x] 待审批
* [x] 审批中
* [x] 已通过
* [x] 已驳回
* [x] 已撤回
* [x] 已关闭

### 后端接口

* [x] 创建审批单接口
* [x] 保存审批草稿接口
* [x] 提交审批接口
* [x] 审批通过接口
* [x] 审批驳回接口
* [x] 撤回审批接口
* [x] 关闭审批接口
* [x] 查询审批详情接口
* [x] 查询我的申请接口
* [x] 查询我的待办接口
* [x] 查询审批记录接口
* [x] 查询审批流转时间线接口

### 审批规则

* [x] 请假审批默认部门负责人审批
* [x] 加班审批默认部门负责人审批
* [x] 报销金额小于等于 1000 元，部门负责人审批
* [x] 报销金额大于 1000 元，部门负责人 + 财务审批
* [x] 审批人不存在时给出明确错误提示
* [x] 审批状态变更合法性校验
* [x] 重复审批校验
* [x] 非审批人不能审批校验

### 并发控制

* [x] 使用数据库乐观锁防止重复审批
* [ ] 使用 Redisson 防止并发审批
* [x] 审批操作记录完整日志

## 6. 考勤模块

### 数据库

* [x] 创建 `attendance_rule` 表
* [x] 创建 `attendance_record` 表
* [x] 创建 `attendance_summary` 表

### 后端接口

* [x] 上班打卡接口
* [x] 下班打卡接口
* [x] 查询今日打卡状态接口
* [x] 查询个人考勤记录接口
* [x] 查询部门考勤记录接口
* [x] 查询月度考勤统计接口
* [x] 配置考勤规则接口

### 业务规则

* [x] 默认上班时间 09:00
* [x] 默认下班时间 18:00
* [x] 晚于 09:00 上班打卡为迟到
* [x] 早于 18:00 下班打卡为早退
* [x] 未上班打卡为上班缺卡
* [x] 未下班打卡为下班缺卡
* [x] 请假审批通过后同步影响考勤状态
* [ ] 加班审批通过后同步影响加班记录
* [x] 防止重复上班打卡
* [x] 防止重复下班打卡

## 7. 消息与待办模块

### 数据库

* [x] 创建 `message_notice` 表
* [x] 创建 `message_todo` 表

### 后端接口

* [x] 查询我的消息接口
* [x] 查询未读消息数量接口
* [x] 标记消息已读接口
* [x] 删除消息接口
* [x] 查询我的待办接口
* [x] 查询待办数量接口
* [x] 完成待办接口

### 消息场景

* [x] 提交审批后生成审批人待办
* [x] 审批通过后通知申请人
* [x] 审批驳回后通知申请人
* [x] 审批超时后通知审批人
* [x] 考勤异常后通知员工

### RabbitMQ

* [x] 创建审批通知队列
* [x] 使用通知队列承载考勤异常通知
* [x] 创建消息消费者
* [x] 消息发送失败同步降级
* [x] 消息发送失败重试
* [x] 消费幂等处理

## 8. 文件模块

### 数据库

* [x] 创建 `file_record` 表

### MinIO

* [x] 接入 MinIO
* [x] 创建 bucket
* [ ] 配置文件上传大小限制
* [ ] 配置允许上传的文件类型

### 后端接口

* [x] 文件上传接口
* [x] 文件下载接口
* [x] 文件删除接口
* [x] 文件详情接口
* [x] 获取文件预览地址接口

### 业务接入

* [ ] 请假证明上传
* [ ] 报销凭证上传
* [ ] 制度文档上传
* [ ] 审批附件关联

## 9. 制度文档与搜索模块

### 数据库

* [x] 创建 `policy_document` 表
* [x] 创建 `policy_document_chunk` 表

### 制度文档

* [x] 上传制度文档
* [x] 编辑制度文档
* [x] 删除制度文档
* [x] 查询制度文档列表
* [x] 查询制度文档详情

### Elasticsearch

当前优先：

* [x] search-service 接入 Elasticsearch
* [x] 创建制度文档索引
* [x] 同步制度文档数据到 ES
* [x] 制度文档搜索接口
* [x] 新增制度文档 ES 冒烟脚本

后续扩展：

* [ ] 创建员工索引
* [ ] 创建审批单索引
* [ ] 同步员工数据到 ES
* [ ] 同步审批单数据到 ES
* [ ] 员工搜索接口
* [ ] 审批单搜索接口

当前已有 MySQL LIKE 搜索，带关键字搜索优先走 ES，ES 不可用时降级 MySQL。

## 10. XXL-JOB 定时任务

* [x] 接入 XXL-JOB
* [x] 创建每日考勤结算任务
* [x] 创建审批超时扫描任务
* [x] 创建月度考勤统计任务
* [ ] 创建 ES 数据同步任务
* [ ] 创建过期消息清理任务

任务说明：

* [x] 每日 23:50 结算当天考勤
* [x] 每 10 分钟扫描审批超时
* [x] 每月 1 日生成上月考勤统计
* [ ] 每天凌晨同步 ES 索引
* [ ] 每天凌晨清理过期消息

## 11. AI 办公助手模块

### 数据库

* [x] 创建 `ai_conversation` 表
* [x] 创建 `ai_message` 表
* [x] 创建 `ai_prompt_template` 表

### Spring AI

* [ ] 接入 Spring AI
* [ ] 配置大模型 API
* [ ] 封装 AI Chat Client
* [ ] 实现流式输出接口
* [x] 保存 AI 会话记录
* [x] 保存 AI 消息记录

### AI 能力

* [x] 制度问答（当前为 search-service 检索降级版）
* [x] 审批摘要
* [x] 智能填单
* [x] 审批风险提示（当前为规则降级版）

### 智能填单

* [x] 从自然语言中识别审批类型
* [ ] 提取请假开始时间
* [ ] 提取请假结束时间
* [ ] 提取请假原因
* [ ] 提取报销金额
* [ ] 提取报销类型
* [x] 生成审批单草稿
* [ ] 用户确认后提交审批

### 制度问答

* [x] 检索制度文档
* [x] 拼接上下文（当前为规则文本上下文）
* [ ] 调用大模型生成回答
* [x] 返回引用来源
* [x] AI 调用失败时降级为普通文档搜索

### 审批摘要

* [x] 根据审批单内容生成摘要
* [x] 根据报销金额生成风险提示
* [ ] 根据请假时长生成审批建议
* [ ] 审批人可查看 AI 辅助信息

## 12. 前端页面

### 基础页面

* [x] 登录页
* [x] 首页工作台
* [x] 个人中心
* [x] 403 页面
* [x] 404 页面

### 系统管理

* [x] 用户管理页
* [x] 角色管理页
* [ ] 菜单管理页

### 组织架构

* [ ] 部门管理页
* [ ] 岗位管理页
* [ ] 员工管理页
* [x] 组织架构树组件

### 审批中心

* [x] 创建审批页
* [x] 我的申请页
* [x] 我的待办页
* [x] 审批详情抽屉
* [x] 审批时间线组件
* [x] 审批操作弹窗

### 考勤管理

* [x] 今日打卡页
* [x] 我的考勤页
* [x] 部门考勤页
* [x] 月度统计页

### 消息中心

* [x] 我的消息页
* [x] 我的待办页
* [x] 消息已读功能

### 文件与制度

* [x] 文件上传组件
* [x] 制度文档管理页
* [x] 制度文档搜索页

### AI 助手

* [ ] AI 办公助手聊天页
* [ ] 智能填单入口
* [ ] 审批摘要展示组件
* [ ] 制度问答页面

## 13. 测试数据

* [x] 创建管理员账号
* [x] 创建普通员工账号
* [x] 创建部门负责人账号
* [x] 创建财务账号
* [x] 创建测试部门
* [x] 创建测试岗位
* [x] 创建测试员工
* [ ] 创建请假审批测试数据
* [ ] 创建加班审批测试数据
* [ ] 创建报销审批测试数据
* [ ] 创建考勤测试数据
* [ ] 创建制度文档测试数据

推荐测试账号：

```text
admin / 123456
manager / 123456
employee / 123456
finance / 123456
```

## 14. 简历亮点准备

* [x] 整理审批状态机设计
* [x] 整理审批人计算规则
* [x] 整理重复审批并发控制方案
* [ ] 整理 RabbitMQ 异步通知方案
* [ ] 整理 XXL-JOB 考勤结算方案
* [ ] 整理 Redis 缓存设计
* [ ] 整理 Elasticsearch 搜索设计
* [ ] 整理 MinIO 文件存储设计
* [ ] 整理 Spring AI 智能填单方案
* [ ] 整理系统架构图
* [ ] 整理数据库 ER 图
* [ ] 整理核心业务流程图

## 15. 开发优先级

### P0：必须完成

* [x] 登录认证
* [x] 用户管理
* [x] 组织架构
* [x] 审批申请
* [x] 审批处理
* [x] 审批记录
* [x] 考勤打卡
* [x] 我的待办
* [x] 基础前端页面

### P1：简历加分

* [x] Redis Token
* [ ] Redisson 防重复审批
* [x] RabbitMQ 异步通知
* [x] MinIO 文件上传
* [x] XXL-JOB 考勤结算
* [x] AI 审批摘要
* [x] AI 智能填单

### P2：进一步包装

* [x] Elasticsearch 搜索
* [ ] 制度问答 RAG
* [x] Gateway
* [x] Nacos
* [x] OpenFeign
* [x] 微服务拆分基础结构与核心业务迁移
* [x] Sentinel 限流熔断

## 16. Codex 开发建议顺序（历史记录）

1. 先生成数据库表结构和实体类。
2. 再生成基础 CRUD。
3. 然后完成登录认证。
4. 接着完成组织架构。
5. 再完成审批主流程。
6. 然后完成考勤打卡。
7. 接入消息待办。
8. 接入文件上传。
9. 接入定时任务。
10. 最后接入 AI 办公助手。

说明：这一段是项目早期建议。当前已经进入微服务联调阶段，新的执行顺序以本文顶部和 `docs/project-roadmap.md` 为准。

## 17. 当前进度与下一步

### 已完成 MVP

* [x] 后端单体模块化结构已完成，包含认证、用户、组织、审批、考勤、消息待办、文件记录、制度文档、AI 基础模块
* [x] 微服务父工程与服务骨架已完成，包含 common、api、gateway、auth、system、org、approval、attendance、message、file、search、ai 模块
* [x] 审批状态机、考勤状态、业务类型已整理为枚举
* [x] MySQL / Redis / Nacos / RabbitMQ / Elasticsearch / MinIO / XXL-JOB Docker Compose 配置已完成，MySQL 默认密码 `123456`
* [x] Docker 中间件统一启动完成，当前本机端口见 `docs/project-roadmap.md`
* [x] 前端 `smart-office-web` 已初始化，完成登录、工作台、用户列表、组织架构、审批中心、今日考勤基础页面
* [x] 已加入 Playwright 浏览器冒烟自测，覆盖登录、主页面访问、新建审批弹窗、考勤按钮可见性
* [x] 已完成 microservice 模式 Playwright 冒烟，覆盖 gateway 登录、审批提交与审批通过、消息通知、文件上传
* [x] ai-service 第一阶段接口已补齐，当前以规则和检索降级方式提供 AI 增强能力

### 下一步建议（历史快照，当前以文档顶部为准）

1. 接入 Elasticsearch：制度文档索引同步与全文检索。
2. 接入 Redis Token：登录态存储、退出失效、后续再补刷新策略。
## 18. 微服务版本迁移进度

* [x] common 已补齐统一返回、分页、业务异常、全局异常处理、BaseEntity、MyBatis-Plus 分页/乐观锁/自动填充配置
* [x] api 已补齐 system-service 用户认证 Feign 契约，支持按用户名查询认证用户、按 ID 查询当前用户、更新最后登录时间
* [x] system-service 已迁移用户认证所需的 sys_user / sys_role / sys_user_role 实体、Mapper、内部 Service 与 Controller
* [x] auth-service 已迁移登录、JWT、Spring Security 过滤器、当前用户、退出接口，并通过 Feign 调 system-service 获取用户信息
* [x] 所有启用 OpenFeign 的业务服务已补 `spring-cloud-starter-loadbalancer`，避免运行时报缺少负载均衡客户端
* [x] 已通过 `mvn -DskipTests compile` 与 `mvn test`
* [x] 已用 JDK 21 临时启动验证 auth-service，`/actuator/health` 返回 UP
* [x] system-service 数据库联调：已从 Nacos 加载 MyBatis 配置并连接 `smart_office_system`
* [x] gateway + Nacos + MySQL 完整登录链路联调

下一步（历史快照，当前以文档顶部为准）：

1. 联调 approval/message，验证审批提交后生成待办与通知。
2. 前端代理切到 gateway，补 Playwright 网关冒烟。
3. 按路线图继续补 MinIO、RabbitMQ、XXL-JOB、Elasticsearch。
## 19. org-service 迁移进度

* [x] org-service 已补齐 MyBatis-Plus / MySQL / Lombok 依赖与数据源配置
* [x] org-service 已迁移公司、部门、岗位、员工实体、Mapper、DTO、VO、Controller、Service
* [x] org-service 保留 `/api/org/**` API 路径，兼容当前前端代理和网关路由
* [x] org-service 通过 Feign 调 system-service 获取用户信息，不直接依赖 `sys_user` 表
* [x] common 已补 `PageQuery`，供微服务分页查询复用
* [x] 已通过 `mvn -pl smart-office-services/smart-office-org-service -am -DskipTests compile`
* [x] 已通过 `mvn -DskipTests compile` 与 `mvn test`
* [x] org-service 已用 JDK 21 临时启动，Spring 容器可启动
* [x] org-service 数据库联调：已切到 `smart_office_org`
* [x] org-service + system-service Feign 运行态联调

当前环境说明：

* Docker Desktop 已可正常使用。
* Docker 中间件使用本机可用端口：Nacos `8951/9951`、Redis `6380`、RabbitMQ `5673`、Elasticsearch transport `9459`。
* 命令行 `java` 默认可能仍是 JDK 11，Maven 使用 JDK 21；运行 Spring Boot 3 服务需显式使用 JDK 17/21。

## 20. 微服务独立库与 Nacos 配置进度

* [x] 参照 `D:\my_project\tjxt\tjxt-javaai02` 的服务写法，将微服务配置拆为 `application.yml` + `application-local.yml`。
* [x] 各服务 `application.yml` 只保留端口、服务名和服务私有库名，公共配置改由 Nacos 导入。
* [x] 所有微服务已接入 `spring-cloud-starter-alibaba-nacos-config`。
* [x] 已新增 Nacos 共享配置文件：`shared-spring.yaml`、`shared-logs.yaml`、`shared-feign.yaml`、`shared-mybatis.yaml`、`shared-redis.yaml`、`shared-mq.yaml`。
* [x] 已新增 Nacos 服务私有配置文件：gateway 路由、auth JWT、各业务服务模块占位配置。
* [x] Docker Compose 已增加 `nacos-config-importer`，Nacos 健康后自动发布 `docker/nacos/config/*.yaml`。
* [x] MySQL 已拆分微服务库：`smart_office_system`、`smart_office_org`、`smart_office_approval`、`smart_office_attendance`、`smart_office_message`、`smart_office_file`、`smart_office_search`、`smart_office_ai`。
* [x] 已新增 `docker/mysql/init/04-smart-office-microservices.sql`，新数据库卷会自动初始化微服务分库；旧卷可手动执行该脚本补库。
* [x] 已通过 `docker compose -f docker/docker-compose.yml config`。
* [x] 已通过 `mvn -DskipTests compile` 与 `mvn test`。
* [x] Docker 中间件已启动：MySQL、Redis、Nacos、RabbitMQ、Elasticsearch、MinIO、XXL-JOB Admin。
* [x] 已验证 system-service 从 Nacos 加载 `shared-mybatis.yaml`，连接 `smart_office_system`，`/actuator/health` 返回 UP。
* [x] 由于 Windows 排除端口和宿主服务占用，当前 Docker 宿主端口临时为：Nacos `8951/9951`、Redis `6380`、RabbitMQ `5673`、Elasticsearch transport `9459`。
* [x] gateway 已补 JWT 校验与 `X-User-Id` / `X-Username` / `X-Real-Name` 透传，并清理外部伪造身份 Header。
* [x] attendance-service 已迁移考勤规则、打卡、今日考勤、个人/部门记录、月度统计，切到 `smart_office_attendance`。
* [x] file-service 已迁移文件记录创建、详情、预览 URL、删除，切到 `smart_office_file`。
* [x] search-service 已迁移制度文档分页、创建、更新、详情、删除，切到 `smart_office_search`。
* [x] ai-service 已从占位推进到第一阶段接口：AI 会话、消息、制度问答、审批摘要、智能填单、风险提示。
* [ ] ai-service 待接入 Spring AI、真实模型配置和流式输出。

下一步（历史快照，当前以文档顶部为准）：

1. search-service 接 Elasticsearch 索引同步与全文检索。
2. auth-service 接 Redis Token 存储与退出失效。
3. ai-service 已推进到第一阶段接口，后续继续接 Spring AI 和前端页面。

## 21. gateway 鉴权与业务服务迁移进度

* [x] gateway 已新增 JWT 解析过滤器，放行 `/api/auth/login` 和 Actuator 健康接口，其余接口必须带 `Authorization: Bearer <token>`。
* [x] gateway 已补 `spring-cloud-starter-loadbalancer`，修复 `lb://smart-office-auth-service` 运行时找不到实例的问题。
* [x] gateway 的 JWT 配置已加入 `docker/nacos/config/smart-office-gateway.yaml`，与 auth-service 使用同一 issuer/secret。
* [x] attendance/file/search 的 `application-local.yml` 已导入 `shared-mybatis.yaml`，从 Nacos 加载独立库数据源。
* [x] org-service 已新增内部接口 `/internal/org/employees/department/{departmentId}/user-ids`，供 attendance-service 查询部门用户。
* [x] 已用 Docker 中间件 + 临时 Java 17 进程完成 gateway 冒烟：未带 token 返回 401，登录成功，`/api/auth/me`、`/api/attendance/today`、文件记录创建/详情、制度文档创建/分页均通过。
* [x] 已通过 `mvn -DskipTests compile`、`mvn -DskipTests package`、`mvn test`。
* [x] 前端已通过 microservice 模式 Playwright 冒烟，从 gateway 覆盖登录、审批、消息、文件上传。

下一步（历史快照，当前以文档顶部为准）：

1. 开始补 Elasticsearch 制度文档索引同步与全文检索。
2. 继续补 Redis Token 这些中间件型能力。
