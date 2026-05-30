# ByteKit 引入评估报告

> 评估日期：2026/05/30
> 评估目标：评估阿里 ByteKit 作为 Luna ASM 基础实现的可行性
> 核心原则：核心层不感知 ByteKit，也不感知 ASM；优先使用 ByteKit；ASM 只处理 ByteKit 无法覆盖的能力

---

## 一、ByteKit 能力全景

### 1.1 拦截点能力（10 种 Location）

| 注解 | LocationType | 说明 | Luna 对应能力 |
|------|-------------|------|--------------|
| `@AtEnter` | ENTER | 方法入口 | `EnterMethodInjector` |
| `@AtExit` | EXIT | 方法正常退出 | `ExitMethodInjector` |
| `@AtExceptionExit` | EXCEPTION_EXIT | 方法异常退出 | ❌ 无 |
| `@AtLine` | LINE | 指定行号 | `BeforeLineInjector` / `AfterLineInjector` |
| `@AtInvoke` | INVOKE / INVOKE_COMPLETED | 子函数调用前/后 | ❌ 无 |
| `@AtInvokeException` | INVOKE_EXCEPTION_EXIT | 子函数调用异常 | ❌ 无 |
| `@AtFieldAccess` | READ / WRITE / READ_COMPLETED / WRITE_COMPLETED | 字段访问 | ❌ 无 |
| `@AtSyncEnter` | SYNC_ENTER / SYNC_ENTER_COMPLETED | 进入同步块 | ❌ 无 |
| `@AtSyncExit` | SYNC_EXIT / SYNC_EXIT_COMPLETED | 退出同步块 | ❌ 无 |
| `@AtThrow` | THROW | 显式 throw | ❌ 无 |

### 1.2 数据绑定能力（20+ 种 Binding）

| 注解 | 类型 | 说明 | Luna 对应能力 |
|------|------|------|--------------|
| `@Binding.This` | Object | this 对象 | `$0` 参数引用 |
| `@Binding.Class` | Class | 当前类 | ❌ 无 |
| `@Binding.Method` | Method | 当前方法 | ❌ 无 |
| `@Binding.MethodName` | String | 方法名 | 通过 InjectionContext 获取 |
| `@Binding.MethodDesc` | String | 方法描述符 | 通过 InjectionContext 获取 |
| `@Binding.Args` | Object[] | 方法入参数组 | `$1, $2...` 参数引用 |
| `@Binding.ArgNames` | String[] | 方法入参名数组 | ❌ 无 |
| `@Binding.Return` | Object | 返回值 | ❌ 无（Exit 注入需要） |
| `@Binding.Throwable` | Throwable | 异常对象 | ❌ 无 |
| `@Binding.LocalVars` | Object[] | **局部变量值数组** | `LocalVariableScanner` + `SnapshotExpressionHandler` |
| `@Binding.LocalVarNames` | String[] | **局部变量名数组** | `LocalVariableScanner` |
| `@Binding.Line` | int | 行号 | 通过 InjectionContext 获取 |
| `@Binding.Field` | 取决于字段 | 字段值 | ❌ 无 |
| `@Binding.InvokeArgs` | Object[] | 子调用入参 | ❌ 无 |
| `@Binding.InvokeReturn` | Object | 子调用返回值 | ❌ 无 |
| `@Binding.InvokeMethodName` | String | 子调用方法名 | ❌ 无 |
| `@Binding.Monitor` | Object | 同步块监控对象 | ❌ 无 |

### 1.3 核心机制

| 机制 | 说明 | Luna 对应能力 |
|------|------|--------------|
| **inline 内联** | 将拦截方法内联到目标方法，消除方法调用开销 | ❌ 无（当前全部是 INVOKESTATIC） |
| **StackSaver** | 保存/恢复操作数栈，支持在非空栈位置插入代码 | ❌ 无（行号注入时栈状态不确定，存在风险） |
| **suppress + ExceptionHandler** | 自动 try/catch 包围插入代码，异常安全 | ❌ 无（当前插入代码无异常保护） |
| **LocationFilter** | 防止同一位置被重复增强 | ❌ 无 |
| **@NewField** | 给目标类动态添加字段 | ❌ 无 |
| **invokeOrigin** | 在增强代码中调用原始方法 | ❌ 无 |
| **ClassLoader 感知 define** | 自动将辅助类 define 到目标 ClassLoader | `LunaAgentClassLoader` 手动处理 |

