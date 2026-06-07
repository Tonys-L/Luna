# 架构决策记录 (ADR)

> **更新日期**: 2026/06/07

---

## ADR-001: 洋葱架构而非分层架构

**状态**: 已采纳

**背景**: Luna 需要确保核心业务逻辑不依赖具体技术实现（ASM/Jetty/CFR），同时支持未来替换底层技术。

**决策**: 采用洋葱架构，依赖方向向内流动。

**取舍**:
- 优势: 核心层稳定，技术实现可替换
- 代价: 接口抽象层增加代码量

---

## ADR-002: 自研 MVC 框架而非引入 Spring

**状态**: 已采纳

**背景**: Agent 运行在目标 JVM 中，引入 Spring 会带来大量依赖和类冲突风险。

**决策**: 实现轻量级注解驱动 MVC 框架。

**取舍**:
- 优势: 零外部依赖，类隔离无冲突
- 代价: 功能有限（无 AOP、事务等）

---

## ADR-003: MPSC RingBuffer 而非 Disruptor

**状态**: 已采纳

**背景**: 注入代码在业务线程中执行，数据传输必须零阻塞。

**决策**: 自研 MPSC 无锁 RingBuffer，基于 AtomicReferenceArray + CAS。

**取舍**:
- 优势: 零依赖，满队列丢弃保护业务线程
- 代价: 无 Disruptor 的高级特性（多消费者、事件处理链）

---

## ADR-004: ASM + ByteKit 双引擎

**状态**: 已采纳

**背景**: 方法级注入 ByteKit 效率更高，行号级注入 ByteKit 覆盖不足。

**决策**: 方法级注入使用 ByteKit，行号级注入使用 ASM，通过 BytecodeInjector 接口统一。

**取舍**:
- 优势: 各取所长
- 代价: 两套实现维护成本

---

## ADR-005: InjectionManager 拆分为 Repository/Registry/Service

**状态**: 已采纳

**背景**: InjectionManager 是 God Class，职责过多，难以测试和演进。

**决策**: 拆分为三层：
- InjectionRepository: 纯存储
- InjectionRegistry: 高性能内存注册表（PackageTrie）
- InjectionService: 领域编排

**取舍**:
- 优势: 单一职责，可独立测试，Registry 性能 O(k)
- 代价: 类数量增加，组装复杂度提升

---

## ADR-006: PersistentInjection 统一模型

**状态**: 已采纳

**背景**: InjectionPoint（运行时）和 InjectionRule（持久化）双轨制导致领域模型分裂。

**决策**: PersistentInjection 统一实体，ephemeral 标记区分临时/持久化。

**取舍**:
- 优势: 前后端模型统一，消除转换逻辑
- 代价: 迁移成本

---

## ADR-007: Bootstrap JAR 精简注入

**状态**: 已采纳

**背景**: 将整个 Agent JAR 注入 Bootstrap CL 会导致 Log4j2 Shade 类被 Bootstrap CL 加载，引发 ClassCastException。

**决策**: BootstrapJarBuilder 仅提取 probe/infra/expression 类构建精简 JAR。

**取舍**:
- 优势: 彻底解决 Log4j2 类加载冲突
- 代价: 构建流程增加一步

---

## ADR-008: StampedLock 而非 ReentrantReadWriteLock

**状态**: 已采纳

**背景**: 插件加载/卸载需要独占，注入操作需要并发读。

**决策**: 使用 StampedLock 实现乐观读锁。

**取舍**:
- 优势: 乐观读零开销，吞吐更高
- 代价: API 复杂度略高

---

## ADR-009: 显式注册 + ServiceLoader 而非纯 SPI

**状态**: 已采纳

**背景**: Luna 是封闭系统，所有模块编译时依赖关系已明确。

**决策**: 核心注册使用显式代码，扩展发现使用 ServiceLoader。

**取舍**:
- 优势: 简单直接、类型安全、调试友好
- 代价: 新增模块需修改注册代码

---

## ADR-010: 条件表达式预编译缓存

**状态**: 已采纳

**背景**: 条件表达式在每次方法调用时求值，重复解析 AST 性能不可接受。

**决策**: ConditionRegistry 预编译缓存 AST，运行时直接对缓存求值。

**取舍**:
- 优势: 避免重复解析，求值 O(1)
- 代价: 内存占用（AST 缓存）
