# Luna 架构深度评估：第三轮

> 日期：2026-05-29  
> 目的：基于当前源码重新评估 Luna 的架构摩擦，并更新下一步深挖顺序。  
> 方法：使用 `improve-codebase-architecture` 的词汇体系，重点观察 Module、Interface、Implementation、Depth、Seam、Adapter、Leverage、Locality，并对可疑 Module 做 deletion test。

## 0. 本轮结论

当前代码已经不是“缺少插件化设计”的状态，而是处在“新架构 Module 已经出现，但生产运行路径还没有收束”的中间态。

已经落地的进展：

- `ExpressionBytecodeAssembler` 已经通过 `ExpressionHandlerRegistry` 分发协议。
- `InjectionTypeRegistry`、`RuleConverterRegistry`、`ExpressionHandlerRegistry`、`CodeCompilerStrategy`、`PluginRegistrationRecord` 等插件扩展点和追踪机制已经存在。
- `InjectionManager` 已经有 `PersistentInjection`、`classIndex`、`pointCache`，并通过 `InjectionQuery` 暴露 O(1) 只读查询。
- `GlobalClassFileTransformer` 已经存在，能直接从 `InjectionQuery` 获取 active injection points。
- `PluginManagerImpl` 已经支持 `initializeAll/load/unload/update/disable/enable`，并能注册插件 Controller 到 `WebServer`。
- `PluginUIController`、`PluginManagerController`、`MarketController` 等插件管理端点已经存在。

但生产路径仍然暴露出四个主要缺口：

- `Agent` 启动仍注册 `RuleClassFileTransformer`，没有注册 `GlobalClassFileTransformer`。
- `Agent` 启动没有组装 `PluginManagerImpl`，因此 SPI 中的 `LogPlugin`、`SnapshotPlugin`、`TracePlugin`、`ConditionalBreakpointPlugin` 不会通过生产启动路径初始化。
- `DefaultInitializer -> CoreModuleInitializer` 直接注册 method/line 类型、injector、rule converter。这个方向本身合理，因为 method/line 是内核能力，不需要走插件加载流程；真正的缺口是这些注册项没有统一的 tracking record，导致 UI manifest、测试可观测性和动态插件记录分叉。
- `ReadyGate` 明确写着仍是 TODO，只是 volatile flag，没有接入 Transformer pipeline，也没有补偿性 retransform。

因此，当前最高价值的 deepening opportunity 不是再创建新的扩展点，而是把启动、插件初始化、注入缓存和 Transformer 注册收束成一个深的运行时 Module。否则代码会继续出现“测试路径能跑、Agent 生产路径没有同样组装”的分叉。

## 1. 候选一：Agent Runtime 启动 Module

**Files**

- `luna-agent/src/main/java/fun/efto/luna/agent/Agent.java`
- `luna-agent/src/main/java/fun/efto/luna/agent/web/JettyWebServer.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/lifecycle/PluginManagerImpl.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/lifecycle/ReadyGate.java`
- `luna-core/src/main/resources/META-INF/services/fun.efto.luna.core.plugin.LunaPlugin`

**现状**

`Agent.startAgent()` 直接完成 bootstrap classpath 注入、ClassLoader 创建、`InitializerManager`、`InstrumentationHolder`、class scanning、`InjectionService` Adapter、`RuleManager`、`TemplateService`、`JettyWebServer`、`RuleClassFileTransformer` 和已加载类 retransform。

与此同时，`PluginManagerImpl` 这条新路径没有在 `Agent` 中出现。测试中大量直接 new `PluginManagerImpl` 并调用 `initializeAll()`，但生产启动路径没有复用这个组装逻辑。

**Problem**

`Agent` 的 Interface 很小，但 Implementation 太宽。调用方只看到 `premain/agentmain`，实际却必须正确理解如下顺序：

1. 初始化日志。
2. append bootstrap classpath。
3. 初始化核心 registry。
4. 初始化 instrumentation。
5. 组装注入端口。
6. 启动 Web。
7. 注册 Transformer。
8. 扫描已加载类。
9. 注册 shutdown hook。

这说明启动顺序没有成为一个深的 Module。删除 `Agent` 不会删除复杂度，只会把复杂度散回一组静态方法和隐式单例。

**Solution**

提炼 `AgentRuntime` Module。`Agent` 只保留 Java Agent 入口 Adapter，`AgentRuntime` 承担启动和关闭 Implementation。

`AgentRuntime` 的第一版不需要 DI 容器，只要显式组装这些 Module：

