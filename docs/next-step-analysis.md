# Luna 下一步推进方案分析

> **文档性质**: 决策辅助文档（非知识库正式文档）
> **生成日期**: 2026-07-19
> **当前阶段**: Beta 准备期（Phase 1 已完成）

---

## 一、当前状态概述

### 1.1 已完成

**Phase 1 核心能力（6 个，全部 READY）**:
- method-target（方法级注入目标）
- line-target（行号级注入目标）
- bytecode-assembly（字节码组装）
- code-compiler-dispatch（代码编译分发）
- injection-lifecycle（注入生命周期）
- transform-pipeline（转换管线）

**核心功能**:
- 方法级注入（ENTER / EXIT / AROUND / EXCEPTION_EXIT / INVOKE）
- 行号级注入（LINE_BEFORE / LINE_AFTER）
- 条件表达式引擎
- 插件热加载/卸载（含 PluginRegistrationRecord 自动清理）
- Web UI 诊断控制台（6 页面）
- 注入持久化与状态管理（ACTIVE / SUSPENDED / ephemeral root/derived）
- 微内核 + 插件架构（LunaPlugin SPI + PluginContext + ProbeHandler + CodeEngine）
- 统一注入运行时（GlobalClassFileTransformer 为唯一 Transformer）
- 核心/插件路由分离（JettyWebServer 硬注册 + LunaController 动态注册）

**最近修复（2026-07-19）**:
- INVOCATION 探针输出修复（项目结构重构，probe/ 包统一）
- UiManifestVO 增加 codeEngines 字段
- LOG 探针对象序列化修复（ValueSerializer 统一）
- README 截图修复
- 知识库同步审计完成

### 1.2 架构深化未完成项

| # | 方向 | 状态 | 缺口 |
|---|------|------|------|
| 3 | 内置能力注册追踪 | ⚠️ 部分完成 | 运行时 registration tracking 查询能力不完整 |
| 4 | 插件生命周期事务 | 待启动 | load/unload 失败无事务回滚 |
| 6 | 验证运行 | ✅ 已完成 | `VerificationService` 已提炼为独立 Module |

---

## 二、候选方向详细分析

### 方向 A: Beta 品质打磨

**来源**: roadmap 明确的"当前优先推进方向"第一项

**目标**: 提升 Beta 可用性，使核心 API 真正可达，提升整体健壮性

**具体待办**:
1. ProbeController 注册到 WebServer，使 API 真正可达
2. 健壮性提升（错误处理、边界条件、并发安全）

**影响范围**:
- 模块: luna-core（WebServer 注册）、luna-agent（Controller 暴露）
- 知识库: api-contracts/（接口可达性确认）
- 不涉及领域模型、能力契约变更

**价值评估**:
- 直接提升 Beta 可用性，是发布前必须完成的
- 工作量可控，风险低
- roadmap 明确的第一优先级
- 属于"打磨"性质，不新增业务能力

**风险**:
- 低。主要是注册路径调整，不涉及核心逻辑变更

**建议工作量**: 中等（1-2 个工作单元）

---

### 方向 B: 完善内置能力注册追踪

**来源**: 架构深化 #3 剩余部分

**目标**: 完善运行时 registration tracking，使"查询某能力注册了什么、状态如何"可观测

**当前缺口**:
- `CoreModuleInitializer` 存在且工作
- 但运行时 registration tracking（查询某能力注册了什么、状态如何）尚不完整

**影响范围**:
- 模块: luna-core（capability registry 查询能力）
- 知识库: architecture-overview/、business-capabilities/
- 不涉及对外接口契约（除非新增查询 API）

**价值评估**:
- 提升系统自观测能力，为后续诊断能力打基础
- 符合"观测优先"哲学
- 当前用户感知较弱，主要服务于后续 Phase 2/3
- 需评估是否过早设计（YAGNI）

**风险**:
- 中。需要明确"追踪什么、暴露什么"，避免过度设计

**建议工作量**: 中等

---

### 方向 C: Phase 2 启动

**来源**: 分期路线 Phase 2

**目标**: 启动 readiness + class-analysis + verification + dependency-graph 等核心能力

**Phase 2 能力清单**:
- instrumentation（Instrumentation 能力封装）
- runtime-readiness（运行时就绪）
- class-analysis（类分析）
- verification-preview（验证预览）

