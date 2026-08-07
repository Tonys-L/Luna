# API 端点定义

> **文档定位**: 定义前后端 HTTP API 接口契约
> **更新时机**: 新增接口、修改接口参数时更新
> **读者**: 前后端开发者

---

## 1. 基础信息

### 1.1 基础 URL

```
http://localhost:8421/api
```

### 1.2 请求格式

- **Content-Type**: `application/json`

### 1.3 自研 MVC 注解

项目使用自研轻量 MVC 框架，支持以下注解：

| 注解 | 用途 |
|------|------|
| `@Controller` | 标记控制器类 |
| `@RequestMapping` | 映射请求路径（类/方法级） |
| `@GetMapping` | 映射 GET 请求 |
| `@PostMapping` | 映射 POST 请求 |
| `@PutMapping` | 映射 PUT 请求 |
| `@DeleteMapping` | 映射 DELETE 请求 |
| `@RequestParam` | 绑定查询参数 |
| `@RequestBody` | 绑定请求体 JSON |
| `@PathVariable` | 绑定路径变量 |

---

## 2. 注入管理

### 2.1 执行注入

```
POST /api/injections
```

**请求参数** (InjectRequest):

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| clazz | String | 是 | 目标类全限定名 |
| method | String | 是 | 目标方法名 |
| probeType | String | 否 | 探针类型（log/snapshot/trace） |
| injectionLocation | String | 是 | 注入位置（ENTER/EXIT/AROUND/BEFORE/AFTER 等） |
| desc | String | 否 | 方法描述符 |
| codeType | String | 否 | 代码类型（EXPRESSION/JAVA/SNAPSHOT） |
| code | String | 否 | 注入内容 |
| lineNumber | int | 否 | 行号（行号注入时必填） |
| ephemeral | boolean | 否 | 是否临时注入（默认 true） |
| groupId | String | 否 | 分组标识 |

> **必填校验说明**：代码 `isValid()` 仅校验 `clazz`、`method`、`injectionLocation` 三个字段。

---

### 2.2 注入点列表

```
GET /api/injections/list?class={className}
```

---

### 2.3 持久化注入列表

```
GET /api/injections/persistent
```

---

### 2.4 删除注入点

```
DELETE /api/injections/{id}
```

---

### 2.5 测试注入

```
POST /api/injections/test
```

完整流程：dryRun → inject → verify → validate

---

### 2.6 预览注入

```
POST /api/injections/dry-run
```

仅预览字节码变换，不实际注入。

---

### 2.7 验证注入

```
POST /api/injections/verify
```

仅验证注入效果。

---

## 3. 类浏览

### 3.1 已加载类列表

```
GET /api/classes?refresh={true|false}
```

---

### 3.2 反编译

```
GET /api/decompile?class={className}
```

**参数校验**：`class` 为 null 或空字符串时返回 `ApiResult.fail("Missing class parameter")`（HTTP 200，`success=false`）。

---

### 3.3 类分析

```
GET /api/analysis?class={className}
```

**参数校验**：`class` 为 null 或空字符串时返回 `ApiResult.fail("缺少class参数")`（HTTP 200，`success=false`）。

---

### 3.4 行号表

```
GET /api/line-numbers?class={className}
```

**参数校验**：`class` 为 null 或空字符串时返回 `ApiResult.fail("缺少class参数")`（HTTP 200，`success=false`）。

---

### 3.5 局部变量表

```
GET /api/local-variables?class={className}&method={methodName}&desc={methodDesc}&line={lineNumber}
```

**参数说明**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| class | String | 是 | 类全限定名 |
| method | String | 是 | 方法名 |
| desc | String | 否 | 方法描述符 |
| line | String | 是 | 行号（String 类型，内部 parseInt 转换） |

**参数校验**（按顺序校验，命中即返回 `ApiResult.fail`，HTTP 200，`success=false`）：

| 校验条件 | 错误信息 |
|----------|----------|
| `class` 为 null 或空 | `缺少class参数` |
| `method` 为 null 或空 | `缺少method参数` |
| `line` 为 null 或空 | `缺少line参数` |
| `line` 无法解析为整数 | `line参数必须是整数` |

---

## 4. 监控指标

### 4.1 JVM 指标

```
GET /api/metrics/jvm
```

返回：内存（Heap/NonHeap）、GC（各代次数和时间）、线程（总数/守护/死锁）、类加载、运行时信息。

---

### 4.2 线程转储

```
GET /api/metrics/threads
```

返回：线程列表 + 死锁检测。

---

## 5. 能力与探针

### 5.1 能力清单

```
GET /api/capabilities
```

返回：核心能力清单 + 插件摘要。

---

### 5.2 探针列表

```
GET /api/probes
```

返回：已注册的 ProbeHandler 列表。

---

### 5.3 代码引擎列表

```
GET /api/probes/engines
```

