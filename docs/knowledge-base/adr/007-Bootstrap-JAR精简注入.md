# ADR-007: Bootstrap-JAR精简注入

## 状态

Accepted

## 背景

Luna 的探针代码（Probe 类）需要在业务方法的字节码中被引用，因此必须通过 `inst.appendToBootstrapClassLoaderSearch()` 注入到 Bootstrap ClassLoader 中，确保所有 ClassLoader 都能加载到 Probe 类。

然而，将整个 Agent JAR 注入 Bootstrap CL 会导致严重问题：Agent JAR 中包含 Log4j2 Shade 类（`fun.efto.luna.shade.log4j2`），这些类被 Bootstrap CL 加载后，与 LunaAgentClassLoader 加载的 Log4j2 类产生 ClassCastException——两个 ClassLoader 加载的同名类在 JVM 中是不同的类型。

## 方案选项

### 选项 A: 注入完整 Agent JAR 到 Bootstrap CL

将整个 Agent JAR 通过 `inst.appendToBootstrapClassLoaderSearch()` 加入 Bootstrap ClassLoader。

优点：
- 实现简单，无需额外构建步骤
- 所有 Agent 类都可通过 Bootstrap CL 加载

缺点：
- Log4j2 Shade 类被 Bootstrap CL 加载，与 LunaAgentClassLoader 加载的 Log4j2 类产生 ClassCastException
- Bootstrap CL 加载的类无法卸载，浪费元空间
- 违反最小化原则，注入了不需要的类

### 选项 B: BootstrapJarBuilder 仅提取精简类构建精简 JAR

通过 BootstrapJarBuilder 仅提取 probe/infra/expression 相关类构建精简 JAR，注入到 Bootstrap CL。

优点：
- 彻底解决 Log4j2 类加载冲突
- 最小化 Bootstrap CL 注入范围，仅包含必要的类
- 符合最小化原则

缺点：
- 构建流程增加一步（BootstrapJarBuilder）
- 需要维护精简 JAR 的类清单
- 开发时需注意哪些类可以放入 Bootstrap CL

## 决策

选择**选项 B：BootstrapJarBuilder 仅提取精简类构建精简 JAR**。

Log4j2 ClassCastException 是真实的运行时故障，不是假设性风险。注入完整 JAR 到 Bootstrap CL 违反最小化原则，且带来的类加载冲突无法绕过。精简 JAR 仅包含探针运行必需的类（probe/infra/expression），构建步骤的增加是合理的工程代价。

## 影响

- Agent 启动流程中增加 BootstrapJarBuilder.build() 步骤
- 仅 probe/infra/expression 相关类被注入 Bootstrap CL
- Agent JAR 中不包含 Log4j2 Shade 类的精简版本
- 开发时需明确哪些类需要放入 Bootstrap CL，新增 Probe 类时需更新清单
