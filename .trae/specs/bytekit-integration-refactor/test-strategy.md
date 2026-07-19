# ByteKit 集成重构 — 测试策略文档

> 日期：2026/05/31
> 方法论：TDD 纵向切片（Tracer Bullet），每个测试验证一个行为，不测实现细节
> 最后更新：2026/05/31

---

## 一、测试原则

### 1.1 行为测试 vs 实现测试

| ✅ 好的测试（测行为） | ❌ 坏的测试（测实现） |
|----------------------|---------------------|
| "ByteKit 注入后目标方法入口处执行了回调" | "ByteKitEnterInjector 调用了 InterceptorProcessor.process()" |
| "注入后的字节码可被 ClassLoader 加载" | "ByteKitInjectorBase 创建了 MethodProcessor" |
| "异常退出注入时 @Binding.Throwable 可获取异常对象" | "ExceptionExitInterceptor 的参数列表包含 ThrowableBinding" |
| "suppress 保护下注入代码抛异常不影响业务" | "TryCatchBlock 的 start label 在 INVOKESTATIC 之前" |

### 1.2 三层验证策略（沿用现有模式）

| 层级 | 验证方式 | 保障 |
|------|---------|------|
| L1: ClassReader 可解析 | `new ClassReader(bytecode).accept(...)` | 字节码格式合法 |
| L2: ClassLoader 可加载 | `BytecodeClassLoader.defineClass()` | JVM 验证通过，无 VerifyError |
| L3: 反射调用可执行 | `method.invoke(instance, args)` | 运行时无异常 |

### 1.3 不使用 Mock

沿用项目现有约定：**不使用 Mockito**，通过真实对象和自定义 ClassLoader 验证。

---

## 二、TDD 行为列表

按优先级排序，每个行为对应一个测试用例。遵循纵向切片原则：**一个测试 → 一个实现 → 提交**。

### Phase 0: 准备与验证 ✅

| # | 行为描述 | 验证方式 | 优先级 | 状态 |
|---|---------|---------|--------|------|
| 0.1 | ByteKit 的 `InterceptorProcessor` 类可被加载 | `Class.forName("com.alibaba.bytekit.asm.interceptor.InterceptorProcessor")` 不抛异常 | P0 | ✅ |
| 0.2 | ByteKit shade ASM 与 Luna ASM 可同时工作 | 分别加载两个包的 ClassReader，均不抛异常 | P0 | ✅ |
| 0.3 | ByteKit 可对简单类执行 @AtEnter 增强并输出合法字节码 | L1 + L2 验证 | P0 | ✅ |

### Phase 1: 核心层抽象接口 ✅ (1.4 未完成)

| # | 行为描述 | 验证方式 | 优先级 | 状态 |
|---|---------|---------|--------|------|
| 1.1 | `CodeInjector` 接口可被 ASM 实现类实现 | `AsmCodeInjector implements CodeInjector` 编译通过 | P1 | ✅ |
| 1.2 | `VariableSnapshotter` 接口可被 LocalVariableScanner 适配 | 调用 `snapshot()` 返回与现有 `scanVisibleLocalVariables()` 一致的结果 | P1 | ✅ |
| 1.3 | `VariableInfo` 数据类可正确存储 name/descriptor/slot | 构造 + getter + equals 验证 | P1 | ✅ |
| 1.4 | 核心层包无 ByteKit / ASM import | 源码扫描断言 | P1 | ⚠️ 未创建 |

### Phase 2: ByteKit 适配器 ✅ (2.8 待修复)

