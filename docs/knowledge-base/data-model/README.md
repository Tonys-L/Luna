# 数据模型

> **TL;DR**: 存储：内存 Map + JSON 文件。核心数据：InjectRequest、PersistentInjection、InjectionPoint。⚠️ 临时注入（ephemeral）不持久化

## 文件索引

| 文件 | 内容 | 何时阅读 |
|------|------|----------|
| tables.md | 数据模型总览、核心数据结构、字段定义 | 数据结构变更 |
| migration.md | 存储策略、缓存策略 | 数据库变更 |
