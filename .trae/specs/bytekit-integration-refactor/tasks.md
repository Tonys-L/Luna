# Tasks — TDD 纵向切片

> 方法论：每个任务 = RED(写失败测试) → GREEN(最小实现) → REFACTOR(重构) → COMMIT(提交)
> 提交格式：`feat: 行为描述` 或 `refactor: 重构描述`
> 最后更新：2026/05/31

---

## Phase 0: 准备与验证

### Task 0.1: 引入 bytekit-core 依赖并验证 ASM 隔离 ✅

- [x] RED: 编写 `ByteKitIsolationTest`
  - 测试 `InterceptorProcessor` 类可被加载（`Class.forName`）
  - 测试 ByteKit shade ASM 的 `ClassReader` 与 Luna ASM 的 `ClassReader` 可同时加载
  - 测试 ByteKit 可对简单字节码执行 `InterceptorProcessor.process()` 并输出合法字节码

- [x] GREEN: 在 `luna-core/pom.xml` 添加 bytekit-core 依赖
  - 添加 `com.alibaba:bytekit-core:0.1.6` 依赖
  - 排除 ByteKit 的 `slf4j-api` 传递依赖（Luna 已有）
  - 验证 `mvn compile` 通过
  - 验证测试通过

- [x] REFACTOR: 无需重构

- [x] COMMIT: `feat: 引入 bytekit-core 依赖，验证 ASM 隔离` (c5085c5)

### Task 0.2: 创建 ByteKit 适配层包结构 ✅

- [x] RED: 编写 `ByteKitPackageStructureTest`
  - 测试 `fun.efto.luna.core.bytekit.adapter` 包存在
  - 测试 `fun.efto.luna.core.bytekit.interceptor` 包存在
  - 测试 `fun.efto.luna.core.bytekit.bridge` 包存在

- [x] GREEN: 创建包结构和 `package-info.java`

- [x] REFACTOR: 删除占位类，保留 `package-info.java`

- [x] COMMIT: `feat: 创建 ByteKit 适配层包结构` (5a9a4d3)

---

## Phase 1: 核心层抽象接口

### Task 1.1: 定义 VariableInfo 数据类 ✅

- [x] RED: 编写 `VariableInfoTest`
  - 测试 `VariableInfo(name, descriptor, slot)` 构造和 getter
  - 测试 `equals` 和 `hashCode`（相同参数的实例相等）

- [x] GREEN: 创建 `VariableInfo` 数据类
  - `fun.efto.luna.core.asm.VariableInfo`
  - final 类，不可变字段，标准 equals/hashCode

- [x] REFACTOR: 无需重构

- [x] COMMIT: `feat: 核心层抽象接口 VariableInfo + VariableSnapshotter + CodeInjector` (8463757)

### Task 1.2: 定义 VariableSnapshotter 接口 ✅

- [x] RED: 编写 `AsmVariableSnapshotterTest`
  - 测试 `AsmVariableSnapshotter`（适配 LocalVariableScanner）实现 `VariableSnapshotter` 接口
  - 测试 `snapshot()` 返回结果与 `LocalVariableScanner.scanVisibleLocalVariables()` 一致

- [x] GREEN: 创建 `VariableSnapshotter` 接口和 `AsmVariableSnapshotter` 适配器
  - `fun.efto.luna.core.asm.VariableSnapshotter`
  - `fun.efto.luna.core.asm.AsmVariableSnapshotter`（委托 `LocalVariableScanner`）

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 1.1 提交 (8463757)

### Task 1.3: 定义 CodeInjector 接口 ✅

- [x] RED: 编写 `AsmCodeInjectorTest`
  - 测试 `AsmCodeInjector`（委托现有 BytecodeInjector）实现 `CodeInjector` 接口
  - 测试 `inject()` 返回 `InjectionResult` 包含转换后字节码

