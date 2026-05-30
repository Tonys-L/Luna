# Luna 自动化回归测试指南

## 概述

Luna 项目采用**双层回归测试体系**：后端 JUnit 单元测试 + 前端 Playwright API 黑盒测试。每个功能开发完成后，必须执行回归测试确保无回归。

## 快速开始

### 一键回归（推荐）

```bash
bash scripts/regression-test.sh
```

脚本自动完成：构建 → 清理持久化文件 → 启动 Agent + Demo App → 等待就绪 → 运行 E2E 测试 → 输出报告 → 清理进程。

### 手动分步执行

```bash
# Step 1: 后端单元测试
mvn test -pl luna-core

# Step 2: 构建
mvn package -DskipTests -q

# Step 3: 启动 Agent + Demo App
java -javaagent:luna-agent/target/luna-agent-1.0-SNAPSHOT.jar \
     -jar example/luna-demo-app/target/luna-demo-app-1.0-SNAPSHOT.jar

# Step 4: API 回归测试
cd luna-ui && npx playwright test e2e/api.spec.js --reporter=list

# Step 5: 清理
# Ctrl+C 停止 Java 进程，删除 luna-rules.json / luna-injections.json
```

## 测试覆盖

### 后端单元测试（JUnit）

| 模块 | 测试文件 | 覆盖内容 |
|------|---------|---------|
| CoreCapabilityRegistry | `CoreCapabilityRegistryTest.java` | 注册、查询、就绪状态 |
| CoreCapability 集成 | `CoreCapabilityIntegrationTest.java` | 初始化流程、依赖解析 |
| InjectionManager | `InjectionManagerTest.java` | CRUD、缓存、降级 |
| TracePlugin | `TracePluginTest.java` | 依赖声明、初始化 |
| PluginDependencyResolver | `PluginDependencyResolverTest.java` | 依赖解析、核心能力检查 |

运行命令：`mvn test -pl luna-core`

### API 黑盒测试（Playwright）

| 测试类别 | 用例数 | 覆盖端点 |
|---------|--------|---------|
| 类浏览与反编译 | 6 | `/api/classes`, `/api/decompile`, `/api/analysis`, `/api/line-numbers`, `/api/local-variables` |
| 方法级注入 | 4 | `POST /api/injections` (METHOD_ENTER/EXIT/AROUND, log/snapshot) |
| 行号级注入 | 5 | `POST /api/injections` (LINE_BEFORE/AFTER, 条件注入) |
| 注入生命周期 | 3 | `GET /api/injections/list`, `DELETE /api/injections/{id}`, `POST /api/injections/dry-run` |
| 规则 CRUD | 4 | `POST /api/rules`, `GET /api/rules`, `DELETE /api/rules/{id}` |
| 系统状态与指标 | 4 | `/api/status`, `/api/capabilities`, `/api/metrics/jvm`, `/api/metrics/threads` |
| 模板管理 | 4 | `/api/templates`, `/api/templates/categories`, `/api/templates/apply` |
| 注入测试与验证 | 3 | `/api/injections/test`, `/api/injections/verify`, `DELETE /api/injections/{id}` |

运行命令：`cd luna-ui && npx playwright test e2e/api.spec.js --reporter=list`

### 跳过的测试（后端未实现）

以下端点测试标记为 `test.describe.skip`，待后端实现后启用：

- 插件管理：`/api/plugins`, `/api/plugins/ui-manifest`, `/api/plugins/{id}/config`
- 插件市场：`/api/plugins/market/search`, `/api/plugins/market/check-updates`
- 前后端一致性：`/api/plugins/{id}/disable`, `/api/plugins/{id}/enable`, `/api/plugins/{id}/unload`

## 测试配置

### Playwright 配置

`luna-ui/playwright.config.js` 支持纯 API 测试模式：

- **baseURL**: `http://localhost:8421`（后端 Agent 地址）
- **timeout**: 30000ms（retransform 操作可能较慢）
- **workers**: 1（串行执行，避免并发注入冲突）
- **无 webServer 配置**：API 测试不需要前端 dev server

### 测试目标常量

`e2e/api.spec.js` 顶部定义了测试目标：

```javascript
const TARGET_CLASS = 'fun.efto.luna.demo.service.UserService'
const TARGET_METHOD = 'createUser'
const TARGET_DESC = '(Ljava/lang/String;I)Lfun/efto/luna/demo/model/User;'
```

## 回归测试脚本详解

`scripts/regression-test.sh` 执行流程：

```
Step 1/6: 构建    → mvn package -DskipTests -q
Step 2/6: 清理    → 删除 luna-rules.json, luna-injections.json
Step 3/6: 启动    → java -javaagent:... -jar demo-app.jar &
Step 4/6: 等待    → 轮询 GET /api/status 直到 200（最多 60s）
Step 5/6: 测试    → npx playwright test e2e/api.spec.js
Step 6/6: 报告    → 输出 PASSED/FAILED 摘要
EXIT:    清理    → trap EXIT 自动杀掉 Java 进程
```

### 超时处理

如果 Agent + Demo App 在 60 秒内未就绪，脚本输出超时错误并退出码 1。

### 持久化文件清理

脚本在启动前自动删除工作目录下的持久化文件，避免旧数据导致 VerifyError：

- `luna-rules.json`
- `luna-injections.json`

搜索范围：项目根目录、`luna-core/`、`example/luna-demo-app/`

## 功能开发回归流程

每个功能开发完成后，按以下步骤执行回归：

1. **后端单元测试**：`mvn test -pl luna-core`
2. **API 回归测试**：`cd luna-ui && npx playwright test e2e/api.spec.js`
3. **前端 E2E 测试**（如涉及 UI 变更）：`cd luna-ui && npx playwright test`

全部通过后方可提交代码。
