# ADR-003: 自研MVC框架替代Spring

## 状态

Accepted

## 背景

Luna Agent 需要内嵌 Web 服务器提供 REST API 和 WebSocket 推送能力。Agent 运行在目标 JVM 中，引入 Spring 会带来大量依赖（Spring Core、Spring Beans、Spring Web、Spring Context 等，超过 200 个类），这些依赖与宿主应用的 Spring 版本极易产生类加载冲突。

此外，Spring 的自动配置、组件扫描等机制在 Agent 类加载器环境下行为不可控，可能引发不可预期的问题。

## 方案选项

### 选项 A: 引入 Spring MVC

使用 Spring MVC + Embedded Jetty 提供 Web 能力。

优点：
- 功能完善（AOP、事务、自动配置、依赖注入等）
- 社区生态丰富，开发者熟悉
- 大量开箱即用的特性

缺点：
- 引入大量依赖，类冲突风险极高
- Spring 的类扫描和自动配置在自定义 ClassLoader 环境下行为不可控
- 违反 Agent 零外部依赖原则
- Fat JAR 体积显著增大

### 选项 B: 自研轻量级注解驱动 MVC 框架

实现基于注解的轻量级 MVC 框架（DispatcherServlet + RouteEngine），仅提供 Luna 所需的 Web 能力。

优点：
- 零外部依赖，类隔离无冲突
- 完全可控，在自定义 ClassLoader 环境下行为确定
- Fat JAR 体积小
- 仅实现所需功能，无冗余

缺点：
- 功能有限（无 AOP、事务、自动配置等）
- 需自行维护 MVC 框架代码
- 开发者无法使用 Spring 生态的便利特性

## 决策

选择**选项 B：自研轻量级注解驱动 MVC 框架**。

Agent 运行在目标 JVM 中，类隔离是生存基础。Spring 的大量依赖与宿主应用冲突的风险远超其带来的便利。Luna 的 Web 层需求明确且有限（REST API + WebSocket），自研 MVC 框架足以覆盖，且完全可控。

## 影响

- luna-agent 的 web/mvc 包包含自研的 DispatcherServlet、RouteEngine 等组件
- Controller 使用自研注解（非 Spring 注解）
- 未来如需更复杂的 Web 能力，需在自研框架中逐步添加
- 开发者需学习 Luna 自研 MVC 框架的使用方式
