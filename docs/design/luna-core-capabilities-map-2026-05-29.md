# Luna Core Capabilities Map

> 日期：2026-05-29  
> 目的：明确 Luna 哪些能力属于核心能力，哪些属于 builtin plugins，哪些只是运行时支撑或未来演进候选。  
> 结论：核心能力不是“随系统内置”的同义词，而是“Luna 注入运行时成立所必需、不能通过插件生命周期卸载或热替换的 Module”。

## 1. 判定标准

一个能力应归为 core capability，至少满足其中一条：

1. 删除后，任何注入都无法创建、生效、恢复或重新织入。
2. 其他 builtin plugin 依赖它作为地基。
3. 它需要在插件初始化前完成。
4. 它不应该支持 `load/unload/update/disable`。
5. 它的失败会让整个 Agent Runtime 不可信，而不是只让某个产品能力不可用。

反过来，如果一个能力只是贡献 expression handler、template、compiler strategy、controller 或 probe helper，它更适合作为 builtin plugin。

## 2. 当前应确认的 Core Capabilities

### 2.1 Instrumentation Capability

**Module**

- `InstrumentationHolder`
- Agent bootstrap 中的 transformer 注册、移除、loaded classes 查询、retransform 调用

**为什么是核心能力**

这是 JVM 级地基。没有它，Luna 无法注册 `ClassFileTransformer`，也无法让新增、删除、禁用注入影响已经加载的类。

**应记录的 capability metadata**

- capability id：`instrumentation`
- readiness：是否已绑定 `java.lang.instrument.Instrumentation`
- operations：`addTransformer`、`removeTransformer`、`findLoadedClasses`、`retransform`
- lifecycle：core-only，不能卸载

### 2.2 Runtime Readiness Capability

**Module**

- `AgentRuntime`
- `ReadyGate`
- startup ordering
- initial retransform / compensating retransform

**为什么是核心能力**

插件、核心注册项、Transformer、Web Runtime 的启动顺序必须一致。否则会出现“类已经加载，但插件策略还没注册”或“ready 前后的织入语义不同”的分叉。

**应记录的 capability metadata**

- capability id：`runtime-readiness`
- readiness state：starting / ready / failed
- ordered dependencies：core registration -> builtin plugins -> transformer -> web routes
- lifecycle：core-only，不能卸载

### 2.3 Injection Lifecycle Capability

**Module**

- `InjectionManager`
- `InjectionLifecycle`
- `InjectionQuery`
- `PersistentInjection`
- `InjectionPersistenceService`
- `InjectionStore`
- `classIndex`
- `pointCache`

**为什么是核心能力**

这是新注入模型的状态中心。新增、删除、更新、启停注入，以及重建 active points 都集中在这里。builtin plugins 只贡献“注入什么”，这里决定“注入如何存在”。

**应记录的 capability metadata**

- capability id：`injection-lifecycle`
- managed model：`PersistentInjection`
- exposed seams：`InjectionLifecycle`、`InjectionQuery`
- runtime indexes：`classIndex`、`pointCache`
- lifecycle：core-only，不能卸载

### 2.4 Transform Pipeline Capability

**Module**

- `GlobalClassFileTransformer`
- `DefaultClassTransformer`
- `ClassTransformer`
- `TransformerResult`

**为什么是核心能力**

这是注入真正生效的运行时 pipeline。它把 `InjectionQuery` 暴露的 active points 转成 JVM 字节码变更。删除它，所有 `PersistentInjection` 都只会停留在状态层。

**应记录的 capability metadata**

- capability id：`transform-pipeline`
- source seam：`InjectionQuery`
- target seam：JVM `ClassFileTransformer`
- transformer：`GlobalClassFileTransformer`
- lifecycle：core-only，不能卸载

### 2.5 Method Target Capability

**Module**

- `MethodInjectionType`
- `EnterMethodInjector`
- `ExitMethodInjector`
- `AroundMethodInjector`
- `MethodRuleConverter`

