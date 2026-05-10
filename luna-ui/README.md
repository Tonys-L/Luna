# Luna 类浏览器前端

基于Vue 3和Element Plus的类浏览器前端界面，用于展示JVM中已加载的类信息。

## 功能特性

- 树形结构展示类加载器和类层次结构
- 搜索类名和包名
- 查看类的详细信息（字段、方法、接口等）
- 反编译源码查看
- 响应式布局设计
- 暗黑主题UI
- 实时展示已加载类的数量

## 技术栈

- Vue 3 (Composition API)
- Element Plus UI 组件库
- Vite 构建工具

## 开发环境

### 环境要求

- Node.js >= 16.0.0
- npm >= 7.0.0

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

默认访问地址: http://localhost:3000

### 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist` 目录。

## 项目结构

```
src/
├── components/        # 公共组件
├── views/             # 页面视图
├── utils/             # 工具函数
├── assets/            # 静态资源
├── App.vue            # 根组件
└── main.js            # 入口文件
```

## UI 特性

### 暗黑主题
项目采用暗黑主题设计，减少长时间使用的眼部疲劳，提供更好的视觉体验。

### 类数量展示
在顶部导航栏实时显示已加载类的总数，帮助用户快速了解当前JVM中的类加载情况。

## API 接口

前端通过以下API接口与后端通信：

- `GET /api/classes` - 获取类列表
- `GET /api/class/{className}` - 获取类详细信息
- `GET /api/decompile/{className}` - 获取反编译代码

## 后续扩展

1. 添加类之间的关系图谱展示
2. 支持类的对比功能
3. 添加性能监控面板
4. 支持导出类信息为多种格式