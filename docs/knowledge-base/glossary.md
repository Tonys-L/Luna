# 术语表

> **TL;DR**: 核心术语：InjectionPoint（注入点）、PersistentInjection（持久化注入）、LunaPlugin（插件）。⚠️ InjectionLocation ≠ InjectionTarget，前者是位置类型，后者是目标描述

---

## 添加规则

## A

### Agent
Java Agent，通过 Instrumentation API 在 JVM 层面拦截和修改类加载行为的组件。

### ASM
Java 字节码操作框架。Luna 使用 ASM 9.4，通过 Maven Shade 重命名为 `fun.efto.luna.shade.asm`。

### ADR
Architecture Decision Record，架构决策记录。

### AffectedClassTracker
受影响类追踪器，记录插件注册的 InjectionLocation 影响了哪些类，用于插件卸载时 retransform。

---

## B

### Bootstrap ClassLoader
JVM 最顶层的类加载器，加载核心类库。Luna 通过 `appendToBootstrapClassLoaderSearch` 将 Agent JAR 加入其搜索路径，使 ProbeOutput 全局可见。

### BootstrapJarBuilder
Bootstrap JAR 构建器，从 Agent JAR 提取 Bootstrap CL 所需类，解决 ClassLoader 分裂问题。

### BytecodeHelper
字节码辅助接口，提供 loadArgument / loadLocalVar / invokeStatic 等字节码生成辅助方法。

### BytecodeInjector
字节码注入器接口，负责在指定位置注入字节码。

### ByteKit
阿里开源的字节码增强工具。Luna 同时支持 ASM 和 ByteKit 两套注入实现。

### BytecodeCache
双层缓存，缓存字节码和分析结果，避免重复转换。

---

## C

### CapabilityKind
能力类型枚举：KERNEL / RUNTIME_SUPPORT / ADAPTER。

### CFR
Java 反编译引擎。Luna 使用 CFR 0.152 版本。

### ClassFileTransformer
JVM Instrumentation 接口，用于在类加载时拦截和修改字节码。

### Clean Slate + Re-apply
同一类多次注入的策略：每次 retransform 时从原始字节码开始，按顺序重新应用所有活跃注入点。

### CodeEngine
代码引擎接口，将 PersistentInjection 编译为 CompiledCode。

### CodeType
代码类型枚举：EXPRESSION / JAVA / SNAPSHOT。

### ConditionRegistry
条件注册表，预编译缓存表达式 AST，运行时直接求值。

### CoreCapabilityRecord
核心能力记录，描述系统启动后的能力状态和就绪情况。

### CoreCapabilityRegistry
核心能力注册表，管理 KERNEL / RUNTIME_SUPPORT / ADAPTER 三类能力。

---

## D

### DefaultClassTransformer
默认类转换器，委托 BytecodeInjectorRegistry 和 ProbeHandlerRegistry 执行注入。

### DefaultInjectionRegistry
默认注入注册表，实现三级索引：exactIndex + PackageTrie + regexFallback。

---

## E

### EvaluationContext
求值上下文，使用 ThreadLocal 实现线程复用，支持变量绑定和参数绑定。

### ephemeral
临时注入标记。ephemeral=true 的注入不持久化，Agent 重启后丢失。

### ExceptionExitInjectionLocation
异常退出注入位置，方法抛出异常时触发。

---

## G

### GlobalClassFileTransformer
全局 ClassFileTransformer，处理规则自动注入和 API 实时注入。

### groupId
注入分组标识，支持批量管理相关注入点。

---

## I

### InjectionLifecycle
注入生命周期接口，定义 addInjection / removeInjection / updateInjection / toggleEnabled。

### InjectionLocation
注入位置抽象类，描述注入在方法/类中的哪个位置。属性：name、description、category、aliases。子类：MethodInjectionLocation、LineNumberInjectionLocation、ExceptionExitInjectionLocation、InvokeInjectionLocation。UI 元数据通过 InjectionLocationUIDescriptor 分离。

### InjectionPoint
注入点，运行时字节码注入的最小单元，由 UUID 标识。

### InjectionQuery
注入查询接口，定义 getActivePointsForClass / getInjectionCount / getInjectionPoints / contains。

### InjectionRegistry
注入注册表接口，继承 InjectionQuery，增加 register / unregister。

### InjectionService
核心注入服务，实现 InjectionLifecycle + InjectionQuery，提供 inject / preview / verify 等方法。

### InjectionStatus
注入状态枚举：ACTIVE / SUSPENDED / DISABLED。

### InjectionTarget
注入目标接口，描述注入的目标类和方法。

### InjectionVerifier
注入验证器接口（Port），由 InjectionTestHarnessAdapter 实现。