| # | 行为描述 | 验证方式 | 优先级 | 状态 |
|---|---------|---------|--------|------|
| 2.1 | ByteKitEnterInjector 对简单方法执行 ENTER 注入后字节码合法 | L1 + L2 验证 | P0 | ✅ |
| 2.2 | ByteKitEnterInjector 注入后方法入口处执行了回调 | L3 反射调用 + ProbeMessage 验证 | P0 | ✅ |
| 2.3 | ByteKitExitInjector 对含多个 return 的方法执行 EXIT 注入后字节码合法 | L1 + L2 验证 | P0 | ✅ |
| 2.4 | ByteKitExitInjector 注入后所有退出点均执行了回调 | L3 反射调用 + ProbeMessage 验证 | P0 | ✅ |
| 2.5 | ByteKitAroundInjector 注入后入口和退出均执行了回调 | L3 反射调用 + ProbeMessage 验证 | P1 | ✅ |
| 2.6 | ByteKitExceptionExitInjector 对抛异常方法执行注入后字节码合法 | L1 + L2 验证 | P1 | ✅ |
| 2.7 | ByteKitExceptionExitInjector 注入后异常退出时执行了回调 | L3 反射调用 + try-catch 验证 | P1 | ✅ |
| 2.8 | ByteKitInvokeInjector 对含子函数调用的方法执行注入后字节码合法 | L1 + L2 验证 | P2 | ✅ |
| 2.9 | ByteKitInvokeInjector 注入后子函数调用前后执行了回调 | L3 反射调用 + ProbeMessage 验证 | P2 | ✅ |
| 2.10 | ByteKit suppress 保护下注入代码抛异常不影响业务 | L3 反射调用，业务方法正常返回 | P0 | ✅ |
| 2.11 | ByteKit inline 模式下注入代码无 INVOKESTATIC 调用 | 字节码扫描验证无 INVOKESTATIC 到 Interceptor 类 | P2 | — 未验证 |
| 2.12 | 生产 Interceptor suppress 保护实际生效 | L3 反射调用，业务方法正常返回 | P0 | ⚠️ BUG-001 |

### Phase 3: LunaSpy 桥接 ✅

| # | 行为描述 | 验证方式 | 优先级 | 状态 |
|---|---------|---------|--------|------|
| 3.1 | LunaSpyBridge.onMethodEnter 可被调用 | 直接调用 + ProbeMessage 验证 | P0 | ✅ |
| 3.2 | LunaSpyBridge.onMethodExit 可被调用 | 直接调用 + ProbeMessage 验证 | P0 | ✅ |
| 3.3 | log: 表达式通过 ByteKit 注入后可正确输出日志 | L3 + ProbeMessage 内容验证 | P1 | ⚠️ 延后 |
| 3.4 | snapshot: 表达式通过 ByteKit 注入后可正确捕获局部变量 | L3 + ProbeMessage 内容验证 | P1 | ⚠️ 延后 |

### Phase 4: 注册表与初始化 ✅

| # | 行为描述 | 验证方式 | 优先级 | 状态 |
|---|---------|---------|--------|------|
| 4.1 | CoreModuleInitializer 注册 ByteKit 方法级注入器 | `BytecodeInjectorRegistry.get(ENTER)` 返回 ByteKitEnterInjector 实例 | P0 | ✅ |
| 4.2 | EXCEPTION_EXIT 注入类型可被 InjectionTypeRegistry 解析 | `InjectionTypeRegistry.resolve("exception_exit")` 不抛异常 | P1 | ✅ |
| 4.3 | INVOKE 注入类型可被 InjectionTypeRegistry 解析 | `InjectionTypeRegistry.resolve("invoke")` 不抛异常 | P2 | ✅ |
| 4.4 | DefaultClassTransformer 方法级注入走 ByteKit 路径 | 构造方法级 InjectionPoint，验证调用了 ByteKit 注入器 | P0 | ✅ |
| 4.5 | DefaultClassTransformer 行号级注入走 ASM 路径 | 构造行号级 InjectionPoint，验证调用了 ASM 注入器 | P0 | ✅ |

### Phase 5: 旧代码清理 ✅

| # | 行为描述 | 验证方式 | 优先级 | 状态 |
|---|---------|---------|--------|------|
| 5.1 | 删除旧方法注入器后 mvn compile 通过 | CI 验证 | P0 | ✅ |
| 5.2 | 删除旧方法注入器后 mvn test 全部通过 | CI 验证 | P0 | ✅ |

