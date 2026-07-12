# 领域事件 & 聚合关系

> **文档定位**: 定义聚合根、聚合关系、领域事件
> **更新时机**: 修改聚合关系或事件时更新
> **读者**: 架构师、开发者

---

## 5. 聚合关系

```text
PersistentInjection (聚合根)
    ├── InjectionLocation (值对象，多态)
    │   ├── MethodInjectionLocation
    │   ├── LineNumberInjectionLocation
    │   ├── ConstructorLocation
    │   ├── ExceptionExitInjectionLocation
    │   ├── InvokeInjectionLocation
    │   └── FieldAccessLocation
    ├── InjectionTarget (值对象)
    │   ├── BaseTarget
    │   ├── MethodTarget
    │   ├── LineNumberTarget
    │   ├── ConstructorTarget
    │   └── FieldAccessTarget
    ├── CompiledCode (值对象)
    └── InjectionStatus (枚举)

InjectionPoint (聚合根)
    ├── InjectionTarget (值对象)
    ├── CompiledCode (值对象)
    └── InjectionLocation (值对象)

LunaPlugin (聚合根)
    ├── PluginInfo (值对象)
    ├── ProbeHandler (策略接口)
    ├── CodeEngine (策略接口)
    ├── InjectionLocation (策略接口)
    ├── BytecodeInjector (策略接口)
    ├── LunaController (策略接口)
    └── PluginLifecycleListener (监听接口)

CoreCapabilityRecord (聚合根)
    ├── CapabilityKind (枚举)
    ├── LifecyclePolicy (枚举)
    └── ReadinessState (枚举)
```

---

## 6. 领域事件

| 事件 | 触发时机 | 影响 |
|------|----------|------|
| InjectionCreated | 创建新注入点 | 触发 retransform |
| InjectionRemoved | 移除注入点 | 触发 retransform 恢复 |
| InjectionUpdated | 更新注入点 | 触发 retransform |
| InjectionSuspended | 挂起注入点 | 插件卸载时关联注入点被挂起 |
| InjectionResumed | 恢复注入点 | 触发 retransform |
| InjectionDisabled | 停用注入点 | 触发 retransform 恢复 |
| InjectionEnabled | 启用注入点 | 触发 retransform |
| PluginLoaded | 插件加载完成 | 注册扩展项 |
| PluginUnloaded | 插件卸载完成 | 清理注册项，关联注入点被挂起 |
| PluginUpdated | 插件更新完成 | 卸载旧版 + 加载新版 |
| ProbeTriggered | 探针触发 | 数据投递到 RingBuffer |
| CapabilityReady | 核心能力就绪 | 解除 ReadyGate 阻塞 |
| CapabilityDegraded | 核心能力降级 | 标记降级状态 |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/17 | 从 domain-model.md 拆分为目录结构 | Tony.L |