- core initialization
- instrumentation ports
- injection runtime
- plugin runtime
- web runtime
- transformer runtime
- shutdown lifecycle

**Benefits**

- **Locality**：启动竞态、插件初始化缺失、Transformer 选择错误集中在一个 Module 内。
- **Leverage**：测试和真实 Agent 入口复用同一条启动组装路径。
- **Tests**：可以用 fake `Instrumentation`、fake `WebServer`、fake `Retransformer` 测启动顺序，而不是通过 Java Agent 入口间接验证。

**建议优先级**

最高。它是后续所有候选能否真正落地到生产路径的前置条件。

## 2. 候选二：统一注入运行时 Module

**Files**

- `luna-core/src/main/java/fun/efto/luna/core/transformer/GlobalClassFileTransformer.java`
- `luna-core/src/main/java/fun/efto/luna/core/transformer/RuleClassFileTransformer.java`
- `luna-core/src/main/java/fun/efto/luna/core/transformer/ClassFileTransformerAdapter.java`
- `luna-core/src/main/java/fun/efto/luna/core/injection/InjectionManager.java`
- `luna-core/src/main/java/fun/efto/luna/core/injection/InjectionService.java`
- `luna-agent/src/main/java/fun/efto/luna/agent/adapter/InjectionTestHarnessAdapter.java`

**现状**

代码中并存三条注入路径：

1. `GlobalClassFileTransformer`：通过 `InjectionQuery.getActivePointsForClass()` 获取 `pointCache`。
2. `RuleClassFileTransformer`：通过 `RuleManager.findRulesForClass()` 找 `InjectionRule`，再通过 `RuleConverterRegistry` 转为 `InjectionPoint`。
3. `ClassFileTransformerAdapter`：验证时临时注册，直接持有单个或多个 `InjectionPoint`。

`InjectionService.inject()` 已经把 `InjectionCommand` 转为 `PersistentInjection` 并交给 `InjectionManager.addInjection()`。这说明统一注入模型已经具备主路径雏形。但 `Agent` 注册的仍是 `RuleClassFileTransformer`，所以 `InjectionManager.pointCache` 这条路径在生产启动里没有成为唯一运行时 Interface。

**Problem**

这三个 Transformer Module 各自都有小 Interface，但整体 Interface 对调用方是浅的：调用方必须知道“规则注入、即时注入、验证注入”分别走哪条 Transformer。真正的业务语义其实只有一个：当前类是否存在 active injection points，以及如何把它们织入字节码。

**Solution**

让统一注入运行时成为唯一外部 Seam：

- 生产启动只注册 `GlobalClassFileTransformer`。
- `RuleClassFileTransformer` 降级为迁移 Adapter，负责把旧 `InjectionRule` 迁移或投递为 `PersistentInjection`，之后退出运行时链路。
- `ClassFileTransformerAdapter` 只作为待删除的验证旧路径；验证运行逐步改为通过 `InjectionManager + GlobalClassFileTransformer`。
- `TemplateService` 不再落到 `RuleManager.addRule()`，而是输出 `PersistentInjection` 并进入 `InjectionLifecycle`。

**Benefits**

- **Locality**：注入生效、移除、禁用、重放原始字节码都集中在 `InjectionManager + GlobalClassFileTransformer`。
- **Leverage**：UI、模板、规则迁移、验证、插件共用一个运行时 test surface。
- **Tests**：一个端到端小闭环即可覆盖 `addInjection -> pointCache -> transform -> removeInjection -> clean retransform`。

**Depth Signal**

删除 `RuleClassFileTransformer` 后复杂度不会消失，但会被迫进入 `PersistentInjection` 统一模型。这个迁移会提升 Depth，而不是单纯移动代码。

## 3. 候选三：内置能力注册追踪 Module

**Files**

- `luna-core/src/main/java/fun/efto/luna/core/init/DefaultInitializer.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/builtin/CoreModuleInitializer.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/builtin/log/LogPlugin.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/builtin/snapshot/SnapshotPlugin.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/builtin/trace/TracePlugin.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/lifecycle/PluginRegistrationRecord.java`

**现状**

`CoreModuleInitializer` 直接注册 method/line 的 `InjectionType`、`BytecodeInjector`、`RuleConverter`。这个设计来自一次合理的架构取舍：method/line 是 Luna 字节码注入的内核能力，不是可选扩展，不应该被卸载、热更新或放进独立 `PluginClassLoader`。

`LogPlugin` 和 `SnapshotPlugin` 通过 `registerCodeCompilerStrategy()` 注册策略，但只有走 `PluginManagerImpl.initializeAll()` 才会生效。`TracePlugin` 只注册 `ExpressionHandler` 和 templates，没有自己的 `CodeCompilerStrategy`。

