# 架构规范

> **文档定位**: 定义分层规则、依赖规则、模块边界、错误处理
> **更新时机**: 架构调整时更新
> **读者**: 架构师、开发者

---

## 1. 分层规则

遵循三层隔离原则（参见 Agent 规则目录中的 `design-principles.md`），各层职责与约束：

| 层 | 职责 | 允许依赖 | 禁止出现 |
|----|------|----------|----------|
| 策略层 | 路由、调度、重试、采样、选择逻辑 | 核心层 | 业务规则实现 |
| 核心层 | 能力契约、领域模型、业务规则、不变量 | 无外部依赖 | SQL、Redis、MQ、HTTP、框架代码 |
| 实现层 | 存储实现、通信实现、字节码操作、第三方接入 | 核心层 | 业务逻辑 |

---

## 2. 依赖规则

- 依赖方向：外层 → 内层，禁止反向依赖
- 核心层（luna-core）禁止出现：SQL / Redis / MQ / HTTP / 框架基础设施代码
- luna-core 的 0 第三方依赖原则（ASM 通过 Shade 隔离）

---

## 3. 错误处理

- 核心层定义业务异常类型，携带业务语义
- 实现层将技术异常转换为业务异常
- 策略层决定错误恢复策略（重试、降级、熔断）
- 禁止吞掉异常（空 catch）
- 禁止在核心层使用技术异常类型（如 SQLException）

---

## 4. 实现保证

### 4.1 非侵入性

1. Agent 必须与业务完全隔离
2. 支持 `retransform` 实现无残留卸载
3. 插件卸载后，所有注入的类必须恢复原始字节码

### 4.2 性能红线

| 指标 | 红线 | 验证方式 |
|------|------|---------|
| 单次插桩判定耗时 | **< 0.1ms** | `DefaultInjectionRegistry.getActivePointsForClass()` 微基准测试 |
| 业务线程阻塞 | **0ms** | RingBuffer.offer() 非阻塞，满则丢弃 |
| 内存静态增量 | **~22MB** | 启动前后堆内存对比 |
| CPU 运行时抖动 | **< 2%** | 100+ TPS 压测 |

### 4.3 线程安全

1. **严禁阻塞业务线程**: 所有注入代码路径不得使用 synchronized/lock
2. **无锁队列**: 数据传输使用 `RingBuffer`（MPSC，CAS 写 + 单消费者读）
3. **StampedLock**: 插件加载/卸载使用读写锁，注入操作持读锁

---

## 5. 工程协议

### 5.1 设计先行

编写任何功能代码前，必须先输出：
- **设计文档**: 包含 Mermaid 逻辑流图、接口定义、关键算法及所选设计模式
- **开发计划**: 明确每一步的验证方案

### 5.2 测试驱动 (TDD)

- 必须先编写失败的测试用例（JUnit 5 / Mockito / Vitest），验证通过后方可交付功能代码
- 每次功能开发完都要执行完整的回归测试

### 5.3 字节码透明化 (ASM Transparency)

编写 ASM 代码时，**必须在代码块下方同步输出对应的 Java 伪代码对照**，确保逻辑直观。

示例：
```java
// ASM 字节码
mv.visitLdcInsn("消息");
mv.visitMethodInsn(INVOKESTATIC, "fun/efto/luna/core/probe/LogProbe", "onLog", "(Ljava/lang/String;)V", false);

// 等价 Java 伪代码
LogProbe.onLog("消息");
```

---

## 6. 服务实现规范

- 接口优先：核心能力通过接口定义，具体实现可替换
- 注册表模式：所有扩展点通过 Registry 注册
- 模板方法模式：AbstractMethodInjector 定义骨架，子类实现 createMethodVisitor()

---

## 7. ASM 透明化规范

- ASM 类引用必须使用 Shade 后的包名 `fun.efto.luna.shade.asm`
- 禁止在核心层直接暴露 ASM 类型给上层

---

## 8. 并发编程规范

**允许**：
- ConcurrentHashMap / CopyOnWriteArrayList
- AtomicLong / AtomicReference
- StampedLock（读写分离）
- ThreadLocal / volatile

**禁止**：
- synchronized / ReentrantLock
- Object.wait() / notify()
- BlockingQueue（使用 RingBuffer 替代）

---

## 9. 日志规范

双轨制：
- **Agent 自身日志**：Log4j2（Shade 隔离），配置文件 luna-log4j2.xml，上下文名 luna-agent
- **注入代码日志**：LogProbe → ProbeOutput → RingBuffer → WebSocket → 前端，不经过 Log4j2/SLF4J

---

## 10. 构建规范

### 10.1 Maven Profiles

| Profile | 用途 | test skip | logging level |
|---------|------|-----------|---------------|
| `dev` (默认) | 开发 | false | DEBUG |
| `prod` | 生产 | true | INFO |
| `perf` | 性能测试 | false | WARN |

### 10.2 Shade 规则

| 原始包 | 重命名包 | 原因 |
|--------|---------|------|
| `org.objectweb.asm` | `fun.efto.luna.shade.asm` | ASM 版本隔离 |
| `org.apache.logging.log4j` | `fun.efto.luna.shade.log4j2` | Log4j2 隔离 |

### 10.3 Bootstrap JAR

1. 仅包含 `probe/` + `infra/` + `expression/` 包
2. 零 Log4j2/SLF4J 依赖
3. 通过 `BootstrapJarBuilder.build()` 动态构建

---

## 11. Java 8 兼容性约束

编译目标 Java 8，禁止使用：
- Lambda 表达式（方法引用除外）
- var 关键字
- switch 表达式
- text block
- record
- sealed class
- pattern matching

---

## 12. 命名规避

避免使用含义模糊的命名，推荐替代：

| 避免 | 推荐 |
|------|------|
| Manager | Service / Registry / Repository |
| Helper | Strategy / Converter / Builder |
| Utils | Functions / Algorithms |
| Common | Shared / Core |
| Base | Abstract / Default |

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
