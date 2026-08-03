# 核心业务能力

> **文档定位**: 定义系统核心业务能力、支撑能力与外部依赖
> **更新时机**: 新增业务能力、调整能力定义时更新
> **读者**: 架构师、开发者

---

## 2. 核心业务能力

### 2.1 动态字节码注入

**能力定义**: 在运行时向目标 JVM 的指定类/方法/行号动态注入观测代码，无需重启应用。

**业务规则**:
- 注入位置（InjectionLocation）：ENTER / EXIT / AROUND / EXCEPTION_EXIT / INVOKE / BEFORE / AFTER
- 注入后立即生效（通过 retransformClasses）
- 同一类多次注入采用 "Clean Slate + Re-apply" 策略
- 注入点可移除，移除后恢复原始字节码
- 支持临时注入（ephemeral），不持久化
- 支持分组注入（groupId），批量管理

**变化点**:
- 未来可能支持更多注入位置（如 FieldAccess）
- Runtime Semantic Architecture 演进可能改变注入定位模型

**对应代码**:
- `luna-core/injection/` — InjectionService, InjectionPoint, InjectionRegistry
- `luna-core/injection/port/` — Retransformer, InjectionStore, BytecodeLoader, InjectionVerifier
- `luna-core/injection/target/` — InjectionLocation, InjectionTarget
- `luna-core/transformer/` — GlobalClassFileTransformer, DefaultClassTransformer

---

### 2.1.1 行号注入已知限制

> 行号注入依赖 LineNumberTable 和 LocalVariableTable，以下场景存在已知限制。

| # | 场景 | 严重度 | 状态 | 规避方案 |
|---|------|--------|------|---------|
| 1 | 无 `-g:none` 编译的类 | 🔴 高 | ✅ 已有明确错误提示 | 使用 `-g` 编译 |
| 2 | 仅有 `-g:lines`（无变量表） | 🟡 中 | ⚠️ 变量捕获为空 | 使用 `-g:vars` 编译 |
| 3 | Lambda 表达式注入 | 🟡 中 | ⚠️ 需通过合成方法名 `lambda$方法名$N` | 选择合成方法名 |
| 4 | 方法引用注入 | 🟡 中 | ❌ 不支持 | 注入目标方法 |
| 5 | 同一行多指令 | 🟡 中 | ⚠️ line_before 只触发一次 | 无 |
| 6 | 空/注释行 | 🟢 低 | ❌ 报错 | 选择有效代码行 |
| 7 | catch 块变量捕获 | 🟡 中 | ⚠️ 可能不精确 | 检查返回变量列表 |
| 8 | try-with-resources 变量 | 🟡 中 | ⚠️ 未验证 | 专项测试 |
| 9 | 不同 ClassLoader 加载的类 | 🟡 中 | ⚠️ 部分支持 | — |

**详细说明**:

- **编译调试信息依赖**：无 LineNumberTable 时行号注入完全不可用；仅有行号表时变量捕获为空
- **Lambda/匿名类**：Lambda 通过合成方法名注入，但 this 引用和外部变量捕获有限制；方法引用无字节码方法体，不可注入
- **行号映射精度**：一行多指令只触发一次；空/注释行无 LineNumberNode
- **异常处理**：catch 块内异常参数作用域可能不精确；try-with-resources 变量未验证
- **类加载器**：Bootstrap ClassLoader 加载的核心类默认不可修改
- **其他 JVM 语言**：Kotlin 部分支持（变量名后缀问题）；Scala/Groovy 未验证
- **性能**：循环内高频触发时 RingBuffer 可能丢弃（容量 4096）；snapshot 在百万次/秒循环中可能产生 GC 压力

---

### 2.2 条件表达式求值

**能力定义**: 在注入点设置条件表达式，仅当条件满足时才触发观测逻辑。

**业务规则**:
- 表达式语法：`${condition}::protocol:content`
- 支持变量引用：`$1`（参数）、`$varName`（局部变量）、`$this.field`（字段）
- 支持运算符：算术、比较、逻辑
- 条件求值失败时静默跳过（不影响业务）
- 预编译缓存避免重复解析（ConditionRegistry）
- ThreadLocal 复用 EvaluationContext