- [x] GREEN: 创建 `CodeInjector` 接口和 `AsmCodeInjector` 适配器
  - `fun.efto.luna.core.injection.CodeInjector`
  - `fun.efto.luna.core.asm.AsmCodeInjector`（委托 `BytecodeInjector`）

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 1.1 提交 (8463757)

### Task 1.4: 验证核心层零 ByteKit/ASM import ✅

- [x] RED: 编写 `CoreLayerPurityTest`
  - 扫描 `fun.efto.luna.core.injection.*` 和 `fun.efto.luna.core.bytecode.*` 包下所有 Java 文件
  - 断言无 `import com.alibaba.bytekit.*` 或 `import org.objectweb.asm.*`
  - → 预期：测试通过（当前核心层已满足）

- [x] GREEN: 无需额外代码

- [x] REFACTOR: 无需重构

- [x] COMMIT: `test: CoreLayerPurityTest - verify core layer has no ByteKit/ASM imports` (7847467)

---

## Phase 2: ByteKit 适配器实现

### Task 2.1: ByteKit @AtEnter 增强验证（Tracer Bullet）✅

这是最关键的 tracer bullet，证明 ByteKit 集成路径端到端可行。

- [x] RED: 编写 `ByteKitEnterInjectorTest`
  - 测试：对 `SimpleTarget.simpleMethod()` 执行 @AtEnter 增强，输出字节码合法（L1 + L2）
  - 测试：增强后的方法可被反射调用（L3）

- [x] GREEN: 创建 `ByteKitEnterInjector` 和 `EnterInterceptor`
  - `fun.efto.luna.core.bytekit.adapter.ByteKitEnterInjector`
    - 实现 `BytecodeInjector` 接口
    - 内部使用 ByteKit `DefaultInterceptorClassParser` 解析 `EnterInterceptor`
    - 使用 `InterceptorProcessor.process()` 执行增强
  - `fun.efto.luna.core.bytekit.interceptor.EnterInterceptor`
    - `@AtEnter(inline = true, suppress = Throwable.class)` 注解
    - 回调方法：`onEnter(@Binding.This Object target, @Binding.MethodName String name)`

- [x] REFACTOR: 提取公共流程到 `ByteKitInjectorBase`
  - 将字节码解析 → MethodProcessor 创建 → InterceptorProcessor 执行 → 输出字节码的流程提取到基类
  - `ByteKitEnterInjector` 继承 `ByteKitInjectorBase`

- [x] COMMIT: `feat: ByteKit @AtEnter 增强验证通过（tracer bullet）` (83e37ad)

### Task 2.2: ByteKitEnterInjector 集成 LunaSpyBridge ✅

- [x] RED: 编写 `LunaSpyBridgeTest`
  - 测试：`LunaSpyBridge.onMethodEnter()` 可被调用
  - 测试：`LunaSpyBridge.onMethodExit()` 可被调用

- [x] GREEN: 创建 `LunaSpyBridge` 并修改 `EnterInterceptor`
  - `fun.efto.luna.core.bytekit.bridge.LunaSpyBridge`
    - `onMethodEnter(Object target, Object[] args, String methodName)` 静态方法
    - `onMethodExit(Object target, Object[] args, String methodName, Object returnValue)` 静态方法
    - 内部委托 `LunaSpy.onLog()`
  - 修改 `EnterInterceptor.onEnter()` 调用 `LunaSpyBridge.onMethodEnter()`

- [x] REFACTOR: 无需重构

- [x] COMMIT: `feat: LunaSpyBridge + ByteKitExitInjector + ByteKitAroundInjector` (f67ea14)

### Task 2.3: ByteKitExitInjector ✅

- [x] RED: 编写 `ByteKitExitInjectorTest`
  - 测试：对含多个 return 的方法执行 @AtExit 增强，字节码合法（L1 + L2）
  - 测试：所有退出点均执行了回调（L3 + ProbeMessage 验证）

