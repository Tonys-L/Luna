# 能力边界与扩展点

> **文档定位**: 定义系统业务边界、扩展点分析与插件化重构对比
> **更新时机**: 新增能力、调整边界、扩展点变化时更新
> **读者**: 架构师、开发者

---

## 7. 业务边界

### 7.1 系统内（我们负责）

- 字节码注入的全生命周期管理
- 注入代码的安全性和性能保障
- 运行时数据的采集、传输和展示
- 插件框架的稳定性
- Agent 与目标 JVM 的隔离
- 插件市场的基础设施

### 7.2 系统外（外部负责）

- 目标应用的业务逻辑
- 目标应用的日志框架
- 目标 JVM 的运行环境
- 网络基础设施

---

## 8. 扩展点分析

| 扩展点 | 当前实现 | 未来可能 | 扩展方式 |
|--------|----------|----------|----------|
| 注入位置 | 6 种 InjectionLocation | FieldAccess 等语义边界注入 | PluginContext.registerInjectionLocation() |
| 注入器 | ASM 系 + ByteKit 系 | 新的字节码引擎 | BytecodeInjectorRegistry |
| 代码引擎 | ExpressionCodeEngine | JAVA 代码类型、新协议 | CodeEngineRegistry |
| 探针处理器 | log/snapshot/trace/conditional | metric/fault-injection/opentelemetry | ProbeHandlerRegistry |
| 插件 | 4 个内置插件 | 社区插件、AI 诊断插件 | LunaPlugin + SPI |
| 分析器 | AsmClassAnalyzer | 新的分析引擎 | AnalyzerRegistry |
| 反编译器 | CfrDecompiler | 新的反编译引擎 | DecompilerFactory |

---

## 8.5 插件化重构 Before/After 对比

以下对比说明了插件化架构如何消除硬编码，实现开闭原则：

### 消除硬编码注册

**Before**: BytecodeInjectorRegistry 构造器 + DefaultInitializer 双重注册同一映射

**After**: 各插件自己注册（`plugin.initialize(ctx)`），PluginContextImpl 双写 Registry + PluginRegistrationRecord

### 消除 switch 分发

**Before**: `ExpressionBytecodeAssembler.doAssemble()` 中 `switch("log"/"snapshot"/"trace")` 无法扩展

**After**: `ProbeHandler.handle()` 委托，各 ProbeHandler 内部分派给 ExpressionHandler 实现

### 消除 resolveInjectionType 重复

**Before**: 3 处独立实现，行为不一致

**After**: `InjectionTypeRegistry.resolve()` 统一类型查找

### 消除前端探针类型硬编码

**Before**: 11 处前端硬编码 LOG/SNAPSHOT/TRACE（plugin-registry.js、ClassDetail.vue、InjectionDialog.vue、InjectionDetailOverlay.vue、LogViewer.vue）

**After**: 后端 ProbeHandler 声明式元数据 + ui-manifest API 暴露 + 前端数据驱动渲染，新增插件前端零改动

---

## 2.9 官方能力边界

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

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/17 | 迁移补充：Before/After代码对比 | Tony.L |
| 2026/06/17 | 从 business-capabilities.md 拆分 | Tony.L |
| 2026/06/20 | 新增"消除前端探针类型硬编码"Before/After对比 | Tony.L |
| 2026/07/03 | 修正 ExpressionHandlerRegistry 描述为实际实现、删除不存在的 hasProtocolPrefix After | Tony.L | TRACE 重构 |
