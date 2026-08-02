# 当前阶段与未来规划

## 当前阶段

* **Beta 准备期**

核心能力已实现：
- 方法级注入（ENTER / EXIT / AROUND / EXCEPTION_EXIT / INVOKE）
- 行号级注入（LINE_BEFORE / LINE_AFTER）
- 条件表达式引擎
- 插件热加载/卸载（含 PluginRegistrationRecord 自动清理）
- Web UI 诊断控制台（6 页面：类浏览、日志、仪表盘、线程、注入管理、插件管理）
- 注入持久化与状态管理（ACTIVE / SUSPENDED / ephemeral root/derived 区分）
- 微内核 + 插件架构（LunaPlugin SPI + PluginContext + ProbeHandler + CodeEngine）
- Core Capability Registry（10 个核心能力已注册，依赖校验生效）
- 统一注入运行时（GlobalClassFileTransformer 为唯一 Transformer）
- 核心/插件路由分离（JettyWebServer 硬注册 + LunaController 动态注册）
- 验证职责内聚（VerificationService 独立 Module，从 InjectionService 提炼）

正在演进：
- Beta 品质打磨（ProbeController 注册可达、健壮性提升）
- Phase 2 已完成（runtime-readiness 依赖校验和依赖图查询已实现）
- INVOCATION 探针调用链分析深化
- Runtime Semantic Architecture 探索

---

## 已知未来规划

### 分期路线

| 阶段 | 方向 | 状态 | 说明 |
|------|------|------|------|
| Phase 1 | Core Capability Registry | ✅ 已完成 | 6 个核心能力已注册（method-target, line-target, bytecode-assembly, injection-lifecycle, code-compiler-dispatch, transform-pipeline），全部 READY |
| Phase 2 | 启动与诊断 | ✅ 已完成 | instrumentation / class-analysis / verification-preview / runtime-readiness 全部注册并 READY，依赖校验生效 |
| Phase 3 | 恢复与观测 | 待启动 | bytecode-recovery + probe-runtime + observation-store + audit + Runtime Execution Graph |
| Phase 4 | 诊断、权限与 AI-native | 待启动 | diagnostic + capability-permission-model + injection-scoped-state + AI Runtime Diagnostics |
| 长期 | Runtime Semantic Engine | 概念 | 从 JVM Runtime Execution 重建 Runtime Semantic World |

注意：

规划不是需求。

未进入开发计划前禁止提前设计。

### 演进路线方向

#### Plugin 分层（Official / Community）

**Official Plugins（官方插件）**：强可信、强恢复、强兼容、生产可用、ABI 稳定。包括 Log/Trace/Snapshot/Metric/Verification/Fault Injection/Diagnostic Plugin。

**Community Plugins（社区插件）**：可扩展 Spring semantic injection、SQL analysis、custom runtime policy、protocol integration、business-oriented hooks。官方不保证业务正确性/安全性/恢复性。

#### Capability 权限模型

Capability 未来应承担"权限模型"职责，让插件声明所需 token，核心层在加载时校验并限制越权行为。

**观测类 token（低风险，社区插件默认可申请）**：

| Token | 含义 |
|-------|------|
| `CAP_OBSERVE_METHOD` | method/line target 只读观测 |
| `CAP_OBSERVE_LINE` | 行号级只读观测 |
| `CAP_RUNTIME_STORAGE` | injection-scoped-state 读写 |
| `CAP_THREAD_CONTEXT` | 跨线程 context 传播 |

**扰动类 token（高风险，需要明确授权）**：

| Token | 含义 |
|-------|------|
| `CAP_MODIFY_RETURN` | METHOD_AROUND 返回值替换 |
| `CAP_THROW_EXCEPTION` | 注入点抛异常 |
| `CAP_CLASS_REWRITE` | 直接修改字节码（非 injector 路径） |

流程：插件在 manifest 声明所需 token → 核心层 load-time 校验 → 高风险 token 运行时也有拦截 hook。Official plugin 持有完整 token；Community plugin 默认只持有观测类 token。

