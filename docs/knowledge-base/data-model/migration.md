# 存储策略与数据迁移

> **文档定位**: 定义缓存策略、持久化策略、数据迁移
> **更新时机**: 存储方式变更时更新
> **读者**: 架构师、开发者

---

## 1. 缓存策略

| 缓存 | 类型 | 实现 | 失效策略 |
|------|------|------|----------|
| BytecodeCache | 双层缓存 | ConcurrentHashMap + ConcurrentHashMap | 注入变更时清除 |
| ConditionRegistry | 预编译缓存 | ConcurrentHashMap<String, ExpressionNode> | unregister 时移除 |
| InjectionPointRegistry | 运行时注册 | ConcurrentHashMap<String, List<InjectionPoint>> | 移除注入点时更新 |

---

## 2. 持久化策略

| 数据 | 存储方式 | 刷盘策略 |
|------|----------|----------|
| PersistentInjection | JSON 文件 | 变更时同步写入 |

---

## 3. 数据迁移

当前无版本化迁移机制。

未来规划：
- Schema 版本号
- PersistentInjection 迁移
- 插件元数据迁移

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