这意味着内置能力被分成两种初始化方式：

- method/line 是 core initializer 直接注册，符合它们作为内核能力的定位。
- log/snapshot/trace/conditional 是 plugin SPI 预期注册，但生产 `Agent` 没有调用插件初始化。

**Problem**

“内置能力不走插件加载流程”和“内置能力需要被统一观测”是两个不同问题。当前实现把这两件事混在一起讨论时容易误判：method/line 不应该重新迁回完整插件机制，但它们的注册项仍需要一个统一的 tracking seam，让 `PluginUIController`、manifest、测试和诊断能解释这些扩展点来自哪里。

**Solution**

保留 method/line 的 core direct initialization，同时新增一个轻量的 registration tracking Module。这个 Module 只记录内核能力注册了哪些 `InjectionType`、`BytecodeInjector`、`RuleConverter`，不提供 load/unload/update 语义，也不创建 `PluginClassLoader`。

建议分两步：

1. 让 `CoreModuleInitializer` 在直接注册 method/line 时同步写入 `CoreRegistrationRecord` 或等价记录。
2. 让 UI manifest 和测试读取“插件记录 + 核心能力记录”的合并视图，而不是假设所有扩展点都来自插件。

**Benefits**

- **Locality**：method/line 的注册来源、注册项和可观测元数据集中记录，但不混入动态插件生命周期。
- **Leverage**：`PluginUIController`、manifest 构建、测试和诊断可以复用统一查询视图。
- **Tests**：核心能力测试不需要模拟插件加载流程，只需断言 direct registration 和 tracking record 一致。

**Depth Signal**

删除 `CoreModuleInitializer` 当前实现不会删除复杂度，因为 method/line 仍然需要一个内核初始化位置。真正浅的是“注册但不可观测”的部分；需要加深的是 registration tracking，而不是把内核能力重新包装成动态插件。

## 4. 候选四：插件生命周期事务 Module

**Files**

- `luna-core/src/main/java/fun/efto/luna/core/plugin/lifecycle/PluginManagerImpl.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/lifecycle/PluginRegistrationRecord.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/lifecycle/PluginRegistryCleaner.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/lifecycle/RuleSuspensionManager.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/lifecycle/AffectedClassTracker.java`

**现状**

`PluginManagerImpl` 已经实现 load/unload/update/disable/enable，并用 `StampedLock` 控制变更。但事务顺序仍然散在方法中，且存在几个值得复查的摩擦点：

- `ReadyGate` 字段存在但没有实际参与 `initializeAll()` 或 Transformer。
- `initializeAll()` 初始化内置/发现插件时没有拿 `transformLock`，和动态 `load()` 路径语义不同。
- `update()` 在写锁内卸载旧插件并加载新插件，失败后释放锁再调用 `load(storedPath)` 回滚；这条路径是否能恢复旧 ClassLoader 和旧 record，需要事务级测试覆盖。
- `doUnloadCore()` 中 `AffectedClassTracker.remove(type.getName(), affectedClasses.toString())` 看起来像把整个 Set 字符串当成单个 className 移除，可能清理不干净。
- `RuleSuspensionManager` 面向 `InjectionManager.getInjections()`，而旧 `RuleManager` 仍然存在，两个状态系统的关系不清楚。

**Problem**

插件变更真正的 Interface 不是 `load/unload/update` 三个方法名，而是一次事务必须保证的 invariants：注册项完整、失败可回滚、ClassLoader 可释放、受影响类被 retransform、禁用状态不参与 UI manifest 和注入运行时。

这些 invariants 目前没有集中表达，导致调用方和测试必须知道许多内部顺序。

**Solution**

提炼插件生命周期事务 Module，至少在 Implementation 内形成显式阶段：

1. prepare classloader and plugin instance
2. register extensions with record
3. publish controllers/templates/handlers
4. suspend or resume affected injections
5. retransform affected classes
6. publish lifecycle event
7. rollback or cleanup

**Benefits**

- **Locality**：插件热更新、卸载安全、Registry 残留、ClassLoader GC、retransform 影响范围集中处理。
- **Leverage**：内置插件、动态插件、市场安装复用同一套事务。
- **Tests**：可以围绕事务 invariants 写测试，而不是只测每个 helper。

**Depth Signal**

`PluginRegistrationRecord` 已经把“注册追踪”命名出来了。下一步应该把“事务”也命名出来，否则 record 只是一个被多个 helper 读写的数据包。