#### AI Runtime Diagnostics

Luna 不应只是"一个 Agent 工具"，而应逐步演进为 **Runtime Reasoning Infrastructure**——让人类和 AI 能理解 JVM Runtime 正在发生什么。

传统 observability 依赖人读日志、人分析 trace、人查看 metric。AI 更擅长理解 runtime relationship、理解因果链、自动规划诊断策略。

未来建议提供 `luna diagnose slow-request` 等 CLI + AI Agent，AI 自动创建 trace/jdbc timing/thread contention injection → 收集 observation events → 分析 runtime causality → 生成 explain report → 自动 cleanup injections。

#### Runtime Graphs

4 类 Runtime Graph 构成可推理的 runtime model：

| Graph | 描述 |
|-------|------|
| **Capability Graph** | capability dependencies、provided entries、readiness、lifecycle policy |
| **Injection Graph** | 哪些 class 被影响、哪些 plugin 提供 injection、transform lineage |
| **Observation Graph** | runtime events、causality chain、request/thread relationship |
| **Diagnostic Graph** | injection failure reason、verification failure、missing dependency、transform conflict |

#### Ephemeral Injection Session

AI Agent、测试工具、临时诊断场景创建的注入，需要与用户持久注入严格隔离，并在 session 结束后自动清理。

Session 核心属性：
- **来源绑定**：AI Agent / 测试 / 临时诊断
- **TTL**：超时自动触发 cleanup，不依赖调用方主动关闭
- **isolation**：不同 session 的注入在 transform pipeline 中互不干扰
- **cleanup 完成同步**：cleanup 完成前 session 不关闭

**当前状态**：`PersistentInjection.ephemeral` 字段已存在（root 注入默认 false，derived 注入显式 true），`InvocationProbeHandler` 已区分 root/derived 行为。但尚无 Session 级别的 TTL、isolation 和自动 cleanup 机制——这是 Phase 3 的 `ephemeral-injection-session` 核心能力要解决的。

#### Semantic Injection Model

当前 Luna 偏**物理 injection model**（method / line / bytecode），未来可扩展**语义 injection model**：

```text
Semantic Target → Physical Injection Points

例如：
HTTP entry → 对应 Controller 方法入口
JDBC query → 对应 Statement.execute 调用
Kafka consumer → 对应 onMessage 方法
Spring transaction boundary → 对应 @Transactional 切面
```

这类能力更适合作为 Community Plugin，而不是核心官方能力。

### 候选核心能力地图

15 个候选核心能力及其分期规划：

| # | 能力 | 分类 | 分期 | 核心职责 | 状态 |
|---|------|------|------|----------|------|
| 1 | method-target | Kernel | Phase 1 | 方法级注入目标 | ✅ READY |
| 2 | line-target | Kernel | Phase 1 | 行号级注入目标 | ✅ READY |
| 3 | bytecode-assembly | Kernel | Phase 1 | 字节码组装 | ✅ READY |
| 4 | code-compiler-dispatch | Kernel | Phase 1 | 代码编译分发 | ✅ READY |
| 5 | injection-lifecycle | Kernel | Phase 1 | 注入生命周期 | ✅ READY |
| 6 | transform-pipeline | Kernel | Phase 1 | 转换管线 | ✅ READY |
| 7 | instrumentation | Runtime Support | Phase 2 | Instrumentation 能力 | ✅ READY |
| 8 | runtime-readiness | Runtime Support | Phase 2 | 运行时就绪 | ✅ READY |
| 9 | class-analysis | Runtime Support | Phase 2 | 类分析 | ✅ READY |
| 10 | verification-preview | Runtime Support | Phase 2 | 验证预览 | ✅ READY |
| 11 | bytecode-recovery | Runtime Support | Phase 3 | 字节码恢复 | 待启动 |
| 12 | probe-runtime | Runtime Support | Phase 3 | 探针运行时（含 back-pressure policy、per-probe 开关、跨线程 context 传播） | 待启动 |
| 13 | observation-store | Runtime Support | Phase 3 | 观测存储（含 event graph、causality edge、thread/request relationship） | 待启动 |
| 14 | ephemeral-injection-session | Runtime Support | Phase 3 | 临时注入会话（TTL-bounded、来源绑定、session isolation） | 待启动 |
| 15 | diagnostic | Runtime Support | Phase 4 | 诊断能力（静态诊断 + 动态诊断，依赖 observation-store event graph） | 待启动 |