- [x] GREEN: 创建 `ByteKitExitInjector` 和 `ExitInterceptor`
  - `fun.efto.luna.core.bytekit.adapter.ByteKitExitInjector`
  - `fun.efto.luna.core.bytekit.interceptor.ExitInterceptor`
    - `@AtExit(inline = true, suppress = Throwable.class)`
    - `@Binding.Return Object returnValue`

- [x] REFACTOR: 提取 `ByteKitInjectorBase` 中的公共逻辑

- [x] COMMIT: 合并到 Task 2.2 提交 (f67ea14)

### Task 2.4: ByteKitAroundInjector ✅

- [x] RED: 编写 `ByteKitAroundInjectorTest`
  - 测试：@AtEnter + @AtExit 组合增强后，入口和退出均执行了回调

- [x] GREEN: 创建 `ByteKitAroundInjector` 和 `AroundInterceptor`
  - 同一个 Interceptor 类中定义两个注解方法

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 2.2 提交 (f67ea14)

### Task 2.5: ByteKitExceptionExitInjector（新能力）✅

- [x] RED: 编写 `ByteKitExceptionExitInjectorTest`
  - 测试：对抛异常的方法执行 @AtExceptionExit 增强，字节码合法
  - 测试：异常退出时回调被触发，`@Binding.Throwable` 可获取异常对象

- [x] GREEN: 创建 `ExceptionExitInjectionType`、`ByteKitExceptionExitInjector`、`ExceptionExitInterceptor`
  - `fun.efto.luna.core.plugin.builtin.method.ExceptionExitInjectionType`
  - `fun.efto.luna.core.bytekit.adapter.ByteKitExceptionExitInjector`
  - `fun.efto.luna.core.bytekit.interceptor.ExceptionExitInterceptor`
    - `@AtExceptionExit(inline = true, suppress = Throwable.class)`
    - `@Binding.Throwable Throwable t`

- [x] REFACTOR: 无需重构

- [x] COMMIT: `feat: ByteKitExceptionExit + InvokeInjector + suppress安全验证` (0966e25)

### Task 2.6: ByteKitInvokeInjector（新能力）✅

- [x] RED: 编写 `ByteKitInvokeInjectorTest`
  - 测试：对含子函数调用的方法执行 @AtInvoke 增强，字节码合法
  - 测试：子函数调用前/后回调被触发

- [x] GREEN: 创建 `InvokeInjectionType`、`ByteKitInvokeInjector`、`InvokeInterceptor`
  - `fun.efto.luna.core.plugin.builtin.method.InvokeInjectionType`
  - `fun.efto.luna.core.bytekit.adapter.ByteKitInvokeInjector`
  - `fun.efto.luna.core.bytekit.interceptor.InvokeInterceptor`
    - `@AtInvoke(inline = true, suppress = Throwable.class, owner = String.class, name = "toUpperCase")`

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 2.5 提交 (0966e25)

### Task 2.7: suppress 异常保护验证 ✅

- [x] RED: 编写 `ByteKitSuppressSafetyTest`
  - 测试：Interceptor 回调抛异常时，业务方法正常返回（suppress 保护）
  - 测试：带参数方法拦截器抛异常时仍正常返回

- [x] GREEN: 创建 `SuppressHandler`（`@ExceptionHandler(inline = true)`）和 `BrokenInterceptor`
  - **关键发现**：ByteKit 的 `suppress = Throwable.class` 必须配合 `suppressHandler = XxxHandler.class`
    使用 `@ExceptionHandler` 注解的 handler 类才能生成 try-catch 块。
    仅设置 `suppress = Throwable.class` 不指定 `suppressHandler` 不会生成 try-catch。

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 2.5 提交 (0966e25)

