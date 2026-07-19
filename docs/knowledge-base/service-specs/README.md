# 服务规格

> **TL;DR**: 核心服务：InjectionService、PluginManagerImpl、AgentRuntime。关键状态机：PluginState。⚠️ 插件卸载必须清理所有注册项

## 文件索引

| 文件 | 内容 | 何时阅读 |
|------|------|----------|
| services.md | 服务总览、职责定义、依赖、接口 | 服务变更 |
| state-machines.md | 状态流转 | 流程变更 |
