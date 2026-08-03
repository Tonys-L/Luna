# ADR Index

## 已采纳决策

| 编号 | 标题 | 业务分类 | 状态 | 影响模块 | 日期 |
|------|------|----------|------|----------|------|
| ADR-001 | 采用整洁架构与三层隔离 | 架构设计 | Accepted | luna-core, luna-agent | 2026-03-28 |
| ADR-002 | ASM 字节码操作通过 Shade 隔离 | 类隔离策略 | Accepted | luna-core | 2026-03-28 |
| ADR-003 | 自研 MVC 框架替代 Spring | Web 层设计 | Accepted | luna-agent | 2026-04-01 |
| ADR-004 | ASM + ByteKit 双引擎 | 字节码引擎 | Accepted | luna-core/bytecode | 2026-05-15 |
| ADR-005 | InjectionManager 拆分为 Repository/Registry/Service | 注入模型 | Accepted | luna-core/injection | 2026-05-20 |
| ADR-006 | PersistentInjection 统一模型 | 注入模型 | Accepted | luna-core/injection | 2026-05-25 |
| ADR-007 | Bootstrap JAR 精简注入 | 类隔离策略 | Accepted | luna-core/bootstrap | 2026-05-28 |
| ADR-008 | StampedLock 替代 ReentrantReadWriteLock | 并发策略 | Accepted | luna-core/plugin | 2026-05-29 |
| ADR-009 | 显式注册 + ServiceLoader | 插件框架 | Accepted | luna-core/plugin | 2026-05-29 |
| ADR-010 | 条件表达式预编译缓存 | 表达式引擎 | Accepted | luna-core/expression | 2026-06-01 |
| ADR-011 | AgentRuntime 统一启动编排 | Agent启动 | Accepted | luna-agent | 2026-06-05 |
| ADR-012 | 分析能力内聚到 analyzer 包 | 类分析 | Accepted | luna-core/analysis | 2026-06-05 |
| ADR-013 | 移除规则/模板功能 | 注入模型 | Accepted | luna-core/injection | 2026-06-10 |
| ADR-014 | MPSC RingBuffer 而非 Disruptor | 数据传输 | Accepted | luna-core/infra | 2026-06-15 |
| ADR-015 | 插件前端扩展点体系与硬编码消除 | 插件框架 | Proposed | luna-core/plugin, luna-ui | 2026-06-17 |
| ADR-016 | 探针输出视图类型声明 | 插件框架 | Proposed | luna-core/plugin, luna-ui | 2026-07-06 |

## ADR 生命周期

```text
Draft → Proposed → Accepted → Deprecated → Superseded
```

## ADR 模板

每个 ADR 文件使用以下结构：

```markdown
# ADR-NNN: 标题

## 状态

[Draft | Proposed | Accepted | Deprecated | Superseded by ADR-XXX]

## 背景

描述驱动此决策的背景和问题。

## 方案选项

### 选项 A

描述。

优点：
- ...

缺点：
- ...

### 选项 B

描述。

优点：
- ...

缺点：
- ...

## 决策

选择了哪个方案，以及为什么。

## 影响

此决策带来的影响和后果。
```

新增 ADR 时：
1. 在 `adr/` 目录下创建文件，命名格式：`NNN-简短标题.md`
2. 编号接续当前最大编号
3. 更新本索引的标题、业务分类、状态、影响模块和日期
4. 填写业务分类（对应 business-capabilities.md 中的能力名称）
