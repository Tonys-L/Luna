# Luna Runtime Philosophy And Evolution Direction

> 日期：2026-05-29
> 目的：明确 Luna 的核心哲学、能力边界、插件生态方向，以及 AI Runtime Diagnostics 演进路线。
> 结论：Luna 的官方定位应聚焦于 Runtime Observability & Diagnostics，而不是业务执行平台；核心能力保持开放，社区可以在此基础上扩展更广泛的 runtime plugin。

---

# 1. 核心哲学

## 1.1 核心原则

```text
Luna exists to observe, diagnose, verify,
and safely perturb runtime behavior.

It is NOT a business execution platform.
```

中文表达：

```text
Luna 的目标是：
观测、诊断、验证，以及安全地扰动运行时行为。

Luna 不是业务执行平台。
```

这是整个架构最重要的边界。

---

# 2. 为什么需要这个边界

Luna 的核心能力天然非常强：

* 动态字节码织入
* method / line injection
* runtime state
* expression execution
* around interceptor
* plugin lifecycle
* transform pipeline

这些能力意味着：

```text
Luna 不仅能“观察业务”，
也天然能“修改业务行为”。
```

例如：

```java
log(user);
```

和：

```java
return mockUser();
```

底层本质上都属于 runtime injection。

因此：

> 真正重要的不是“能力能不能做到”，而是“官方是否鼓励这个方向”。

---

# 3. Luna 的定位

## 3.1 Luna Kernel 的定位

Luna Kernel 应定位为：

```text
Runtime Observability & Diagnostics Infrastructure
```

核心关注：

* Runtime Observability
* Runtime Diagnostics
* Runtime Verification
* Runtime Explainability
* Controlled Runtime Perturbation

而不是：

```text
General Runtime Business Extension Platform
```

---

# 4. 官方能力边界

## 4.1 官方应该维护的能力

官方能力应严格聚焦于：

### Observability

* log
* trace
* snapshot
* metric
* runtime event
* thread analysis

### Diagnostics

* transform diagnostics
* injection diagnostics
* runtime explain plan
* active point explainability
* verification report

### Verification

* preview
* verify-only
* verification scenario
* runtime validation
* local variable validation

### Safe Perturbation

* delay injection
* fault injection
* exception simulation
* timeout simulation
* controlled behavior override

### Runtime Safety

* rollback
* bytecode recovery
* injection lifecycle
* transform isolation
* runtime governance

---

## 4.2 官方不应该深入的方向

以下方向不建议作为官方能力：

### 业务逻辑扩展

例如：

* 动态订单逻辑
* 动态风控
* 动态业务规则
* runtime workflow

### 持久业务状态

例如：

* 插件维护订单状态
* runtime 持久业务 session

### 业务编排

例如：

```text
if A then call B
```

这些会让 Luna 从：

```text
Observability Runtime
```

演化成：

```text
Business Runtime Platform
```

从而导致：

* 生命周期复杂度爆炸
* 插件可信性下降
* JVM 可预测性下降
* Runtime 安全性下降
* 运维风险显著上升

---

# 5. 架构模型

## 5.1 推荐架构方向

Luna 应采用：

```text
开放内核 + 收敛官方能力
```

模型。

即：

```text
Luna Kernel
    -> Core Capabilities
        -> Extension Points
            -> Plugins
```

---

# 6. Plugin 分层

## 6.1 Official Plugins（官方插件）

官方插件特点：

* 强可信
* 强恢复
* 强兼容
* 生产可用
* ABI 稳定

推荐包括：

* Log Plugin
* Trace Plugin
* Snapshot Plugin
* Metric Plugin
* Verification Plugin
* Fault Injection Plugin
* Diagnostic Plugin

---

## 6.2 Community Plugins（社区插件）

社区插件可以扩展：

* Spring semantic injection
* SQL analysis
* custom runtime policy
* protocol integration
* business-oriented hooks

但：

```text
官方不保证：
- 业务正确性
- 业务语义一致性
- 插件安全性
- 插件恢复性
```

---

# 7. Capability 不只是“能力”

未来 capability 还应承担：

# “权限模型”

例如：

```text
CAP_OBSERVE_METHOD
CAP_OBSERVE_LINE
CAP_RUNTIME_STORAGE
CAP_MODIFY_RETURN
CAP_THROW_EXCEPTION
CAP_CLASS_REWRITE
CAP_THREAD_CONTEXT
```