**为什么是核心能力**

method 是最基础的注入位置。`TracePlugin` 等产品能力依赖 method 注入才能表达进入、退出和环绕行为。它不应该被卸载，也不应该重新包装成 `MethodInjectionPlugin`。

**应记录的 capability metadata**

- capability id：`method-target`
- injection types：`METHOD_ENTER`、`METHOD_EXIT`、`METHOD_AROUND`
- registered entries：`InjectionType`、`BytecodeInjector`、`RuleConverter`
- lifecycle：core-only，不能卸载

### 2.6 Line Target Capability

**Module**

- `LineNumberInjectionType`
- `BeforeLineInjector`
- `AfterLineInjector`
- `LineRuleConverter`
- line number visitor

**为什么是核心能力**

line 是另一类基础注入位置。条件断点、局部变量观察、行级探针都依赖它。它和 method 一样属于注入位置地基，而不是插件产品能力。

**应记录的 capability metadata**

- capability id：`line-target`
- injection types：`LINE_BEFORE`、`LINE_AFTER`
- registered entries：`InjectionType`、`BytecodeInjector`、`RuleConverter`
- lifecycle：core-only，不能卸载

### 2.7 Bytecode Assembly Capability

**Module**

- `BytecodeAssemblerRegistry`
- `CodeType.EXPRESSION`
- `ExpressionBytecodeAssembler`
- `ExpressionBytecodeGenerator`
- expression parser / segment model

**为什么是核心能力**

Luna 的注入代码需要从用户表达式变成可织入字节码。表达式装配机制本身是核心能力；但 `log:`、`snapshot:`、`trace:` 等具体协议处理器不是核心能力。

**应记录的 capability metadata**

- capability id：`bytecode-assembly`
- core code types：`EXPRESSION`
- registered assemblers：`ExpressionBytecodeAssembler`
- extension seam：`ExpressionHandlerRegistry`
- lifecycle：core-only，不能卸载

### 2.8 Code Compiler Dispatch Capability

**Module**

- `CodeCompiler`
- `CodeCompilerStrategy`

**为什么是核心能力**

`CodeCompiler` 是把 `PersistentInjection` 编译成 `InjectableCode` 的调度器。调度器是核心；具体策略可以来自 builtin plugin 或动态插件。

**应记录的 capability metadata**

- capability id：`code-compiler-dispatch`
- strategy registry：`CodeCompiler.getStrategies()`
- extension seam：`CodeCompilerStrategy`
- lifecycle：core-only，不能卸载

### 2.9 Class Analysis Capability

**Module**

- `AnalyzerRegistry`
- `AsmClassAnalyzer`
- class scanning / class resource loading adapters
- method / line / local variable discovery

**为什么是核心能力**

UI 创建注入、验证行号、选择方法签名都依赖类结构分析。它不一定直接参与织入，但它决定注入点能不能被正确定位。

**应记录的 capability metadata**

- capability id：`class-analysis`
- analyzers：`ASM`
- outputs：class metadata、method metadata、line metadata、local variable metadata
- lifecycle：core runtime support，不能通过插件卸载核心 analyzer

### 2.10 Verification And Preview Capability

**Module**

- `BytecodePreviewer`
- `InjectionVerifier`
- `LocalVarValidator`
- `InjectionTestHarnessAdapter`
- future `VerificationRun`

**为什么是核心能力**

验证和预览不是某个插件的私有能力，而是注入安全性的公共测试 surface。当前实现还需要深化，但这个 capability 应作为 core runtime support，而不是 builtin plugin。

**应记录的 capability metadata**

- capability id：`verification-preview`
- operations：preview、verifyOnly、verifyWithExpectation、local var validation
- target seam：`InjectionManager + GlobalClassFileTransformer`
- lifecycle：core runtime support，不能卸载

### 2.11 Runtime Storage Capability

**Module**

