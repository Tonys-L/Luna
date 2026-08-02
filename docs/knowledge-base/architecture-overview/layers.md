# 分层结构

## 架构风格

Luna 采用**整洁架构**（Clean Architecture）思想，核心原则为依赖倒置与核心隔离。

### 1.1 分层结构

```text
┌─────────────────────────────────────────────────┐
│              Frameworks & Drivers                 │
│   Luna-UI (Vue 3) / Luna-Attacher / Jetty        │
├─────────────────────────────────────────────────┤
│              Interface Adapters                   │
│   Controllers / WebSocket / ClassScanner          │
│   DispatcherServlet / VO / InjectionTestHarness   │
├─────────────────────────────────────────────────┤
│              Application Business Rules            │
│   InjectionService / VerificationService           │
│   PluginManagerImpl / AgentRuntime                 │
│   BuiltinPluginProvider                             │
├─────────────────────────────────────────────────┤
│              Enterprise Business Rules             │
│   InjectionPoint / PersistentInjection             │
│   InjectionLifecycle / InjectionQuery              │
│   InjectionLocation / ProbeHandler / CodeEngine    │
├─────────────────────────────────────────────────┤
│              Ports (Interfaces)                    │
│   Retransformer / InjectionStore / BytecodeLoader  │
│   BytecodePreviewer / InjectionVerifier            │
│   LocalVarValidator / WebServer                    │
└─────────────────────────────────────────────────┘
```

**整洁架构核心规则**：

```text
1. 依赖规则：依赖只能从外层指向内层，内层不知道外层的存在
2. 跨层通信：通过 Port（接口）实现，外层提供实现，内层定义接口
3. 核心隔离：业务规则不依赖任何技术框架
```

**三层隔离原则**：

```text
策略层（易变）— 路由策略、采样策略、重试策略
    ↓
核心层（稳定）— 能力契约、领域模型、业务规则、不变量
    ↑
实现层（可替换）— 存储实现、通信实现、字节码操作
```

---

## 各层职责

### Luna-Core

**职责**:
- 核心业务建模与注入契约定义（injection 包）
- 字节码分析与注入引擎（bytecode 包，含 ASM 和 ByteKit 两套实现）
- 条件表达式引擎（expression 包）
- 插件框架（plugin 包，含生命周期管理、加载器、注册表）
- 探针运行时（probe 包，RingBuffer + ProbeOutput）
- 核心能力注册（bootstrap.capability 包）
- 类分析与反编译（analysis 包）
- 自定义 Web 框架接口（infra.web 包）
- 插件市场（market 包）

**核心包结构**:

