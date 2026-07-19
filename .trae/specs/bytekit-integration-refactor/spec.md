# ByteKit 集成重构 Spec

## Why

Luna 当前的字节码注入层直接使用 ASM Core API（Visitor 模式）和 Tree API，存在以下问题：
1. **方法级注入器手写 Visitor 代码冗长**，`EnterMethodVisitor`/`ExitMethodVisitor`/`AroundMethodVisitor` 重复样板代码多
2. **缺少异常保护**，插入代码无 try/catch 包围，拦截逻辑异常可能影响业务
3. **缺少栈保存机制**，在非空栈位置（如 Exit/Throw）插入代码存在正确性风险
4. **缺少方法内联优化**，所有回调均为 INVOKESTATIC，有方法调用开销
5. **缺少高价值能力**：异常退出拦截（@AtExceptionExit）、子函数调用拦截（@AtInvoke）、返回值捕获（@Binding.Return）、异常对象捕获（@Binding.Throwable）

阿里 ByteKit 提供了声明式注解 API、inline 内联、suppress 异常保护、StackSaver 栈保存等能力，可显著提升 Luna 注入层的安全性、性能和可维护性。

**核心原则**：核心层不感知 ByteKit，也不感知 ASM；优先使用 ByteKit；ASM 只处理 ByteKit 无法覆盖的能力。

## What Changes

- **引入 bytekit-core 依赖**：通过 Maven shade 重打包的 ASM（`com.alibaba.deps.org.objectweb.asm`）与 Luna 的 `org.objectweb.asm` 隔离
- **新增 ByteKit 适配层**：`luna-core/src/main/java/fun/efto/luna/core/bytekit/` 包，将 ByteKit 的 Interceptor 模式适配到 Luna 的 `BytecodeInjector` 接口
- **新增核心层抽象接口**：`CodeInjector` / `VariableSnapshotter` / `ClassAnalyzer`，核心层零 ByteKit / ASM import
- **方法级注入迁移到 ByteKit**：`EnterMethodInjector` → `ByteKitEnterInjector`，`ExitMethodInjector` → `ByteKitExitInjector`，`AroundMethodInjector` → `ByteKitAroundInjector`
- **新增 ByteKit 独有能力**：异常退出注入、子函数调用拦截、返回值捕获、异常对象捕获
- **行号注入保持双轨**：简单场景用 ByteKit `@AtLine`，精确场景（excludeSameLineStart、AfterLine 行末定位）保留 ASM
- **表达式引擎和动态管理保留 ASM**：ByteKit 声明式模型无法覆盖运行时表达式解析和动态注入点管理
- **删除旧方法注入器**：`AbstractMethodInjector` / `EnterMethodVisitor` / `ExitMethodVisitor` / `AroundMethodVisitor` / `TryFinallyMethodVisitor`

## Impact

- Affected specs: method-target, line-target, core-module-initializer
- Affected code:
  - `luna-core/pom.xml` — 新增 bytekit-core 依赖
  - `fun.efto.luna.core.asm.injector.*` — 注入器接口和注册表
  - `fun.efto.luna.core.plugin.builtin.method.*` — 方法注入器族
  - `fun.efto.luna.core.plugin.builtin.line.*` — 行号注入器（保持不变）
  - `fun.efto.luna.core.transformer.*` — 转换器层（适配双引擎）
  - `fun.efto.luna.core.plugin.builtin.CoreModuleInitializer` — 核心模块初始化
  - `fun.efto.luna.core.asm.AsmInjectionContext` — 注入上下文
  - `fun.efto.luna.core.bytecode.BytecodeAssembler` — 组装器接口

## ADDED Requirements

### Requirement: ByteKit 依赖引入与 ASM 隔离验证

系统 SHALL 在 `luna-core/pom.xml` 中引入 `bytekit-core` 依赖，且 ByteKit 的 shade ASM（`com.alibaba.deps.org.objectweb.asm`）与 Luna 的 `org.objectweb.asm` 无类冲突。

