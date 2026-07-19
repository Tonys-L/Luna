# ADR-005: InjectionManager拆分为Repository-Registry-Service

## 状态

Accepted

## 背景

InjectionManager 是 Luna 注入子系统的核心管理类，承担了过多职责：存储注入点、维护注册表、执行注入编排、处理查询、管理生命周期等。这是一个典型的 God Class，存在以下问题：

1. **职责过多**：存储、注册、编排、查询混在一个类中，多个变化原因
2. **难以测试**：无法独立测试存储逻辑或注册逻辑
3. **难以演进**：任何修改都可能影响其他职责
4. **性能瓶颈**：包名匹配使用线性遍历，注入点数量增长后性能下降

## 方案选项

### 选项 A: 保持 InjectionManager 不变

维持现有的 God Class 结构，通过内部方法组织不同职责。

优点：
- 无需重构，零迁移成本
- 所有注入相关逻辑在一处，查找方便

缺点：
- 职责继续膨胀，问题持续恶化
- 无法独立测试各职责
- 包名匹配性能瓶颈无法针对性优化
- 违反单一职责原则

### 选项 B: 拆分为 InjectionRepository/InjectionRegistry/InjectionService 三层

按职责拆分为三个独立模块：

- **InjectionRepository**：纯存储，负责注入点的持久化和查询
- **InjectionRegistry**：高性能内存注册表，基于 PackageTrie 实现 O(k) 的包名匹配
- **InjectionService**：领域编排，协调 Repository 和 Registry 完成注入流程

优点：
- 单一职责，每个模块职责明确
- 可独立测试，Repository/Registry/Service 各自验证
- Registry 基于 PackageTrie 实现包名匹配，性能 O(k)（k 为包名层级）
- 未来可独立演进各模块

缺点：
- 类数量增加（1 → 3）
- 组装复杂度提升，需要协调三个模块的协作
- 迁移成本：需要将现有 InjectionManager 的逻辑分散到三个类中

## 决策

选择**选项 B：拆分为 InjectionRepository/InjectionRegistry/InjectionService 三层**。

InjectionManager 的 God Class 问题是真实存在的架构腐化，职责过多导致难以测试和演进。拆分后每个模块职责单一，可独立测试和优化。PackageTrie 的 O(k) 包名匹配是 Registry 层的性能优化，在 God Class 中无法独立实现。类数量增加和组装复杂度是合理的工程代价。

## 影响

- InjectionManager 被拆分为 InjectionRepository、InjectionRegistry、InjectionService 三个类
- InjectionRegistry 基于 PackageTrie 实现高性能包名匹配
- InjectionService 负责领域编排，协调 Repository 和 Registry
- 现有依赖 InjectionManager 的代码需迁移到对应的新类