```text
fun.efto.luna.core
├── analysis/              # 类分析与反编译
│   ├── analyzer/          #   ClassAnalyzer, AnalyzerRegistry, AsmClassAnalyzer
│   └── decompile/         #   Decompiler, CfrDecompiler, DecompilerFactory
├── bootstrap/             # 启动与初始化
│   ├── capability/        #   CoreCapabilityRegistry, CoreCapabilityRecord, CapabilityKind
│   └── init/              #   Initializer, DefaultInitializer, InitializerManager
├── bytecode/              # 字节码引擎
│   ├── asm/               #   ASM 实现：injector, analyzer, assembler
│   └── bytekit/           #   ByteKit 实现：adapter, bridge, interceptor
├── expression/            # 条件表达式引擎
│   ├── ast/               #   ExpressionNode 体系（7 个节点类型）
│   ├── bytecode/          #   ExpressionBytecodeGenerator, VariableSlotResolver
│   ├── context/           #   EvaluationContext (ThreadLocal)
│   └── parser/            #   ExpressionParser, ConditionalExpressionParser
├── injection/             # 注入域模型（核心）
│   ├── code/              #   CompiledCode
│   ├── port/              #   Retransformer, InjectionStore, BytecodeLoader, InjectionVerifier 等
│   └── target/            #   InjectionTarget, InjectionLocation, MethodTarget, LineNumberTarget 等
├── infra/                 # 基础设施
│   ├── config/            #   ConfigManager
│   ├── type/              #   BaseType, RegisterableType, Registry, TypeRegistry
│   ├── util/              #   ClassNameUtils, AccessFlagsConverter, VisibleForTesting
│   └── web/               #   WebServer, @Controller, RouteEngine, ApiResult
├── market/                # 插件市场
│   ├── MarketClient, MarketController, PluginMetadata
│   └── PluginRepository, RepositoryConfig
├── plugin/                # 插件框架
│   ├── builtin/           #   内置插件：log, snapshot, trace, conditional
│   │   ├── line/          #     BeforeLineInjector, AfterLineInjector, LineNumberVisitor
│   │   ├── log/           #     LogPlugin, LogProbe, LogProbeHandler, ExpressionCodeEngine
│   │   ├── method/        #     MethodInjectionLocation, ExceptionExitInjectionLocation, InvokeInjectionLocation
│   │   ├── snapshot/      #     SnapshotPlugin, SnapshotProbe, SnapshotSerializer
│   │   └── trace/         #     TracePlugin, TraceProbe, TraceProbeHandler
│   ├── codegen/           #   DefaultBytecodeHelper, DefaultGenerateContext
│   ├── lifecycle/         #   PluginManagerImpl, ReadyGate, AffectedClassTracker
│   ├── loader/            #   PluginClassLoader, PluginDependencyResolver, PluginLoader
│   ├── registry/          #   InjectionTypeRegistry, ProbeHandlerRegistry
│   └── web/               #   PluginManagerController, PluginUIController, UiManifest
├── probe/                 # 探针运行时
│   ├── ProbeMessage, ProbeOutput (RingBuffer 4096)
│   └── BootstrapClassRegistry
├── transformer/           # ClassFileTransformer 适配层
    ├── ClassTransformer, DefaultClassTransformer
    ├── GlobalClassFileTransformer, ClassFileTransformerAdapter
    └── TransformerResult, InjectionResult
└── verification/          # 验证预览服务（从 InjectionService 提炼）
    └── VerificationService  #   preview / verify / injectWithTest
```

**设计原则**:
- 运行时依赖精简（slf4j-api、ASM、ASM-Tree、CFR、FastJSON、ByteKit），ASM 通过 Shade 重命名隔离，最终 Fat JAR 不暴露第三方类
- 核心层禁止出现 SQL / Redis / MQ / HTTP / RPC / 框架基础设施代码
- 不变量优先保护
- injection 包采用端口-适配器架构：核心层定义 Port 接口，外层提供实现

---

### Luna-Agent

**职责**:
- Agent 入口（premain / agentmain）
- 生命周期管理（AgentRuntime 10 步启动流程）
- 类隔离加载器（LunaAgentClassLoader）
- 内嵌 Jetty Web 服务器
- 自定义 MVC 框架（DispatcherServlet + RouteEngine）
- WebSocket 实时推送
- 类扫描与过滤

**核心包结构**:

```text
fun.efto.luna.agent
├── Agent.java             # premain/agentmain 入口
├── adapter/               # InjectionTestHarnessAdapter
├── clazz/                 # ClassScanner, ClassResourceHelper, LoadedClass
│                          # ExcludeClassFilter 体系（Agent/Array/ClassLoader/Generated）
├── log/                   # LoggerInitializer
├── runtime/               # AgentRuntime, AgentRuntimeContext, BuiltinPluginProvider
└── web/                   # Web 层
    ├── controller/        #   7 个 Controller（Status/Class/Injection/Metrics/Capability/Probe/Test）
    ├── mvc/               #   DispatcherServlet, ServletRequestContext
    ├── vo/                #   15 个 View Object
    ├── ws/                #   LogDispatcher, LogWebSocketEndpoint, LogWebSocketServlet
    ├── JettyWebServer, JettyConfiguration
    ├── MethodInvokeService, MetricsService
```

**设计原则**:
- 通过反射切换 ClassLoader 实现类隔离
- Agent 自身日志与目标应用日志完全隔离
- MVC 框架与 Servlet 容器解耦（DispatcherServlet 委托给 RouteEngine）

**子包职责表**:

