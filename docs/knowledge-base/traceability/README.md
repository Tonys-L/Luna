# Traceability Matrix

## 需求追踪

| 需求ID | 描述 | 业务分类 | ADR | 代码模块 | 测试 |
|--------|------|----------|-----|----------|------|
| REQ-001 | 方法级字节码注入 | 动态字节码注入 | ADR-001, ADR-002 | luna-core/injection, luna-core/transformer | InjectionRegistryTest |
| REQ-002 | 行号级字节码注入 | 动态字节码注入 | ADR-001 | luna-core/transformer | IntegrationTest |
| REQ-003 | 条件表达式求值 | 条件表达式求值 | ADR-001 | luna-core/expression | ExpressionParserTest |
| REQ-004 | 变量快照捕获 | 变量快照捕获 | ADR-004 | luna-core/plugin, luna-core/probe | SnapshotSerializerTest |
| REQ-005 | 方法耗时追踪 | 方法耗时追踪 | ADR-004 | luna-core/plugin | PluginIntegrationTest |
| REQ-006 | 插件热管理 | 插件热管理 | ADR-005, ADR-006 | luna-core/plugin | PluginManagerTest |
| REQ-007 | 源码反编译与类分析 | 源码反编译与类分析 | ADR-001 | luna-core (decompile/analyzer) | IntegrationTest |
| REQ-008 | Web UI 诊断控制台 | 支撑能力 | ADR-003 | luna-agent/web, luna-ui | E2E 测试 |

## ADR追踪

| ADR | 影响模块 |
|-----|----------|
| ADR-001 | luna-core, luna-agent |
| ADR-002 | luna-core (ASM Shade) |
| ADR-003 | luna-agent (自研 MVC) |
| ADR-004 | luna-core (RingBuffer) |
| ADR-005 | luna-core (PluginManager) |
| ADR-006 | luna-core (Core Capabilities) |
| ADR-007 | luna-core (InjectionModel) |
