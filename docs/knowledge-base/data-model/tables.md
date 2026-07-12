# 数据模型总览与数据实体

> **文档定位**: 定义数据结构、字段定义、枚举类型
> **更新时机**: 新增/修改数据模型时更新
> **读者**: 架构师、开发者

---

## 1. 数据模型总览

Luna 的数据存储分为两层：

1. **内存数据**：运行时状态（InjectionPoint、PluginState 等）
2. **持久化数据**：JSON 文件（规则/注入点持久化）

---

## 2. 数据实体

### 2.1 InjectRequest — 注入请求

**用途**: API 层注入请求，用户创建注入时提交。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `clazz` | String | 是 | 目标类全限定名 |
| `method` | String | 是 | 目标方法名 |
| `probeType` | String | 是 | 探针类型 (LOG/SNAPSHOT/TRACE) |
| `injectionLocation` | String | 是 | 注入位置 (method_enter/method_exit/method_around/line_before/line_after/invoke/exception_exit) |
| `desc` | String | 否 | 方法描述符 |
| `codeType` | String | 否 | 代码类型 (EXPRESSION)，可选，SNAPSHOT/TRACE 不需要 |
| `code` | String | 否 | 注入代码内容，SNAPSHOT/TRACE 不需要 |
| `lineNumber` | Integer | 否 | 行号（行号注入时必填） |
| `ephemeral` | boolean | 否 | 是否临时注入（默认 true，Agent 重启后失效） |
| `groupId` | String | 否 | 分组 ID（成对注入关联，如 TRACE 的 start/end） |

---

### 2.2 PersistentInjection — 持久化注入

**存储位置**: JSON 文件（luna-injections.json）

**字段定义**:

| 字段名 | 类型 | 默认值 | 说明 | 索引 |
|--------|------|--------|------|------|
| id | String | UUID | 唯一标识 | 是（主键） |
| clazz | String | - | 目标类全限定名（支持 `*` 通配符） | 是（classIndex） |
| methodName | String | - | 目标方法名 | 否 |
| methodDescriptor | String | - | 方法描述符 | 否 |
| probeType | String | - | 探针类型 | 否 |
| injectionLocation | String | - | 注入位置 | 否 |
| codeType | String | - | 代码类型（可选，nullable） | 否 |
| code | String | - | 注入代码（支持协议前缀，可选，nullable） | 否 |
| lineNumber | Integer | 0 | 行号 | 否 |
| expression | String | - | 条件表达式 | 否 |
| fieldName | String | - | 字段名 | 否 |
| fieldDescriptor | String | - | 字段描述符 | 否 |
| enabled | boolean | true | 是否启用 | 否 |
| ephemeral | boolean | false | 是否临时注入 | 否 |
| status | InjectionStatus | ACTIVE | 状态 | 否 |
| groupId | String | - | 分组 ID | 否 |
| suspendReason | String | - | 挂起原因 | 否 |

**约束**:
- id 全局唯一
- injectionLocation 必须为合法值
- 行号注入时 lineNumber 必填且 > 0
- SNAPSHOT/TRACE 的 codeType 可为 null

---

### 2.3 InjectionPoint — 运行时注入点

**用途**: 编译后的织入点缓存，由 GlobalClassFileTransformer 消费。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | UUID，与 PersistentInjection.id 一致 |
| `target` | InjectionTarget | 注入目标 |
| `code` | CompiledCode | 编译后的代码（nullable，SNAPSHOT/TRACE 为 null） |
| `codeType` | String | 代码类型（独立存储，nullable） |
| `probeType` | String | 探针类型 |
| `source` | PersistentInjection | 原始持久化实体 |

> **注意**: `injectionLocation` 不是直接字段，而是通过 `target.location` 派生的属性。

### 2.4 InjectionTarget — 注入目标（4 子类型）

#### BaseTarget（公共父类）

| 字段 | 类型 | 说明 |
|------|------|------|
| `targetClass` | String | 目标类名 |

#### MethodTarget

| 字段 | 说明 |
|------|------|
| `methodName` | 方法名 |
| `methodDescriptor` | 方法描述符 |
| `location` | METHOD_ENTER / EXIT / AROUND |

#### LineNumberTarget

| 字段 | 类型 | 说明 |
|------|------|------|
| `methodName` | String | 方法名 |
| `methodDescriptor` | String | 方法描述符 |
| `lineNumber` | int | 目标行号 |
| `lineNumberOffset` | int | 行号偏移量 |
| `location` | LINE_BEFORE / AFTER |

#### ConstructorTarget

| 字段 | 说明 |
|------|------|
| `methodDescriptor` | 构造器描述符 |
| `location` | 由构造参数传入（通常为 CONSTRUCTOR） |

#### FieldAccessTarget

| 字段 | 说明 |
|------|------|
| `fieldName` | 字段名 |
| `fieldDescriptor` | 字段描述符 |
| `accessType` | 访问类型（`FieldAccessTarget.AccessType`：READ / WRITE） |
| `location` | 由构造参数传入（通常为 FIELD_ACCESS） |

---

### 2.5 CompiledCode — 编译后代码

| 字段 | 类型 | 说明 |
|------|------|------|
| `condition` | String | 条件表达式（nullable） |
| `content` | String | 代码内容（纯内容，无前缀） |
| `segments` | List | 解析后的代码片段（EXPRESSION 引擎特有，nullable） |

---

### 2.6 ProbeMessage — 探针消息

| 字段 | 类型 | 说明 |
|------|------|------|
| `type` | String | 消息类型 (LOG/SNAPSHOT/TRACE_END/TRACE_ALERT) |
| `payload` | String | 消息负载（JSON 字符串，包含 threadName/content/className/methodName/durationMs 等） |
| `timestamp` | long | 时间戳 |

> **注意**: `payload` 为 JSON 字符串，具体字段因 `type` 而异。

---

### 2.7 PluginInfo — 插件信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | 插件 ID |
| `displayName` | String | 显示名称 |
| `version` | String | 版本号 |
| `author` | String | 作者 |
| `category` | String | 分类 |
| `state` | PluginState | 状态 |
| `dependencies` | List\<String\> | 依赖列表 |

> **注意**: 内置插件保护逻辑在 `PluginManagerImpl` 内部通过插件 ID 判断，不在 PluginInfo 中标记。

---

### 2.8 枚举类型

#### InjectionStatus

| 值 | 说明 |
|-----|------|
| `ACTIVE` | 激活状态 |
| `SUSPENDED` | 挂起状态 |
| `DISABLED` | 禁用状态 |

#### PluginState

| 值 | 说明 |
|-----|------|
| `LOADING` | 加载中 |
| `ACTIVE` | 已激活 |
| `DISABLED` | 已禁用 |
| `UNLOADING` | 卸载中 |
| `UNLOADED` | 已卸载 |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