| 子包 | 职责 |
|------|------|
| `agent/` | Agent 入口类（薄 Adapter），仅 ClassLoader 切换 + 反射调用 AgentRuntime |
| `agent/runtime/` | AgentRuntime（启动编排）、AgentRuntimeContext（运行期上下文）、BuiltinPluginProvider（内置插件发现） |
| `agent/web/` | JettyWebServer、JettyConfiguration |
| `agent/web/controller/` | 7 个 Controller（Status/Class/Injection/Test/Metrics/Capability/Probe） |
| `core/plugin/web/` | 2 个 Controller（PluginManager/PluginUI） |
| `core/market/` | 1 个 Controller（Market） |
| `agent/web/mvc/` | 自定义 MVC 框架（DispatcherServlet、路由、参数解析） |
| `agent/web/ws/` | WebSocket（LogWebSocketServlet/Endpoint/Dispatcher） |
| `agent/web/vo/` | 视图对象（VO） |
| `agent/clazz/` | 类扫描、类资源加载、类过滤 |
| `agent/log/` | LoggerInitializer（Log4j2 上下文隔离） |
| `agent/adapter/` | InjectionTestHarnessAdapter |

---

### Luna-Attacher

**职责**:
- 运行时 Attach 工具
- 通过 PID 将 Agent 注入目标 JVM

**包含模块**:
- `luna-attacher/src/main/java/fun/efto/luna/attacher/Attacher.java`

**设计原则**:
- 轻量级工具，仅依赖 JDK Attach API

---

### Luna-UI

**职责**:
- 诊断控制台（6 个视图 + 9 个组件）
- 实时监控看板
- 反编译查看器
- 规则编辑器
- 插件管理

**核心结构**:

```text
luna-ui/src/
├── views/                 # 6 个视图
│   ├── Dashboard.vue      #   监控看板
│   ├── LogViewer.vue      #   日志查看
│   ├── ClassTreeViewer.vue#   类树浏览
│   ├── ConfigurationViewer.vue # 规则配置
│   ├── PluginManager.vue  #   插件管理
│   └── ThreadAnalyzer.vue #   线程分析
├── components/            # 9 个组件
│   ├── DebuggerPanel.vue  #   调试面板
│   ├── InjectionDialog.vue#   注入对话框
│   ├── InjectionDetailOverlay.vue
│   ├── ClassDetail.vue / ClassHeader.vue / ClassOutline.vue
│   ├── ThreadDetail.vue / ThreadList.vue
│   └── VariableTreeNode.vue
├── utils/                 # api.js, request.js, plugin-registry.js
└── i18n/                  # 国际化（en/zh）
```

**设计原则**:
- Vue 3 + Vite + Element Plus + Monaco Editor
- 通过 HTTP API 和 WebSocket 与 Agent 通信
- E2E 测试：8 个 Playwright spec 文件

**视图职责表**:

| 视图 | 职责 |
|------|------|
| `Dashboard.vue` | 系统概览、JVM 指标 |
| `ClassTreeViewer.vue` | 类浏览器、反编译、注入操作 |
| `PluginManager.vue` | 插件列表、启用/禁用/卸载 |
| `LogViewer.vue` | 实时日志流（WebSocket） |
| `ThreadAnalyzer.vue` | 线程转储、死锁检测 |
| `ConfigurationViewer.vue` | 配置管理 |

**组件职责表**:

| 组件 | 职责 |
|------|------|
| `InjectionDialog.vue` | 注入对话框 |
| `InjectionDetailOverlay.vue` | 注入详情覆盖层 |
| `ClassDetail.vue` | 类详情面板 |
| `ClassOutline.vue` | 类结构大纲 |
| `ClassHeader.vue` | 类头部信息 |
| `DebuggerPanel.vue` | 调试面板 |
| `ThreadList.vue` | 线程列表 |
| `ThreadDetail.vue` | 线程详情 |
| `VariableTreeNode.vue` | 变量树节点 |

---

## 变更记录

| 日期 | 变更内容 | 变更人 | 关联变更 |
|------|----------|--------|----------|
| 2026/06/16 | 初始版本 | Tony.L | — |
| 2026/06/16 | 修正架构术语（洋葱→整洁），补充实际代码结构 | Tony.L | — |
| 2026/06/17 | 从 architecture-overview.md 拆分为独立文件 | Tony.L | architecture-overview.md 拆分 |
| 2026/08/02 | 新增 verification 包至核心包结构；Application Business Rules 层新增 VerificationService | Tony.L | #feat/phase2-verification-extract |
