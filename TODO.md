# TODO

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

* [ ] MySQL
* [x] Redis

后续再接入：

* [ ] RabbitMQ
* [x] Elasticsearch
* [x] MinIO
* [ ] XXL-JOB

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
* [ ] Redis 存储登录 Token
* [ ] 用户退出时删除 Token

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
* [ ] 报销金额大于 1000 元，部门负责人 + 财务审批
* [x] 审批人不存在时给出明确错误提示
* [x] 审批状态变更合法性校验
* [ ] 重复审批校验
* [x] 非审批人不能审批校验

### 并发控制

* [ ] 使用数据库乐观锁防止重复审批
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
* [ ] 未上班打卡为上班缺卡
* [ ] 未下班打卡为下班缺卡
* [ ] 请假审批通过后同步影响考勤状态
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
* [ ] 审批超时后通知审批人
* [ ] 考勤异常后通知员工

### RabbitMQ

* [ ] 创建审批通知队列
* [ ] 创建考勤通知队列
* [ ] 创建消息消费者
* [ ] 消息发送失败重试
* [ ] 消费幂等处理

## 8. 文件模块

### 数据库

* [x] 创建 `file_record` 表

### MinIO

* [ ] 接入 MinIO
* [ ] 创建 bucket
* [ ] 配置文件上传大小限制
* [ ] 配置允许上传的文件类型

### 后端接口

* [ ] 文件上传接口
* [ ] 文件下载接口
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

* [ ] 接入 Elasticsearch
* [ ] 创建员工索引
* [ ] 创建审批单索引
* [ ] 创建制度文档索引
* [ ] 同步员工数据到 ES
* [ ] 同步审批单数据到 ES
* [ ] 同步制度文档数据到 ES
* [ ] 员工搜索接口
* [ ] 审批单搜索接口
* [ ] 制度文档搜索接口

第一阶段可先实现 MySQL LIKE 搜索，后续替换为 ES。

## 10. XXL-JOB 定时任务

* [ ] 接入 XXL-JOB
* [ ] 创建每日考勤结算任务
* [ ] 创建审批超时扫描任务
* [ ] 创建月度考勤统计任务
* [ ] 创建 ES 数据同步任务
* [ ] 创建过期消息清理任务

任务说明：

* [ ] 每日 23:50 结算当天考勤
* [ ] 每 10 分钟扫描审批超时
* [ ] 每月 1 日生成上月考勤统计
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

* [ ] 制度问答
* [x] 审批摘要
* [x] 智能填单
* [ ] 审批风险提示

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

* [ ] 检索制度文档
* [ ] 拼接上下文
* [ ] 调用大模型生成回答
* [ ] 返回引用来源
* [ ] AI 调用失败时降级为普通文档搜索

### 审批摘要

* [x] 根据审批单内容生成摘要
* [ ] 根据报销金额生成风险提示
* [ ] 根据请假时长生成审批建议
* [ ] 审批人可查看 AI 辅助信息

## 12. 前端页面

### 基础页面

* [x] 登录页
* [x] 首页工作台
* [ ] 个人中心
* [ ] 403 页面
* [ ] 404 页面

### 系统管理

* [x] 用户管理页
* [ ] 角色管理页
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
* [ ] 审批详情页
* [ ] 审批时间线组件
* [ ] 审批操作弹窗

### 考勤管理

* [x] 今日打卡页
* [ ] 我的考勤页
* [ ] 部门考勤页
* [ ] 月度统计页

### 消息中心

* [ ] 我的消息页
* [ ] 我的待办页
* [ ] 消息已读功能

### 文件与制度

* [ ] 文件上传组件
* [ ] 制度文档管理页
* [ ] 制度文档搜索页

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

* [ ] 整理审批状态机设计
* [ ] 整理审批人计算规则
* [ ] 整理重复审批并发控制方案
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

