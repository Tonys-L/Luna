# Knowledge Base

本目录存放项目知识库。

知识库用于记录项目事实、业务知识、架构设计、领域模型、技术决策和历史经验。

---

# 核心文档

每个核心文档已拆分为目录结构：`README.md`（TL;DR + 索引）+ 若干小文件。

## project-context/

项目上下文。

| 文件 | 内容 |
|------|------|
| README.md | TL;DR + 索引 |
| goals.md | 项目目标、目标用户 |
| constraints.md | 人员/技术/环境约束 |
| roadmap.md | 已知未来规划 |

适用于理解：

* 项目解决什么问题
* 项目处于什么阶段
* 有哪些约束条件

---

## business-capabilities/

业务能力。

| 文件 | 内容 |
|------|------|
| README.md | TL;DR + 索引 |
| background.md | 业务背景、解决的问题 |
| capabilities.md | 核心业务能力、支撑能力、外部依赖 |
| boundaries.md | 能力边界、扩展点 |
| flows.md | 业务流程 |

适用于理解：

* 系统解决什么问题
* 系统提供哪些能力
* 能力边界在哪里

---

## domain-model/

领域模型。

| 文件 | 内容 | 必读 |
|------|------|------|
| README.md | TL;DR + 索引 | |
| entities.md | 实体定义、属性、业务规则 | |
| value-objects.md | 值对象、枚举 | |
| invariants.md | 业务不变量 | **是** |
| events.md | 领域事件、聚合关系 | |

适用于理解：

* 核心业务对象
* 对象关系
* 业务规则

---

## architecture-overview/

架构总览。

| 文件 | 内容 |
|------|------|
| README.md | TL;DR + 索引 |
| layers.md | 架构风格、分层结构、各层职责 |
| dependencies.md | 依赖规则 |
| data-flows.md | 数据流向、扩展点设计 |

适用于理解：

* 系统如何组织
* 模块如何协作
* 依赖方向

---

## development-guide/

开发规范。

| 文件 | 内容 |
|------|------|
| README.md | TL;DR + 索引 |
| code-standards.md | 命名、文件组织、代码风格、注释 |
| architecture-rules.md | 分层规则、依赖规则、模块边界、错误处理 |
| testing.md | 测试规范、开发流程、代码审查、提交规范 |

适用于理解：

* 如何开发
* 如何测试
* 如何组织代码

---

## data-model/

数据模型。

| 文件 | 内容 |
|------|------|
| README.md | TL;DR + 索引 |
| tables.md | 核心表结构、索引设计 |
| migration.md | 存储策略、数据迁移 |

适用于理解：

* 数据如何组织
* 数据如何存储

---

## glossary.md

术语表（本身不大，保持单文件）。

适用于统一项目语言。

---

# 条件文档

根据项目特点按需创建。不涉及时仅读 README.md 的 TL;DR 即可。

## service-specs/

服务规格。

| 文件 | 内容 |
|------|------|
| README.md | TL;DR + 索引 |
| services.md | 服务定义、职责、依赖、接口 |
| state-machines.md | 状态流转 |

适用于复杂业务流程场景。

---

## api-contracts/

API 契约。

| 文件 | 内容 |
|------|------|
| README.md | TL;DR + 索引 |
| endpoints.md | 接口定义、请求/响应参数 |
| errors.md | 错误码、响应格式 |

适用于对外接口场景。

---

## protocol-docs/

协议文档。

| 文件 | 内容 |
|------|------|
| README.md | TL;DR + 索引 |
| messages.md | 连接信息、消息格式、消息详情 |

适用于实时通信场景。

---

# ADR

目录：

```text
adr/
```

用于记录重要架构决策。

详见：

```text
adr/README.md
```

---

# Traceability

目录：

```text
traceability/
```

用于追踪：

* 需求与业务分类的映射
* 需求与 ADR 的映射
* ADR 与代码模块的映射

详见：

```text
traceability/README.md
```

---

# Lessons Learned

目录：

```text
lessons/
```

用于记录：

* Bug 复盘
* 线上事故
* 设计失误
* 性能问题
* 实践经验

帮助项目持续积累经验并避免重复踩坑。

---

# 健康度检查

定期或在重大变更后，执行以下检查确保知识库与代码保持一致：

```text
□ 每个核心文档是否都有实质内容（非纯占位符）
□ 变更记录是否与代码提交历史对应
□ 文档间交叉引用是否有效（无死引用）
□ glossary.md 中的术语是否在其他文档中一致使用
□ 是否存在超过 30 天未更新但代码已变更的文档
□ 是否存在标记为"未来规划"但已实现的功能（应移入正式文档）
□ 代码实现与文档描述是否一致（分层结构、模块职责、接口定义）
```

发现不一致时，优先更新知识库，再继续开发。

---

# 文档联动规则

修改一个文档时，必须联动检查相关文档：

| 修改了 | 必须检查 |
|--------|----------|
| `project-context/` | `business-capabilities/`、`architecture-overview/`、`development-guide/` |
| `business-capabilities/` | `domain-model/`、`architecture-overview/`、`service-specs/` |
| `domain-model/` | `data-model/`、`business-capabilities/`、`service-specs/`、`glossary.md` |
| `architecture-overview/` | `development-guide/`、`domain-model/`、`business-capabilities/` |
| `development-guide/` | `architecture-overview/` |
| `api-contracts/` | `domain-model/`、`service-specs/` |
| `data-model/` | `domain-model/`、`api-contracts/` |
| `service-specs/` | `domain-model/`、`api-contracts/`、`protocol-docs/` |
| `protocol-docs/` | `service-specs/`、`api-contracts/` |

联动检查不意味着必须修改，而是确认是否需要同步更新。