返回：已注册的 CodeEngine 列表。

---

## 6. 插件管理

### 6.1 插件列表

```
GET /api/plugins
```

返回所有已注册插件列表。

---

### 6.2 插件详情

```
GET /api/plugins/{pluginId}
```

返回指定插件的详细信息。

---

### 6.3 禁用插件

```
POST /api/plugins/{pluginId}/disable
```

---

### 6.4 启用插件

```
POST /api/plugins/{pluginId}/enable
```

---

### 6.5 卸载插件

```
POST /api/plugins/{pluginId}/unload
```

---

### 6.6 加载插件

```
POST /api/plugins/{pluginId}/load
```

---

### 6.7 更新插件

```
POST /api/plugins/{pluginId}/update
```

---

### 6.8 获取插件配置

```
GET /api/plugins/{pluginId}/config
```

---

### 6.9 保存插件配置

```
PUT /api/plugins/{pluginId}/config
```

**请求体**：`Map<String, String>`（插件配置键值对）

**参数校验**：请求体为 null 时返回 `ApiResult.fail("缺少配置内容")`（HTTP 200，`success=false`）。

---

### 6.10 插件 UI 扩展信息（聚合 API）

```
GET /api/plugins/ui-manifest
```

返回所有插件的 UI 扩展信息（注入类型、表达式协议、模板等），前端据此动态渲染。

**响应字段** (UiManifestVO):

| 字段 | 类型 | 说明 |
|------|------|------|
| injectionLocations | InjectionLocationEntry[] | 可用注入位置列表 |
| probeTypes | ProbeTypeEntry[] | 可用探针类型列表 |

**ProbeTypeEntry 字段**:

| 字段 | 类型 | 说明 |
|------|------|------|
| probeType | String | 探针类型标识（LOG, TRACE, SNAPSHOT, INVOCATION） |
| displayName | String | 显示名称 |
| syntax | String | 语法提示 |
| icon | String | 图标类名 |
| category | String | 分类 |
| usesCode | Boolean | 是否需要代码输入 |
| codeType | String | 代码引擎类型（usesCode=true 时非空，如 "EXPRESSION"） |
| supportedInjectionLocations | String[] | 支持的注入位置 |
| quickActionBehavior | String | 快捷行为（DIRECT/FORM） |
| glyphColor | String | 标记颜色 |
| configSchema | FormFieldSchema[] | 配置表单定义 |

---

### 6.11 插件市场搜索

```
GET /api/plugins/market/search?keyword={keyword}
```

**参数校验**：`keyword` 为 null 或空白（`trim()` 后为空）时返回 `ApiResult.fail("缺少搜索关键词")`（HTTP 200，`success=false`）。

> **注意**：MarketClient 未配置时返回 503。

---

### 6.12 安装插件

```
POST /api/plugins/market/install/{pluginId}
```

---

### 6.13 插件市场详情

```
GET /api/plugins/market/plugins/{pluginId}
```

---

### 6.14 检查/执行插件更新

```
POST /api/plugins/market/plugins/{pluginId}/update
```

---

### 6.15 检查所有插件更新

```
GET /api/plugins/market/check-updates
```

---

### 6.16 卸载市场插件

```
DELETE /api/plugins/market/plugins/{pluginId}/uninstall
```

**处理流程**：先调用 `PluginManager.unload(pluginId)` 卸载插件，卸载失败时返回 `ApiResult.fail(errorMessage, 400)`；卸载成功后递归删除插件目录文件。

**响应格式**（`ApiResult.ok(response)`，`response` 为 Map）：

| 字段 | 类型 | 说明 |
|------|------|------|
| unloadResult | PluginUnloadResult | 卸载结果 |
| filesRemoved | boolean | 文件清理是否成功，`false` 表示清理异常但已被捕获（不影响卸载成功状态） |

> **说明**：文件清理异常不会导致整体请求失败，仅将 `filesRemoved` 置为 `false` 并记录 WARNING 日志。

---

## 7. 状态与测试

### 7.1 Agent 状态

```
GET /api/status
```

---

### 7.2 健康检查

```
GET /api/test/health
```

---

### 7.3 简化类列表

```
GET /api/test/classes
```

---

### 7.4 反射调用方法

```
POST /api/test/invoke
```

**请求参数**（JSON Body）：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| className | String | 是 | 目标类全限定名 |
| methodName | String | 是 | 目标方法名 |

**参数校验**（命中即返回 `ApiResult.fail`，HTTP 200，`success=false`）：

| 校验条件 | 错误信息 |
|----------|----------|
| 请求体为 null | `缺少请求体` |
| `className` 或 `methodName` 为 null | `缺少 className 或 methodName` |

**业务错误响应**（HTTP 200，`success=false`，`code=400`）：