## 5. 候选五：模板应用 Module

**Files**

- `luna-core/src/main/java/fun/efto/luna/core/rule/template/TemplateService.java`
- `luna-core/src/main/java/fun/efto/luna/core/rule/template/TemplateEngine.java`
- `luna-core/src/main/java/fun/efto/luna/core/rule/RuleManager.java`
- `luna-core/src/main/java/fun/efto/luna/core/injection/InjectionLifecycle.java`
- `luna-agent/src/main/java/fun/efto/luna/agent/web/controller/TemplateController.java`

**现状**

`TemplateService.applyTemplate()` 当前仍调用 `TemplateEngine.apply()` 生成 `InjectionRule`，再调用 `RuleManager.addRule()`。这和 `design_injection_unification.md` 里的统一注入方向相反：模板应用应该生成扁平的 `PersistentInjection` 并进入 `InjectionLifecycle`。

**Problem**

模板是用户创建注入的一种入口，不应该成为旧 `InjectionRule` 模型的保留地。否则用户从模板创建的注入和从 `/injections` 创建的注入会进入不同生命周期，增加状态同步和测试复杂度。

**Solution**

让模板应用 Module 只依赖 `InjectionLifecycle`，不依赖 `RuleManager`。`TemplateEngine` 可以短期保留模板参数渲染，但输出对象应迁移为 `PersistentInjection` 或一个专用 draft，再由 `InjectionPointFactory/CodeCompiler` 统一编译。

**Benefits**

- **Locality**：模板参数渲染、注入创建、持久化状态集中到统一注入模型。
- **Leverage**：所有模板都获得 `InjectionManager` 的 enable/disable/remove/retransform 行为。
- **Tests**：模板测试不再需要验证旧 `RuleManager`，只需验证创建出的 `PersistentInjection` 和 active point。

**Depth Signal**

删除 `RuleManager` 对模板的依赖会让模板应用复杂度集中到统一注入生命周期，这是想要的集中。

## 6. 候选六：验证运行 Module

**Files**

- `luna-agent/src/main/java/fun/efto/luna/agent/adapter/InjectionTestHarnessAdapter.java`
- `luna-core/src/main/java/fun/efto/luna/core/injection/InjectionService.java`
- `luna-agent/src/main/java/fun/efto/luna/agent/web/controller/InjectionController.java`
- `luna-agent/src/main/java/fun/efto/luna/agent/web/vo/TestStepVO.java`

**现状**

`InjectionService.injectWithTest()` 已经走 `InjectionManager.addInjection()` 激活注入，但验证步骤仍在 `InjectionVerifier.verifyOnly()` 中通过反射执行目标方法并捕获 `System.out`。`verify(cmd)` 还会调用 `InjectionVerifier.testInjection()`，而 `InjectionTestHarnessAdapter.testInjection()` 仍然手动创建 `MethodTarget` 和 `ClassFileTransformerAdapter`。

同时，`InjectionService` 里仍有 `expectedContent.startsWith("log:")`，说明表达式协议知识泄漏到注入工作流。

**Problem**

验证运行的 Interface 过浅。调用方要的其实是“验证某个注入请求在目标方法调用后产生了预期 observation”，但 Implementation 分散在 service、adapter、Transformer、反射调用和 UI step mapping 中。

**Solution**

提炼验证运行 Module：

- 统一接收注入请求、调用计划和期望 observation。
- 复用统一注入运行时，不再临时创建 `ClassFileTransformerAdapter`。
- 把 expected output 解析交给表达式协议或 CodeCompiler 策略，不在 `InjectionService` 写 `log:` 特例。

**Benefits**

- **Locality**：验证输出、默认参数、反射调用、捕获策略集中。
- **Leverage**：log、snapshot、trace、future metric 协议都能提供自己的验证语义。
- **Tests**：可以用 fake invocation adapter 和 fake output sink 测验证 Module，不必每次真实 retransform。

## 7. 候选七：Web Runtime 注册 Module

**Files**

- `luna-agent/src/main/java/fun/efto/luna/agent/web/JettyWebServer.java`
- `luna-agent/src/main/java/fun/efto/luna/agent/web/mvc/DispatcherServlet.java`
- `luna-core/src/main/java/fun/efto/luna/core/web/WebServer.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/web/PluginManagerController.java`
- `luna-core/src/main/java/fun/efto/luna/core/plugin/web/PluginUIController.java`
- `luna-core/src/main/java/fun/efto/luna/core/market/MarketController.java`

**现状**