#### Scenario: ASM 版本隔离验证
- **WHEN** Luna 运行时同时加载 `org.objectweb.asm.ClassReader` 和 `com.alibaba.deps.org.objectweb.asm.ClassReader`
- **THEN** 两个类由不同的 ClassLoader 加载，无 LinkageError 或 ClassNotFoundException
- **AND** ByteKit 的 `InterceptorProcessor` 可正常处理字节码

### Requirement: 核心层抽象接口

系统 SHALL 定义实现无关的核心层接口，核心层代码零 ByteKit / ASM import：

```java
public interface CodeInjector {
    InjectionResult inject(InjectionPoint point, byte[] bytecode);
}

public interface VariableSnapshotter {
    List<VariableInfo> snapshot(byte[] bytecode, String method, String desc, int line);
}

public interface ClassAnalyzer {
    ClassInfo analyze(byte[] bytecode);
}
```

#### Scenario: 核心层无实现泄漏
- **WHEN** 检查 `fun.efto.luna.core.injection.*` 和 `fun.efto.luna.core.bytecode.*` 包下的所有 Java 文件
- **THEN** 无任何 `import com.alibaba.bytekit.*` 或 `import org.objectweb.asm.*` 语句

### Requirement: ByteKit 方法入口注入器

系统 SHALL 提供 `ByteKitEnterInjector` 实现 `BytecodeInjector` 接口，使用 ByteKit 的 `@AtEnter` 注解驱动方法入口注入，替代原有的 `EnterMethodInjector`。

#### Scenario: 方法入口注入
- **WHEN** 对目标类的指定方法执行 ENTER 类型注入
- **THEN** ByteKit 在方法入口处插入回调代码
- **AND** 插入代码被 inline 内联到目标方法中（零方法调用开销）
- **AND** 插入代码被 suppress try/catch 包围（异常安全）

#### Scenario: 数据绑定
- **WHEN** 注入表达式需要访问 `this` 或方法参数
- **THEN** ByteKit 通过 `@Binding.This` 和 `@Binding.Args` 自动生成参数压栈指令
- **AND** 原始类型自动装箱为 Object

### Requirement: ByteKit 方法退出注入器

系统 SHALL 提供 `ByteKitExitInjector` 实现 `BytecodeInjector` 接口，使用 ByteKit 的 `@AtExit` 注解驱动方法退出注入，替代原有的 `ExitMethodInjector`。

#### Scenario: 方法退出注入
- **WHEN** 对目标类的指定方法执行 EXIT 类型注入
- **THEN** ByteKit 在所有 RETURN 指令前插入回调代码
- **AND** 通过 StackSaver 保存/恢复操作数栈上的返回值
- **AND** 通过 `@Binding.Return` 可获取方法返回值

### Requirement: ByteKit 方法环绕注入器

系统 SHALL 提供 `ByteKitAroundInjector` 实现 `BytecodeInjector` 接口，使用 `@AtEnter` + `@AtExit` 组合注解驱动方法环绕注入，替代原有的 `AroundMethodInjector`。

#### Scenario: 方法环绕注入
- **WHEN** 对目标类的指定方法执行 AROUND 类型注入
- **THEN** ByteKit 在方法入口和所有退出点均插入回调代码
- **AND** 入口和退出共享同一个 Interceptor 类的上下文

### Requirement: 异常退出注入能力

系统 SHALL 新增 `EXCEPTION_EXIT` 注入类型，使用 ByteKit 的 `@AtExceptionExit` 注解驱动方法异常退出注入。

#### Scenario: 异常退出注入
- **WHEN** 对目标类的指定方法执行 EXCEPTION_EXIT 类型注入
- **THEN** ByteKit 在方法抛出异常时插入回调代码
- **AND** 通过 `@Binding.Throwable` 可获取异常对象

### Requirement: 子函数调用拦截能力

系统 SHALL 新增 `INVOKE` 注入类型，使用 ByteKit 的 `@AtInvoke` 注解驱动子函数调用拦截。

#### Scenario: 子函数调用前拦截
- **WHEN** 对目标方法中指定的子函数调用执行 INVOKE 类型注入（whenComplete=false）
- **THEN** ByteKit 在子函数调用指令前插入回调代码
- **AND** 通过 `@Binding.InvokeArgs` 可获取子函数调用参数

