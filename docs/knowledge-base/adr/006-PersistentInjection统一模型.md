# ADR-006: PersistentInjection统一模型

## 状态

Accepted

## 背景

Luna 的注入领域存在双轨制模型：InjectionPoint（运行时模型）和 InjectionRule（持久化模型）。两套模型导致：

1. **领域模型分裂**：同一概念（注入点）在不同阶段使用不同模型，语义不一致
2. **转换逻辑复杂**：运行时模型和持久化模型之间需要双向转换，转换逻辑容易出错
3. **规则/模板功能增加复杂度**：RuleManager、TemplateEngine、RuleConverterRegistry 等组件增加了系统复杂度，但实际使用率低

规则/模板功能引入了额外的复杂度（RuleManager、TemplateEngine、RuleClassFileTransformer、RuleConverterRegistry 等），但实际使用率低。PersistentInjection 统一模型已能覆盖规则的核心需求（通配符匹配、持久化），模板功能可通过前端预设实现。

## 方案选项

### 选项 A: 保持双轨制模型

维持 InjectionPoint（运行时）和 InjectionRule（持久化）两套模型，通过转换逻辑桥接。

优点：
- 无需迁移，零迁移成本
- 运行时模型和持久化模型各自优化

缺点：
- 领域模型分裂，同一概念两种表达
- 转换逻辑持续维护成本
- 规则/模板功能增加系统复杂度但使用率低
- 前后端模型不一致，增加理解成本

### 选项 B: PersistentInjection 统一实体

使用 PersistentInjection 作为统一实体，通过 ephemeral 标记区分临时注入和持久化注入。

优点：
- 前后端模型统一，消除转换逻辑
- 语义一致：注入点只有一个模型
- ephemeral 标记简洁地区分临时/持久化
- 简化领域模型，降低理解成本

缺点：
- 迁移成本：需要将现有 InjectionPoint 和 InjectionRule 统一到 PersistentInjection
- 现有代码中依赖 InjectionPoint/InjectionRule 的地方需要适配
- 数据存储格式可能需要调整

## 决策

选择**选项 B：PersistentInjection 统一实体**。

双轨制模型是历史演进的产物，同一概念两种表达增加了不必要的复杂度。统一模型消除转换逻辑，前后端语义一致，ephemeral 标记简洁地区分临时/持久化。迁移成本是一次性的，而模型分裂的维护成本是持续的。

## 影响

- InjectionPoint 和 InjectionRule 统一为 PersistentInjection
- ephemeral 标记区分临时注入（运行时动态创建）和持久化注入（通过 API 创建）
- 前后端使用同一模型，消除转换逻辑
- 现有依赖 InjectionPoint/InjectionRule 的代码需迁移
