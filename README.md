# Luna

[![Java](https://img.shields.io/badge/Java-8+-orange)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

> **在代码的阴影里，露娜为你标记真相。**

Luna（露娜），命名灵感来自《三角洲行动》中的侦察干员露娜——她的侦察箭矢洞悉战场，敌情分析与队友共享；正如 Luna 在 JVM 中无侵入地侦察运行时状态，标记问题真相，让排查不再是盲人摸象。

## Luna 是什么

Luna 是基于 Java Agent 的**运行时动态诊断工具**。无需修改业务代码、无需重启应用，即可按需注入观测逻辑，实时获取方法级运行时数据。

**解决的核心痛点：**

- **排障效率低** — 传统方式依赖加日志重启，无法实时获取运行时状态
- **侵入性强** — APM 方案需要修改代码或引入 SDK，影响业务稳定性
- **观测粒度粗** — 现有工具只能看到方法级耗时，无法深入到变量级别
- **缺乏动态性** — 问题出现时无法按需注入观测逻辑，必须预埋或重启

**Luna 不是什么：** 业务执行平台、业务逻辑扩展框架、持久业务状态管理器。Luna 只做观测、诊断、验证——安全扰动运行时行为，绝不替 JVM 执行业务。

## 功能截图

### 类浏览器 — 代码级注入
![类浏览器](docs/screenshot-class-detail.png)

### 日志监控 — 实时探针输出
![日志监控](docs/screenshot-log-viewer.png)

### 监控大盘 — 运行状态概览
![监控大盘](docs/screenshot-dashboard.png)

### 线程分析
![线程分析](docs/screenshot-thread-analyzer.png)

### 注入管理
![注入管理](docs/screenshot-injection-management.png)

### 插件管理
![插件管理](docs/screenshot-plugin-management.png)

## 核心能力

| 探针类型 | 功能 | 示例 |
|----------|------|------|
| **LOG** | 方法入参/返回值/变量日志 | `line before: user=${user}, age=${age}` |
| **TRACE** | 方法耗时追踪与阈值告警 | `processOrder took 230ms` |
| **SNAPSHOT** | 执行快照（局部变量+调用栈） | 完整方法执行现场 |
| **INVOCATION** | 调用链追踪（树形层级） | `OrderService.processOrder → UserService.getUser → UserDao.findById` |

### 关键特性

- **零侵入** — 不修改业务代码，基于 Java Instrumentation API 实现
- **不重启** — 支持运行时 Attach，诊断逻辑按需注入/移除
- **类隔离** — Agent 依赖通过 Bootstrap ClassLoader + Shade 隔离，不污染目标应用
- **安全可控** — 注入代码受沙箱保护，异常不传播到业务线程；RingBuffer 满时丢弃不阻塞
- **插件化** — 核心能力稳定，产品能力通过 ProbeHandler 插件扩展
- **实时推送** — WebSocket 实时推送诊断数据到 Web UI

## 架构

```
┌─────────────┐     ┌──────────────────────────────────────┐
│  Luna UI     │◄───►│  Luna Agent                          │
│  (Vue.js)    │ API │  ┌──────────┐  ┌─────────────────┐  │
│              │     │  │ Web Layer│  │ Plugin System   │  │
│              │     │  │ (Jetty)  │  │ ┌─────────────┐ │  │
│              │     │  └──────────┘  │ │ ProbeHandler │ │  │
│              │     │                │ │ LOG/TRACE/   │ │  │
│              │◄──WS─│  ┌──────────┐  │ │ SNAPSHOT/   │ │  │
│              │     │  │Bootstrap │  │ │ INVOCATION   │ │  │
│              │     │  │   JAR    │  │ └─────────────┘ │  │
│              │     │  └──────────┘  └─────────────────┘  │
│              │     │  ┌──────────────────────────────┐   │
│              │     │  │ ASM Bytecode Injection Engine │   │
│              │     │  └──────────────────────────────┘   │
└─────────────┘     └──────────────────────────────────────┘
                                    │
                              attach to JVM
                                    │
                    ┌───────────────────────────────┐
                    │     Target Java Application    │
                    └───────────────────────────────┘
```

## 快速开始

```bash
# 构建
mvn clean package -DskipTests

# 启动 Demo 应用（含 Agent）
cd example/scripts
start.bat

# 访问 UI
open http://localhost:8421
```

## 技术栈

- **Agent Core**: Java Instrumentation API, ASM 字节码操作, Shade 依赖隔离
- **Plugin System**: 可扩展的 ProbeHandler 插件架构
- **Web Layer**: Jetty 嵌入式服务, REST API + WebSocket 实时推送
- **Frontend**: Vue 3 + Monaco Editor + TailwindCSS

## 项目结构

```
luna-core/          # 核心库：字节码注入引擎、插件系统、探针运行时
luna-agent/         # Agent 入口：premain/agentmain、Web 服务、REST API
luna-ui/            # 前端：类浏览器、注入对话框、日志查看器
example/            # Demo 应用与脚本
docs/knowledge-base/# 知识库驱动开发文档
```

## 性能约束

| 指标 | 基准值 |
|------|--------|
| 启动延迟 | < 450ms |
| 单次插桩判定 | < 0.1ms |
| CPU 抖动 | < 2% |
| RingBuffer 满时策略 | 丢弃不阻塞业务线程 |

## License

Apache License 2.0