**影响范围**:
- 模块: luna-core 多个新 Module
- 知识库: business-capabilities/、domain-model/、architecture-overview/、api-contracts/
- 新增业务能力、可能新增领域模型

**价值评估**:
- 推进项目分期路线，扩大能力版图
- 为 Phase 3（恢复与观测）打基础
- 工作量大，影响面广
- 在 Beta 品质未打磨完成前启动，可能引入不稳定因素

**风险**:
- 高。需要完整的门禁流程（影响分析 + 设计方案 + 用户确认）
- 需要先评估"是否先完成 Beta 打磨再启动 Phase 2"

**建议工作量**: 大（多个工作单元）

---

### 方向 D: 插件生命周期事务

**来源**: 架构深化 #4

**目标**: 为插件 load/unload 失败提供事务回滚机制

**当前缺口**:
- `PluginRegistrationRecord` 提供了卸载自动清理
- 但 load/unload 失败时尚无事务回滚

**影响范围**:
- 模块: luna-core（PluginManager 事务化）
- 知识库: service-specs/（插件生命周期状态机）、lessons/plugin-lifecycle.md
- 不涉及对外 API 变更

**价值评估**:
- 提升插件管理健壮性，避免失败后状态不一致
- 符合"不变量优先"原则
- 当前失败场景是否高频需评估（YAGNI）
- 事务机制引入复杂度，需谨慎设计

**风险**:
- 中。事务边界、回滚顺序、并发场景需仔细设计

**建议工作量**: 中等偏大

---

### 方向 E: 验证运行提炼为独立 Module ✅ 已完成

**来源**: 架构深化 #6

**目标**: 将 `InjectionService.injectWithTest()` 提炼为独立的 verification Module

**完成状态**:
- `VerificationService` 已创建，包含 preview / verify / injectWithTest 三个方法
- `InjectionService` 已移除验证相关方法和依赖，保留共享校验方法（validateLocalVarReferences / toPersistentInjection / isLineInjection / validateProbeHandler）
- 依赖方向：VerificationService → InjectionService（单向）
- `InjectionController` 已切换为依赖 VerificationService
- `AgentRuntime` 和 `JettyWebServer` 已完成装配
- Phase 2 三个核心能力（instrumentation / class-analysis / verification-preview）已注册

**影响范围**:
- 模块: luna-core（新增 verification 包）、luna-agent（AgentRuntime / JettyWebServer / InjectionController 装配调整）
- 知识库: business-capabilities/、architecture-overview/、roadmap 已同步

---

## 三、优先级评估矩阵

| 方向 | 业务价值 | 紧急度 | 风险 | 工作量 | 综合优先级 |
|------|----------|--------|------|--------|------------|
| A. Beta 品质打磨 | 高 | 高 | 低 | 中 | **P0** |
| B. 能力注册追踪 | 中 | 中 | 中 | 中 | P2 |
| C. Phase 2 启动 | — | — | — | — | ✅ 已完成 |
| D. 插件生命周期事务 | 中 | 低 | 中 | 中偏大 | P3 |
| E. 验证运行提炼 | — | — | — | — | ✅ 已完成 |

**评估依据**:
- **业务价值**: 对 Beta 发布的贡献度
- **紧急度**: 是否阻塞发布或后续工作
- **风险**: 引入不稳定因素的可能性
- **工作量**: 实施所需投入

---

## 四、推荐推进路径

### 推荐顺序: A -> B -> D（C 和 E 已完成）

#### 第一阶段: Beta 品质打磨（方向 A）

**状态**: 部分完成
- ✅ ProbeController/MarketController 已注册到 WebServer，API 可达
- ✅ 第一批高严重度不变量违反问题已修复（H-1/H-2/H-3/H-4）：
  - INV-011 落实：triggerRetransform 和 GlobalClassFileTransformer 改为 catch Throwable
  - INV-013 新增：addInjection retransform 失败回滚
  - DefaultInjectionRepository.persist() 加锁消除并发竞态
  - 补全 5 个 retransform 调用方的 try-catch(Throwable)
