# 测试指南

> **更新日期**: 2026/06/07

---

## 1. 后端测试

### 1.1 框架与工具

| 工具 | 版本 | 用途 |
|------|------|------|
| JUnit 5 | 5.10.0 | 单元测试 |
| Mockito | - | Mock 框架 |
| JaCoCo | 0.8.10 | 代码覆盖率 |

### 1.2 测试目录

```
luna-core/src/test/java/fun/efto/luna/core/
├── expression/       # 表达式引擎测试
├── infra/            # 基础设施测试 (RingBuffer, BytecodeCache)
├── injection/        # 注入域测试
├── injector/         # 注入器测试
├── integration/      # 集成测试
├── performance/      # 性能测试
├── plugin/           # 插件框架测试
├── probe/            # 探针测试
└── snapshot/         # 快照测试
```

### 1.3 运行命令

```bash
# 全部测试
mvn test

# 单个测试类
mvn test -Dtest=RingBufferTest

# 指定模块
mvn test -pl luna-core
```

### 1.4 测试隔离

- 单例类测试使用 `@Execution(SAME_THREAD)` 或 `setInstance(null)` 隔离
- `PluginManager` 测试需注意 StampedLock 状态重置

---

## 2. 前端测试

### 2.1 框架与工具

| 工具 | 版本 | 用途 |
|------|------|------|
| Playwright | 1.52+ | E2E 测试 |

### 2.2 测试目录

```
luna-ui/e2e/
├── api.spec.js              # API 接口测试
├── class-tree.spec.js       # 类浏览器测试
├── configuration.spec.js    # 配置管理测试
├── dashboard.spec.js        # 仪表盘测试
├── log-viewer.spec.js       # 日志查看器测试
├── navigation.spec.js       # 导航测试
├── plugin-manager.spec.js   # 插件管理测试
└── thread-analyzer.spec.js  # 线程分析器测试
```

### 2.3 运行命令

```bash
# E2E 测试
npm run test:e2e

# 有界面模式
npm run test:e2e:headed

# 查看报告
npm run test:e2e:report
```

---

## 3. 回归测试

### 3.1 自动化脚本

```bash
scripts/regression-test.sh
```

### 3.2 执行流程

1. Maven 构建
2. 清理旧进程
3. 启动 Agent + Demo 应用
4. 等待就绪
5. 执行后端单元测试
6. 执行前端 E2E 测试
7. 生成测试报告
8. 清理进程

### 3.3 覆盖范围

**后端**: CoreCapabilityRegistry、InjectionService、TracePlugin、PluginDependencyResolver

**前端**: 33 个用例覆盖类浏览、方法级注入、行号级注入、注入生命周期、规则 CRUD、系统状态、模板管理、注入测试与验证

---

## 4. 性能测试

### 4.1 红线验证

| 测试项 | 红线 | 测试方法 |
|--------|------|---------|
| Registry 查询 | < 0.1ms | 1000 规则 + 10000 类名微基准 |
| RingBuffer 吞吐 | > 10M ops/s | 多线程 offer/poll 压测 |
| 注入延迟 | < 450ms | Agent 启动到就绪 |
| CPU 抖动 | < 2% | 100+ TPS 压测 |

### 4.2 运行

```bash
mvn test -Dtest=PerformanceTest -pl luna-core
```