> ⚠️ **已发现问题 BUG-001**：生产 Interceptor（EnterInterceptor/ExitInterceptor/AroundInterceptor/
> ExceptionExitInterceptor/InvokeInterceptor）均只有 `suppress = Throwable.class`，
> **缺少 `suppressHandler`**，不会生成 try-catch 块，suppress 保护实际未生效。
> 需要新增 Task 2.8 修复此问题。

---

## Phase 2 补充: suppressHandler 修复

### Task 2.8: 生产 Interceptor 补充 suppressHandler ✅

- [x] RED: 编写 `ProductionSuppressTest`
  - 测试：EnterInterceptor 增强后字节码包含 try-catch 异常保护
  - 测试：ExitInterceptor 增强后字节码包含 try-catch 异常保护
  - 测试：AroundInterceptor 增强后字节码包含 try-catch 异常保护
  - 测试：ExceptionExitInterceptor 增强后字节码包含 try-catch 异常保护
  - 测试：InvokeInterceptor 增强后字节码包含 try-catch 异常保护
  - → 实际结果：4/5 测试失败（Enter/Exit/Around/Invoke 缺少 suppressHandler，ExceptionExit 自带 try-catch 语义通过）

- [x] GREEN: 创建 `SuppressHandler` 类并修改所有生产 Interceptor
  - 创建 `fun.efto.luna.core.bytekit.interceptor.SuppressHandler`
    - `@ExceptionHandler(inline = true)` 注解
    - `onSuppress(@Binding.Throwable Throwable t)` 静态方法
  - 修改 `EnterInterceptor`：添加 `suppressHandler = SuppressHandler.class`
  - 修改 `ExitInterceptor`：添加 `suppressHandler = SuppressHandler.class`
  - 修改 `AroundInterceptor`：添加 `suppressHandler = SuppressHandler.class`（两处）
  - 修改 `ExceptionExitInterceptor`：添加 `suppressHandler = SuppressHandler.class`
  - 修改 `InvokeInterceptor`：添加 `suppressHandler = SuppressHandler.class`

- [x] REFACTOR: 无需重构

- [x] COMMIT: `fix: add SuppressHandler to production Interceptors - BUG-001 suppress protection now works` (37d5d1e)

---

## Phase 3: 表达式引擎集成 ✅

### Task 3.1: log: 表达式通过 ByteKit 注入 ✅

- [x] RED: 编写 `ByteKitLogExpressionTest`
  - 测试：`log:hello` 表达式通过 ByteKit ENTER 注入后，反射调用目标方法时输出日志
  - 测试：`log:hello $1` 表达式通过 ByteKit ENTER 注入后，日志包含参数值
  - 测试：`log:done` 表达式通过 ByteKit EXIT 注入后，反射调用目标方法时输出日志
  - → 实际结果：3/3 测试失败（ByteKitInjectorBase 忽略 bytecodeAssembler 参数）

- [x] GREEN: 创建 `AsmMethodExpressionInjector` + 修改 `ByteKitInjectorBase`
  - 创建 `fun.efto.luna.core.bytekit.adapter.AsmMethodExpressionInjector`
    - 实现 `BytecodeInjector` 接口
    - 支持 Phase.ENTER / EXIT / AROUND
    - 使用 ASM Tree API + `TreeApiBytecodeHelper.assemble()` 生成表达式字节码
    - ENTER 阶段仅扫描方法参数（避免加载未初始化的局部变量）
    - EXIT 阶段扫描所有可见局部变量
  - 修改 `ByteKitInjectorBase.inject()`：
    - 检测 `bytecodeAssembler instanceof ExpressionBytecodeAssembler`
    - 有表达式时委托给 `AsmMethodExpressionInjector`
    - 无表达式时使用 ByteKit 原有路径
  - 修改 `ByteKitExitInjector` / `ByteKitAroundInjector` 覆盖 `getExpressionPhase()`

- [x] REFACTOR: 无需重构

- [x] COMMIT: `feat: log expression through ByteKit injection - AsmMethodExpressionInjector hybrid strategy` (edd20d2)