- `InjectionPersistenceService`
- `BytecodeCache`
- `InjectionPointRegistry`
- future original bytecode store

**为什么是核心能力**

注入删除、禁用和恢复需要稳定的状态与字节码缓存策略。当前这些能力分散，但概念上属于“运行时状态与恢复”地基。

**应记录的 capability metadata**

- capability id：`runtime-storage`
- stored models：persistent injections、compiled points、original/transformed bytecode cache
- invariants：remove/disable 后可恢复干净字节码
- lifecycle：core runtime support，不能卸载

## 3. 当前 Builtin Plugins

这些能力随系统分发，但不属于 core capability。它们应通过 `BuiltinPluginProvider` 进入 `PluginManagerImpl.initializeAll()`，拥有插件注册记录，并可参与插件生命周期语义。

### 3.1 Log Plugin

- plugin id：`log`
- Module：`LogPlugin`
- contributes：`LogExpressionHandler`、`LogCodeCompilerStrategy`、`LogTemplates`、`LogProbe`
- 依赖：method/line target、bytecode assembly、code compiler dispatch、transform pipeline

### 3.2 Snapshot Plugin

- plugin id：`snapshot`
- Module：`SnapshotPlugin`
- contributes：`SnapshotExpressionHandler`、`SnapshotCodeCompilerStrategy`、`SnapshotTemplates`、`SnapshotProbe`、`SnapshotSerializer`
- 依赖：method/line target、bytecode assembly、code compiler dispatch、transform pipeline

### 3.3 Trace Plugin

- plugin id：`trace`
- Module：`TracePlugin`
- contributes：`TraceExpressionHandler`、`TraceTemplates`、`TraceProbe`
- 依赖：method target、bytecode assembly、transform pipeline

### 3.4 Conditional Breakpoint Plugin

- plugin id：`conditional-breakpoint`
- Module：`ConditionalBreakpointPlugin`
- contributes：conditional expression handler/templates
- 依赖：line target、expression parsing、bytecode assembly、transform pipeline

### 3.5 Metric Plugin

- plugin id：`metric`
- contributes：`MetricExpressionHandler`、`MetricCodeCompilerStrategy`、`MetricProbe`
- 行为：在方法入口/出口记录调用次数、耗时、错误率，输出到 Micrometer / Prometheus
- 依赖：method target、bytecode assembly、code compiler dispatch、transform pipeline、probe-runtime（5.5）

### 3.6 Fault Injection Plugin

- plugin id：`fault-injection`
- contributes：`FaultInjectionExpressionHandler`、`FaultInjectionCodeCompilerStrategy`
- 行为：在注入点抛指定异常、返回特定值、注入延迟，用于混沌测试和条件模拟
- 依赖：method target（METHOD_AROUND）、bytecode assembly、transform pipeline
- 前提：需要确认 `AroundMethodInjector` 支持替换返回值和吞掉异常

### 3.7 OpenTelemetry Plugin

- plugin id：`opentelemetry`
- contributes：`OtelExpressionHandler`、`OtelTraceProbe`、`OtelContextPropagator`
- 行为：自动在方法入口创建 span、出口结束 span，传播 trace context
- 依赖：method target、bytecode assembly、transform pipeline、probe-runtime（5.5）、injection-scoped-state（5.13）
- **定位说明**：OTel 介于 official 和 community 之间。它依赖 5.13 injection-scoped-state（Phase 4 候选），ABI 稳定性承诺必须与 5.13 的成熟度对齐。建议先作为 community plugin 发布，待 5.13 稳定后再提升为 official plugin。在此之前，3.7 的条目视为占位，不承诺 ABI 稳定。

## 4. 应避免归为 Core Capability 的能力

这些能力重要，但不应进入 core capability registry，除非未来发现它们满足“没有它任何注入都不可信”的标准。

