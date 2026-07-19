# 不变量 (Invariants)

> ⚠️ **必读文档**：任何任务都必须阅读本文档，不变量不可被绕过。

| 编号 | 不变量描述 | 检查位置 |
|------|-----------|----------|
| INV-001 | 同一类的所有活跃注入点必须在 retransform 时全部重新应用 | DefaultClassTransformer |
| INV-002 | 注入代码异常不能传播到业务代码 | SuppressHandler / try-catch 防御 |
| INV-003 | RingBuffer 满时丢弃不阻塞业务线程 | RingBuffer.offer() |
| INV-004 | 条件表达式求值失败时默认跳过 | ConditionRegistry.test() |
| INV-005 | 插件卸载时必须清理所有注册项和挂起关联注入点 | PluginManagerImpl |
| INV-006 | Agent 依赖不能污染目标应用 ClassLoader | LunaAgentClassLoader |
| INV-007 | retransform 操作必须持有 transformLock（StampedLock） | PluginManagerImpl |
| INV-008 | 临时注入（ephemeral）不持久化 | InjectionPersistenceService |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/17 | 从 domain-model.md 拆分为目录结构 | Tony.L |
