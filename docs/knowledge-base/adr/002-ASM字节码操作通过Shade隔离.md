# ADR-002: ASM字节码操作通过Shade隔离

## 状态

Accepted

## 背景

Luna 作为 Java Agent 运行在目标 JVM 中，ASM 是核心字节码操作依赖。目标应用可能自身依赖不同版本的 ASM（如 Spring Boot 内置 ASM、Arthas 使用的 ASM 等），如果 Luna 直接使用 `org.objectweb.asm` 包名，当目标 JVM 中已存在同包名不同版本的 ASM 类时，会导致类加载冲突（ClassNotFoundException、NoSuchMethodError、ClassCastException 等）。

这是 Agent 类隔离的核心问题之一——Agent 的依赖不能与宿主应用的依赖产生冲突。

## 方案选项

### 选项 A: 直接使用 ASM 原始包名

在 luna-core 中直接依赖 `org.objectweb.asm` 包，不做任何重命名处理。

优点：
- 开发时代码直观，import 路径与 ASM 官方文档一致
- 无需额外构建步骤
- 调试时堆栈信息清晰

缺点：
- 与宿主应用的 ASM 产生类加载冲突，导致 Agent 运行失败
- 不同版本的 ASM API 不兼容，无法保证在所有目标 JVM 中正常工作
- 违反 luna-core 的"0 第三方依赖"原则

### 选项 B: 通过 Maven Shade Plugin 重命名 ASM 包

使用 Maven Shade Plugin 将 `org.objectweb.asm` 重命名为 `fun.efto.luna.shade.asm`，实现类名级别的完全隔离。

优点：
- 全限定名不同，即使宿主应用依赖不同版本 ASM 也不冲突
- 保持 luna-core 的 0 第三方依赖原则（Shade 后的 ASM 视为自身代码）
- 与 LunaAgentClassLoader + Bootstrap JAR 形成三重隔离体系

缺点：
- 代码中必须使用 Shade 后的包名 `fun.efto.luna.shade.asm`，与 ASM 官方文档不一致
- 构建流程增加 Shade 步骤
- 调试时堆栈信息中显示 Shade 后的包名，需要心智映射

## 决策

选择**选项 B：通过 Maven Shade Plugin 重命名 ASM 包**。

类隔离是 Java Agent 的生存基础，ASM 版本冲突是真实且高频发生的问题。Shade 重命名是零运行时开销的隔离方案，构建时的额外步骤是合理的工程代价。同时，Log4j2 也采用相同的 Shade 隔离策略（`org.apache.logging.log4j` → `fun.efto.luna.shade.log4j2`），保持隔离策略的一致性。

## 影响

- luna-core 代码中所有 ASM 引用必须使用 `fun.efto.luna.shade.asm` 包名
- Maven 构建配置中需维护 Shade 规则
- 与 ByteKit 的影子 ASM 包（`com.alibaba.deps.org.objectweb.asm`）形成三套 ASM 命名空间，互不干扰
- 开发规范中明确要求：ASM 类引用必须使用 Shade 后的包名