- plugin market：市场安装、下载、版本源。
- plugin class loading：动态插件隔离机制。
- plugin lifecycle transaction：这是插件 Module 的内部深化，不是核心注入能力。
- Web controller 清单：这是 Agent Runtime 的 presentation adapter。
- UI manifest 具体格式：它消费 core/plugin records，但自身不是核心能力。
- log/snapshot/trace/conditional 的协议语义：它们是 builtin plugin 能力。
- RuleManager / InjectionRule：旧模型，应迁移或删除，不应提升为核心能力。

## 5. 候选核心能力：后续可以慢慢实现

这些能力目前未必完整存在，但从 Luna 的方向看，值得作为 future core capabilities 设计。

### 5.1 Capability Registry

把 core capabilities 变成可观测记录，而不是散落在 initializer、registry、controller 里。

建议记录：

- capability id
- display name
- kind：kernel / runtime-support / adapter
- provided entries：injection types、assemblers、registries、ports
- readiness state
- dependencies
- lifecycle policy：core-only / builtin-plugin / dynamic-plugin

第一期可先覆盖：

- `method-target`
- `line-target`
- `bytecode-assembly`
- `code-compiler-dispatch`
- `injection-lifecycle`
- `transform-pipeline`

### 5.2 Capability Dependency Graph

显式表达能力依赖关系，避免启动顺序靠阅读代码推断。

示例：

```text
instrumentation
  -> transform-pipeline
  -> injection-lifecycle

method-target
line-target
  -> transform-pipeline

bytecode-assembly
code-compiler-dispatch
  -> injection-lifecycle

runtime-readiness
  -> builtin-plugin initialization
```

Leverage：启动检查、UI manifest、测试都可以复用同一张 dependency graph。

### 5.3 Injection Policy Capability

集中管理哪些类、方法、classloader、包名可以被注入。

未来可承载：

- exclude agent classes
- exclude bootstrap/system classes
- package allow/deny list
- classloader policy
- max injection count per class
- unsafe method filter

这能避免安全规则散落在 scanner、Transformer、Controller 和 verifier。

### 5.4 Bytecode Recovery Capability

删除、禁用、插件卸载后，如何保证字节码恢复干净，应该成为独立 core capability。

未来可承载：

- original bytecode cache
- transformed bytecode lineage
- retransform rollback
- failed transform recovery
- affected classes tracking

这和 plugin lifecycle transaction 有交集，但它的地位更基础：即使没有动态插件，注入删除也需要恢复能力。

### 5.5 Probe Runtime Capability

`LogProbe`、`SnapshotProbe`、`TraceProbe` 目前属于各 builtin plugin，但它们背后可能需要一个统一 probe runtime。

未来可承载：

- probe event envelope
- event sink
- ring buffer
- rate limiting
- sampling
- correlation id
- thread / request context capture

各插件只贡献事件类型和渲染方式，核心 probe runtime 负责传输、限流、隔离和观测。

**需要补充的设计细节**

back-pressure / overflow policy：ring buffer 满时的策略必须明确，否则高频注入场景会直接影响业务线程。应支持：

- `DROP_OLDEST`：丢弃最旧事件，适合实时观测
- `DROP_NEWEST`：丢弃最新事件，适合历史重放
- `SAMPLE_DOWN`：自动降采样
- `BLOCK`：不允许，probe runtime 不能阻塞业务线程

per-probe enabled/disabled 开关：在不卸载插件的前提下关闭某类 probe，用于临时降级而不影响注入状态。

跨线程 context 传播：Metric、OTel 等插件需要跨线程传递 trace context 和 correlation id。probe runtime 应提供统一的 context propagation hook，而不是各插件自己继承 ThreadLocal。传播策略应与 5.13 injection-scoped-state 对齐。

### 5.6 Observation Store Capability

把运行时观测数据从“打印/临时 buffer”提升为可查询模型。

未来可承载：

- log events
- snapshot events
- trace spans
- condition hit records
- verification results
- transform errors

这不等于具体插件能力，而是所有观测能力共享的存储与查询 seam。

**需要补充的设计细节**

