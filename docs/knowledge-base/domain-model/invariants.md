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
| INV-009 | LOG 探针必须填写非空表达式代码 | ProbeHandler / 校验逻辑 |
| INV-010 | AsmMethodExpressionInjector 处理 ATHROW 异常路径时必须使用 DUP 指令保留异常引用，替代 ACONST_NULL | AsmMethodExpressionInjector |
| INV-011 | GlobalClassFileTransformer 必须捕获并处理 VerifyError 等 Error 类型异常，避免穿透；InjectionService 所有触发 retransform 的方法（addInjection/removeInjection/updateInjection/toggleEnabled/suspendInjectionsByLocation/resumeInjectionsByLocation）必须捕获 Throwable 而非 Exception | GlobalClassFileTransformer、InjectionService |
| INV-012 | ProbeHandler.getCodeType() 当 usesCode() 为 true 时必须返回非空值 | ProbeHandler |
| INV-013 | InjectionService.addInjection() 必须在 retransform 失败时回滚持久化数据和注册表条目，避免数据与运行时状态不一致 | InjectionService.addInjection() |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/17 | 从 domain-model.md 拆分为目录结构 | Tony.L |
| 2026/07/19 | 补充8条业务不变量 | Tony.L |
| 2026/08/02 | INV-011 扩展到 InjectionService 所有 retransform 路径；新增 INV-013（addInjection 回滚） | Tony.L |
