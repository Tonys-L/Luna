# ADR-009: 显式注册加ServiceLoader

## 状态

Accepted

## 背景

Luna 的插件框架需要一种模块发现和注册机制。常见的方案包括纯 SPI（ServiceLoader 全权发现）和显式注册（硬编码调用）。

Luna 是一个封闭系统，所有模块在编译时的依赖关系已经明确。核心模块（探针处理器、注入器、表达式处理器等）的数量有限且稳定，不需要运行时动态发现。但扩展点（如插件提供的 ExpressionHandler、ProbeHandler）需要灵活的发现机制。

## 方案选项

### 选项 A: 纯 SPI（ServiceLoader 全权发现）

所有模块的注册和发现都通过 ServiceLoader 机制实现。

优点：
- 松耦合，模块可独立部署
- 符合 Java SPI 标准规范
- 新增模块无需修改注册代码

缺点：
- 配置文件（META-INF/services）容易遗漏，调试困难
- 运行时才发现配置错误，类型安全弱
- 对于核心模块，增加了不必要的间接层
- 封闭系统中，松耦合的价值有限

### 选项 B: 显式注册 + ServiceLoader

核心注册使用显式代码（硬编码调用），扩展发现使用 ServiceLoader。

优点：
- 简单直接、类型安全、调试友好
- 核心模块注册在编译时即可验证
- 扩展点通过 ServiceLoader 灵活发现
- 符合"封闭系统优先显式"的原则

缺点：
- 新增核心模块需修改注册代码
- 核心模块与注册代码存在编译时依赖

## 决策

选择**选项 B：显式注册 + ServiceLoader**。

Luna 是封闭系统，核心模块编译时依赖关系已明确，纯 SPI 的松耦合价值有限且增加了调试难度。显式注册让核心模块的注册在编译时即可验证，类型安全且调试友好。扩展点（插件提供的处理器等）使用 ServiceLoader 发现，保持灵活性。

## 影响

- 核心模块（BuiltinPluginProvider 等）使用显式注册
- 扩展点（ExpressionHandler、ProbeHandler 等）使用 ServiceLoader 发现
- 新增核心模块需修改 BuiltinPluginProvider 的注册代码
- 插件开发者通过 META-INF/services 配置扩展点