#### Scenario: 子函数调用后拦截
- **WHEN** 对目标方法中指定的子函数调用执行 INVOKE 类型注入（whenComplete=true）
- **THEN** ByteKit 在子函数调用指令后插入回调代码
- **AND** 通过 `@Binding.InvokeReturn` 可获取子函数返回值

### Requirement: ByteKit 适配器与 Luna 探针桥接

系统 SHALL 提供 ByteKit Interceptor 回调与 Luna 探针系统（`LunaSpy`）的桥接机制，使 ByteKit 生成的内联代码能正确调用 `LunaSpy.onLog()` / `LunaSpy.onSnapshot()` / `LunaSpy.onTraceStart()` 等方法。

#### Scenario: ByteKit 内联代码调用 LunaSpy
- **WHEN** ByteKit inline 模式将 Interceptor 方法内联到目标方法
- **THEN** 内联后的代码中 `LunaSpy.onLog()` 调用仍可正确解析
- **AND** `LunaSpy` 类被 Agent ClassLoader 正确加载

### Requirement: 行号注入双轨策略

系统 SHALL 对行号注入采用双轨策略：
- 简单行号注入（无局部变量需求）→ ByteKit `@AtLine`
- 精确行号注入（需 excludeSameLineStart）→ 保留 ASM `BeforeLineInjector`
- AfterLine 精确语义（行末定位）→ 保留 ASM `AfterLineInjector`

#### Scenario: 简单行号注入走 ByteKit
- **WHEN** 行号注入不涉及局部变量快照
- **THEN** 使用 ByteKit `@AtLine` 执行注入

#### Scenario: 精确行号注入走 ASM
- **WHEN** 行号注入需要 excludeSameLineStart 或行末定位
- **THEN** 保留使用 ASM Tree API 的 `BeforeLineInjector` / `AfterLineInjector`

### Requirement: 表达式引擎保留 ASM

系统 SHALL 保留 Luna 的表达式引擎（`ExpressionBytecodeGenerator`、`ReferenceExpressionParser`、`ConditionRegistry`）使用 ASM 实现，ByteKit 的声明式模型不覆盖运行时表达式解析。

#### Scenario: 表达式协议解析
- **WHEN** 注入内容为 `log:$name` 或 `snapshot:` 或 `trace:` 格式
- **THEN** 仍由 Luna 的 `ExpressionHandler` 体系解析和生成字节码
- **AND** ByteKit 仅负责定位注入点和提供数据绑定

### Requirement: 动态注入点管理保留原有机制

系统 SHALL 保留 `GlobalClassFileTransformer` 作为 JVM `ClassFileTransformer` 入口，ByteKit 仅负责字节码生成，不替代 Luna 的动态注入/卸载模型。

#### Scenario: 动态增删注入点
- **WHEN** 运行时新增或删除注入点
- **THEN** `GlobalClassFileTransformer` 仍通过 `InjectionQuery` 查询激活注入点
- **AND** ByteKit 作为字节码生成引擎被 `DefaultClassTransformer` 调用

## MODIFIED Requirements

### Requirement: BytecodeInjector 接口扩展

`BytecodeInjector` 接口 SHALL 保持 `inject(InjectionContext, byte[], BytecodeAssembler)` 方法签名不变，ByteKit 适配器在内部将调用委托给 ByteKit 的 `InterceptorProcessor`。

### Requirement: DefaultClassTransformer 双引擎调度

`DefaultClassTransformer` SHALL 支持根据 `InjectionType` 自动选择 ByteKit 或 ASM 引擎：
- 方法级注入类型（ENTER/EXIT/AROUND/EXCEPTION_EXIT/INVOKE）→ ByteKit 引擎
- 行号注入类型（BEFORE/AFTER）→ ASM 引擎

### Requirement: CoreModuleInitializer 注册扩展

`CoreModuleInitializer` SHALL 注册新增的注入类型和注入器：
- `EXCEPTION_EXIT` 注入类型 + `ByteKitExceptionExitInjector`
- `INVOKE` 注入类型 + `ByteKitInvokeInjector`
- 方法级注入器替换为 ByteKit 实现

## REMOVED Requirements

