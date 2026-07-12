# 插件生命周期经验教训

> 记录插件加载、卸载、更新、探针兼容性等相关的经验教训。

---

### 2.1 TRACE 探针 + LINE_BEFORE 非法组合

**问题**: 用户选择"方法耗时"探针后，在行号注入位置提交注入，后端返回 "Probe type 'TRACE' does not support location: LINE_BEFORE"。

**原因**: 前端 `InjectionDialog` 的注入位置下拉框未根据当前探针类型的 `supportedInjectionLocations` 过滤，允许用户选择不兼容的组合。同时 `injectionTypes.name`（`ENTER_METHOD`）与 `supportedInjectionLocations`（`method_enter`）命名格式不一致，导致简单的大小写转换无法匹配。

**解决方案**:

1. `injectionTypes` 增加 `canonicalName` 字段，与后端 `InjectionLocation.getName()` 一致
2. `groupedInjectionTypes` 根据 `canonicalName` 与 `supportedInjectionLocations` 做过滤
3. 切换探针类型时，自动将不兼容的注入位置重置为第一个可用位置

**预防措施**: 前后端共享的枚举值必须使用统一的命名约定，或在数据模型中增加显式映射字段。

**影响模块**: plugin/builtin/trace, plugin/builtin/line
**日期**: 2026-06
**标签**: #探针兼容性 #前端过滤

---

### 2.2 TRACE 注入成功但不生效

**问题**: TRACE 探针注入后端日志显示 "Successfully applied"，但方法执行后没有耗时输出。

**原因**: 三层问题叠加：

1. 前端 TRACE `usesCode=false` 时清空 `logContent`，导致 `code` 传空字符串
2. 后端 `DefaultClassTransformer` 在 `usesCode=false` 时跳过 code 编译，`compiledCode` 为 null
3. `TreeApiBytecodeHelper.assemble()` 未处理 `compiledCode` 为 null 的情况，NPE 被 catch 吞掉

**解决方案**:

1. `TreeApiBytecodeHelper.assemble()` 增加 `compiledCode` null 安全处理
2. `DefaultClassTransformer` 在有 code 时即使 `usesCode=false` 也编译，默认 codeType 为 EXPRESSION
3. 前端 TRACE 探针根据注入位置自动填充 `start`/`end:0` 协议指令

**预防措施**: `usesCode=false` 不等于"不需要 code 字段"，需区分"用户不输入"和"系统不需要"两种语义。

**影响模块**: plugin/builtin/trace
**日期**: 2026-06
**标签**: #TRACE探针 #usesCode语义

---

### 2.3 ByteKitInjectorBase 路由错误

**问题**: TRACE 探针注入后端日志显示 "Successfully applied"，但方法执行后没有耗时输出。

**原因**: `ByteKitInjectorBase.inject()` 使用 `probeHandler.usesCode()` 作为路由条件：`usesCode=true` 走 ASM 表达式注入路径，`usesCode=false` 走 ByteKit 拦截器路径。TRACE 探针 `usesCode=false`，走了 ByteKit 路径，但 ByteKit 的 `EnterInterceptor.onEnter()` 是空方法，完全忽略了 `probeHandler`，导致 TRACE 探针的字节码没有被生成。

**解决方案**: `ByteKitInjectorBase.inject()` 在 `probeHandler != null` 时始终走 `injectWithExpression()` 路径，只有 `probeHandler == null` 时才走 `injectWithByteKit()` 路径。`usesCode` 不应作为路由条件。

**预防措施**: 当一个方法有两个路由分支，且其中一个分支忽略了关键参数时，应审查路由条件是否正确。

**影响模块**: bytecode/bytekit
**日期**: 2026-06
**标签**: #路由逻辑 #ByteKit

---

### 2.4 插件 update() 时序错误

**问题**: `update()` 中 `registry.getOldVersion()` 调用时序错误，返回 null 或抛异常。

**原因**: `doUnload()` 执行后旧插件已从 registry 移除，`getOldVersion()` 无法再取到旧版本号。

```java
// 修复前：doUnload 后旧版本已移除
PluginUnloadResult unloadResult = doUnload(pluginId);
return PluginUpdateResult.success(pluginId,
    registry.getOldVersion(pluginId),  // ← 返回 null
    newPlugin.getVersion());

// 修复后：在 doUnload 前保存旧版本
String oldVersion = registry.getPlugin(pluginId)
    .map(LunaPlugin::getVersion).orElse("unknown");
PluginUnloadResult unloadResult = doUnload(pluginId);
return PluginUpdateResult.success(pluginId, oldVersion, newPlugin.getVersion());
```

**解决方案**: 在 `doUnload()` 之前保存旧版本信息。

**影响模块**: plugin/lifecycle
**日期**: 2026-06
**标签**: #时序错误 #插件更新

---

### 2.5 插件 load() ClassLoader 泄漏

**问题**: `load()` 在 ID 冲突时 early-return 路径未关闭 `PluginClassLoader`，文件句柄泄漏，且插件 JAR 会被锁定（Windows 下尤其明显）。

**原因**: `PluginClassLoader` 已创建并实例化插件后，检查 ID 冲突直接返回 failure，未关闭 ClassLoader。catch 块中有关闭逻辑，但 early-return 路径遗漏。

```java
// 修复前：early-return 未关闭 CL
cl = new PluginClassLoader(...);
plugin = instantiate(cl);
if (registry.contains(plugin.getId())) {
    return PluginLoadResult.failure("...");  // ← CL 未关闭
}

// 修复后：所有退出路径关闭 CL
if (registry.contains(plugin.getId())) {
    try { cl.close(); } catch (IOException ignored) {}
    return PluginLoadResult.failure("...");
}
```

**解决方案**: 所有退出路径必须关闭 ClassLoader。

**影响模块**: plugin/loader
**日期**: 2026-06
**标签**: #资源泄漏 #ClassLoader

---

### 2.6 插件 load() TOCTOU 竞态

**问题**: ID 冲突检查到注册之间无锁，两个并发的 `load()` 调用可同时通过检查，最终双重注册同一 ID 的插件。

**原因**: `registry.contains()` 检查和 `registry.register()` 注册之间没有持写锁，存在 Time-of-Check to Time-of-Use (TOCTOU) 竞态。

**解决方案**: 检查和注册必须在同一写锁内完成。

**影响模块**: plugin/lifecycle
**日期**: 2026-06
**标签**: #并发安全 #TOCTOU

---

### 2.7 PluginManager 自我监听重复调用

**问题**: PluginManager 既是监听者又在 `load()` 内部直接调用，导致 `resumeSuspendedRules` 被调用两次。

**原因**: `load()` 内部步骤 7 已调用 `resumeSuspendedRules()`，PluginManager 又通过事件监听 `onLoaded` 再次触发。第二次在规则已经是 ACTIVE 状态时再走一遍更新逻辑，是无效操作但产生冗余日志和 DB 写入。

**解决方案**: 明确职责边界，避免自我监听。`load()` 内部直接调用，不通过事件触发。

**影响模块**: plugin/lifecycle
**日期**: 2026-06
**标签**: #设计问题 #重复调用
