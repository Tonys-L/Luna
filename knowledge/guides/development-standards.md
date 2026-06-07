# 开发规范

> **更新日期**: 2026/06/07

---

## 1. 实现保证

### 1.1 非侵入性

- Agent 必须与业务完全隔离
- 支持 `retransform` 实现无残留卸载
- 插件卸载后，所有注入的类必须恢复原始字节码

### 1.2 性能红线

| 指标 | 红线 | 验证方式 |
|------|------|---------|
| 单次插桩判定耗时 | **< 0.1ms** | `DefaultInjectionRegistry.getActivePointsForClass()` 微基准测试 |
| 业务线程阻塞 | **0ms** | RingBuffer.offer() 非阻塞，满则丢弃 |
| 内存静态增量 | **~22MB** | 启动前后堆内存对比 |
| CPU 运行时抖动 | **< 2%** | 100+ TPS 压测 |

### 1.3 线程安全

- **严禁阻塞业务线程**: 所有注入代码路径不得使用 synchronized/lock
- **无锁队列**: 数据传输使用 `RingBuffer`（MPSC，CAS 写 + 单消费者读）
- **StampedLock**: 插件加载/卸载使用读写锁，注入操作持读锁

---

## 2. 工程协议

### 2.1 设计先行

编写任何功能代码前，必须先输出：
- **设计文档**: 包含 Mermaid 逻辑流图、接口定义、关键算法及所选设计模式
- **开发计划**: 明确每一步的验证方案

### 2.2 测试驱动 (TDD)

- 必须先编写失败的测试用例（JUnit 5 / Mockito / Vitest），验证通过后方可交付功能代码
- 每次功能开发完都要执行完整的回归测试

### 2.3 字节码透明化 (ASM Transparency)

编写 ASM 代码时，**必须在代码块下方同步输出对应的 Java 伪代码对照**，确保逻辑直观。

示例：
```java
// ASM 字节码
mv.visitLdcInsn("消息");
mv.visitMethodInsn(INVOKESTATIC, "fun/efto/luna/core/probe/LunaSpy", "onLog", "(Ljava/lang/String;)V", false);

// 等价 Java 伪代码
LunaSpy.onLog("消息");
```

---

## 3. 源码规范

### 3.1 类头声明

所有新文件必须包含以下头部，**在类名上方**：

```java
/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/07 14:00
 */
public class ClassName {
```

### 3.2 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `InjectionService` |
| 方法名 | camelCase | `addInjection()` |
| 常量 | UPPER_SNAKE_CASE | `LOG_BUFFER` |
| 包名 | 全小写 | `fun.efto.luna.core.injection` |
| 接口 | 无前缀 | `InjectionRegistry` |
| 实现类 | Default/Impl 后缀 | `DefaultInjectionRegistry` |
| VO | VO 后缀 | `InjectionPointVO` |

### 3.3 避免的命名

| 避免 | 原因 | 替代 |
|------|------|------|
| `Manager` | 职责模糊 | `Service` / `Registry` / `Repository` |
| `Helper` | 职责模糊 | 具体职责命名 |
| `Utils` | 职责模糊 | 具体功能命名 |
| `Common` | 含义不清 | 具体领域命名 |
| `Base` | 过度抽象 | 具体抽象命名 |

### 3.4 Java 版本

- **编译目标**: Java 8
- **不使用**: Lambda（部分场景）、var、switch 表达式等 Java 9+ 特性
- **原因**: 兼容性基线

---

## 4. 包结构规范

### 4.1 luna-core 包规则

| 规则 | 说明 |
|------|------|
| 核心包不依赖 Agent 包 | 依赖方向: Agent → Core |
| 端口定义在 `port/` 子包 | 六角架构端口接口 |
| 内置插件在 `builtin/` 子包 | 每个插件一个子包 |
| 注册表在 `registry/` 子包 | ConcurrentHashMap + 线程安全 |

### 4.2 新增模块规则

- 新增能力优先扩展，避免修改稳定模块
- 新增注入位置: 实现 `InjectionLocation` + 注册到 `InjectionTypeRegistry`
- 新增探针类型: 实现 `ProbeHandler` + 注册到 `ProbeHandlerRegistry`
- 新增代码引擎: 实现 `CodeEngine` + 注册到 `CodeEngineRegistry`

---

## 5. 并发编程规范

### 5.1 允许

- `ConcurrentHashMap` — 注册表存储
- `CopyOnWriteArrayList` — 读多写少列表
- `AtomicLong` / `AtomicReference` — 原子操作
- `StampedLock` — 插件加载/卸载
- `ThreadLocal` — 求值上下文、追踪计时
- `volatile` — 状态标志

### 5.2 禁止

- `synchronized` — 业务代码路径
- `ReentrantLock` — 业务代码路径
- `Object.wait/notify` — 业务代码路径
- `BlockingQueue` — 数据传输（使用 RingBuffer 替代）

---

## 6. 日志规范

### 6.1 Agent 自身日志

- 使用 Log4j2（Shade 重命名为 `fun.efto.luna.shade.log4j2`）
- 配置文件: `luna-log4j2.xml`
- 上下文名称: `luna-agent`
- 与目标应用日志完全隔离

### 6.2 注入代码日志

- 通过 `LunaSpy.onLog()` → `RingBuffer` → `WebSocket` → 前端
- 不经过 Log4j2/SLF4J
- 不写入目标应用日志文件

---

## 7. 构建规范

### 7.1 Maven Profiles

| Profile | 用途 | test skip | logging level |
|---------|------|-----------|---------------|
| `dev` (默认) | 开发 | false | DEBUG |
| `prod` | 生产 | true | INFO |
| `perf` | 性能测试 | false | WARN |

### 7.2 Shade 规则

| 原始包 | 重命名包 | 原因 |
|--------|---------|------|
| `org.objectweb.asm` | `fun.efto.luna.shade.asm` | ASM 版本隔离 |
| `org.apache.logging.log4j` | `fun.efto.luna.shade.log4j2` | Log4j2 隔离 |

### 7.3 Bootstrap JAR

- 仅包含 `probe/` + `infra/` + `expression/` 包
- 零 Log4j2/SLF4J 依赖
- 通过 `BootstrapJarBuilder.build()` 动态构建

---

## 8. 测试规范

### 8.1 后端测试

- **框架**: JUnit 5 + Mockito
- **覆盖率**: JaCoCo
- **目录**: `luna-core/src/test/java/`
- **运行**: `mvn test`

### 8.2 前端测试

- **框架**: Playwright
- **目录**: `luna-ui/e2e/`
- **运行**: `npm run test:e2e`

### 8.3 回归测试

- **脚本**: `scripts/regression-test.sh`
- **流程**: 构建 → 清理 → 启动 → 等待就绪 → 测试 → 报告 → 清理