**变化点**:
- 未来可能支持更多内置函数
- 表达式引擎可能独立为可替换模块

### 协议前缀格式

code 字段格式：`${condition}::protocol:content`

2 种协议：
- `log:内容` — 日志输出
- `snapshot:` — 变量快照捕获

> TRACE 探针已改为 method_around 单注入点模型，不再使用协议前缀。threshold 参数通过 ProbeHandler.getConfigSchema() 传递，handle() 根据 GenerateContext.phase() 自动生成 onTraceStart 或 onTraceEnd 代码。

条件前缀可选，省略时无条件执行。

**对应代码**:
- `luna-core/expression/` — ConditionRegistry, ExpressionParser, ConditionalExpressionParser
- `luna-core/expression/ast/` — ExpressionNode 体系（7 个节点类型）
- `luna-core/expression/bytecode/` — ExpressionBytecodeGenerator
- `luna-core/expression/context/` — EvaluationContext

---

### 2.3 变量快照捕获

**能力定义**: 在注入点捕获当前可见的局部变量、方法参数和对象字段的值。

**业务规则**:
- 快照包含变量名、类型和值
- 对象类型支持深度/截断控制
- 序列化采用防御式设计（SnapshotSerializer），避免业务异常传播
- 通过 ProbeOutput.offer() 投递到 RingBuffer

**变化点**:
- 未来可能支持更精细的深度控制
- 快照数据可能进入 Observation Store

**对应代码**:
- `luna-core/plugin/builtin/snapshot/` — SnapshotPlugin, SnapshotProbe, SnapshotSerializer

---

### 2.4 方法耗时追踪

**能力定义**: 追踪方法执行耗时，支持阈值过滤和慢方法告警。

**业务规则**:
- 使用 method_around 单注入点模型，一次注入同时覆盖入口和出口
- Phase.ENTER 时调用 onTraceStart() 记录入口时间（ThreadLocal）
- Phase.EXIT 时调用 onTraceEnd() 计算耗时，超过阈值时输出
- 耗时计算不包含注入逻辑本身的开销
- 用户只需选择"方法耗时"即可，不需要关心入口/出口的实现细节

**变化点**:
- 未来可能支持百分位统计（P50/P95/P99）
- 可能集成 Micrometer / Prometheus

**对应代码**:
- `luna-core/plugin/builtin/trace/` — TracePlugin, TraceProbe, TraceProbeHandler

---

### 2.4.1 方法耗时 Capability 设计

> 方法耗时是 builtin plugin capability，不是 core capability。它依赖核心能力 method-target / bytecode-assembly / probe-handler-dispatch / injection-lifecycle / transform-pipeline / runtime-storage。

**CapabilityDescriptor**:

```text
capabilityId = method-timing
pluginId = trace
displayName = 方法耗时
category = performance
supportedTargets = method
supportedLocations = method_around
persistenceSupport = SUPPORTED
runtimeShape = SINGLE_AROUND_RUNTIME_INJECTION
requiredCoreCapabilities = method-target, injection-lifecycle, transform-pipeline
```

**RuntimeShape**:

```java
public enum RuntimeShape {
    SINGLE_RUNTIME_INJECTION,
    PAIRED_RUNTIME_INJECTIONS
}
```

方法耗时使用 `SINGLE_AROUND_RUNTIME_INJECTION`——用户创建一个逻辑注入，系统内部生成单条 runtime injection，运行时由 Phase 机制处理入口/出口：

```text
method_around -> TRACE (Phase.ENTER -> onTraceStart, Phase.EXIT -> onTraceEnd)
```

**单注入数据模型**:

- 单条 InjectionDefinition，method_around 位置
- materialize 后生成一条 RuntimeInjection，运行时由 Phase 机制处理入口/出口
- InitializationReport 携带单个 runtime id

**Runtime 语义**:

