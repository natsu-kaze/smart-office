# Smart Office Web

Vue 3 + Vite + TypeScript + Pinia + Element Plus 前端项目。

## 开发启动

```powershell
npm install
npm run dev
```

默认地址：

```text
http://localhost:5173
```

开发环境通过 Vite proxy 将 `/api` 转发到后端：

```text
http://localhost:8080
```

切换到微服务网关代理：

```powershell
npm run dev -- --mode microservice
```

微服务模式下 `/api` 会转发到：

```text
http://localhost:8000
```

测试账号：

```text
admin / 123456
manager / 123456
employee / 123456
finance / 123456
```

## 构建

```powershell
npm run build
```

构建产物输出到 `dist/`。

## E2E 自测

后端和前端启动后执行：

```powershell
npm run test:e2e
```