- ✅ 第二批中严重度问题已修复（M-1/M-2/M-3/M-4）：
  - M-1: DefaultInjectionRepository null 校验（save/delete/findById/findByGroupId）
  - M-2: DefaultInjectionRegistry 正则编译失败抛 IllegalArgumentException（不再静默吞噬）
  - M-3: toggleEnabled enable 失败时回滚 enabled 标志 + unregister registry 条目（INV-013 扩展）
  - M-4: validateLocalVarReferences 异常时返回错误信息（不再静默返回 null）
- ✅ 第三批中严重度问题已修复（M-5~M-15）：
  - M-5~M-8: Controller 层防御性 null/空校验（ClassController/TestController/PluginManagerController/MarketController）
  - M-9: MarketController.uninstall 异常吞噬 → 响应中附带 filesRemoved 状态
  - M-10: addInjection 回滚后抛出异常让调用方感知失败（INV-013 扩展）
  - M-11: updateInjection retransform 失败时回滚到旧状态（INV-013 扩展）
  - M-12: suspendInjectionsByLocation retransform 失败时回滚状态不加入 suspendedIds（INV-013 扩展）
  - M-13: InjectionPersistenceService.save null 校验
  - M-14: MethodInvokeService 移除硬编码回退构造器
  - M-15: JettyWebServer.getStats 修复 uptime 永远为 0 的逻辑错误
- ⏳ Beta 品质打磨方向 A 全部完成，可进入下一阶段

#### 第二阶段: Phase 2 推进（方向 C）✅ 已完成

**状态**: 全部完成
- ✅ instrumentation（已注册 READY）
- ✅ class-analysis（已注册 READY）
- ✅ verification-preview（已注册 READY，VerificationService 已提炼为独立 Module）
- ✅ runtime-readiness（已注册 READY，CoreCapabilityRegistry 增加依赖校验和依赖图查询）

**下一步**: Phase 3 启动

#### 第三阶段: 架构深化补全（方向 B / D）

**理由**:
- 这些是"打磨"性质的工作，不阻塞主流程
- 可在 Phase 2 推进过程中按需穿插
- 严格遵循 YAGNI，仅在真实需求出现时推进

---

## 五、决策建议

### 已完成: 方向 E（验证运行提炼）+ Phase 2 全部能力注册

**已完成内容**:
- VerificationService 从 InjectionService 提炼为独立 Module
- Phase 2 四个核心能力全部注册 READY（instrumentation / class-analysis / verification-preview / runtime-readiness）
- CoreCapabilityRegistry 增加依赖校验（声明 READY 但依赖未满足时降级为 NOT_INITIALIZED）和依赖图查询（getDependencies / getDependents）
- 知识库已同步更新

### 下一步推进建议

**选项 1: Beta 品质打磨（方向 A 剩余部分）**
- 健壮性提升（错误处理、边界条件、并发安全）
- Beta 发布前必须完成

**选项 2: 架构深化 #3（能力注册追踪）**
- 完善运行时 registration tracking 查询能力

**选项 3: Phase 3 启动（恢复与观测）**
- bytecode-recovery / probe-runtime / observation-store
- 工作量大，风险高

---

## 六、待用户确认事项

1. **下一步方向**: 选择 Beta 品质打磨、能力注册追踪、还是 Phase 3 启动？

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026-07-19 | 初始版本：基于知识库审计结果生成下一步推进方案分析 | Tony.L |
| 2026-08-02 | 方向 E（验证运行提炼）标记为已完成；Phase 2 部分能力已注册；更新推荐路径和决策建议 | Tony.L |
| 2026-08-02 | Phase 2 全部完成：runtime-readiness 已注册 READY，CoreCapabilityRegistry 增加依赖校验和依赖图查询；更新推荐路径 | Tony.L |
| 2026-08-02 | Beta 品质打磨第一批完成：修复 4 个高严重度不变量违反问题（H-1/H-2/H-3/H-4），新增 INV-013，更新 INV-011 | Tony.L |
| 2026-08-02 | Beta 品质打磨第二批完成：修复 4 个中严重度问题（M-1/M-2/M-3/M-4），INV-013 扩展到 toggleEnabled，补充 lessons 1.9 节 | Tony.L |
| 2026-08-02 | Beta 品质打磨第三批完成：修复 11 个中严重度问题（M-5~M-15），INV-013 扩展到 addInjection/updateInjection/suspend，方向 A 全部完成 | Tony.L |