- 正常返回：Phase.ENTER 记录 start → Phase.EXIT 计算 elapsed
- 异常返回：AROUND 语义保证 EXIT 阶段也会执行（包含异常路径），耗时计算完整
- 嵌套调用：当前 ThreadLocal<Long> 不支持嵌套，后续改为 ThreadLocal<Deque<Long>>

---

### 2.5 条件断点

**能力定义**: 在指定位置设置条件断点，当条件满足时暂停执行并捕获上下文。

**业务规则**:
- 基于 ConditionalExpressionParser 解析条件
- 条件满足时捕获变量快照
- 不实际暂停业务线程（非阻塞式断点）

**对应代码**:
- `luna-core/plugin/builtin/conditional/` — ConditionalBreakpointPlugin

---

### 2.6 插件热管理

**能力定义**: 支持插件的动态加载、卸载、更新，无需重启 Agent。

**业务规则**:
- 插件通过 LunaPlugin 接口定义（SPI 发现）
- 插件可贡献：ProbeHandler、CodeEngine、InjectionLocation、BytecodeInjector、LunaController
- 卸载插件时自动清理注册项和挂起关联注入点
- 并发安全：StampedLock 保护 transform 操作
- 插件依赖解析：PluginDependencyResolver
- 插件生命周期监听：PluginLifecycleListener

**内置插件对照表**:

| 插件 | ID | 分类 | 探针类型 | 注入位置 |
|------|-----|------|---------|---------|
| **LogPlugin** | `log` | injection | LOG | method_enter, method_exit, method_around, line_before, line_after, invoke, exception_exit |
| **SnapshotPlugin** | `snapshot` | injection | SNAPSHOT | method_enter, method_exit, line_before, line_after |
| **TracePlugin** | `trace` | performance | TRACE | method_around |
| **ConditionalBreakpointPlugin** | `conditional-breakpoint` | injection | _(无 ProbeHandler)_ | line_before |

**插件类加载器隔离**:

| 插件类型 | ClassLoader | 理由 |
|---------|-------------|------|
| 内置插件 | LunaAgentClassLoader（与框架同级） | 零额外开销，无外部依赖 |
| 社区插件（无外部依赖） | LunaAgentClassLoader（追加 URL） | 简单，直接放入 `luna-plugins/` 目录 |
| 社区插件（有外部依赖） | PluginClassLoader（child-first，父 = LunaAgentClassLoader） | 隔离依赖冲突 |

**内置插件保护**: 内置插件由 `PluginManagerImpl` 内部通过插件 ID 判断，不允许卸载。`LunaPlugin` 接口中没有 `isBuiltin()` 方法，保护逻辑在 `PluginManagerImpl` 内部实现。

**变化点**:
- 未来可能支持社区插件市场和动态 ClassLoader 隔离
- 可能引入 Capability Permission Model

**对应代码**:
- `luna-core/plugin/` — PluginManager(Impl), PluginContext, PluginClassLoader
- `luna-core/plugin/lifecycle/` — PluginManagerImpl, ReadyGate, AffectedClassTracker
- `luna-core/plugin/loader/` — PluginClassLoader, PluginDependencyResolver

---

### 2.7 源码反编译与类分析

**能力定义**: 实时查看已加载类的反编译源码，分析类结构、方法签名和行号表。

**业务规则**:
- 使用 CFR 引擎反编译
- 三级类查找：ContextClassLoader → AgentClassLoader → Instrumentation
- 行号注释：反编译结果每行前添加 `/* N */` 格式的源码行号
- 类分析提供方法列表、字段列表、行号表、局部变量表
- AnalyzerRegistry 支持多种分析引擎

**变化点**:
- 反编译不是 Runtime Truth，行号可能漂移
- Runtime Semantic Architecture 演进可能改变定位模型

**对应代码**:
- `luna-core/analysis/analyzer/` — ClassAnalyzer, AnalyzerRegistry, AsmClassAnalyzer
- `luna-core/analysis/decompile/` — Decompiler, CfrDecompiler, DecompilerFactory

---

### 2.8 插件市场

