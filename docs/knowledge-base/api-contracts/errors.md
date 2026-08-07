# 错误码与响应格式

> **文档定位**: 定义 API 响应格式、HTTP 状态码
> **更新时机**: 修改响应格式、新增错误码时更新
> **读者**: 前后端开发者

---

## 1. 响应格式

### 1.1 ApiResult

```typescript
interface ApiResult<T> {
  success: boolean
  status: number
  data: T
  error: string | null
}
```

---

## 2. HTTP 状态码

| 状态码 | 含义 | 处理建议 |
|--------|------|----------|
| 200 | 成功 | 正常处理 |
| 400 | 请求参数错误 | 检查请求参数 |
| 404 | 资源不存在 | 检查路径和参数 |
| 500 | 服务器内部错误 | 稍后重试 |
| 503 | 服务不可用 | 服务依赖未配置（如 Market 客户端未配置），需后端启用对应能力 |

---

## 3. 参数校验错误响应

参数校验失败时统一返回 `ApiResult.fail(错误信息)`：
- HTTP 状态码 200
- `success=false`
- `error` 字段为对应错误信息

### 3.1 校验规则与错误信息

| 端点 | 校验条件 | 错误信息 |
|------|----------|----------|
| `GET /api/analysis?class=` | `className` 为 null 或空 | `缺少class参数` |
| `GET /api/decompile?class=` | `className` 为 null 或空 | `Missing class parameter` |
| `GET /api/line-numbers?class=` | `className` 为 null 或空 | `缺少class参数` |
| `GET /api/local-variables?class=&method=` | `className` 为 null 或空 | `缺少class参数` |
| `GET /api/local-variables?class=&method=` | `method` 为 null 或空 | `缺少method参数` |
| `POST /api/test/invoke` | body 为 null | `缺少请求体` |
| `PUT /api/plugins/{pluginId}/config` | `config` 为 null | `缺少配置内容` |
| `GET /api/market/search?keyword=` | `keyword` 为 null 或空白 | `缺少搜索关键词` |

### 3.2 响应示例

```json
{
  "success": false,
  "status": 200,
  "data": null,
  "error": "缺少class参数"
}
```

### 3.3 503 响应示例（Market 未配置）

Market 相关端点（`GET /api/market/search`、`GET /api/market/plugins` 等）在 MarketClient 未配置时返回：

```json
{
  "success": false,
  "status": 503,
  "data": null,
  "error": "Market not configured"
}
```

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/17 | 对照代码修正：ApiResult 字段顺序 | Tony.L |
| 2026/08/02 | 补充 503 状态码和参数校验错误响应说明 | Tony.L |