Observation Store 的数据模型应支持 graph 查询，而不只是 flat 事件列表。否则 AI Diagnostics 和 Runtime Causality Analysis 无法在此之上工作。

Observation Graph 应描述：

- runtime events 节点
- causality chain：哪个事件导致了哪个事件（例如连接池耗尽 → JDBC 超时 → HTTP 500）
- request / thread relationship：哪些事件属于同一请求、同一线程
- injection source：事件由哪个注入点触发

这个 graph 结构是 AI Agent 自动诊断的基础数据层，也是 5.7 Diagnostic Capability 的上游。没有它，Diagnostic 只能做静态配置检查，无法做 runtime causality 推断。

**应记录的 capability metadata**

- capability id：`observation-store`
- data model：event graph，支持 causality edge 和 thread/request relationship
- query seam：按 injection、class、thread、request、time range 查询
- lifecycle：core runtime support

### 5.7 Diagnostic Capability

让 Luna 能解释“为什么这个注入没有生效”。

未来可承载：

- capability readiness report
- missing strategy report
- missing injector report
- class not found / not modifiable report
- transform failure report
- plugin contribution report
- active point explain plan

这个能力很适合 AI agent 使用，因为它把排障路径从“读很多 registry”收敛成一个诊断 Interface。

**需要补充的设计细节**

Diagnostic Capability 应包含两类诊断：

静态诊断（不依赖运行时事件）：

- capability readiness report
- missing strategy / injector report
- class not found / not modifiable report
- plugin contribution report

动态诊断（依赖 Observation Graph）：

- runtime causality explain：给定一个 observation event，追溯其 causality chain
- injection effectiveness report：注入是否真正触发、触发频率、是否被 rate limit 截断
- transform conflict report：多个 plugin 修改同一类时的字节码冲突分析

Diagnostic Graph 应描述：

- injection failure reason（class not found / method not found / transform rejected）
- verification failure 原因链
- missing dependency 路径
- transform conflict 节点

这两类诊断的数据来源不同：静态诊断直接查 capability registry 和 plugin registry，动态诊断依赖 5.6 Observation Store 的 event graph。两者应统一通过同一个 Diagnostic Interface 暴露，调用方无需区分。

**应记录的 capability metadata**

- capability id：`diagnostic`
- static source：capability registry、plugin registry、injection-lifecycle
- dynamic source：observation-store（event graph）
- output：Diagnostic Report，包含静态 + 动态两段
- lifecycle：core runtime support

### 5.8 Verification Scenario Capability

把验证运行从“调用某个方法看看输出”升级为可声明的 scenario。

未来可承载：

- target invocation
- input arguments
- expected events
- expected snapshot fields
- expected trace shape
- timeout
- cleanup

这样验证能力可以复用在 TDD、UI 预览、插件开发和回归测试里。

### 5.9 Capability Manifest Capability

UI manifest 不应直接拼各 registry，而应消费 capability records + plugin records 的合并视图。

未来可承载：

- core capabilities
- builtin plugins
- dynamic plugins
- extension points
- unsupported operations
- readiness / degraded status
- dependency status

这会让 UI 不需要知道 method/line 为什么不是插件，也不需要硬编码哪些插件不可卸载。

### 5.10 Runtime Audit Capability

记录运行时关键变更的审计事件。

未来可承载：

- injection created / updated / removed
- plugin loaded / unloaded / failed
- capability ready / degraded
- transformer registered / removed
- class retransform requested / completed / failed

这不是业务日志，而是架构层面的可追踪性。

### 5.11 Sandbox And Isolation Capability

动态插件未来会带来隔离需求。部分隔离能力应作为核心策略，而不是 PluginClassLoader 的内部细节。

未来可承载：

- plugin classloader policy
- reflection access policy
- controller route namespace policy
- expression protocol namespace policy
- resource limits

### 5.12 Schema And Migration Capability