插件声明：

```yaml
requiredCapabilities:
  - CAP_OBSERVE_METHOD
  - CAP_RUNTIME_STORAGE
```

这样才能：

* 限制社区插件
* 建立 sandbox
* 控制 runtime 风险
* 支持插件市场

**设计补充**

token 分为两类：

观测类（低风险，社区插件默认可申请）：

```text
CAP_OBSERVE_METHOD      method/line target 只读观测
CAP_OBSERVE_LINE
CAP_RUNTIME_STORAGE     injection-scoped-state 读写
CAP_THREAD_CONTEXT      跨线程 context 传播
```

扮动类（高风险，需要明确授权）：

```text
CAP_MODIFY_RETURN       METHOD_AROUND 返回值替换
CAP_THROW_EXCEPTION     注入点抛异常
CAP_CLASS_REWRITE       直接修改字节码（非 injector 路径）
```

流程：插件在 manifest 里声明所需 token → 核心层在 load-time 校验 → 高风险 token 在运行时也有拦截 hook。

official plugin 持有完整 token；community plugin 默认只持有观测类 token，扮动类 token 需要审核。

对应 Capabilities Map 5.15 Capability Permission Model。

---

# 8. Luna 的真正方向

Luna 不应只是：

```text
“一个 Agent 工具”
```

而应逐步演进为：

```text
Runtime Reasoning Infrastructure
```

也就是：

```text
让人类和 AI 能理解 JVM Runtime 正在发生什么
```

---

# 9. AI Runtime Diagnostics

## 9.1 为什么 AI 很适合 Luna

传统 observability：

```text
人读日志
人分析 trace
人查看 metric
```

AI 更擅长：

```text
理解 runtime relationship
理解因果链
自动规划诊断策略
```

因此：

Luna + AI 的方向非常合理。

---

# 10. Luna CLI + AI Agent

未来建议提供：

```bash
luna diagnose slow-request
```

AI 自动：

1. 创建 trace injection
2. 创建 jdbc timing injection
3. 创建 thread contention injection
4. 收集 observation events
5. 分析 runtime causality
6. 生成 explain report
7. 自动 cleanup injections

---

# 11. AI-friendly Runtime Model

未来 Luna 不应只提供：

```text
method injection
line injection
```

还应提供：

```text
可推理的 runtime model
```

---

# 12. Runtime Graphs

## 12.1 Capability Graph

描述：

* capability dependencies
* provided entries
* readiness
* lifecycle policy

---

## 12.2 Injection Graph

描述：

* 哪些 class 被影响
* 哪些 plugin 提供 injection
* transform lineage

---

## 12.3 Observation Graph

描述：

* runtime events
* causality chain
* request/thread relationship

---

## 12.4 Diagnostic Graph

描述：

* injection failure reason
* verification failure
* missing dependency
* transform conflict

---

# 13. Runtime Explainability

这是未来最重要的能力之一。

例如：

```text
为什么 injection 没生效
为什么 trace 不完整
为什么 verifier reject
为什么 class 不可修改
```

未来应提供：

```text
Runtime Explain Plan
```

类似数据库：

```sql
EXPLAIN
```

---

# 14. Runtime Causality Analysis

未来 observability 不应停留在：

```text
“看到日志”
```

而应升级为：

```text
“理解因果关系”
```

例如：

```text
线程阻塞
  -> JDBC 变慢
      -> 连接池耗尽
          -> HTTP 超时
```

---

# 15. Ephemeral Injection Session

AI 创建的 injection 必须支持：

* 自动生命周期
* 自动 rollback
* 自动 cleanup
* session isolation

否则：

AI 很容易把生产 JVM 变成“永久插桩状态”。

**设计补充**

这与当前 2.3 Injection Lifecycle 的语义不同。现有 injection lifecycle 是全局持久语义：AI Agent 创建的注入和用户手动创建的注入在同一个 store 里，无法区分。

Session 的核心属性：

* 来源绑定：AI Agent / 测试 / 临时诊断
* TTL：超时自动触发 cleanup，不依赖调用方主动关闭
* isolation：不同 session 的注入在 transform pipeline 中不相干扰
* cleanup 完成同步：cleanup 完成前 session 不关闭

