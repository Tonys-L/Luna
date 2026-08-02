# 字节码注入经验教训

> 记录字节码注入、ASM 操作、类加载等相关的经验教训。

---

### 1.1 VerifyError 修复

**问题**: `line_before` 行号级注入触发 `java.lang.VerifyError: null`，导致字节码注入失败。

**原因**: 3 个相互作用的 Bug：

1. **Bug1 - 加载未初始化局部变量**: `LocalVariableScanner` 将第 N 行声明的变量报告为"可见"（作用域从第 N 行开始），但在 `LineNumberNode` 之前的位置该变量尚未被赋值。`ExpressionBytecodeAssembler.generateConditionCheck()` 加载所有可见局部变量绑定到 `EvaluationContext`，对未初始化变量执行 `ILOAD` 导致 JVM 验证器拒绝。
2. **Bug2 - FrameNode 与 COMPUTE_FRAMES 冲突**: 使用 `ClassReader.EXPAND_FRAMES` 读取类文件时，`MethodNode.instructions` 中产生 `FrameNode` 对象。当 `MethodNode.accept()` 写入 `ClassWriter` 时，残留的 `FrameNode` 干扰帧计算。
3. **Bug3 - contextVarIndex 与局部变量 slot 冲突**: `contextVarIndex = maxSlot + 1` 仅基于当前可见的参数和局部变量计算，但方法中可能存在不可见但已占用 slot 的变量（如循环变量 `i`），导致 `contextVarIndex` 与已占用 slot 冲突。

**解决方案**:

- Bug1: `LocalVariableScanner` 新增 `excludeSameLineStart` 参数，排除在注入行才声明的变量
- Bug2: 改用 `ClassReader.SKIP_FRAMES` 读取 + 写入前移除所有 `FrameNode`
- Bug3: 优先使用 `MethodNode.maxLocals` 作为 `contextVarIndex`，并在注入后更新 `maxLocals`

```java
// Bug3 修复：contextVarIndex 计算
int contextVarIndex = asmContext.getMaxLocals() > 0
        ? asmContext.getMaxLocals()    // 使用 maxLocals，避免 slot 冲突
        : maxSlot + 1;                // 回退到旧逻辑
```

**影响模块**: bytecode/injection
**日期**: 2026-05
**标签**: #VerifyError #ASM #字节码注入

---

### 1.2 Log4j2 ClassCastException

**问题**: Agent 启动后 `StrLookup.asSubclass()` 抛出 `ClassCastException`。

**原因**: 整个 Agent JAR 被注入 Bootstrap CL，Log4j2 的 lazy `PluginType.getPluginClass()` 导致类被 Bootstrap CL 加载，而 `Interpolator` 的 `StrLookup.class` 由 `LunaAgentClassLoader` 加载，两个 ClassLoader 加载的同类不兼容。

**解决方案**:

1. Logger 初始化必须在 `appendToBootstrapClassLoaderSearch()` 之前
2. 仅注入精简 Bootstrap JAR（仅含 probe+infra+expression，零 Log4j2 依赖）

**影响模块**: bootstrap, plugin/loader
**日期**: 2026-05
**标签**: #类加载冲突 #ClassCastException #BootstrapClassLoader

---

### 1.3 LineNumberInjectionType 并发 Bug

**问题**: `LineNumberInjectionType.withLineNumber()` 在并发下修改静态常量，破坏单例。

**原因**: `BEFORE`/`AFTER` 是 `static final` 单例字段，`withLineNumber()` 直接修改其 `lineNumber` 字段后返回 `this`。高并发下两个线程同时处理不同行号的规则时，会互相覆盖 `lineNumber`，导致字节码注入到错误的行。