### Task 3.2: snapshot: 表达式通过 ByteKit 注入 ✅

- [x] RED: 编写 `ByteKitSnapshotExpressionTest`
  - 测试：`snapshot:true` 表达式通过 ByteKit ENTER 注入后，可正确捕获方法参数
  - → 实际结果：VerifyError（AsmMethodExpressionInjector 扫描了所有局部变量，包括未初始化的 `sum`）

- [x] GREEN: 修复 `AsmMethodExpressionInjector.scanMethodParameters()`
  - ENTER 阶段：只返回方法参数（slot < paramSlotCount），避免加载未初始化变量
  - 新增 `computeParamSlotCount(access, desc)` 方法，从方法描述符计算参数槽位数
  - EXIT/AROUND 阶段：返回所有可见局部变量

- [x] REFACTOR: 无需重构

- [x] COMMIT: `feat: snapshot expression through ByteKit injection - AsmMethodExpressionInjector ENTER phase filters params only` (3cf0d3b)

---

## Phase 4: 注册表与初始化适配

### Task 4.1: CoreModuleInitializer 注册 ByteKit 注入器 ✅

- [x] RED: 编写 `ByteKitRegistrationTest`
  - 测试：`BytecodeInjectorRegistry.get(ENTER)` 返回 `ByteKitEnterInjector` 实例
  - 测试：`BytecodeInjectorRegistry.get(EXIT)` 返回 `ByteKitExitInjector` 实例
  - 测试：`BytecodeInjectorRegistry.get(AROUND)` 返回 `ByteKitAroundInjector` 实例

- [x] GREEN: 修改 `CoreModuleInitializer`
  - 方法级注入器替换为 ByteKit 实现
  - 新增 `EXCEPTION_EXIT` 和 `INVOKE` 注册

- [x] REFACTOR: 无需重构

- [x] COMMIT: `feat: CoreModuleInitializer 注册 ByteKit 注入器 + 双引擎调度 + 新类型注册` (6c9f742)

### Task 4.2: DefaultClassTransformer 双引擎调度 ✅

- [x] RED: 编写 `DualEngineDispatchTest`
  - 测试：方法级 InjectionPoint 走 ByteKit 路径
  - 测试：行号级 InjectionPoint 走 ASM 路径

- [x] GREEN: 修改 `DefaultClassTransformer`
  - 根据 `InjectionType` 名称判断引擎类型

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 4.1 提交 (6c9f742)

### Task 4.3: 新增 InjectionType 注册 ✅

- [x] RED: 编写 `NewInjectionTypeTest`
  - 测试：`InjectionTypeRegistry.resolve("exception_exit")` 返回 `ExceptionExitInjectionType`
  - 测试：`InjectionTypeRegistry.resolve("invoke")` 返回 `InvokeInjectionType`

- [x] GREEN: 在 `CoreModuleInitializer` 中注册新类型和 RuleConverter

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 4.1 提交 (6c9f742)

---

## Phase 5: 旧代码清理

### Task 5.1: 删除旧方法注入器 ✅

- [x] RED: 确认所有 ByteKit 替代测试通过
  - 运行 `ByteKitEnterInjectorTest`、`ByteKitExitInjectorTest`、`ByteKitAroundInjectorTest`
  - 确认 ByteKit 注入器已完全替代旧注入器

- [x] GREEN: 删除旧代码
  - 删除 `AbstractMethodInjector`
  - 删除 `EnterMethodInjector` / `ExitMethodInjector` / `AroundMethodInjector`
  - 删除 `EnterMethodVisitor` / `ExitMethodVisitor` / `AroundMethodVisitor` / `TryFinallyMethodVisitor`
  - 清理 `CoreModuleInitializer` 中对旧注入器的 import

- [x] REFACTOR: 清理仅被旧注入器使用的工具方法

- [x] COMMIT: `refactor: 删除旧方法注入器，ByteKit 替代完成` (6c9f742)