`PersistentInjection`、plugin metadata、capability records 都会演进。需要核心 schema/migration 能力避免旧数据散落处理。

未来可承载：

- model version
- persistent injection migration
- plugin metadata migration
- capability record migration
- validation report

### 5.13 Injection-Scoped Runtime State Capability

部分插件需要在多个注入点之间共享运行时状态，例如：

- Taint Tracking：标记在方法 A 产生，需要在方法 B 检查是否流入
- Distributed Context：trace context 在线程内跨多个调用点传播
- Time Travel：多个注入点的快照需要按调用顺序关联成一条链

这不是单点注入能解决的问题。每个注入点只有自己的局部变量和参数，没有跨点共享状态的 seam。如果不提供核心能力，每个插件都会自己用 `ThreadLocal` 或静态 Map 实现，导致语义不一致、资源泄漏风险各异，且无法统一清理。

未来可承载：

- injection-scoped context carrier（ThreadLocal-based，支持线程继承）
- context lifecycle：创建、传播、清理
- context key 注册（避免不同插件的 key 冲突）
- 跨线程传播策略（inherit / copy / clear）
- context 泄漏检测

各插件只注册自己的 context key，读写通过统一 carrier，核心能力负责生命周期和传播。

**应记录的 capability metadata**

- capability id：`injection-scoped-state`
- carrier model：scoped context carrier，ThreadLocal-backed
- extension seam：context key registry
- lifecycle：core-only，不能卸载

### 5.14 Ephemeral Injection Session Capability

AI Agent、测试工具、临时诊断场景创建的注入，需要与用户持久注入严格隔离，并在 session 结束后自动清理，不能依赖调用方主动删除。

这与 5.4 Bytecode Recovery 不同：Recovery 解决的是"删除后字节码能否恢复干净"，Session 解决的是"谁创建的、什么时候自动销毁、不同来源的注入互不干扰"。

当前 2.3 Injection Lifecycle 没有 ephemeral 概念，所有注入都是 persistent 语义。如果 AI Agent 方向是真实路线，这个缺口会很早暴露：AI 调试一次就留下一批永久注入。

未来可承载：

- session 创建：绑定来源（AI Agent / 测试 / 临时诊断）、TTL、最大注入数
- session-scoped injection：注入归属到 session，不进入全局 persistent store
- 自动 cleanup：TTL 到期或 session 关闭时，自动触发 rollback + bytecode recovery
- session isolation：不同 session 的注入在 transform pipeline 中互不影响
- session status：active / expired / cleanup-in-progress / cleaned

**应记录的 capability metadata**

- capability id：`ephemeral-injection-session`
- session model：TTL-bounded，来源绑定
- isolation unit：session-scoped injection store，独立于 persistent store
- cleanup trigger：TTL expiry / explicit close / agent disconnect
- dependency：injection-lifecycle（2.3）、bytecode-recovery（5.4）
- lifecycle：core runtime support，不能卸载

---

### 5.15 Capability Permission Model

演进路线第 7 节提出 capability 未来应承担权限模型职责，让插件声明它需要哪些 capability，核心层在加载时校验并限制越权行为。没有这个模型，社区插件生态无法安全运行，插件市场也无法落地。

这与 5.11 Sandbox And Isolation 不同：Sandbox 解决的是运行时隔离（classloader、reflection、resource），Permission Model 解决的是"加载前声明 + 核心层授权"，是 Sandbox 的上游。

未来可承载：

- capability token 定义：每个 core capability 对应一组 token，例如：

```text
CAP_OBSERVE_METHOD      method/line target 只读观测
CAP_RUNTIME_STORAGE     injection-scoped-state 读写
CAP_MODIFY_RETURN       METHOD_AROUND 返回值替换
CAP_THROW_EXCEPTION     注入点抛异常
CAP_CLASS_REWRITE       直接修改字节码（非 injector 路径）
CAP_THREAD_CONTEXT      跨线程 context 传播
```

- 插件 manifest 声明：