`WebServer` Seam 已经存在，`PluginManagerImpl` 可以通过它注册插件 Controller。但 `JettyWebServer` 构造函数仍直接注册固定 Controller 清单，生产启动路径也没有创建 `PluginManagerController`、`PluginUIController`、`MarketController` 并注册进去。

**Problem**

核心路由和插件路由的 Interface 还没有分开。随着插件管理 UI 和市场端点接入，`JettyWebServer` 会继续变成“知道所有 Controller 的地方”。

**Solution**

保留 `WebServer` Seam，把内部注册拆成：

- core route registration
- plugin route registration
- market/plugin-management route registration
- route conflict policy

**Benefits**

- **Locality**：路由冲突、插件卸载后的 route cleanup、固定 Controller 清单集中处理。
- **Leverage**：Agent Runtime 不需要知道每个 Controller，只启动 Web Runtime。
- **Tests**：可以单测 route registration，不必启动 Jetty。

## 8. 建议推进顺序

### Phase 1：让生产启动路径和测试路径合流

优先完成候选一、二、三的最小切片：

1. 新增 `AgentRuntime` Module，`Agent.startAgent()` 委托它。
2. `AgentRuntime` 组装 `PluginManagerImpl`，初始化 builtin plugins。
3. method/line 保留 `CoreModuleInitializer` 直接初始化，但同步写入核心能力 registration record。
4. 生产启动注册 `GlobalClassFileTransformer(InjectionManager.getInstance())`。
5. `RuleClassFileTransformer` 暂时保留但标记为 legacy Adapter，停止作为新注入路径。

验收点：

- 启动后 `CodeCompiler.getStrategies()` 包含 log/snapshot 策略。
- 启动后 `ExpressionHandlerRegistry` 包含 log/snapshot/trace/conditional 协议。
- `/api/plugins/ui-manifest` 能看到插件注册的 injection types、protocols、templates。
- `POST /api/injections` 创建的注入点能通过 `GlobalClassFileTransformer` 生效。

### Phase 2：迁移模板和规则旧模型

推进候选五：

1. `TemplateService` 从依赖 `RuleManager` 改为依赖 `InjectionLifecycle`。
2. `TemplateEngine` 输出 `PersistentInjection` draft。
3. `RuleController` 和 `RuleManager` 作为迁移入口收敛到统一注入模型；功能迁移完成后进入删除计划。

验收点：

- 模板应用创建的是 `PersistentInjection`。
- 模板注入和手动注入在 `InjectionManager.getInjectionPoints()` 中表现一致。
- 删除模板生成的注入后，retransform 能恢复干净字节码。

### Phase 3：插件事务化和验证深化

推进候选四、六、七：

1. 明确插件 lifecycle transaction 的阶段和 rollback invariants。
2. 修复/验证 `AffectedClassTracker.remove(typeName, affectedClasses.toString())` 这类清理风险。
3. 验证运行不再使用 `ClassFileTransformerAdapter`。
4. Web Runtime 接入插件管理和市场 Controller。

验收点：

- 插件 load 初始化失败不留下 registry 项。
- unload 后 CodeCompiler、Template、ExpressionHandler、Controller 都无残留。
- update 失败后旧插件仍可用。
- 新增表达式协议不需要改 `InjectionService.injectWithTest()`。

## 9. 暂不建议做的事

- 不建议继续扩展市场安装能力。市场端点已经存在，但生产运行时尚未接入插件管理 Module。
- 不建议继续拆小 helper。当前摩擦不是类太大，而是关键 Interface 没有足够 Depth。
- 不建议先删除 `RuleManager`。应先让模板和生产 Transformer 都走统一注入模型，再删除旧路径。
- 不建议引入 DI 容器。Agent 环境对 ClassLoader 和启动成本敏感，轻量显式组装更合适。

## 10. 推荐下一步

推荐下一步只做一个垂直切片：

**把生产 Agent 启动路径改为：Agent -> AgentRuntime -> PluginManagerImpl.initializeAll(builtin plugins) -> GlobalClassFileTransformer。**

这个切片小，但能验证最核心的架构假设：

- 插件初始化是否真的进入生产路径。
- `CodeCompilerStrategy` 是否在注入前可用。
- `InjectionManager.pointCache` 是否能成为唯一运行时查询 Interface。
- `GlobalClassFileTransformer` 是否能替代新注入路径中的临时/规则 Transformer。

完成这个切片后，再决定 `RuleClassFileTransformer` 是直接删除，还是作为 `InjectionRule -> PersistentInjection` 的短期迁移 Adapter 保留。