### Task 5.2: 验证 mvn test 全部通过 ✅

- [x] RED: 运行 `mvn test`
  - 确认所有测试通过（302 tests, 0 failures, 1 skipped）

- [x] GREEN: 无需修复

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 5.1 提交

---

## Phase 6: 回归测试与性能验证

### Task 6.1: 性能测试 ✅（代码未提交）

- [x] RED: 编写 `ByteKitPerformanceTest`
  - 测试：ByteKit 注入判定耗时 < 0.1ms
  - 实测结果：~430ns（含 InterceptorProcessor 缓存 + 注入结果缓存）

- [x] GREEN: 优化 ByteKit 注入路径
  - InterceptorProcessor DCL 缓存（避免重复解析）
  - 注入结果缓存（`InjectionCacheEntry`）
  - ClassReader 感知 ClassWriter（`ByteKitClassLoaderAwareClassWriter`）
  - `getCommonSuperClass` 静态缓存

- [x] REFACTOR: 无需重构

- [x] COMMIT: `test: ByteKit performance test + retransform verification + InterceptorProcessor cache optimization` (c81dd00)

### Task 6.2: retransform 动态增删验证 ✅

- [x] RED: 编写 `ByteKitRetransformTest`
  - 测试：动态新增注入点后 retransform 正常
  - 测试：动态删除注入点后 retransform 正常

- [x] GREEN: 无需修复（GlobalClassFileTransformer 不变）

- [x] REFACTOR: 无需重构

- [x] COMMIT: 合并到 Task 6.1 提交 (c81dd00)

---

# Task Dependencies

```
Phase 0:  0.1 → 0.2
Phase 1:  1.1 → 1.2 → 1.3 → 1.4
Phase 2:  1.4 → 2.1 → 2.2 → 2.3 → 2.4 → 2.5 → 2.6 → 2.7 → 2.8(新增)
Phase 3:  2.2 → 3.1 → 3.2  (medium 优先级，延后)
Phase 4:  2.7 → 4.1 → 4.2 → 4.3
Phase 5:  4.3 → 5.1 → 5.2
Phase 6:  5.2 → 6.1 → 6.2
```

# 待办事项优先级排序

| 优先级 | Task | 描述 |
|--------|------|------|
| ~~P0~~ | ~~Task 2.8~~ | ~~生产 Interceptor 补充 suppressHandler（BUG-001 修复）~~ ✅ |
| ~~P0~~ | ~~Phase 6 提交~~ | ~~提交性能测试和 retransform 测试代码~~ ✅ |
| ~~P1~~ | ~~Task 1.4~~ | ~~核心层 import 纯度测试~~ ✅ |
| ~~P2~~ | ~~Task 3.1~~ | ~~log: 表达式通过 ByteKit 注入~~ ✅ |
| ~~P2~~ | ~~Task 3.2~~ | ~~snapshot: 表达式通过 ByteKit 注入~~ ✅ |

# Commit 规范

| 类型 | 格式 | 示例 |
|------|------|------|
| 新功能 | `feat: 描述` | `feat: ByteKit @AtEnter 增强验证通过` |
| 重构 | `refactor: 描述` | `refactor: 删除旧方法注入器` |
| 测试 | `test: 描述` | `test: ByteKit suppress 异常保护验证` |
| 文档 | `docs: 描述` | `docs: ByteKit 集成设计文档` |
| 修复 | `fix: 描述` | `fix: 生产 Interceptor 补充 suppressHandler` |

# 已知问题

| ID | 描述 | 严重程度 | 状态 |
|----|------|---------|------|
| ~~BUG-001~~ | ~~生产 Interceptor 缺少 suppressHandler，suppress 保护未生效~~ | ~~高~~ | ✅ 已修复 |
| ~~TODO-001~~ | ~~Phase 6 代码未提交到 Git~~ | ~~中~~ | ✅ 已提交 |
