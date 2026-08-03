# Luna - Java Agent 动态诊断平台

> 早期古法编程时代的 Luna 源码快照

## 这是什么

这是 Luna 项目在 **Vibe Coding（AI 辅助编程）** 模式介入之前的代码状态留存。

这个分支记录了纯手工编写 Java Agent 字节码注入框架的早期开发方式——没有 AI 辅助，没有自动生成，每一行代码都是人肉 debug 出来的。

## 古法编程特征

- 🔧 手写 ASM 字节码注入逻辑，逐条指令对照 JVM Spec
- 🐛 靠 `javap -c` 反编译验证字节码正确性
- 📝 在 IDE 里用 `System.out.println` 调试 ClassLoader 隔离问题
- 🔄 反复重启 JVM 验证 Agent 的 premain/agentmain 行为
- 📖 翻阅 JDK 源码理解 ClassFileTransformer 的调用时机
- ⏱️ 一个 Bootstrap ClassLoader 的 Bug 可能要花一整天排查

## 技术栈

- Java Agent (Instrumentation API)
- ASM 字节码操作
- Jetty 嵌入式 Web 服务
- Vue.js 前端


Copyright © 2025-2026 Tony.L