对应 Capabilities Map 5.14 Ephemeral Injection Session Capability。

---

# 16. Semantic Injection Model

当前 Luna 更偏：

```text
物理 injection model
```

例如：

* method
* line
* bytecode

未来可以扩展：

```text
语义 injection model
```

例如：

* HTTP entry
* JDBC query
* Kafka consumer
* Spring transaction boundary

即：

```text
Semantic Target
    -> Physical Injection Points
```

但：

这类能力更适合作为：

```text
Community Plugin
```

而不是核心官方能力。

---

# 17. 核心风险

## 17.1 Capability 粒度失控

未来最大的风险之一：

```text
任何东西都叫 capability
```

最终 capability registry 会退化成：

```text
Spring Bean List
```

---

# 18. 推荐的能力分层

建议长期保持：

| 类型                         | 含义                |
| -------------------------- | ----------------- |
| Kernel Capability          | 没它 runtime 不成立    |
| Runtime Support Capability | 提升可信性与恢复能力        |
| Extension Capability       | 提供 extension seam |
| Official Plugin            | 官方产品能力            |
| Community Plugin           | 社区扩展能力            |
| Adapter                    | Web / CLI / AI 接口 |
| Integration                | 三方系统接入            |

---

# 19. Luna 的长期方向

Luna 最终更接近：

```text
Kubernetes for JVM Runtime Instrumentation
```

或者：

```text
IntelliJ Platform for Runtime Diagnostics
```

而不是：

```text
一个普通 Java Agent
```

---

# 20. 长期最重要的能力

未来真正重要的，不是：

```text
再多几个 injector
```

而是：

* Runtime Explainability
* Runtime Reasoning
* Runtime Governance
* Runtime Recovery
* Runtime Graph
* AI-native Diagnostics

---

# 21. 最终建议

未来的 Luna：

## 应该深化：

* Observability
* Diagnostics
* Verification
* Runtime Explainability
* Runtime Causality
* Controlled Perturbation
* AI-native Runtime Diagnostics

---

## 而不应该演化成：

* 业务插件平台
* Runtime Workflow Engine
* 动态业务执行平台

---

# 22. 分期路线与 Capabilities Map 的对应关系

演进路线的每个方向在 Capabilities Map 中都有对应的 capability 条目。下表记录两份文档的对应关系，避免哲学方向与工程实现脱节。

| 演进路线方向 | 对应 Capability | 分期 |
| --- | --- | --- |
| Runtime Observability | probe-runtime（5.5）、observation-store（5.6） | Phase 3 |
| Runtime Diagnostics | diagnostic（5.7） | Phase 4 |
| Runtime Explainability | diagnostic（5.7） + observation-store graph | Phase 4 |
| Runtime Causality Analysis | observation-store（event graph + causality edge） | Phase 3（模型）/ Phase 4（查询） |
| Runtime Graphs | capability-dependency-graph（5.2）、observation-store（5.6）、diagnostic（5.7） | Phase 2-4 逐步 |
| Ephemeral Injection Session | ephemeral-injection-session（5.14） | Phase 3 |
| Capability 权限模型 | capability-permission-model（5.15） | Phase 4 |
| Injection-Scoped State | injection-scoped-state（5.13） | Phase 4 |
| Safe Perturbation | fault-injection plugin（3.6）、method-target（2.5） | Phase 1（基础）/ 待确认 AroundMethodInjector |
| AI-native Diagnostics | diagnostic（5.7） + observation-store（5.6） + CLI/Agent adapter | Phase 4+ |
| 插件市场 | capability-permission-model（5.15）、sandbox-and-isolation（5.11） | Phase 4+ |

重要节序约束：

* observation-store 的 event graph 模型必须在 Phase 3 建立，否则 Phase 4 的动态 diagnostic 无从实现
* capability-permission-model 必须在插件市场前就绪，不能等市场上线后再补
* ephemeral-injection-session 如果 AI Agent 方向已启动，应提前到 Phase 3

---

# 23. 最重要的判断标准

以后设计 capability/plugin 时，始终问一句：

> “这个能力是在帮助理解 JVM，还是在替 JVM 执行业务？”

如果越来越偏后者：

说明架构开始偏航。