```java
// 修复前：直接修改静态常量
public LineNumberInjectionType withLineNumber(int n) {
    this.lineNumber = n;   // ← 直接修改 static final 单例的字段！
    return this;
}

// 修复后：返回新实例
public LineNumberInjectionType withLineNumber(int n) {
    LineNumberInjectionType copy = new LineNumberInjectionType(getName(), getDescription());
    copy.lineNumber = n;
    return copy;   // 返回新实例，不污染静态常量
}
```

**解决方案**: 每次返回新实例，不修改静态常量。

**影响模块**: plugin/builtin/line
**日期**: 2026-05
**标签**: #并发安全 #单例模式

---

### 1.4 BytecodeCache 内存泄漏

**问题**: `BytecodeCache` 使用 `ConcurrentHashMap` 缓存字节码和分析结果，无驱逐策略，生产环境只增不减，长时间运行后内存持续增长。

**原因**: `ConcurrentHashMap` 无 LRU 驱逐机制，`remove()` 只在类被卸载时调用，但 JVM 中大多数类不会被卸载。应用长时间运行后，每个被注入过的类都常驻缓存。

```java
// 问题代码
private static final ConcurrentHashMap<String, byte[]> CACHE = new ConcurrentHashMap<>();

public static void putIfAbsent(String className, byte[] originalBytecode) {
    CACHE.putIfAbsent(className, originalBytecode);  // 只增不减
}
```

**解决方案**: 添加 LRU 驱逐或最大容量限制。

**影响模块**: infra
**日期**: 2026-05
**标签**: #内存泄漏 #缓存

---

### 1.5 LocalVariableScanner 算法漏洞

**问题**: `LocalVariableScanner` 局部变量可见性算法存在多个边界场景漏洞。

**原因**: 算法从行号比较改为字节码位置比较后，引入了新的边界问题。

**已确认漏洞**:

| 编号 | 严重度 | 漏洞描述 |
|------|--------|---------|
| V17 | P0 | `bytecode.length == 0` 时 ClassReader 抛出 ArrayIndexOutOfBoundsException 未被捕获 |
| V1 | P1 | else 块变量不可见：编译器未为 else 块首行生成 LineNumberNode → queryPos=-1 → 返回空列表 |
| V11/V19 | P2 | 循环回边：同一行号出现两次时，`findInstructionPosForLine` 只返回第一个匹配位置 |
| V15 | P2 | excludeSameLineStart + 循环回边交互：循环回边位置已初始化的变量仍被排除 |
| V3 | P3 | try-with-resources 编译器生成的隐藏变量（`$is0`）暴露给用户 |
| V12 | P3 | `startPos < 0` 保守显示变量，混淆字节码可能利用此漏洞 |
| V13 | P3 | `endPos < 0` 变量永不过期，混淆字节码可能利用此漏洞 |
| V14 | P3 | 无 methodDescriptor 时匹配第一个同名方法，重载方法可能返回错误变量 |

**解决方案**: 逐个修复，V17 为最高优先级。已修复：V17（bytecode.length 校验）、V1 的 queryPos<0 返回空列表。待修复：V1（else 块行号缺失）、V3（隐藏变量过滤）、V12/V13（保守策略改为保守隐藏）。

**影响模块**: bytecode/asm/analyzer
**日期**: 2026-06
**标签**: #算法漏洞 #LocalVariableScanner

---

### 1.6 UiManifestVO 缺失 codeType 导致 LOG 探针不显示变量

**问题**: LOG 探针输出仅有前缀（如 "line before:"），不显示用户输入的表达式变量内容。

**原因**: `UiManifestVO` 的 `ProbeTypeEntry` 缺少 `codeType` 字段，前端 `PluginRegistry` 的 `codeEngines` 列表始终为空，`getDefaultCodeType()` 返回空字符串 `""`。