```yaml
requiredCapabilities:
  - CAP_OBSERVE_METHOD
  - CAP_RUNTIME_STORAGE
```

- 加载时校验：核心层检查插件声明的 token 是否在其允许范围内；超出范围的插件拒绝加载
- 运行时拦截：高风险 token（`CAP_MODIFY_RETURN`、`CAP_CLASS_REWRITE`）在运行时也应有拦截 hook
- 权限降级：official plugin 持有完整 token，community plugin 默认只持有 observe 类 token

**应记录的 capability metadata**

- capability id：`capability-permission-model`
- token registry：capability token 定义与描述
- enforcement point：plugin load-time check + runtime hook（高风险 token）
- dependency：capability-registry（5.1）、sandbox-and-isolation（5.11）
- lifecycle：core runtime support，不能卸载


---

## 6. 推荐分期

### Phase 1：最小核心能力记录

实现 `CoreCapabilityRegistry` 或等价记录，只覆盖启动时已经明确存在的能力：

1. `method-target`
2. `line-target`
3. `bytecode-assembly`
4. `code-compiler-dispatch`
5. `injection-lifecycle`
6. `transform-pipeline`

TDD 验收：

- core capability records 能列出 method/line 的 injection types、injectors、rule converters。
- manifest 能合并 core capability records 与 plugin records。
- method/line 不出现在可卸载插件列表。

### Phase 2：启动与诊断

把 readiness 和 dependency graph 纳入能力记录：

1. `instrumentation`
2. `runtime-readiness`
3. `class-analysis`
4. `verification-preview`

TDD 验收：

- AgentRuntime 启动后 readiness report 为 ready。
- 缺失 `CodeCompilerStrategy` 时诊断能指出缺失的 extension seam。
- 缺失 `BytecodeInjector` 时诊断能指出对应 injection type。

### Phase 3：恢复与观测

把恢复、probe runtime、observation store 逐步提升为深 Module：

1. `bytecode-recovery`
2. `probe-runtime`（含 back-pressure policy 和 per-probe 开关）
3. `observation-store`（含 event graph 和 causality edge）
4. `runtime-audit`
5. `ephemeral-injection-session`（如果 AI Agent 方向已启动，提前到本期）

TDD 验收：

- 删除注入后字节码恢复行为可测试。
- log/snapshot/trace 输出统一进入 observation store，可按 thread/request 查询。
- ring buffer 满时 DROP_OLDEST 行为可测试，业务线程不阻塞。
- AI Agent 创建的 ephemeral session TTL 到期后注入自动清理，字节码恢复干净。
- plugin unload 后观测和注册项无残留。

### Phase 4：诊断、权限与 AI-native 能力

把动态诊断、权限模型和 AI-native runtime 能力提升为深 Module：

1. `diagnostic`（静态 + 动态两段，依赖 observation-store event graph）
2. `capability-permission-model`（token 定义 + 插件 load-time 校验）
3. `injection-scoped-state`（跨注入点共享 context）

TDD 验收：

- 给定一个 observation event，Diagnostic 能追溯其 causality chain。
- 声明了 `CAP_MODIFY_RETURN` 但未授权的 community plugin 拒绝加载，报错指向具体 token。
- 两个插件注册不同 context key，互不干扰，session 结束后均清理干净。

## 7. 当前建议

短期不要把所有未来能力一次性实现。先做一件小而深的事：

> 新增 core capability record，让 method/line、bytecode assembly、code compiler dispatch、injection lifecycle、transform pipeline 在 UI manifest 和测试里可解释。

这个 Module 的 Interface 应该很小，只回答：

1. 当前有哪些 core capabilities？
2. 每个 capability 提供了哪些 extension entries？
3. 每个 capability 是否 ready？
4. 每个 capability 为什么不能 unload/disable？

只要这四个问题能稳定回答，后续再把 dependency graph、diagnostic、audit、recovery 慢慢接进来。
  