* [ ] Redis Token
* [ ] Redisson 防重复审批
* [ ] RabbitMQ 异步通知
* [ ] MinIO 文件上传
* [ ] XXL-JOB 考勤结算
* [ ] AI 审批摘要
* [ ] AI 智能填单

### P2：进一步包装

* [ ] Elasticsearch 搜索
* [ ] 制度问答 RAG
* [x] Gateway
* [x] Nacos
* [x] OpenFeign
* [ ] 微服务拆分
* [ ] Sentinel 限流熔断

## 16. Codex 开发建议顺序

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

不要一开始就做微服务拆分，也不要一开始就接入全部中间件。

## 17. 当前进度与下一步

### 已完成 MVP

* [x] 后端单体模块化结构已完成，包含认证、用户、组织、审批、考勤、消息待办、文件记录、制度文档、AI 占位模块
* [x] 微服务父工程与服务骨架已完成，包含 common、api、gateway、auth、system、org、approval、attendance、message、file、search、ai 模块
* [x] 审批状态机、考勤状态、业务类型已整理为枚举
* [x] MySQL / Redis / Nacos / RabbitMQ / Elasticsearch / MinIO / XXL-JOB Docker Compose 配置已完成，MySQL 默认密码 `123456`
* [ ] 统一端口实测启动完成：当前 Redis / Nacos / Elasticsearch / MinIO 已启动，MySQL `3306` 与 RabbitMQ `5672` 仍被宿主机进程占用
* [x] 前端 `smart-office-web` 已初始化，完成登录、工作台、用户列表、组织架构、审批中心、今日考勤基础页面
* [x] 已加入 Playwright 浏览器冒烟自测，覆盖登录、主页面访问、新建审批弹窗、考勤按钮可见性

### 下一步建议

1. 先迁 system-service：拆出用户、角色、菜单实体、Mapper、Service、Controller，并补内部用户查询 Feign 实现。
2. 再迁 auth-service：拆出登录认证、JWT、安全过滤器，并通过 Feign 调 system-service 获取用户信息。
3. 接着迁 org-service：拆公司、部门、岗位、员工，并处理与 system-service 用户信息的依赖。
4. 然后迁 approval / attendance / message：优先跑通审批提交、待办生成、考勤打卡这条主链路。
5. 业务服务迁完后，前端代理切到 gateway:9000，再补完整 Playwright 网关冒烟测试。
## 18. 微服务版本迁移进度

* [x] common 已补齐统一返回、分页、业务异常、全局异常处理、BaseEntity、MyBatis-Plus 分页/乐观锁/自动填充配置
* [x] api 已补齐 system-service 用户认证 Feign 契约，支持按用户名查询认证用户、按 ID 查询当前用户、更新最后登录时间
* [x] system-service 已迁移用户认证所需的 sys_user / sys_role / sys_user_role 实体、Mapper、内部 Service 与 Controller
* [x] auth-service 已迁移登录、JWT、Spring Security 过滤器、当前用户、退出接口，并通过 Feign 调 system-service 获取用户信息
* [x] 所有启用 OpenFeign 的业务服务已补 `spring-cloud-starter-loadbalancer`，避免运行时报缺少负载均衡客户端
* [x] 已通过 `mvn -DskipTests compile` 与 `mvn test`
* [x] 已用 JDK 21 临时启动验证 auth-service，`/actuator/health` 返回 UP
* [ ] system-service 数据库联调：服务可启动，当前 `/actuator/health` 因 Docker/MySQL 未连通返回 DOWN
* [ ] gateway + Nacos + MySQL 完整登录链路联调

下一步：

1. 先恢复当前终端 Docker CLI 与 Docker Desktop 的连接，启动 MySQL / Nacos。
2. 启动 system-service、auth-service、gateway，验证 `/api/auth/login` 能通过 gateway 登录。
3. 继续迁移 org-service，补公司、部门、岗位、员工基础查询与管理接口。
