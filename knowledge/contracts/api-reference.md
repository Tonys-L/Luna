# API 接口契约

> **更新日期**: 2026/06/07

---

## 1. 概述

- **基础路径**: `/api`
- **协议**: HTTP/1.1 + WebSocket
- **端口**: 8421
- **编码**: UTF-8
- **响应格式**: `ApiResult { success: boolean, status: int, error: String, data: Object }`

---

## 2. 系统状态

### GET /api/status

获取系统运行状态。

**响应 data**:
```json
{
  "status": "running",
  "version": "1.0.0"
}
```

---

## 3. 类浏览

### GET /api/classes

获取已加载类列表。

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `refresh` | String | 否 | 刷新类列表 |

**响应 data**: 类树结构

### GET /api/decompile

反编译指定类。

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `class` | String | 是 | 类全限定名 |

**响应 data**:
```json
{
  "decompiled": "/* 1 */ package com.example;\n/* 2 */ public class Service { ... }"
}
```

### GET /api/analysis

获取类分析信息（方法列表、字段列表、行号表）。

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `class` | String | 是 | 类全限定名 |

### GET /api/line-numbers

获取类行号表。

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `class` | String | 是 | 类全限定名 |

### GET /api/local-variables

获取指定行号处的局部变量表。

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `class` | String | 是 | 类全限定名 |
| `method` | String | 是 | 方法名 |
| `desc` | String | 否 | 方法描述符 |
| `line` | int | 是 | 行号 |

**响应 data**:
```json
{
  "variables": [
    { "name": "userId", "descriptor": "I", "slot": 1 }
  ]
}
```

---

## 4. 注入管理

### POST /api/injections

执行注入。

**请求体** (InjectRequest):
```json
{
  "clazz": "com.example.UserService",
  "method": "getUser",
  "desc": "(I)Lcom/example/User;",
  "probeType": "LOG",
  "injectionLocation": "method_enter",
  "codeType": "EXPRESSION",
  "code": "log:调用 getUser, 参数: $1",
  "lineNumber": null
}
```

**响应 data**:
```json
{
  "success": true,
  "injectionId": "uuid-xxx"
}
```

### GET /api/injections/list

获取指定类的注入点列表。

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `class` | String | 是 | 类全限定名 |

### DELETE /api/injections/{id}

移除注入点。

### POST /api/injections/test

注入测试（dry-run + inject + verify + validate）。

**请求体**: 同 InjectRequest

**响应 data**:
```json
{
  "success": true,
  "injectionId": "uuid-xxx",
  "generatedSize": 2048,
  "originalSize": 1024,
  "output": "验证输出内容",
  "failedStep": null,
  "error": null
}
```

### POST /api/injections/dry-run

预览注入（不实际 retransform）。

### POST /api/injections/verify

验证注入结果。

---

## 5. 规则管理

### GET /api/rules

获取规则列表。

### GET /api/rules/{id}

获取规则详情。

### POST /api/rules

创建规则。

**请求体** (InjectionRule):
```json
{
  "targetClass": "com.example.*",
  "targetMethod": "*",
  "probeType": "LOG",
  "injectionLocation": "method_enter",
  "code": "log:方法调用",
  "codeType": "EXPRESSION",
  "enabled": true
}
```

### PUT /api/rules/{id}

更新规则。

### DELETE /api/rules/{id}

删除规则。

---

## 6. 模板管理

### GET /api/templates

获取模板列表。

### GET /api/templates/categories

获取模板分类。

### GET /api/templates/{name}

获取模板详情。

### POST /api/templates/apply

应用模板。

**请求体**:
```json
{
  "templateName": "method-timing",
  "targetClass": "com.example.UserService",
  "targetMethod": "getUser",
  "methodDesc": "(I)Lcom/example/User;",
  "paramValues": {
    "threshold": "100"
  }
}
```

---

## 7. JVM 监控

### GET /api/metrics/jvm

获取 JVM 指标。

**响应 data**:
```json
{
  "heapUsed": 134217728,
  "heapMax": 536870912,
  "gcCount": 5,
  "gcTime": 230,
  "threadCount": 42,
  "peakThreadCount": 50,
  "uptime": 3600000
}
```

### GET /api/metrics/threads

获取线程转储。

---

## 8. 插件管理

### GET /api/plugins

获取插件列表。

### GET /api/plugins/{pluginId}

获取插件详情。

### POST /api/plugins/{pluginId}/disable

禁用插件。

### POST /api/plugins/{pluginId}/enable

启用插件。

### POST /api/plugins/{pluginId}/unload

卸载插件。

### POST /api/plugins/{pluginId}/load

加载插件。

### POST /api/plugins/{pluginId}/update

更新插件。

### GET /api/plugins/{pluginId}/config

获取插件配置。

### PUT /api/plugins/{pluginId}/config

保存插件配置。

### GET /api/plugins/ui-manifest

获取所有插件的 UI 扩展信息（聚合 API）。

---

## 9. 插件市场

### GET /api/plugins/market/search

搜索插件。

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `keyword` | String | 是 | 搜索关键词 |

### POST /api/plugins/market/install/{pluginId}

安装插件。

### GET /api/plugins/market/plugins/{pluginId}

获取插件市场插件详情。

### POST /api/plugins/market/plugins/{pluginId}/update

检查/执行插件更新。

### GET /api/plugins/market/check-updates

检查所有插件更新。

### DELETE /api/plugins/market/plugins/{pluginId}/uninstall

卸载已安装的市场插件。

---

## 10. 探针与引擎

### GET /api/probes

获取已注册的探针类型列表。

### GET /api/probes/engines

获取已注册的代码引擎列表。

---

## 11. 能力查询

### GET /api/capabilities

获取核心能力列表。

---

## 12. 测试接口

### GET /api/test/health

健康检查。

### GET /api/test/classes

测试用类列表。

### POST /api/test/invoke

反射调用方法。

---

## 13. WebSocket

### ws://localhost:8421/ws/log

实时日志推送。

**消息格式** (ProbeMessage):
```json
{
  "type": "LOG|SNAPSHOT|TRACE_END|TRACE_ALERT",
  "payload": { "threadName":"http-nio-8080-exec-1", "content":"日志内容", "className":"com.example.Service", "methodName":"getUser", "durationMs":150 },
  "timestamp": 1717737600000
}
```

> **注意**: `payload` 为 JSON 对象（非字符串），具体字段因 `type` 而异。当 payload 内容不是 JSON 对象时，作为字符串输出。

---

## 14. 自定义 MVC 框架

### 注解体系

| 注解 | 作用 |
|------|------|
| `@Controller` | 标记控制器类 |
| `@RequestMapping` | 类级路由前缀 |
| `@GetMapping` | GET 路由 |
| `@PostMapping` | POST 路由 |
| `@PutMapping` | PUT 路由 |
| `@DeleteMapping` | DELETE 路由 |
| `@PathVariable` | 路径变量注入 |
| `@RequestParam` | 查询参数注入 |
| `@RequestBody` | 请求体 JSON 反序列化 |

### 路由匹配

- **精确路由**: `Map<String, RouteHandler>`，key 为 `METHOD:PATH`
- **模式路由**: `List<PatternRoute>`，`{varName}` 编译为正则捕获组
- **优先级**: 精确路由 > 模式路由

### 统一响应

```java
ApiResult {
    boolean success;
    int status;
    String error;
    Object data;
}
```