### Requirement: 旧方法注入器族
**Reason**: 被 ByteKit 适配器替代，ByteKit 提供 inline + suppress + StackSaver 等安全机制
**Migration**: 以下类将被删除：
- `AbstractMethodInjector`
- `EnterMethodInjector` → 替换为 `ByteKitEnterInjector`
- `ExitMethodInjector` → 替换为 `ByteKitExitInjector`
- `AroundMethodInjector` → 替换为 `ByteKitAroundInjector`
- `EnterMethodVisitor` / `ExitMethodVisitor` / `AroundMethodVisitor` / `TryFinallyMethodVisitor`

---

## 架构设计

### 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Luna 核心层                              │
│  (不感知 ByteKit，不感知 ASM)                                │
│                                                             │
│  InjectionPoint / InjectionType / InjectionContext          │
│  ExpressionHandler / BytecodeHelper / BytecodeAssembler     │
│  ConditionRegistry / EvaluationContext                      │
│  ProbeOutput / LogProbe / SnapshotProbe / TraceProbe        │
│  CodeInjector / VariableSnapshotter / ClassAnalyzer (新增)   │
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
│  │  @AtInvoke          │  │  局部变量扫描:               │ │
│  │  @AtLine (简单场景)  │  │  LocalVariableScanner         │ │
│  │                     │  │  (精确行号过滤)              │ │
│  │  局部变量快照:       │  │                              │ │
│  │  @Binding.LocalVars │  │  表达式引擎:                  │ │
│  │  @Binding.LocalVar- │  │  ExpressionBytecodeGenerator │ │
│  │    Names            │  │  ReferenceExpressionParser   │ │
│  │                     │  │                              │ │
│  │  新能力:            │  │  动态注入管理:                │ │
│  │  @Binding.Return    │  │  GlobalClassFileTransformer  │ │
│  │  @Binding.Throwable │  │  (动态增删注入点)            │ │
│  │  inline + suppress  │  │                              │ │
│  │  StackSaver         │  │                              │ │
│  └─────────────────────┘  └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### ByteKit 适配器核心流程

```
InjectionPoint (ENTER/EXIT/AROUND/EXCEPTION_EXIT/INVOKE)
    │
    ▼
ByteKitXxxInjector (implements BytecodeInjector)
    │
    ├─ 1. 从 InjectionPoint 提取目标类/方法信息
    ├─ 2. 构建 ByteKit Interceptor 类（动态生成或模板类）
    │     └─ 使用 @AtEnter/@AtExit/@AtExceptionExit/@AtInvoke 注解
    │     └─ 使用 @Binding.This/@Binding.Args/@Binding.Return 等绑定
    ├─ 3. 解析 Interceptor 类 → InterceptorProcessor 列表
    │     └─ DefaultInterceptorClassParser.parse()
    ├─ 4. 读取目标类字节码 → ClassNode + MethodNode
    ├─ 5. 对目标方法执行 InterceptorProcessor.process()
    │     └─ LocationMatcher.match() → List<Location>
    │     └─ Binding.pushOntoStack() → 参数压栈
    │     └─ INVOKESTATIC → 回调调用
    │     └─ inline 内联（可选）
    │     └─ suppress try/catch（可选）
    ├─ 6. 在 Interceptor 回调中桥接 Luna 探针系统
    │     └─ 调用 LunaSpy.onLog() / onSnapshot() / onTraceStart()
    └─ 7. 输出转换后字节码
```

### 关键设计决策

1. **ByteKit 仅作为字节码生成引擎**：不替代 `GlobalClassFileTransformer` 的动态管理职责
2. **Interceptor 回调桥接 LunaSpy**：ByteKit inline 后的代码直接调用 `LunaSpy` 静态方法，无需运行时依赖 Interceptor 类
3. **表达式引擎保持独立**：`ExpressionHandler` 体系继续使用 ASM `MethodVisitor` 生成字节码，ByteKit 仅负责定位注入点
4. **行号注入精确场景保留 ASM**：ByteKit `@AtLine` 不支持 `excludeSameLineStart` 和行末定位
5. **ASM 版本隔离**：ByteKit 使用 `com.alibaba.deps.org.objectweb.asm`，Luna 使用 `org.objectweb.asm`，无冲突
