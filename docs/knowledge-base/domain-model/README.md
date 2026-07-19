# 领域模型

> **TL;DR**: 核心实体：InjectionPoint、PersistentInjection、LunaPlugin。关键关系：InjectionPoint 由 PersistentInjection 创建。核心不变量：注入代码异常不能传播到业务代码。⚠️ 同一类所有活跃注入点必须全部重新应用

## 文件索引

| 文件 | 内容 | 何时阅读 | 必读 |
|------|------|----------|------|
| entities.md | 实体定义、属性、业务规则 | 实体变更 | |
| value-objects.md | 值对象、枚举 | 类型变更 | |
| invariants.md | 业务不变量 | **任何任务** | **是** |
| events.md | 领域事件、聚合关系 | 流程变更 | |

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/17 | 从 domain-model.md 拆分为目录结构 | Tony.L |
