# ByteKit 集成重构 Checklist

> 最后更新：2026/05/31

## Phase 0: 准备与验证 ✅

- [x] `ByteKitIsolationTest` 通过：InterceptorProcessor 可加载
- [x] `ByteKitIsolationTest` 通过：ByteKit shade ASM 与 Luna ASM 可同时工作
- [x] `ByteKitIsolationTest` 通过：ByteKit 可对简单字节码执行增强
- [x] `luna-core/pom.xml` 已添加 `bytekit-core` 依赖
- [x] `mvn compile` 通过
- [x] `mvn test` 通过
- [x] `fun.efto.luna.core.bytekit` 包结构已创建（adapter/interceptor/bridge）

## Phase 1: 核心层抽象接口 ✅

- [x] `VariableInfo` 数据类已创建，equals/hashCode 正确
- [x] `VariableSnapshotter` 接口已定义，零 ByteKit/ASM import
- [x] `AsmVariableSnapshotter` 适配器已创建，委托 LocalVariableScanner
- [x] `VariableSnapshotterTest` 通过：snapshot() 结果与 scanVisibleLocalVariables() 一致
- [x] `CodeInjector` 接口已定义，零 ByteKit/ASM import
- [x] `AsmCodeInjector` 适配器已创建，委托 BytecodeInjector
- [x] `CodeInjectorTest` 通过：inject() 返回 InjectionResult
- [x] `CoreLayerPurityTest` 通过：核心层无 ByteKit/ASM import

## Phase 2: ByteKit 适配器实现 ✅

- [x] `ByteKitEnterInjectorTest` 通过：@AtEnter 增强后字节码合法（L1+L2）
- [x] `ByteKitEnterInjectorTest` 通过：增强后方法可被反射调用（L3）
- [x] `ByteKitInjectorBase` 抽象基类已创建，封装通用流程
- [x] `LunaSpyBridgeTest` 通过：LunaSpyBridge 被触发
- [x] `LunaSpyBridge` 已创建，委托 LunaSpy
- [x] `ByteKitExitInjectorTest` 通过：@AtExit 增强后字节码合法
- [x] `ByteKitExitInjectorTest` 通过：所有退出点均执行回调
- [x] `ByteKitAroundInjectorTest` 通过：入口和退出均执行回调
- [x] `ByteKitExceptionExitInjectorTest` 通过：@AtExceptionExit 增强后字节码合法
- [x] `ByteKitExceptionExitInjectorTest` 通过：@Binding.Throwable 可获取异常对象
- [x] `ByteKitInvokeInjectorTest` 通过：@AtInvoke 增强后字节码合法
- [x] `ByteKitInvokeInjectorTest` 通过：子函数调用前后回调被触发
- [x] `ByteKitSuppressSafetyTest` 通过：suppress 保护下注入代码抛异常不影响业务
- [x] 生产 Interceptor 补充 suppressHandler ✅ BUG-001 已修复

## Phase 3: 表达式引擎集成 ✅

- [x] `ByteKitLogExpressionTest` 通过：log: 表达式通过 ByteKit 注入后输出日志
- [x] `ByteKitLogExpressionTest` 通过：log:hello $1 表达式包含参数值
- [x] `ByteKitSnapshotExpressionTest` 通过：snapshot: 表达式通过 ByteKit 注入后捕获方法参数
- [x] `AsmMethodExpressionInjector` 已创建，支持 ENTER/EXIT/AROUND 阶段
- [x] `ByteKitInjectorBase` 检测表达式时委托给 ASM 路径

## Phase 4: 注册表与初始化适配 ✅

- [x] `ByteKitRegistrationTest` 通过：ENTER/EXIT/AROUND 注册 ByteKit 注入器
- [x] `DualEngineDispatchTest` 通过：方法级走 ByteKit，行号级走 ASM
- [x] `NewInjectionTypeTest` 通过：exception_exit 和 invoke 类型可被解析
- [x] `CoreModuleInitializer` 已更新：方法级注入器替换为 ByteKit 实现
- [x] `CoreModuleInitializer` 已更新：新增 EXCEPTION_EXIT 和 INVOKE 注册

## Phase 5: 旧代码清理 ✅

- [x] `AbstractMethodInjector` 已删除
- [x] `EnterMethodInjector` / `ExitMethodInjector` / `AroundMethodInjector` 已删除
- [x] `EnterMethodVisitor` / `ExitMethodVisitor` / `AroundMethodVisitor` / `TryFinallyMethodVisitor` 已删除
- [x] `CoreModuleInitializer` 中对旧注入器的 import 已清理
- [x] 仅被旧注入器使用的工具方法已清理
- [x] `mvn compile` 通过
- [x] `mvn test` 全部通过（302 tests, 0 failures, 1 skipped）

## Phase 6: 回归测试与性能验证 ✅

- [x] `ByteKitPerformanceTest` 通过：注入判定耗时 < 0.1ms（实测 ~430ns）
- [x] `ByteKitRetransformTest` 通过：动态增删注入点正常
- [x] 所有现有测试通过
- [x] retransform 动态增删注入点正常工作
- [x] Phase 6 代码提交到 Git

## 性能优化记录

| 优化项 | 优化前 | 优化后 | 说明 |
|--------|--------|--------|------|
| InterceptorProcessor 缓存 | 576,026ns | ~2,000ns | DCL 缓存避免重复解析 |
| 注入结果缓存 | ~2,000ns | ~430ns | InjectionCacheEntry 缓存 |
| ClassReader 感知 ClassWriter | N/A | 减少 COMPUTE_FRAMES 开销 | ByteKitClassLoaderAwareClassWriter |
| getCommonSuperClass 静态缓存 | N/A | 减少反射调用 | 缓存类继承关系 |

## 已知问题

| ID | 描述 | 严重程度 | 状态 | 关联 Task |
|----|------|---------|------|-----------|
| ~~BUG-001~~ | ~~生产 Interceptor 缺少 suppressHandler，suppress 保护未生效~~ | ~~高~~ | ✅ 已修复 | Task 2.8 |
| ~~TODO-001~~ | ~~Phase 6 代码未提交到 Git~~ | ~~中~~ | ✅ 已提交 | Task 6.1/6.2 |
| ~~TODO-002~~ | ~~CoreLayerPurityTest 未创建~~ | ~~低~~ | ✅ 已补充 | Task 1.4 |