其他候选能力（Phase 4+）：capability-permission-model、injection-scoped-state、runtime-audit、sandbox-and-isolation、schema-and-migration、verification-scenario、capability-manifest。

### 架构深化评估

7 个候选深化方向评估分析：

| # | 深化方向 | 状态 | 说明 |
|---|----------|------|------|
| 1 | 统一注入运行时 | ✅ 已完成 | `RuleClassFileTransformer` 已删除，`GlobalClassFileTransformer` 为唯一 Transformer |
| 2 | 统一 Transformer 路径 | ✅ 已完成 | 双路径已统一，`ByteKitInjectorBase.inject()` 统一分发 |
| 3 | 内置能力注册追踪 | ⚠️ 部分完成 | `CoreModuleInitializer` 存在且工作，但运行时 registration tracking（查询某能力注册了什么、状态如何）尚不完整 |
| 4 | 插件生命周期事务 | 待启动 | `PluginRegistrationRecord` 提供了卸载自动清理，但 load/unload 失败时尚无事务回滚 |
| 5 | ~~模板应用~~ | ❌ 已移除 | `TemplateService` 从未实现，不再需要此方向 |
| 6 | 验证运行 | ✅ 已完成 | `VerificationService` 已从 `InjectionService` 提炼为独立 Module，职责内聚（preview/verify/injectWithTest） |
| 7 | Web Runtime 注册 | ✅ 已完成 | 核心 Controller 硬注册于 `JettyWebServer`，插件 Controller 通过 `LunaController` 动态注册 |

**当前优先推进方向**：

1. Beta 品质打磨：ProbeController/MarketController 已注册到 WebServer，下一步是健壮性提升
2. Phase 3 启动：恢复与观测能力（bytecode-recovery / probe-runtime / observation-store）
3. 完善内置能力注册追踪（#3 剩余部分）

**暂不建议做的事**：
- 不继续扩展市场安装能力
- 不继续拆小 helper
- 不引入 DI 容器

---

## 变更记录

| 日期 | 变更内容 | 变更人 | 关联变更 |
|------|----------|--------|----------|
| 2026/06/17 | 从 project-context.md 拆分 | Tony.L | — |
| 2026/07/19 | 对照代码审计更新：阶段改为 Beta 准备期、已实现能力补充（EXCEPTION_EXIT/INVOKE/SUSPENDED/ephemeral/统一运行时/路由分离）、架构深化 #1#2#7 标记已完成、#5 模板应用移除、Ephemeral 描述修正、能力地图补充状态列、推进方向更新 | Tony.L | 知识库同步审计 |
| 2026/07/19 | Beta 品质打磨推进方向更新：ProbeController/MarketController 已注册到 WebServer | Tony.L | #feat/beta-quality-polish 同步更新 api-contracts/endpoints.md |
| 2026/08/02 | Phase 2 部分推进：instrumentation/class-analysis/verification-preview 已注册 READY；架构深化 #6 验证运行提炼为 VerificationService 独立 Module（已完成） | Tony.L | #feat/phase2-verification-extract 同步更新 architecture-overview/layers.md、business-capabilities/capabilities.md |
| 2026/08/02 | Phase 2 完成：runtime-readiness 能力已注册 READY，CoreCapabilityRegistry 增加依赖校验和依赖图查询 | Tony.L | #feat/phase2-runtime-readiness |
