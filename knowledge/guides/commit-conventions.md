# 提交规范

> **更新日期**: 2026/06/07

---

## 1. 提交消息格式

采用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 1.1 Type（必填）

| Type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构（不改变外部行为） |
| `docs` | 文档变更 |
| `test` | 测试相关 |
| `perf` | 性能优化 |
| `chore` | 构建/工具变更 |
| `style` | 代码格式（不影响逻辑） |

### 1.2 Scope（可选）

| Scope | 说明 |
|-------|------|
| `core` | luna-core 模块 |
| `agent` | luna-agent 模块 |
| `attacher` | luna-attacher 模块 |
| `ui` | luna-ui 模块 |
| `injection` | 注入域 |
| `plugin` | 插件框架 |
| `expression` | 表达式引擎 |
| `bytecode` | 字节码引擎 |
| `probe` | 探针通信 |
| `transformer` | 转换器层 |
| `rule` | 规则管理 |
| `template` | 模板体系 |

### 1.3 Subject（必填）

- 简洁描述变更内容
- 不超过 50 字符
- 不以句号结尾
- 使用祈使语气（"add" 而非 "added"）

### 1.4 Body（可选）

- 详细描述变更原因和内容
- 每行不超过 72 字符

### 1.5 Footer（可选）

- 关联 Issue: `Closes #123`
- 破坏性变更: `BREAKING CHANGE: description`

---

## 2. 示例

### 新功能

```
feat(injection): add InjectionService with Repository/Registry separation

Implement the three-layer injection architecture:
- InjectionRepository: pure storage
- InjectionRegistry: in-memory index with PackageTrie
- InjectionService: orchestration layer

Closes #42
```

### Bug 修复

```
fix(bytecode): exclude uninitialized variables in BeforeLine injection

LocalVariableScanner now uses excludeSameLineStart=true for BeforeLine
to avoid accessing variables that haven't been initialized yet.

Fixes VerifyError in line_before injection scenarios.
```

### 重构

```
refactor(plugin): replace InjectionManager with InjectionService/Registry/Repository

Remove the God Class InjectionManager and split into three
single-responsibility modules following the hexagonal architecture.
```

### 破坏性变更

```
feat(injection): unify InjectionPoint and InjectionRule into PersistentInjection

BREAKING CHANGE: InjectionRule is replaced by PersistentInjection.
All rule-based APIs now use the unified injection model.
```

---

## 3. 分支策略

| 分支 | 用途 | 命名 |
|------|------|------|
| `main` | 稳定发布 | - |
| `develop` | 开发集成 | - |
| `feature/*` | 功能开发 | `feature/injection-definition-layer` |
| `fix/*` | Bug 修复 | `fix/verify-error-line-before` |
| `refactor/*` | 重构 | `refactor/injection-manager-split` |

---

## 4. 提交粒度

- **一个提交一个关注点**: 不要混合功能、修复、重构
- **原子性**: 每个提交应该可以独立 revert
- **可编译**: 每个提交都应该通过编译
- **测试通过**: 每个提交都应该通过相关测试