**能力定义**: 搜索、安装、更新、卸载社区插件。

**业务规则**:
- MarketClient 通过 HTTP 与插件仓库交互
- SHA256 校验下载完整性
- RepositoryConfig 管理多个仓库源
- 支持信任仓库配置

**变化点**:
- 当前为初始实现，仓库协议待完善

**对应代码**:
- `luna-core/market/` — MarketClient, MarketController, PluginMetadata, PluginRepository

---

### 2.9 官方能力边界

Luna 的能力边界分为五类：

| 类别 | 包含能力 | 说明 |
|------|----------|------|
| Observability | log/trace/snapshot/metric/runtime event/thread analysis | 观测类，低风险 |
| Diagnostics | transform/injection/explain plan/verification | 诊断类 |
| Verification | preview/verify-only/scenario | 验证类 |
| Safe Perturbation | delay/fault/exception/timeout/behavior override | 扰动类，高风险需授权 |
| Runtime Safety | rollback/recovery/lifecycle/isolation/governance | 安全保障类 |

**不应深入**：业务逻辑扩展、持久业务状态、业务编排。

---

## 3. 支撑能力

| 能力 | 说明 | 对应代码 |
|------|------|----------|
| 无锁 RingBuffer | MPSC 无锁队列，容量 4096，生产者不阻塞 | `luna-core/infra/RingBuffer.java` |
| ProbeOutput | 探针输出门面，统一投递 ProbeMessage | `luna-core/probe/ProbeOutput.java` |
| WebSocket 实时推送 | 三层架构：Servlet → Endpoint → Dispatcher | `luna-agent/web/ws/` |
| 注入持久化 | 内存 Map + JSON 文件，脏标志 + 定时刷盘 | `luna-core/injection/DefaultInjectionRepository.java` |
| 字节码缓存 | 双层缓存（字节码 + 分析结果） | `luna-core/infra/BytecodeCache.java` |
| 类隔离 | LunaAgentClassLoader + Shade + BootstrapJarBuilder | `luna-core/plugin/loader/`, `luna-core/bootstrap/` |
| 自研 MVC 框架 | 注解驱动，RouteEngine + DispatcherServlet | `luna-core/infra/web/`, `luna-agent/web/mvc/` |
| 类扫描过滤 | CompositeExcludeClassFilter（4 种过滤策略） | `luna-agent/clazz/` |
| 核心能力注册 | CoreCapabilityRegistry + ReadinessState | `luna-core/bootstrap/capability/` |
| 配置管理 | ConfigManager（Agent 配置 + 插件配置） | `luna-core/infra/config/ConfigManager.java` |
| 注入验证 | InjectionTestHarnessAdapter（retransform + 反射调用） | `luna-agent/adapter/` |
| 包前缀匹配 | PackageTrie（通配符类名匹配） | `luna-core/injection/PackageTrie.java` |

---

## 6. 外部依赖能力

| 依赖 | 用途 | 替换成本 |
|------|------|----------|
| ASM 9.4 | 字节码分析与操作 | 高（核心依赖，已 Shade 隔离） |
| ByteKit | 字节码注入（阿里开源） | 中（与 ASM 并行实现） |
| CFR 0.152 | 反编译引擎 | 中（有 Decompiler 接口抽象） |
| Jetty 9.4 | 内嵌 Web 服务器 | 中（有 WebServer 接口抽象） |
| FastJSON 2 | JSON 序列化 | 低（可替换） |
| Log4j2 | Agent 自身日志 | 低（已 Shade 隔离） |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/16 | 补充：条件断点能力、插件市场能力、InjectionLocation 多态、ByteKit 引擎、支撑能力细化 | Tony.L |
| 2026/06/17 | 迁移补充：内置插件对照表、插件类加载器隔离、内置插件保护 | Tony.L |
| 2026/06/17 | 从 business-capabilities.md 拆分 | Tony.L |
| 2026/07/03 | TRACE 改为 method_around 单注入点、协议前缀简化、Capability 设计更新 | Tony.L | TRACE 重构 |