---

## 二、Luna 功能 vs ByteKit 覆盖评估

### 2.1 逐功能覆盖评估

| # | Luna 功能 | 当前实现方式 | ByteKit 覆盖 | 覆盖度 | 说明 |
|---|----------|------------|-------------|--------|------|
| 1 | **方法入口注入** | `EnterMethodInjector` (Visitor API) | `@AtEnter` | ✅ **100%** | ByteKit 原生支持，且自带 inline + suppress |
| 2 | **方法退出注入** | `ExitMethodInjector` (Visitor API) | `@AtExit` | ✅ **100%** | ByteKit 原生支持，且支持 `@Binding.Return` |
| 3 | **方法环绕注入** | `AroundMethodInjector` (Visitor API) | `@AtEnter` + `@AtExit` 组合 | ✅ **100%** | 需在同一个 Interceptor 类中组合两个注解 |
| 4 | **行号前注入** | `BeforeLineInjector` (Tree API) | `@AtLine(whenComplete=false)` | ⚠️ **80%** | ByteKit 默认在 LineNumberNode 之前插入，等同于 line_before；但**不支持 excludeSameLineStart** |
| 5 | **行号后注入** | `AfterLineInjector` (Tree API) | `@AtLine(whenComplete=true)` | ⚠️ **70%** | ByteKit 的 `whenComplete=true` 在指令**之后**插入，但**不是该行最后一条指令之后**；Luna 的 AfterLine 需要定位到该行最后一条指令 |
| 6 | **局部变量扫描** | `LocalVariableScanner` (Tree API) | `AsmOpUtils.validVariables()` | ⚠️ **60%** | ByteKit 的 `validVariables` 只做简单的 `start <= insn < end` 判断，**不做行号级精确过滤**，不做 excludeSameLineStart |
| 7 | **日志探针生成** | `LogExpressionHandler` (Visitor API) | 自定义 Binding + Interceptor | ⚠️ **50%** | ByteKit 不支持表达式协议解析（`log:xxx`），需自定义 Binding 将探针调用内联 |
| 8 | **快照探针生成** | `SnapshotExpressionHandler` (Visitor API) | `@Binding.LocalVars` + `@Binding.LocalVarNames` | ✅ **90%** | ByteKit 原生支持局部变量快照，但**不支持 excludeSameLineVariables 过滤** |
| 9 | **耗时追踪探针** | `TraceExpressionHandler` (Visitor API) | `@AtEnter` + `@AtExit` 组合 | ✅ **85%** | 可通过 Enter/Exit 组合实现，但需自定义 Binding 传递 traceId |
| 10 | **条件判断** | `ConditionRegistry` + `EvaluationContext` | 自定义 Binding | ⚠️ **40%** | ByteKit 无内置条件引擎，需在 Interceptor 回调中手动实现条件判断 |
| 11 | **表达式引用解析** | `ReferenceExpressionParser` | ❌ 无对应 | ❌ **0%** | ByteKit 不支持 `$varName` 风格的表达式引用 |
| 12 | **表达式编译** | `ExpressionBytecodeGenerator` | ❌ 无对应 | ❌ **0%** | ByteKit 不支持运行时表达式编译 |
| 13 | **类结构分析** | `AsmClassAnalyzer` (Visitor API) | ByteKit 内部有类似能力 | ⚠️ **50%** | ByteKit 的 `AsmUtils` 有部分分析能力，但不暴露为公共 API |
| 14 | **ClassLoader 隔离** | `LunaAgentClassLoader` | ByteKit 有 `DefineConfig` | ⚠️ **60%** | ByteKit 的 define 机制可辅助，但不完全覆盖 Luna 的隔离需求 |
| 15 | **ClassFileTransformer** | `GlobalClassFileTransformer` | `InstrumentTransformer` | ⚠️ **50%** | ByteKit 的 Transformer 是一次性匹配，Luna 需要动态增删注入点 |

### 2.2 覆盖度汇总