完整影响链：
1. `UiManifestVO` 无 `codeType` 字段 → 前端 `codeEngines = []`
2. `getDefaultCodeType()` → `engines.length === 0` → 返回 `''`
3. `InjectRequest.codeType = ""` → `PersistentInjection.codeType = ""`
4. `AbstractProbeHandler.validate()` 仅检查 `null`，不检查空字符串 → 验证通过
5. `InjectionPointFactory.compileCode()` 检查 `codeType.isEmpty()` → 返回 `null`
6. `TreeApiBytecodeHelper.assemble()` → `compiledCode = null` → `content = ""`
7. `LogExpressionHandler.generateBytecode()` → `expression = ""` → 仅输出前缀

**解决方案**:
- `ProbeHandler` 接口增加 `getCodeType()` 默认方法
- `LogProbeHandler` 覆写返回 `"EXPRESSION"`
- `ProbeTypeEntry` 增加 `codeType` 字段
- `AbstractProbeHandler.validate()` 同时检查 null 和空字符串
- 前端 `getDefaultCodeType()` 直接从 `handler.codeType` 获取

**教训**: API 契约字段必须与前端依赖一致。当前端需要某个值但 API 不提供时，会静默降级而非报错，导致难以排查的运行时问题。`validate()` 应同时检查 null 和空字符串。

**影响模块**: plugin/web, plugin, frontend
**日期**: 2026-07
**标签**: #API契约缺失 #前端-后端数据不一致 #静默降级

---

### 1.9 retransform 失败回滚与 Throwable 捕获

**问题**: `InjectionService.addInjection()` 在 retransform 失败时不回滚持久化数据和注册表条目，导致数据与运行时状态不一致；`triggerRetransform()` 和 `GlobalClassFileTransformer.transform()` 捕获 Exception 而非 Throwable，VerifyError 等 Error 类型异常穿透。

**原因**:
1. `triggerRetransform()` 内部 `catch (Exception e)` 静默吞噬异常，调用方无法感知失败
2. `addInjection()` 的 `catch (Exception e)` 块因 `triggerRetransform()` 已吞噬异常而完全不可达，回滚逻辑即使加上也无法触发
3. `GlobalClassFileTransformer.transform()` 循环内 `catch (Exception e)` 无法捕获 VerifyError，且 Error 会中断循环导致后续注入点未应用（违反 INV-001）
4. `DefaultInjectionRepository.persist()` 无同步机制，并发写入导致文件损坏

**解决方案**:
1. `triggerRetransform()` 改为 `catch (Throwable t)` 并抛出 RuntimeException，让调用方感知失败
2. `addInjection()` 改为 `catch (Throwable t)` 并执行回滚（unregister + delete），回滚失败仅记录日志
3. `GlobalClassFileTransformer.transform()` 改为 `catch (Throwable t)`，不中断循环
4. 所有调用 `triggerRetransform()` 的方法（removeInjection/updateInjection/toggleEnabled/suspendInjectionsByLocation/resumeInjectionsByLocation）补全 try-catch(Throwable)
5. `DefaultInjectionRepository.persist()` 使用 synchronized 保护
6. `toggleEnabled()` enable 失败时必须同时回滚 enabled 标志 **和** unregister registry 条目（仅回滚标志会导致 registry 与持久化状态不一致）

```java
// addInjection 回滚示例
catch (Throwable t) {
    LOGGER.error("Failed to register injection point: {}, rolling back", injection.getId(), t);
    try {
        injectionRegistry.unregister(injection.getId());
        injectionRepository.delete(injection.getId());
    } catch (Exception rollbackEx) {
        LOGGER.error("Rollback failed for injection: {}", injection.getId(), rollbackEx);
    }
}

// toggleEnabled 回滚示例（M-3 修复）
catch (Throwable t) {
    LOGGER.error("Failed to create injection point on enable: {}, rolling back", id, t);
    injection.setEnabled(false);
    injectionRepository.save(injection);
    injectionRegistry.unregister(id);  // 必须同时 unregister，否则 registry 残留
}
```

**影响模块**: injection/transformer
**日期**: 2026-08
**标签**: #retransform #回滚 #Throwable #VerifyError #并发
**关联不变量**: INV-001、INV-011、INV-013
