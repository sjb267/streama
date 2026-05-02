# streama-admin

`streama-admin` 是 Streama 的后台管理端（前端）。

## 技术栈

- Vue 3 + Vite
- Pinia
- Vue Router
- Element Plus
- Axios

与 `streama-web` 保持同版本工具链与核心依赖，方便并行开发与维护。

## 启动方式

```sh
npm install
npm run dev
```

## 接口联调

- 本地开发默认将 `/admin` 代理到 `http://127.0.0.1:7070`
- 可通过环境变量 `VITE_API_TARGET` 覆盖后端地址

## 当前结构

- 管理端基础布局（侧边导航 + 顶部栏）
- 仪表盘首页
- 用户管理/视频管理/审核中心/系统设置占位页面
- 与 `streama-web` 一致的 `/api` 代理配置