```
┌─────────────────────────────────────────────────────────────┐
│              ByteKit 覆盖度雷达图                            │
│                                                             │
│              方法级注入  ████████████ 100%                   │
│              局部变量    ████████░░░░  70%                   │
│              行号注入    ████████░░░░  75%                   │
│              探针生成    ██████░░░░░░  55%                   │
│              条件引擎    ████░░░░░░░░  40%                   │
│              表达式      ██░░░░░░░░░░  20%                   │
│              动态管理    ████░░░░░░░░  40%                   │
│                                                             │
│              综合覆盖度：约 57%                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、ByteKit 关键差距分析

### 3.1 🔴 不可覆盖的能力（必须保留 ASM）

| 差距 | 影响范围 | 严重程度 | 说明 |
|------|---------|---------|------|
| **表达式协议解析** | `log:$name` / `snapshot:` / `trace:` | 🔴 高 | ByteKit 的 Interceptor 模型是**声明式**的（注解驱动），不支持运行时动态解析表达式协议 |
| **表达式编译引擎** | 条件表达式 `${count > 5}::log:xxx` | 🔴 高 | ByteKit 无运行时表达式编译能力，Luna 的 `ExpressionBytecodeGenerator` 需要保留 |
| **动态注入点管理** | 运行时增删注入点 | 🔴 高 | ByteKit 的 `InstrumentTransformer` 是一次性注册，不支持 Luna 的动态注入/卸载模型 |
| **AfterLine 精确语义** | 行后注入需定位该行最后一条指令 | 🟡 中 | ByteKit `@AtLine(whenComplete=true)` 在 LineNumberNode 之后插入，但该行可能有多条指令 |
| **excludeSameLineStart** | BeforeLine 排除同行未初始化变量 | 🟡 中 | ByteKit 的 `@Binding.LocalVars` 不支持排除同行声明变量 |

### 3.2 🟡 部分覆盖的能力（需扩展 ByteKit）

| 差距 | 影响范围 | 扩展方案 | 工作量 |
|------|---------|---------|--------|
| **局部变量精确过滤** | `LocalVariableScanner` | 自定义 `Binding` + `LocationMatcher` | 中 |
| **条件判断集成** | `ConditionRegistry` | 在 Interceptor 回调中调用 Luna 条件引擎 | 低 |
| **自定义探针调用** | `LogProbe.onLog()` / `SnapshotProbe.onSnapshot()` | 自定义 Binding 将探针调用内联 | 中 |
| **ClassLoader 隔离** | `LunaAgentClassLoader` | 保留 Luna 的 CL 隔离机制 | 低 |

### 3.3 🟢 ByteKit 带来的新能力（Luna 当前不具备）

| 新能力 | 说明 | 价值 |
|--------|------|------|
| **inline 内联** | 拦截代码内联到目标方法，零方法调用开销 | 🟢 高性能 |
| **suppress 异常保护** | 自动 try/catch 包围插入代码 | 🟢 安全性 |
| **StackSaver 栈保存** | 在非空栈位置安全插入代码 | 🟢 正确性 |
| **@AtExceptionExit** | 方法异常退出拦截 | 🟢 新能力 |
| **@AtInvoke** | 子函数调用拦截 | 🟢 新能力 |
| **@AtFieldAccess** | 字段访问拦截 | 🟢 新能力 |
| **@Binding.Return** | 获取方法返回值 | 🟢 新能力 |
| **@Binding.Throwable** | 获取异常对象 | 🟢 新能力 |
| **LocationFilter** | 防止重复增强 | 🟢 安全性 |

---

## 四、架构适配方案

### 4.1 分层架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     Luna 核心层                              │
│  (不感知 ByteKit，不感知 ASM)                                │
│                                                             │
│  InjectionPoint / InjectionType / InjectionContext          │
│  ExpressionHandler / BytecodeHelper / BytecodeAssembler     │
│  ConditionRegistry / EvaluationContext                      │
│  ProbeOutput / LogProbe / SnapshotProbe / TraceProbe        │
└──────────────────────┬──────────────────────────────────────┘
                       │ 适配接口
┌──────────────────────┴──────────────────────────────────────┐
│                   实现层 (SPI)                               │
│                                                             │
│  ┌─────────────────────┐  ┌──────────────────────────────┐ │
│  │  ByteKit 实现层      │  │  ASM 兜底实现层               │ │
│  │                     │  │                              │ │
│  │  方法级注入:         │  │  行号注入(精确):              │ │
│  │  @AtEnter           │  │  BeforeLine (精确行定位)      │ │
│  │  @AtExit            │  │  AfterLine (行末定位)         │ │
│  │  @AtExceptionExit   │  │                              │ │
│  │  @AtLine (简单场景)  │  │  局部变量扫描:               │ │
│  │                     │  │  LocalVariableScanner         │ │
│  │  局部变量快照:       │  │  (精确行号过滤)              │ │
│  │  @Binding.LocalVars │  │                              │ │
│  │  @Binding.LocalVar- │  │  表达式引擎:                  │ │
│  │    Names            │  │  ExpressionBytecodeGenerator │ │
│  │                     │  │  ReferenceExpressionParser   │ │
│  │  新能力:            │  │                              │ │
│  │  @AtInvoke          │  │  动态注入管理:                │ │
│  │  @AtFieldAccess     │  │  GlobalClassFileTransformer  │ │
│  │  @Binding.Return    │  │  (动态增删注入点)            │ │
│  │  @Binding.Throwable │  │                              │ │
│  └─────────────────────┘  └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 核心层接口抽象

核心层定义**实现无关**的接口，ByteKit 和 ASM 分别提供实现：

```java
// 核心层：注入器接口（不感知 ByteKit / ASM）
public interface CodeInjector {
    InjectionResult inject(InjectionPoint point, byte[] bytecode);
}

