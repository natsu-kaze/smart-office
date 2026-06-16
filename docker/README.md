# Docker 依赖环境

第一阶段只启动 MySQL 和 Redis。

```powershell
cd docker
docker compose up -d
```

默认端口：

| 服务 | 容器端口 | 宿主端口 |
|---|---:|---:|
| MySQL | 3306 | 3307 |
| Redis | 6379 | 6379 |

后端连接 Docker MySQL 时使用：

```powershell
$env:MYSQL_PORT='3307'
$env:MYSQL_PASSWORD='123456'
```

MySQL 首次启动会自动执行：

```text
smart-office-server/src/main/resources/db/schema.sql
smart-office-server/src/main/resources/db/data.sql
```

如果需要重建数据库，删除 Docker volume 后重新启动：

```powershell
docker compose down -v
docker compose up -d
```
