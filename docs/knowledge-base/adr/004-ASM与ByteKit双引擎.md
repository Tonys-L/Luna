# ADR-004: ASM与ByteKit双引擎

## 状态

Accepted

## 背景

Luna 的字节码注入需要支持两种粒度：方法级注入（方法入口、方法退出）和行号级注入（指定行号前/后插入代码）。

ByteKit（阿里开源字节码增强工具）在方法级注入场景下效率更高：声明式注解模型（@AtEnter/@AtExit）、自动 inline、异常抑制（suppress）、StackSaver 等特性大幅简化了注入代码的编写。但在行号级注入场景下，ByteKit 的 @AtLine 覆盖不足，无法满足精确行号定位的需求。

ASM Tree API 在行号级注入场景下更灵活，可以精确定位 LineNumberNode 并在指定位置插入代码，但方法级注入需要手写 Visitor API 代码，开发效率较低。

## 方案选项

### 选项 A: 仅使用 ASM

所有注入场景统一使用 ASM Tree API / Visitor API 实现。

优点：
- 单一实现，维护成本低
- 完全控制字节码生成的每个细节
- 无外部依赖风险

缺点：
- 方法级注入需要手写 Visitor API 代码，开发效率低
- 缺少 ByteKit 的 inline、suppress、StackSaver 等高级特性
- 需要自行处理异常抑制、局部变量索引重映射等复杂逻辑

### 选项 B: 仅使用 ByteKit

所有注入场景统一使用 ByteKit 实现。

优点：
- 声明式注解模型，开发效率高
- 内置 inline、suppress、StackSaver 等特性
- 代码更简洁、更安全

缺点：
- @AtLine 精确度不足，无法满足行号级注入的精确需求
- ByteKit 项目维护活跃度低（最后 release 为 0.1.6）
- ByteKit 不支持 retransform 动态增删
- ByteKit 的声明式模型无法覆盖运行时表达式解析和动态注入点管理

### 选项 C: ASM + ByteKit 双引擎

方法级注入使用 ByteKit，行号级注入使用 ASM，通过 BytecodeInjector 接口统一。

优点：
- 各取所长：方法级注入用 ByteKit 效率高，行号级注入用 ASM 精确
- 核心层通过接口抽象，不感知具体引擎
- ByteKit 的新能力（@AtExceptionExit、@AtInvoke、@Binding.Return 等）可直接使用

缺点：
- 两套实现维护成本
- ByteKit 使用影子 ASM 包（`com.alibaba.deps.org.objectweb.asm`），与 Luna 的 Shade ASM（`fun.efto.luna.shade.asm`）和标准 ASM（`org.objectweb.asm`）形成三套命名空间
- 需要确保核心层接口抽象不泄漏 ByteKit 特性

## 决策

选择**选项 C：ASM + ByteKit 双引擎**。

方法级注入和行号级注入是两个不同的变化方向，各有最优解。通过 BytecodeInjector 接口统一，核心层不感知具体引擎，符合"核心层不依赖技术实现"的原则。两套实现的维护成本是获得最优注入效果的合理代价。

**边界说明**：ByteKitInjectorBase 使用 ByteKit 影子 ASM 包（`com.alibaba.deps.org.objectweb.asm`），与标准 ASM（`org.objectweb.asm`）不兼容，无法使用 BytecodeAnalyzer。这是合理的边界——ByteKit 管线要求自己的 ClassNode/MethodNode 类型。

## 影响

- 核心层定义 BytecodeInjector 接口，ByteKit 和 ASM 分别提供实现
- 方法级注入（Enter/Exit）优先使用 ByteKit
- 行号级注入（BeforeLine/AfterLine）使用 ASM
- 表达式引擎和动态注入管理保留 ASM 实现
- 核心层代码禁止出现 ByteKit/ASM 的 import

---

## ByteKit 覆盖度评估详情

> 评估日期：2026/05/30

### 覆盖度雷达

| 维度 | 覆盖度 |
|------|--------|
| 方法级注入 | 100% |
| 局部变量 | 70% |
| 行号注入 | 75% |
| 探针生成 | 55% |
| 条件引擎 | 40% |
| 表达式 | 20% |
| 动态管理 | 40% |
| **综合覆盖度** | **约 57%** |

### 不可覆盖能力（必须保留 ASM）

| 差距 | 严重程度 | 说明 |
|------|---------|------|
| 表达式协议解析 | 🔴 高 | ByteKit 声明式模型不支持运行时动态解析表达式协议 |
| 表达式编译引擎 | 🔴 高 | ByteKit 无运行时表达式编译能力 |
| 动态注入点管理 | 🔴 高 | ByteKit InstrumentTransformer 一次性注册，不支持动态增删 |
| AfterLine 精确语义 | 🟡 中 | @AtLine(whenComplete=true) 不等于该行最后一条指令之后 |
| excludeSameLineStart | 🟡 中 | @Binding.LocalVars 不支持排除同行声明变量 |

### ByteKit 带来的新能力

| 新能力 | 价值 |
|--------|------|
| inline 内联 | 🟢 高性能，零方法调用开销 |
| suppress 异常保护 | 🟢 安全性，自动 try/catch |
| StackSaver 栈保存 | 🟢 正确性，非空栈位置安全插入 |
| @AtExceptionExit | 🟢 新能力，方法异常退出拦截 |
| @AtInvoke | 🟢 新能力，子函数调用拦截 |
| @Binding.Return | 🟢 新能力，获取方法返回值 |
| LocationFilter | 🟢 安全性，防止重复增强 |

### 迁移路线

| Phase | 内容 | 周期 |
|-------|------|------|
| Phase 0 | 引入 bytekit-core 依赖，验证 ASM 版本无冲突 | 1-2 天 |
| Phase 1 | 优先引入新能力（@AtExceptionExit、@AtInvoke、@Binding.Return、inline+suppress） | 3-5 天 |
| Phase 2 | 方法级注入迁移（Enter/Exit/Around → @AtEnter/@AtExit） | 2-3 天 |
| Phase 3 | 行号注入适配（简单场景用 @AtLine，精确场景保留 ASM） | 3-5 天 |
| Phase 4 | 核心层抽象（CodeInjector/VariableSnapshotter/ClassAnalyzer 接口 + SPI） | 2-3 天 |
