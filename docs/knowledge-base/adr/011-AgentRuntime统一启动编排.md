# ADR-011: AgentRuntime统一启动编排

## 状态

Accepted

## 背景

Agent.startAgent() 直接硬编码约 280 行组件组装逻辑，存在多个严重问题：

1. **PluginManagerImpl 以不完整参数创建**：retransformer/analyzer/decompiler/webServer 均为 null，导致插件管理功能残缺
2. **ReadyGate.markReady() 从未调用**：就绪门控形同虚设，无法保证启动顺序一致性
3. **插件 Controller 无法注册到 WebServer**：WebServer 初始化时插件尚未注册，导致插件管理端点不可用
4. **生产启动路径和测试启动路径不一致**：测试使用 InjectionTestHarnessAdapter，生产使用 Agent.startAgent()，两者组装逻辑不同，行为不可预测

## 方案选项

### 选项 A: 保持现有硬编码启动逻辑

维持 Agent.startAgent() 中的 280 行硬编码组装逻辑，仅修复具体 Bug。

优点：
- 修改范围小，仅修复已知 Bug
- 不引入新的间接层

缺点：
- 280 行硬编码组装逻辑仍然难以维护
- 生产与测试启动路径不一致的问题无法根本解决
- 组件组装顺序依赖开发者记忆，容易遗漏步骤
- ReadyGate 语义不完整，无法保证启动顺序

### 选项 B: 新增 AgentRuntime 封装完整启动顺序

新增 AgentRuntime 封装完整的 10 步启动流程，Agent 变为薄 Adapter（仅 ClassLoader 切换 + 反射调用）。同时新增 AgentRuntimeContext 保存运行期对象引用，新增 BuiltinPluginProvider 统一内置插件发现。

优点：
- 启动路径单一化，生产与测试一致
- 插件管理端点可用（WebServer 初始化前插件已注册）
- ReadyGate 语义完整（启动完成后调用 markReady）
- Agent 变为薄 Adapter，职责清晰

缺点：
- 新增 3 个类（AgentRuntime、AgentRuntimeContext、BuiltinPluginProvider）
- 启动流程间接层增加
- 迁移成本：需要将现有 280 行逻辑迁移到 AgentRuntime

## 决策

选择**选项 B：新增 AgentRuntime 封装完整启动顺序**。

280 行硬编码组装逻辑是典型的"启动腐化"问题，多个 Bug（PluginManagerImpl 参数不完整、ReadyGate 未调用、插件 Controller 无法注册）都是同一根因——缺乏统一的启动编排。AgentRuntime 将启动流程显式化、步骤化，生产与测试使用同一路径，ReadyGate 语义完整。新增 3 个类的代价远小于持续修复硬编码逻辑引入的 Bug。

## 影响

- Agent 变为薄 Adapter，仅负责 ClassLoader 切换和反射调用
- AgentRuntime 封装完整的 10 步启动流程
- AgentRuntimeContext 保存运行期对象引用
- BuiltinPluginProvider 统一内置插件发现
- ReadyGate.markReady() 在启动完成后调用
- 生产启动路径和测试启动路径统一
