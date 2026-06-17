# Docker 依赖环境

本地开发环境统一使用 Docker Compose 启动中间件。

```powershell
cd docker
docker compose up -d
```

如果宿主机已经启动了本地 MySQL、RabbitMQ 或其他占用同名端口的服务，`3306`、`5672` 等统一端口会导致容器启动失败。当前仓库默认按正式端口映射，启动前需要先释放这些端口。

默认端口：

| 服务 | 容器端口 | 宿主端口 |
|---|---:|---:|
| MySQL | 3306 | 3306 |
| Redis | 6379 | 6379 |
| Nacos | 8848 | 8848 |
| Nacos gRPC | 9848 | 9848 |
| RabbitMQ | 5672 | 5672 |
| RabbitMQ Management | 15672 | 15672 |
| Elasticsearch | 9200 | 9200 |
| Elasticsearch Transport | 9300 | 9300 |
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
$env:NACOS_SERVER_ADDR='127.0.0.1:8848'
```

MySQL 首次启动会自动执行：

```text
smart-office-server/src/main/resources/db/schema.sql
smart-office-server/src/main/resources/db/data.sql
docker/mysql/init/03-xxl-job.sql
```

默认控制台账号：

| 服务 | 地址 | 账号 |
|---|---|---|
| Nacos | http://localhost:8848/nacos | 无认证 |
| RabbitMQ | http://localhost:15672 | smartoffice / 123456 |
| MinIO | http://localhost:9001 | smartoffice / smartoffice123456 |
| XXL-JOB | http://localhost:8088/xxl-job-admin | admin / 123456 |

如果需要重建数据库，删除 Docker volume 后重新启动：

```powershell
docker compose down -v
docker compose up -d
```