| 场景 | 错误信息 |
|------|----------|
| 目标类未加载 | `类未加载: {className}` |
| 目标方法不存在 | `方法不存在: {methodName}` |
| 目标类无法实例化（非静态方法且缺少公共无参构造器） | `类无法实例化，缺少公共无参构造器: {className}` |

> **说明**：非静态方法调用时通过 `getDeclaredConstructor().newInstance()` 实例化目标类；实例化失败时直接返回错误，不再使用硬编码回退构造器。

---

## 8. WebSocket

### 8.1 日志实时推送

```
ws://localhost:8421/ws/log
```

**消息格式**: 文本帧，JSON 格式的 ProbeMessage

**ProbeMessage 字段**：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | String | 消息唯一标识 |
| type | String | 探针类型 |
| className | String | 目标类名 |
| methodName | String | 目标方法名 |
| content | String | 消息内容 |
| timestamp | long | 时间戳（毫秒） |

---

## 9. 完整路由汇总

| 完整路径 | HTTP 方法 | Controller | 说明 |
|----------|-----------|------------|------|
| `/api/status` | GET | StatusController | Agent 状态 |
| `/api/classes` | GET | ClassController | 已加载类列表 |
| `/api/decompile` | GET | ClassController | 反编译 |
| `/api/analysis` | GET | ClassController | ASM 分析 |
| `/api/line-numbers` | GET | ClassController | 行号表 |
| `/api/local-variables` | GET | ClassController | 局部变量 |
| `/api/injections/list` | GET | InjectionController | 注入点列表 |
| `/api/injections/persistent` | GET | InjectionController | 持久化注入列表 |
| `/api/injections` | POST | InjectionController | 执行注入 |
| `/api/injections/test` | POST | InjectionController | 测试注入 |
| `/api/injections/dry-run` | POST | InjectionController | 预览注入 |
| `/api/injections/verify` | POST | InjectionController | 验证注入 |
| `/api/injections/{id}` | DELETE | InjectionController | 删除注入 |
| `/api/test/health` | GET | TestController | 健康检查 |
| `/api/test/classes` | GET | TestController | 简化类列表 |
| `/api/test/invoke` | POST | TestController | 反射调用方法 |
| `/api/metrics/jvm` | GET | MetricsController | JVM 指标 |
| `/api/metrics/threads` | GET | MetricsController | 线程 dump |
| `/api/capabilities` | GET | CapabilityController | 能力清单 |
| `/api/probes` | GET | ProbeController | 探针列表 |
| `/api/probes/engines` | GET | ProbeController | 代码引擎列表 |
| `/api/plugins` | GET | PluginManagerController | 插件列表 |
| `/api/plugins/{pluginId}` | GET | PluginManagerController | 插件详情 |
| `/api/plugins/{pluginId}/disable` | POST | PluginManagerController | 禁用插件 |
| `/api/plugins/{pluginId}/enable` | POST | PluginManagerController | 启用插件 |
| `/api/plugins/{pluginId}/unload` | POST | PluginManagerController | 卸载插件 |
| `/api/plugins/{pluginId}/load` | POST | PluginManagerController | 加载插件 |
| `/api/plugins/{pluginId}/update` | POST | PluginManagerController | 更新插件 |
| `/api/plugins/{pluginId}/config` | GET | PluginManagerController | 获取插件配置 |
| `/api/plugins/{pluginId}/config` | PUT | PluginManagerController | 保存插件配置 |
| `/api/plugins/ui-manifest` | GET | PluginUIController | 插件 UI 扩展信息 |
| `/api/plugins/market/search` | GET | MarketController | 插件市场搜索 |
| `/api/plugins/market/install/{pluginId}` | POST | MarketController | 安装插件 |
| `/api/plugins/market/plugins/{pluginId}` | GET | MarketController | 市场插件详情 |
| `/api/plugins/market/plugins/{pluginId}/update` | POST | MarketController | 检查/执行更新 |
| `/api/plugins/market/check-updates` | GET | MarketController | 检查所有更新 |
| `/api/plugins/market/plugins/{pluginId}/uninstall` | DELETE | MarketController | 卸载市场插件 |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/16 | 对照代码补充：实际路由、InjectRequest 字段、插件市场 API、完整路由汇总 | Tony.L |
| 2026/06/17 | 对照代码修正：端口号 8080→8421、ephemeral 默认值→true、必填校验对齐 isValid()、ProbeController/MarketController 标注未注册、ApiResult 字段顺序、ProbeMessage.timestamp、自研 MVC 注解、local-variables line 类型→String、/api/test/invoke 请求参数 | Tony.L |
| 2026/07/19 | ProbeController 注册到 JettyWebServer、MarketController 注册到 AgentRuntime，移除"尚未注册到 WebServer"标注 | Tony.L |
| 2026/08/02 | 补充 M-5~M-9/M-14 接口行为变更（参数校验、uninstall 响应格式、invoke 错误响应） | Tony.L |
