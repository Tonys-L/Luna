# 架构深化：重塑 Injection 核心模块边界

重构现有的 `InjectionManager` 和 `InjectionService`，解决职责过度交织与运行时的性能隐患。

## User Review Required

> [!IMPORTANT]
> **正则匹配策略退化确认**
>
> 我们确定了在 `InjectionRegistry` 中引入字典树（Trie）以优化 `ClassFileTransformer` 的高频调用性能（匹配耗时必须 **< 0.1ms**）。
> - Trie 可以将**包名前缀匹配**（例如 `fun.efto.luna.*`）的耗时降至 O(k)。
> - 但 Trie 无法优化后缀匹配（如 `.*ServiceImpl`）或复杂的中间正则。
> - **我们的策略**：Registry 内部将对前缀规则放入 Trie 进行极速匹配；对于少量复杂的正则规则，保留一个 Fallback 的遍历匹配池。考虑到实际业务中绝大多数情况都是按包注入的，这个 Fallback 造成的开销是可控的。您是否同意此退化策略？

## 核心领域语言 (已同步至 CONTEXT.md)

1. **InjectionRepository**: 纯存储层，负责 `PersistentInjection` 的持久化操作。
2. **InjectionRegistry**: 内存注册表，内部采用 Trie 树等结构以达到极致查询性能。
3. **InjectionService**: 领域编排服务，整合校验、编译、存储、以及触发重转换的工作流。

## Proposed Changes

### Core API 与核心模块分离

#### [DELETE] `luna-core/src/main/java/fun/efto/luna/core/injection/InjectionManager.java`
彻底移除这个 God Class，它的指责将被拆分。

#### [NEW] `luna-core/src/main/java/fun/efto/luna/core/injection/InjectionRepository.java`
新增存储接口及其内存/持久化实现（暂时保留原有的 `InjectionStore` 的能力并改名，或将其作为实现）。
- `save(PersistentInjection)`
- `delete(String id)`
- `findAll()`

#### [NEW] `luna-core/src/main/java/fun/efto/luna/core/injection/InjectionRegistry.java`
新增专注运行时的内存注册表，并引入基于字典树（Trie）的匹配算法。
- `register(InjectionPoint)`
- `unregister(String id)`
- `getActivePointsForClass(String className)` (供 Transformer 使用)

#### [MODIFY] `luna-core/src/main/java/fun/efto/luna/core/injection/InjectionService.java`
将原有的验证逻辑与 Manager 的编排逻辑合并。
引入必要的依赖倒置，持有 `InjectionRepository`, `InjectionRegistry`, 和 `Retransformer` 的引用。
工作流：接受请求 -> 校验变量 -> `Repository.save()` -> 编译为 `InjectionPoint` -> `Registry.register()` -> 触发重转换。

### 关联适配层改造

#### [MODIFY] `luna-core/src/main/java/fun/efto/luna/core/transformer/GlobalClassFileTransformer.java`
- 依赖由 `InjectionManager` 改为 `InjectionRegistry`（或引入 `InjectionQuery` 接口供 Registry 实现）。

#### [MODIFY] `luna-agent/src/main/java/fun/efto/luna/agent/Agent.java`
- 重新编排启动逻辑：初始化 Repository、Registry，并组装给 Service。原来对 Manager 单例的依赖将被消除。

#### [MODIFY] `luna-core/src/main/java/fun/efto/luna/core/infra/web/InjectionController.java`
- 移除对 Manager 的直接调用，全部转交 `InjectionService` 处理。

#### [MODIFY] `luna-core/src/main/java/fun/efto/luna/core/injection/rule/RuleManager.java`
- RuleManager 也曾经与 InjectionManager 耦合用于同步规则，修改为通过 InjectionService 同步。

## 验证计划

### 自动化测试
- **Registry 性能微基准测试**：编写专门的测试，注入包含 1000 个无规则冲突的 Trie 树以及极少量的复杂正则。模拟类加载场景，使用 10,000 个类名进行连续探测，断言单次查找时间严格控制在 **< 0.1ms** 之下。
- **边界与流程测试**：测试完整的注入流程 (`Service` -> `Repo` -> `Registry`)，确保 Mock 掉 Repository 时依然能进行逻辑覆盖。

### 集成验证
- 在 WebServer 模块发一次完整 HTTP POST，模拟真实的动态代码注入全链路，并观测注入是否在应用层生效。