### Phase 6: 回归测试 ✅

| # | 行为描述 | 验证方式 | 优先级 | 状态 |
|---|---------|---------|--------|------|
| 6.1 | 所有现有测试通过 | `mvn test`（302 tests, 0 failures, 1 skipped） | P0 | ✅ |
| 6.2 | ByteKit 注入后单次插桩判定耗时 < 0.1ms | PerformanceTest（实测 ~430ns） | P0 | ✅ |
| 6.3 | retransform 动态增删注入点正常 | IntegrationTest | P0 | ✅ |

---

## 三、测试基础设施

### 3.1 沿用现有工具

| 工具 | 用途 |
|------|------|
| `TestSetup.init()` | 全局注册表初始化 |
| `LineInjectionTestHelper.BytecodeClassLoader` | 加载注入后字节码 |
| `LineInjectionTestHelper.getClassBytecode()` | 读取类字节码 |
| `ProbeOutput.BUFFER` + `pollProbeMessages()` | 验证探针输出 |

### 3.2 新增测试工具

| 工具 | 用途 | 状态 |
|------|------|------|
| `ByteKitTestHelper` | ByteKit 注入测试辅助（读取字节码、执行注入、加载验证） | ✅ 已创建 |
| `ByteKitTestHelper.TestTargetService` | ByteKit 注入目标类（含多种方法场景） | ✅ 已创建 |
| `BytecodeClassLoader`（内嵌于各测试类） | 自定义 ClassLoader 加载增强后字节码 | ✅ 已创建 |

---

## 四、TDD 循环模板

每个任务遵循以下循环：

```
1. RED:    编写一个失败测试，描述期望行为
2. GREEN:  编写最小实现代码使测试通过
3. REFACTOR: 重构（提取公共代码、消除重复、深化模块）
4. COMMIT: git commit -m "feat: 行为描述"
```

---

## 五、测试命名约定

沿用项目现有风格：

```java
@Test
@DisplayName("ByteKit ENTER 注入 - 字节码合法")
void testByteKitEnterProducesValidBytecode() { ... }

@Nested
@DisplayName("ByteKit EXIT 注入")
class ByteKitExitTests {
    @Test
    @DisplayName("多 return 路径 - 所有退出点被拦截")
    void testMultipleReturnPaths() { ... }
}
```

---

## 六、性能测试策略

沿用项目 `PerformanceTest` 的微基准模式：

```java
@Test
@DisplayName("ByteKit 注入判定耗时 < 0.1ms")
void testByteKitInjectionPerformance() {
    // 100 次 warmup
    for (int i = 0; i < 100; i++) {
        byteKitInjector.inject(ctx, bytecode, assembler);
    }

    // 1000 次测量
    long totalNanos = 0;
    for (int i = 0; i < 1000; i++) {
        long start = System.nanoTime();
        byteKitInjector.inject(ctx, bytecode, assembler);
        totalNanos += System.nanoTime() - start;
    }

    long avgNanos = totalNanos / 1000;
    assertTrue(avgNanos < 100_000, "平均耗时 " + avgNanos + "ns 超过 0.1ms 红线");
}
```

**实测结果**：~430ns（0.00043ms），性能余量 232x。

---

## 七、待补充测试

| # | 测试类 | 行为描述 | 优先级 | 关联 |
|---|--------|---------|--------|------|
| T1 | `CoreLayerPurityTest` | 核心层无 ByteKit/ASM import | P1 | Task 1.4 |
| T2 | `ProductionSuppressTest` | 生产 Interceptor suppress 保护实际生效 | P0 | Task 2.8 / BUG-001 |
| T3 | `ByteKitLogExpressionTest` | log: 表达式通过 ByteKit 注入 | P2 | Task 3.1 |
| T4 | `ByteKitSnapshotExpressionTest` | snapshot: 表达式通过 ByteKit 注入 | P2 | Task 3.2 |
