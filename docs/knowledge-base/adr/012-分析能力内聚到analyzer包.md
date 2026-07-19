# ADR-012: 分析能力内聚到analyzer包

## 状态

Accepted

## 背景

字节码分析能力（ClassReader 解析、方法定位、行号→偏移量映射、局部变量可见性判断）散落在多个 Injector 和业务类中，导致：

1. **ClassReader + cn.methods 遍历在 5 个文件中重复**
2. **行号解析逻辑在 BeforeLineInjector/AfterLineInjector 中重复**
3. **removeFrameNodes 等工具方法在 AsmMethodExpressionInjector 和 LineInjectorHelper 中重复**
4. **Injector 承担了"怎么找注入点"的分析职责，违反单一职责**

Injector 应该只负责"怎么注入"（字节码生成），而非"怎么找注入点"（字节码分析）。分析职责与注入职责的耦合导致代码重复和职责混乱。

## 方案选项

### 选项 A: 保持现状，分析逻辑分散在 Injector 中

维持现有的分析逻辑分散在各 Injector 和业务类中的结构。

优点：
- 无需重构，零迁移成本
- 每个 Injector 自包含，不依赖外部分析类

缺点：
- 分析逻辑在 5 个文件中重复
- Injector 承担分析职责，违反单一职责
- 分析算法改进需修改多个 Injector
- 重复代码增加维护成本

### 选项 B: 将所有字节码分析能力内聚到 analyzer 包

将所有字节码分析能力内聚到 `bytecode/asm/analyzer/` 包：

- **BytecodeAnalyzer**：统一字节码解析和方法定位，替代各 Injector 的 ClassReader + cn.methods 遍历
- **LineOffsetResolver**：统一行号→注入点位置解析，BeforeLineInjector/AfterLineInjector 共享
- **LocalVariableScanner**：局部变量可见性扫描，策略与机制分离
- **LineInjectorHelper**：行级注入器公共工具方法（computeMaxLocalIndex、removeFrameNodes、isReturnOrThrow）

优点：
- 分析能力内聚，Injector 只负责"怎么注入"
- 消除重复代码（ClassReader 遍历、行号解析、工具方法）
- 分析算法改进只需修改一处
- 符合单一职责原则

缺点：
- 新增 BytecodeAnalyzer 和 LineInjectorHelper 两个类
- Injector 需依赖 analyzer 包的类
- ByteKitInjectorBase 使用 ByteKit 影子 ASM 包，无法使用 BytecodeAnalyzer（合理边界）

## 决策

选择**选项 B：将所有字节码分析能力内聚到 analyzer 包**。

分析逻辑在 5 个文件中重复是真实的代码腐化，违反三次法则（已超过三次重复）。内聚到 analyzer 包后，Injector 只负责"怎么注入"，分析能力集中维护，消除重复代码。

**边界说明**：ByteKitInjectorBase 使用 ByteKit 影子 ASM 包（`com.alibaba.deps.org.objectweb.asm`），与标准 ASM（`org.objectweb.asm`）不兼容，无法使用 BytecodeAnalyzer。这是合理的边界——ByteKit 管线要求自己的 ClassNode/MethodNode 类型。

## 影响

- 新增 `bytecode/asm/analyzer/` 包，包含 BytecodeAnalyzer、LineOffsetResolver、LocalVariableScanner、LineInjectorHelper
- 各 Injector 的 ClassReader + cn.methods 遍历替换为 BytecodeAnalyzer 调用
- BeforeLineInjector/AfterLineInjector 的行号解析逻辑替换为 LineOffsetResolver 调用
- 重复的工具方法（removeFrameNodes 等）收敛到 LineInjectorHelper
- ByteKitInjectorBase 不使用 BytecodeAnalyzer（使用 ByteKit 影子 ASM 包，类型不兼容）