### InjectionStore
注入存储接口（Port），由 InjectionPointRegistry 实现。

### Instrumentation
JVM Instrumentation API，提供类转换和重定义能力。

### InvokeInjectionLocation
方法调用注入位置，在调用指定方法前后触发。

---

## L

### LineNumberTarget
行号注入目标，包含 lineNumber 和 LineNumberInjectionLocation。

### LocalVariableScanner
局部变量扫描器，基于 ASM Tree API 扫描指定行号处可见的局部变量。

### LogDispatcher
日志分发器，后台守护线程轮询 RingBuffer，通过 WebSocket 广播数据。

### LunaPlugin
插件接口，定义插件的初始化、销毁和贡献能力。

### LunaSpy → ProbeOutput
跨 ClassLoader 通信桥梁，被注入的字节码直接调用其静态方法。

---

## M

### MethodInjectionLocation
方法注入位置，静态实例：ENTER / EXIT / AROUND。

### MPSC
Multi-Producer Single-Consumer，多生产者单消费者模式。RingBuffer 采用此模式。

---

## P

### PackageTrie
包前缀字典树，支持通配符类名匹配（如 `com.example.*`）。

### PersistentInjection
持久化注入记录，InjectionPoint 的持久化形态。

### PluginClassLoader
插件级 ClassLoader，隔离动态加载的插件依赖。

### PluginContext
插件上下文接口，提供注册 InjectionLocation / ProbeHandler / CodeEngine 等能力。

### PluginDependencyResolver
插件依赖解析器，拓扑排序确定插件初始化顺序。

### PluginManager
插件管理器接口，定义 load / unload / update / disable / enable 等方法。

### PluginManagerImpl
插件管理器实现，含 StampedLock 保护 transform 操作。

### ProbeHandler
探针处理器接口，定义 getProbeType / validate / handle 等方法。

### ProbeMessage
探针消息，注入代码运行时产生的数据。

### ProbeOutput
探针输出门面，封装 RingBuffer，提供 offer(ProbeMessage) 方法。

### Port
端口接口，整洁架构中核心层定义的接口，由外层提供实现。如 Retransformer, InjectionStore, BytecodeLoader, InjectionVerifier。

---

## R

### ReadyGate
就绪门控，确保 Agent 启动顺序一致。

### ReadinessState
就绪状态枚举：NOT_INITIALIZED / READY / DEGRADED / FAILED。

### retransform
类重转换，通过 Instrumentation.retransformClasses() 对已加载的类重新应用字节码修改。

### Retransformer
类重转换接口（Port），由 InstrumentationHolder 实现。

### RingBuffer
无锁环形缓冲区，MPSC 模式，生产者不阻塞，消费者轮询。

---

## S

### Shade
Maven Shade Plugin，将第三方依赖重命名以避免冲突。Luna 对 ASM 和 Log4j2 进行了 Shade。

### SnapshotSerializer
防御式对象序列化器，用于变量快照的序列化。

### SuppressHandler
异常抑制处理器，ByteKit 注入代码中的 try-catch 防御。

### SUSPENDED
注入挂起状态。插件卸载时，关联的注入点被挂起而非删除，保留恢复能力。

---

## T

### TransformerResult
转换结果，包含 bytecode / isTransformed / message。

### TreeApiBytecodeHelper
Tree API 字节码辅助，支持行号级注入的 MethodNode 操作。

---

## V

### Visitor API
ASM 的流式字节码访问 API，适合方法级注入。

### ValidationResult
验证结果，工厂方法：ok() / fail(String) / okWithWarnings(List)。

### VerifyError
JVM 字节码验证错误。Luna 通过 ClassLoaderAwareClassWriter 和 COMPUTE_FRAMES 避免此问题。

---

## 缩写表

| 缩写 | 全称 | 说明 |
|------|------|------|
| ASM | 无全称 | Java 字节码操作框架名 |
| CFR | Class File Reader | Java 反编译引擎 |
| CFG | Control Flow Graph | 控制流图 |
| MPSC | Multi-Producer Single-Consumer | 多生产者单消费者 |
| SPI | Service Provider Interface | Java 服务发现机制 |
| APM | Application Performance Monitoring | 应用性能监控 |
| KDD | Knowledge Driven Development | 知识库驱动开发 |
| ADR | Architecture Decision Record | 架构决策记录 |
| TDD | Test-Driven Development | 测试驱动开发 |
| VO | Value Object | 视图对象 |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/16 | 补充：ByteKit、Port、InjectionLocation 体系、CoreCapability、ephemeral、groupId、SUSPENDED、SuppressHandler 等术语 | Tony.L |
| 2026/07/03 | 删除 CompilationLocation（不存在）、更新 InjectionLocation 描述 | Tony.L | TRACE 重构 |
