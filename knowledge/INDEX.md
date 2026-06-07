# Luna 项目知识库 - 索引地图

> **项目定位**: 基于 Java Agent 的运行时动态诊断基础设施 (Runtime Observability & Diagnostics Infrastructure)
> **版本**: 1.0-SNAPSHOT
> **维护者**: Tony.L (286269159@qq.com)
> **更新日期**: 2026/06/07

---

## 知识库结构

```
knowledge/
├── INDEX.md                    ← 你正在这里（总索引）
├── architecture/
│   ├── architecture.md         ← 系统架构全景
│   ├── module-responsibility.md ← 模块职责与边界
│   └── design-patterns.md      ← 设计模式与核心机制
├── business/
│   ├── business-flow.md        ← 核心业务流程
│   ├── injection-model.md      ← 注入域模型
│   └── plugin-architecture.md  ← 插件架构体系
├── contracts/
│   ├── api-reference.md        ← API 接口契约
│   └── data-model.md           ← 数据模型定义
├── guides/
│   ├── development-standards.md ← 开发规范
│   ├── commit-conventions.md   ← 提交规范
│   └── testing-guide.md        ← 测试指南
├── decisions/
│   └── architecture-decisions.md ← 架构决策记录
└── lessons/
    └── README.md               ← 经验教训索引
```

---

## 快速导航

### 架构层

| 文档 | 说明 | 关键词 |
|------|------|--------|
| [系统架构全景](architecture/architecture.md) | 模块组成、依赖关系、分层架构、技术栈 | 洋葱架构、四模块、类隔离 |
| [模块职责与边界](architecture/module-responsibility.md) | 每个模块负责什么/不负责什么 | Core/Agent/Attacher/UI |
| [设计模式与核心机制](architecture/design-patterns.md) | 模板方法、策略模式、注册表体系、SPI | Registry、ProbeHandler、CodeEngine |

### 业务层

| 文档 | 说明 | 关键词 |
|------|------|--------|
| [核心业务流程](business/business-flow.md) | 注入流程、规则流程、启动流程、数据流 | inject/retransform/rule |
| [注入域模型](business/injection-model.md) | InjectionPoint/PersistentInjection/三层模型 | Repository/Registry/Service |
| [插件架构体系](business/plugin-architecture.md) | LunaPlugin/ProbeHandler/PluginManager/生命周期 | load/unload/hot-swap |

### 契约层

| 文档 | 说明 | 关键词 |
|------|------|--------|
| [API 接口契约](contracts/api-reference.md) | 全部 REST API + WebSocket 接口 | /api/injections, /api/rules, /ws/log |
| [数据模型定义](contracts/data-model.md) | PersistentInjection/InjectRequest/InjectionRule | 字段定义、协议前缀 |

### 规范层

| 文档 | 说明 | 关键词 |
|------|------|--------|
| [开发规范](guides/development-standards.md) | 编码标准、类头声明、ASM 透明化、性能红线 | TDD、0.1ms、无锁队列 |
| [提交规范](guides/commit-conventions.md) | Git 提交消息格式、分支策略 | Conventional Commits |
| [测试指南](guides/testing-guide.md) | 单元测试、E2E 测试、回归测试 | JUnit5/Playwright |

### 决策层

| 文档 | 说明 | 关键词 |
|------|------|--------|
| [架构决策记录](decisions/architecture-decisions.md) | 关键架构决策的背景、方案、取舍 | ADR |

### 经验层

| 文档 | 说明 | 关键词 |
|------|------|--------|
| [经验教训索引](lessons/README.md) | Bug 复盘、设计失误、架构问题 | VerifyError、并发、性能 |

---

## 项目概览

### 定位演进

| 阶段 | 定位 |
|------|------|
| **初始** | 动态日志埋点工具 |
| **当前** | 微内核 + 插件架构的运行时诊断平台 |
| **演进方向** | Runtime Observability & Diagnostics Infrastructure |

### 四大模块

| 模块 | 职责 | 技术栈 |
|------|------|--------|
| **luna-core** | 核心业务建模、注入契约、字节码引擎、插件框架 | Java 8, ASM 9.4, 自研无锁队列 |
| **luna-agent** | Agent 入口、生命周期管理、Web 服务器、类隔离 | Instrumentation, Jetty 9.4, 自研 MVC |
| **luna-attacher** | 运行时 Attach 工具 | VirtualMachine API |
| **luna-ui** | 诊断控制台、实时监控、反编译查看器 | Vue 3, Vite, Element Plus, Monaco Editor |

### 核心能力

- **方法级注入**: ENTER / EXIT / AROUND
- **行号级注入**: LINE_BEFORE / LINE_AFTER
- **条件表达式引擎**: `${condition}::protocol:content`
- **变量快照捕获**: 局部变量 + 参数 + 字段
- **方法耗时追踪**: trace:start / trace:end / trace:alert
- **插件热加载/卸载**: StampedLock 并发安全
- **源码反编译**: CFR 引擎 + 行号注释

### 性能红线

| 指标 | 红线 |
|------|------|
| 单次插桩判定 | < 0.1ms |
| 启动延迟 | < 450ms |
| 内存静态增量 | ~22MB |
| CPU 运行时抖动 | < 2% (100+TPS) |
