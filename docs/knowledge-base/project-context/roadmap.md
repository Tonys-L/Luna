# 当前阶段与未来规划

## 当前阶段

* **MVP → Beta 过渡期**

核心能力已实现：
- 方法级注入（ENTER / EXIT / AROUND）
- 行号级注入（LINE_BEFORE / LINE_AFTER）
- 条件表达式引擎
- 插件热加载/卸载
- Web UI 诊断控制台
- 规则持久化与模板系统

正在演进：
- 微内核 + 插件架构深化
- Runtime Semantic Architecture 探索
- Core Capabilities 体系化

---

## 已知未来规划

### 分期路线

| 阶段 | 方向 | 说明 |
|------|------|------|
| Phase 1 | Core Capability Registry | 最小核心能力记录，覆盖 method/line/bytecode-assembly 等 11 项 |
| Phase 2 | 启动与诊断 | readiness + dependency graph + class-analysis + verification + observation-store |
| Phase 3 | 恢复与观测 | bytecode-recovery + probe-runtime + observation-store + audit + Runtime Execution Graph |
| Phase 4 | 诊断、权限与 AI-native | diagnostic + capability-permission-model + injection-scoped-state + AI Runtime Diagnostics |
| 长期 | Runtime Semantic Engine | 从 JVM Runtime Execution 重建 Runtime Semantic World |

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

当前 Injection Lifecycle 没有 ephemeral 概念，所有注入都是 persistent 语义。如果 AI Agent 方向是真实路线，这个缺口会很早暴露。

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

| # | 能力 | 分类 | 分期 | 核心职责 |
|---|------|------|------|----------|
| 1 | method-target | Kernel | Phase 1 | 方法级注入目标 |
| 2 | line-target | Kernel | Phase 1 | 行号级注入目标 |
| 3 | bytecode-assembly | Kernel | Phase 1 | 字节码组装 |
| 4 | code-compiler-dispatch | Kernel | Phase 1 | 代码编译分发 |
| 5 | injection-lifecycle | Kernel | Phase 1 | 注入生命周期 |
| 6 | transform-pipeline | Kernel | Phase 1 | 转换管线 |
| 7 | instrumentation | Runtime Support | Phase 2 | Instrumentation 能力 |
| 8 | runtime-readiness | Runtime Support | Phase 2 | 运行时就绪 |
| 9 | class-analysis | Runtime Support | Phase 2 | 类分析 |
| 10 | verification-preview | Runtime Support | Phase 2 | 验证预览 |
| 11 | bytecode-recovery | Runtime Support | Phase 3 | 字节码恢复 |
| 12 | probe-runtime | Runtime Support | Phase 3 | 探针运行时（含 back-pressure policy、per-probe 开关、跨线程 context 传播） |
| 13 | observation-store | Runtime Support | Phase 3 | 观测存储（含 event graph、causality edge、thread/request relationship） |
| 14 | ephemeral-injection-session | Runtime Support | Phase 3 | 临时注入会话（TTL-bounded、来源绑定、session isolation） |
| 15 | diagnostic | Runtime Support | Phase 4 | 诊断能力（静态诊断 + 动态诊断，依赖 observation-store event graph） |

其他候选能力（Phase 4+）：capability-permission-model、injection-scoped-state、runtime-audit、sandbox-and-isolation、schema-and-migration、verification-scenario、capability-manifest。

### 架构深化评估

7 个候选深化方向评估分析：

| # | 深化方向 | Depth Signal | 推荐优先级 |
|---|----------|-------------|-----------|
| 1 | 统一注入运行时 | GlobalClassFileTransformer 成为唯一外部 Seam | **Phase 1 最高优先** |
| 2 | 统一 Transformer 路径 | RuleClassFileTransformer 降级为迁移 Adapter | Phase 1 |
| 3 | 内置能力注册追踪 | CoreModuleInitializer + registration tracking | Phase 1 |
| 4 | 插件生命周期事务 | load/unload/update 事务化，保证 invariants | Phase 3 |
| 5 | 模板应用 | TemplateService 改为依赖 InjectionLifecycle | Phase 2 |
| 6 | 验证运行 | 提炼验证 Module，统一接收注入请求 | Phase 3 |
| 7 | Web Runtime 注册 | 核心路由与插件路由 Interface 分开 | Phase 3 |

**推荐推进顺序**：

1. Phase 1：让生产启动路径和测试路径合流（候选一、二、三的最小切片）
2. Phase 2：迁移模板和规则旧模式（候选五）
3. Phase 3：插件事务化和验证深化（候选四、六、七）

**暂不建议做的事**：
- 不继续扩展市场安装能力
- 不继续拆小 helper
- 不先初始化 RuleManager
- 不引入 DI 容器

---

## 变更记录

| 日期 | 变更内容 | 变更人 | 关联变更 |
|------|----------|--------|----------|
| 2026/06/17 | 从 project-context.md 拆分 | Tony.L | — |