// 核心层：变量快照接口
public interface VariableSnapshotter {
    List<VariableInfo> snapshot(byte[] bytecode, String method, String desc, int line);
}

// 核心层：类分析接口
public interface ClassAnalyzer {
    ClassInfo analyze(byte[] bytecode);
    Map<String, List<Integer>> getLineNumbers(byte[] bytecode);
}
```

### 4.3 ByteKit 适配器示例

```java
// ByteKit 实现：方法入口注入
public class ByteKitEnterInjector implements CodeInjector {
    @Override
    public InjectionResult inject(InjectionPoint point, byte[] bytecode) {
        // 1. 构建 ByteKit Interceptor 类
        // 2. 使用 @AtEnter + @Binding.This + @Binding.Args
        // 3. 在回调中调用 Luna 的 Probe 类
        // 4. 利用 ByteKit 的 inline + suppress 机制
    }
}

// ASM 兜底实现：行号精确注入
public class AsmLineInjector implements CodeInjector {
    @Override
    public InjectionResult inject(InjectionPoint point, byte[] bytecode) {
        // 保留现有的 BeforeLineInjector / AfterLineInjector 逻辑
        // 使用 Tree API 精确定位行号
    }
}
```

---

## 五、迁移风险评估

### 5.1 技术风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| ByteKit ASM 版本与 Luna ASM 版本冲突 | 🟡 中 | 🔴 高 | ByteKit 使用 shade 重打包的 ASM（`com.alibaba.bytekit.asm`），与 Luna 的 `org.objectweb.asm` 隔离 |
| ByteKit inline 模式下局部变量索引冲突 | 🟡 中 | 🟡 中 | inline 时 ByteKit 自动重映射局部变量索引，风险可控 |
| ByteKit 不支持 retransform 动态增删 | 🔴 高 | 🔴 高 | 保留 Luna 的 `GlobalClassFileTransformer` 作为入口，ByteKit 仅负责字节码生成 |
| ByteKit `@AtLine` 精确度不足 | 🟡 中 | 🟡 中 | 简单行号场景用 ByteKit，精确场景保留 ASM |
| ByteKit 项目维护活跃度低 | 🔴 高 | 🟡 中 | 最后一次 release 是 0.1.6，需评估是否 fork 维护 |

### 5.2 工程风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 核心层接口抽象不足，导致 ByteKit 特性泄漏 | 🟡 中 | 🔴 高 | 严格 Code Review，确保核心层 import 不含 ByteKit/ASM 包 |
| 迁移过程中功能回归 | 🟡 中 | 🔴 高 | 先增量引入 ByteKit（新能力优先），再逐步替换现有 ASM 实现 |
| 两套实现并存增加维护成本 | 🟡 中 | 🟡 中 | 通过 SPI 机制统一调度，对外只暴露核心层接口 |

---

## 六、迁移路线建议

### Phase 0：准备（1-2 天）

- [ ] 在 `luna-core/pom.xml` 中引入 `bytekit-core` 依赖
- [ ] 验证 ByteKit shade ASM 与 Luna ASM 无冲突
- [ ] 创建 `luna-core/src/main/java/fun/efto/luna/core/bytekit/` 适配层包

### Phase 1：新能力引入（3-5 天）

优先引入 ByteKit **独有**的新能力，不影响现有功能：

- [ ] `@AtExceptionExit` → 方法异常退出注入
- [ ] `@AtInvoke` → 子函数调用拦截
- [ ] `@Binding.Return` → 返回值捕获
- [ ] `@Binding.Throwable` → 异常对象捕获
- [ ] inline + suppress → 性能优化 + 异常安全

### Phase 2：方法级注入迁移（2-3 天）

替换现有方法级注入器为 ByteKit 实现：

- [ ] `EnterMethodInjector` → `@AtEnter`
- [ ] `ExitMethodInjector` → `@AtExit`
- [ ] `AroundMethodInjector` → `@AtEnter` + `@AtExit` 组合
- [ ] 删除 `AbstractMethodInjector` / `EnterMethodVisitor` / `ExitMethodVisitor` / `AroundMethodVisitor` / `TryFinallyMethodVisitor`

### Phase 3：行号注入适配（3-5 天）

部分场景迁移到 ByteKit，精确场景保留 ASM：

- [ ] 简单行号注入（无局部变量需求）→ `@AtLine`
- [ ] 精确行号注入（需 excludeSameLineStart）→ 保留 ASM
- [ ] AfterLine 精确语义（行末定位）→ 保留 ASM
- [ ] 局部变量快照（简单场景）→ `@Binding.LocalVars` + `@Binding.LocalVarNames`
- [ ] 局部变量精确过滤 → 保留 `LocalVariableScanner`

### Phase 4：核心层抽象（2-3 天）

将 ByteKit 和 ASM 实现统一到核心层接口下：

- [ ] 定义 `CodeInjector` / `VariableSnapshotter` / `ClassAnalyzer` 核心接口
- [ ] ByteKit 实现和 ASM 实现分别适配核心接口
- [ ] 通过 SPI 机制自动选择实现
- [ ] 核心层代码清除所有 ByteKit / ASM import

---

## 七、结论

### 7.1 综合评估

| 维度 | 评分 | 说明 |
|------|------|------|
| **功能覆盖度** | ⭐⭐⭐ (57%) | 方法级注入 100%，行号注入 75%，表达式 20% |
| **新能力增益** | ⭐⭐⭐⭐⭐ | inline、suppress、StackSaver、@AtInvoke、@AtExceptionExit、@Binding.Return |
| **架构适配性** | ⭐⭐⭐⭐ | ByteKit 的 Interceptor 模型与 Luna 的注入模型天然对齐 |
| **迁移风险** | ⭐⭐⭐ | ASM 版本冲突可控，但 retransform 动态管理需保留 ASM |
| **维护成本** | ⭐⭐⭐ | 两套实现并存，但通过 SPI 统一调度 |

### 7.2 最终建议

**✅ 建议引入 ByteKit，但采用增量迁移策略：**

1. **优先引入新能力**（Phase 1）：inline + suppress + @AtExceptionExit + @AtInvoke + @Binding.Return，这些是 Luna 当前不具备的高价值能力
2. **逐步替换方法级注入**（Phase 2）：ByteKit 的 `@AtEnter` / `@AtExit` 比手写 Visitor API 更简洁、更安全
3. **行号注入保持双轨**（Phase 3）：简单场景用 ByteKit `@AtLine`，精确场景保留 ASM
4. **表达式引擎和动态管理保留 ASM**（长期）：ByteKit 的声明式模型无法覆盖 Luna 的运行时表达式解析和动态注入点管理

**核心原则落地方式**：
- 核心层定义 `CodeInjector` 等接口，**零 ByteKit / ASM import**
- ByteKit 和 ASM 分别作为实现层，通过 SPI 注册
- 优先使用 ByteKit（方法级注入、局部变量快照、新能力）
- ASM 只处理 ByteKit 无法覆盖的能力（精确行号注入、表达式引擎、动态注入管理）
