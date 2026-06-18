# Docker 依赖环境

本地开发环境统一使用 Docker Compose 启动中间件。

```powershell
docker compose -f docker/docker-compose.yml up -d
```

当前仓库按 Windows 本机可用端口做了映射，尽量避免和宿主机已有 Redis、RabbitMQ、Nacos 等服务冲突。MySQL 仍使用 `3306`，默认密码为 `123456`。

当前端口：

| 服务 | 容器端口 | 宿主端口 |
|---|---:|---:|
| MySQL | 3306 | 3306 |
| Redis | 6379 | 6380 |
| Nacos | 8848 | 8951 |
| Nacos gRPC | 9848 | 9951 |
| RabbitMQ | 5672 | 5673 |
| RabbitMQ Management | 15672 | 15672 |
| Elasticsearch | 9200 | 9200 |
| Elasticsearch Transport | 9300 | 9459 |
| MinIO API | 9000 | 9000 |
| MinIO Console | 9001 | 9001 |
| XXL-JOB Admin | 8080 | 8088 |

后端连接 Docker MySQL 时使用：

```powershell
$env:MYSQL_PORT='3306'
$env:MYSQL_PASSWORD='123456'
```

微服务本地注册到 Nacos 时使用：

```powershell
$env:NACOS_SERVER_ADDR='127.0.0.1:8951'
$env:REDIS_PORT='6380'
$env:RABBITMQ_PORT='5673'
```

MySQL 首次启动会自动执行：

```text
smart-office-server/src/main/resources/db/schema.sql
smart-office-server/src/main/resources/db/data.sql
docker/mysql/init/04-smart-office-microservices.sql
docker/mysql/init/03-xxl-job.sql
```

默认控制台账号：

| 服务 | 地址 | 账号 |
|---|---|---|
| Nacos | http://localhost:8951/nacos | 无认证 |
| RabbitMQ | http://localhost:15672 | smartoffice / 123456 |
| MinIO | http://localhost:9001 | smartoffice / smartoffice123456 |
| XXL-JOB | http://localhost:8088/xxl-job-admin | admin / 123456 |

XXL-JOB 初始化脚本会预置 `smart-office-job-executor` 执行器，以及 `attendanceDailySettlementJob`、`attendanceMonthlySummaryJob` 两条考勤任务，默认处于停止状态。

如果需要重建数据库，删除 Docker volume 后重新启动：

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d
```
