# 测试规范

> **文档定位**: 定义测试规范、开发流程、代码审查、提交规范
> **更新时机**: 规范调整、新增流程时更新
> **读者**: 所有开发者

---

## 1. 后端测试

### 1.1 框架与工具

| 工具 | 版本 | 用途 |
|------|------|------|
| JUnit 5 | 5.10.0 | 单元测试 |
| Mockito | - | Mock 框架 |
| JaCoCo | 0.8.10 | 代码覆盖率 |

### 1.2 测试目录

```text
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

```text
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

## 3. 测试文件位置

- 单元测试：`src/test/java/` 与源码同包路径
- E2E 测试：`luna-ui/e2e/`
- 集成测试：`luna-core/src/test/java/.../integration/`

---

## 4. 测试命名

```java
// JUnit 5
@DisplayName("should register injection point successfully")
void shouldRegisterInjectionPointSuccessfully() { ... }
```

---

## 5. 测试覆盖要求

| 模块类型 | 覆盖率要求 | 重点测试 |
|----------|-----------|----------|
| 核心注入逻辑 | >= 80% | 注入流程、状态流转、并发安全 |
| 插件框架 | >= 80% | 加载/卸载/更新、并发安全 |
| 表达式引擎 | >= 80% | 解析、求值、边界条件 |
| Web 层 | >= 70% | API 契约、错误处理 |
| 前端 | E2E 覆盖 | Playwright 65 用例 |

---

## 6. 行号注入测试场景

> 目标：测试"通过源代码行号精确定位注入点，并捕获该行可见局部变量"的功能。
> 覆盖维度：功能正确性、边界条件、异常兼容、性能与安全、不同编译/运行环境。

### 6.1 基础功能场景

**单行注入**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| F-01 | 在普通方法（无分支）的某一行注入，该行有多个不同类型局部变量（int, String, Object） | 注入成功，捕获到所有可见局部变量，类型正确 |
| F-02 | 在方法的第一行（方法入口后第一行）注入 | 捕获到方法参数及该方法内已初始化的变量 |
| F-03 | 在方法的最后一行（return 之前）注入 | 捕获到当前作用域内所有局部变量 |
| F-04 | 在同一行设置多个注入点（不同插件） | 所有注入点按顺序执行，互不干扰，变量捕获各自独立 |
| F-05 | 在空行（只有注释或括号）上注入 | 应自动调整到该空行后的第一个有效指令行，或明确拒绝并提示 |

**变量捕获完整性**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| V-01 | 捕获基本类型（byte, short, int, long, float, double, char, boolean） | 能正确读取值并转为可传输的包装类型（或保留原始类型标识） |
| V-02 | 捕获引用类型（String, 自定义对象, 数组, 集合） | 能获取引用值，可按策略进行深度快照或仅打印 toString |
| V-03 | 捕获被 `final` 修饰的局部变量 | 正常捕获 |
| V-04 | 捕获在作用域内但尚未初始化的局部变量（如声明后未赋值） | 应报告未初始化（或值为 null/0），不应导致注入失败 |
| V-05 | 捕获 lambda 表达式内引用的外部局部变量（effectively final） | 能正确捕获该外部变量的当前值 |
| V-06 | 捕获在 try-with-resources 中声明的资源变量 | 能在 try 块内捕获该资源变量 |
| V-07 | 捕获循环变量（for 循环的 i, foreach 的 item） | 在循环体内注入，捕获当前迭代的值 |

**注入方式与生命周期**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| L-01 | 动态添加行号注入（通过 API / UI） | 目标类被 retransform，新注入立即生效 |
| L-02 | 动态删除行号注入 | 目标类被 retransform 恢复原始字节码，不再触发 |
| L-03 | 动态更新行号注入（修改表达式或捕获变量集） | 旧注入被替换，新行为生效 |
| L-04 | 同一行号注入在类被重定义后是否依然有效 | 若类字节码变化（如热加载不同版本），应检测并失效或需用户确认 |

### 6.2 复杂代码结构场景

**分支与循环**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| C-01 | 在 `if` 语句的条件表达式所在行注入 | 条件执行前捕获，可访问到条件中使用的变量 |
| C-02 | 在 `if-else` 分支内部的不同行分别注入 | 每个分支独立触发，捕获各自作用域内变量 |
| C-03 | 在 `switch` 表达式的某一行（如 case 内部）注入 | 正确映射到字节码位置，捕获该 case 块内的变量 |
| C-04 | 在 `for` 循环的条件判断行注入 | 每次条件判断前触发，捕获 `i` 等变量 |
| C-05 | 在循环体内注入，循环执行多次 | 每次迭代都触发，每次捕获的变量值符合迭代状态 |
| C-06 | 在 `while` 循环的条件表达式所在行注入 | 进入循环体前每次都触发 |

**异常处理**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| E-01 | 在 `try` 块内的某行注入，该行抛出异常 | 注入代码先执行，然后抛出异常（注入不应改变异常行为） |
| E-02 | 在 `catch` 块内的某行注入，捕获异常对象 | 能捕获到异常对象（参数 `e`）及其他局部变量 |
| E-03 | 在 `finally` 块内的某行注入 | 能正常注入，但注意 `finally` 块中变量作用域有限 |
| E-04 | 在 `try` 块中某行注入，该行后面有 `return` | 注入触发后继续执行，最终 return |
| E-05 | 方法声明 `throws` 异常，在 throw 语句所在行注入 | 捕获到要抛出的异常对象 |

**同步与多线程**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| T-01 | 在 `synchronized` 块的第一行注入 | 能捕获进入 synchronized 前的局部变量 |
| T-02 | 在 `synchronized` 方法内的某行注入 | 正常注入，注意方法同步语义不受影响 |
| T-03 | 多线程同时执行同一注入行 | 每个线程独立捕获自己的局部变量值，无交叉干扰 |
| T-04 | 在 `Thread.run()` 方法内的某行注入 | 能捕获线程局部变量 |

**内联与编译器优化**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| O-01 | 在可能被 JIT 内联的方法内行注入 | 内联不会导致注入丢失（retransform 会阻止内联或正确处理） |
| O-02 | 在 getter/setter 短方法中注入 | 注入成功，性能开销可接受 |
| O-03 | 在 final 方法中注入 | 正常 |
| O-04 | 在静态方法中注入 | 正常捕获静态方法内的局部变量 |
| O-05 | 在 native 方法中请求行号注入 | 应该拒绝（native 方法没有字节码） |

**Lambda 与匿名类**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| L-01 | 在 lambda 表达式体内的某行注入 | 能正确映射（lambda 会被编译为合成方法），捕获 lambda 参数及外部捕获的变量 |
| L-02 | 在匿名内部类的方法内的某行注入 | 正常注入，捕获该匿名类方法内的局部变量 |
| L-03 | 在方法引用（`Class::method`）所在行注入 | 方法引用本身只是一条指令，可能需要特殊处理 |

### 6.3 边界与异常场景

**行号定位失败**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| B-01 | 指定的行号超出方法源代码行范围 | 返回明确错误（行号不存在） |
| B-02 | 类没有调试信息（无 LineNumberTable） | 无法定位，应拒绝注入并提示缺少调试信息 |
| B-03 | 行号表中存在，但该行对应多个字节码指令 | 选择第一个指令注入（或可配置策略），行为可预测 |
| B-04 | 行号对应的是不可注入位置（如 `LINE_NUMBER` 伪指令本身） | 自动调整到其后第一条可执行指令 |
| B-05 | 多次重定义类后，LineNumberTable 发生变化 | 注入应基于当前类实际字节码，不应使用缓存的行号映射 |

**局部变量访问异常**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| B-06 | 指定捕获的变量名在作用域内不存在 | 捕获时跳过或返回 null，给出警告 |
| B-07 | 变量名存在，但类型无法序列化（如 Socket, Thread） | 捕获其 `toString()` 或标记为 `[unserializable]`，不导致注入失败 |
| B-08 | 变量作用域结束但槽位被复用，同一行号能访问到两个不同变量 | 按 `LocalVariableTable` 中最近作用域为准，或两者都返回 |
| B-09 | 局部变量是 `long` 或 `double` 占用两个槽位 | 正确读取完整值，不影响相邻槽位 |
| B-10 | 捕获 `this` 引用（非静态方法内） | 应能够捕获 `this`，变量名可以是 `this` |

**注入安全与冲突**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| S-01 | 注入的代码本身抛出异常（如 NPE） | 不影响原业务逻辑（异常被捕获并记录） |
| S-02 | 注入点被多个插件同时修改（顺序冲突） | 最终字节码应包含所有注入，顺序可配置，不损坏原方法 |
| S-03 | 注入代码修改局部变量值 | 默认禁止（只读模式），若允许修改则需要特殊测试，确保类型安全 |
| S-04 | 注入点在循环内，触发频率极高（千万次/秒） | 注入逻辑应足够轻量（如异步处理），不导致 CPU 飙升或 GC 压力 |

### 6.4 编译与运行环境兼容性

**不同 Java 版本**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| J-01 | Java 8, 11, 17, 21 分别测试 | 行号表格式兼容，注入正常工作 |
| J-02 | 启用不同编译选项（`-g:none` 无调试信息，`-g:lines` 只有行号，`-g:vars` 有变量表） | 无调试信息时拒绝；只有行号时能定位但无法捕获变量名；有变量表时完美工作 |
| J-03 | 编译优化 `-O`（早期版本）或 JIT 高级优化 | 注入后的方法可能阻止部分优化，功能仍需正确 |

**其他 JVM 语言**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| K-01 | Kotlin 代码：在行号注入，捕获局部变量（包括可空类型） | 应能工作，注意 Kotlin 编译器生成的变量名可能带有后缀 |
| S-01 | Scala 代码：在行号注入 | Scala 编译器会生成大量合成变量，行号映射可能不直观，需测试典型情况 |
| G-01 | Groovy 代码 | 正常 |

**容器与模块化**

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| M-01 | 应用运行在 Spring Boot 胖 jar 中 | 类加载正常，注入成功 |
| M-02 | 类由不同 ClassLoader 加载（如插件类） | 能正确定位并修改 |
| M-03 | 模块化（JPMS）环境中，注入带模块边界的类 | 需确保模块对 Agent 开放反射和 instrumentation |

### 6.5 性能与压力测试

| 场景ID | 验收指标 |
|--------|----------|
| P-01 | 每秒触发 10 万次注入的 CPU 开销 | 增加不超过 5% CPU |
| P-02 | 捕获包含 20 个局部变量的快照，内存分配速率 | 无内存泄漏，单次快照对象大小可控 |
| P-03 | 连续执行 100 万次注入，平均耗时 | p99 耗时 < 200μs（含捕获与异步传输） |
| P-04 | 注入点所在方法被递归调用深层（1000层） | 不栈溢出，注入逻辑不加重栈帧 |
| P-05 | 大量类（10000+）同时注册行号注入时的初始化时间 | 类重定义的总时间 < 2 秒 |

### 6.6 安全与容错场景

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| A-01 | 注入到系统类（如 `java.lang.String`） | 默认禁止（或需特殊开关），否则可能造成 JVM 不稳定 |
| A-02 | 注入到 Agent 自身使用的类 | 应拒绝或引发循环调用保护 |
| A-03 | 恶意表达式递归调用自身导致无限循环 | 沙箱应检测并中止（超时或次数限制） |
| A-04 | 捕获的变量包含敏感信息（如密码字符串） | 支持 PII 过滤或脱敏配置 |
| A-05 | 注入点在 finally 块中，且业务代码抛出异常 | 注入代码先执行，然后继续抛出异常（未吞掉） |

### 6.7 IDE / 源码映射集成场景

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| I-01 | 源码行号发生变化（类版本不一致） | Agent 应校验源代码与运行时代码是否匹配，不匹配时给出警告 |
| I-02 | 反编译后的行号与原始源码不同 | 仍然基于字节码行号注入，但 UI 显示需对应反编译行号 |
| I-03 | 用户从 IDE 选择行号，发送到 Agent | 完整闭环测试：添加 -> 触发 -> 删除 -> 不再触发 |

### 6.8 回归测试场景

| 场景ID | 描述 | 预期结果 |
|--------|------|----------|
| R-01 | 未使用行号注入时，应用性能与原始相同 | 零开销 |
| R-02 | 同时存在行号注入和方法名注入，互不干扰 | 各自独立生效 |
| R-03 | 删除行号注入后，类字节码完全恢复 | 与未注入前字节码一致（可通过字节码比对验证） |
| R-04 | 多次添加/删除同一行号注入，状态稳定 | 无残留 transformer，无内存泄漏 |

> **使用建议**：按优先级分阶段执行测试。第一阶段（MVP）覆盖 F-01~F-05, V-01~V-03, B-01~B-03, B-06, S-01；第二阶段加入复杂结构 C/E/T；第三阶段加入性能与安全。
> 从 6.4 编译与运行环境兼容性开始往后可以先不考虑。

---

## 7. 开发流程

### 7.1 新增功能流程

```text
1. 确认业务能力归属（参见 business-capabilities.md）
2. 在核心层定义能力契约（接口）
3. 编写核心层业务逻辑 + 单元测试
4. 在实现层提供具体实现 + 集成测试
5. 在策略层组装依赖 + 验证流程
6. 更新知识库文档
```

### 7.2 代码审查清单

**代码质量**：
- [ ] 是否符合命名规范？
- [ ] 函数/文件是否超出长度限制？
- [ ] 是否存在魔法数字？
- [ ] 是否存在重复代码（三次法则）？
- [ ] 是否包含类头声明？

**架构合规**：
- [ ] 是否正确分层？
- [ ] 核心层是否引入了技术依赖？
- [ ] 模块间是否通过接口通信？
- [ ] 错误处理是否符合规范？

**测试覆盖**：
- [ ] 是否包含测试？
- [ ] 核心业务规则是否有单元测试？
- [ ] 是否覆盖异常路径？

**知识库同步**：
- [ ] 是否需要更新领域模型？
- [ ] 是否需要更新 API 契约？
- [ ] 是否需要更新术语表？

### 7.3 提交规范

采用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

#### 7.3.1 Type（必填）

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

#### 7.3.2 Scope（可选）

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

#### 7.3.3 Subject（必填）

- 简洁描述变更内容
- 不超过 50 字符
- 不以句号结尾
- 使用祈使语气（"add" 而非 "added"）

#### 7.3.4 Body（可选）

- 详细描述变更原因和内容
- 每行不超过 72 字符

#### 7.3.5 Footer（可选）

- 关联 Issue: `Closes #123`
- 破坏性变更: `BREAKING CHANGE: description`

#### 7.3.6 示例

**新功能**

```
feat(injection): add InjectionService with Repository/Registry separation

Implement the three-layer injection architecture:
- InjectionRepository: pure storage
- InjectionRegistry: in-memory index with PackageTrie
- InjectionService: orchestration layer

Closes #42
```

**Bug 修复**

```
fix(bytecode): exclude uninitialized variables in BeforeLine injection

LocalVariableScanner now uses excludeSameLineStart=true for BeforeLine
to avoid accessing variables that haven't been initialized yet.

Fixes VerifyError in line_before injection scenarios.
```

**重构**

```
refactor(plugin): replace InjectionManager with InjectionService/Registry/Repository

Remove the God Class InjectionManager and split into three
single-responsibility modules following the hexagonal architecture.
```

**破坏性变更**

```
feat(injection): unify InjectionPoint and InjectionRule into PersistentInjection

BREAKING CHANGE: InjectionRule is replaced by PersistentInjection.
All rule-based APIs now use the unified injection model.
```

#### 7.3.7 分支策略

| 分支 | 用途 | 命名 |
|------|------|------|
| `main` | 稳定发布 | - |
| `develop` | 开发集成 | - |
| `feature/*` | 功能开发 | `feature/injection-definition-layer` |
| `fix/*` | Bug 修复 | `fix/verify-error-line-before` |
| `refactor/*` | 重构 | `refactor/injection-manager-split` |

#### 7.3.8 提交粒度

- **一个提交一个关注点**: 不要混合功能、修复、重构
- **原子性**: 每个提交应该可以独立 revert
- **可编译**: 每个提交都应该通过编译
- **测试通过**: 每个提交都应该通过相关测试

---

## 8. 性能红线

### 8.1 红线指标

| 指标 | 红线 | 说明 |
|------|------|------|
| 单次插桩判定 | < 0.1ms | 注入逻辑不能成为性能瓶颈 |
| 启动延迟 | < 450ms | Agent 启动不能明显拖慢应用 |
| 内存静态增量 | ~22MB | Agent 自身内存占用要小 |
| CPU 运行时抖动 | < 2% (100+TPS) | 注入代码不能显著影响业务性能 |
| RingBuffer 容量 | 4096 | 满时丢弃不阻塞 |

### 8.2 性能测试验证

| 测试项 | 红线 | 测试方法 |
|--------|------|---------|
| Registry 查询 | < 0.1ms | 1000 规则 + 10000 类名微基准 |
| RingBuffer 吞吐 | > 10M ops/s | 多线程 offer/poll 压测 |
| 注入延迟 | < 450ms | Agent 启动到就绪 |
| CPU 抖动 | < 2% | 100+ TPS 压测 |

### 8.3 运行命令

```bash
mvn test -Dtest=PerformanceTest -pl luna-core
```

---

## 9. 回归测试

### 9.1 一键回归

脚本：`scripts/regression-test.sh`

### 9.2 执行流程

1. Maven 构建
2. 清理旧进程
3. 启动 Agent + Demo 应用
4. 等待就绪
5. 执行后端单元测试
6. 执行前端 E2E 测试
7. 生成测试报告
8. 清理进程

### 9.3 覆盖范围

**后端**: CoreCapabilityRegistry、InjectionService、TracePlugin、PluginDependencyResolver

**前端**: 33 个用例覆盖类浏览、方法级注入、行号级注入、注入生命周期、系统状态、注入测试与验证

### 9.4 功能开发回归流程

```text
后端单元测试 → API 回归测试 → 前端 E2E 测试
```

### 9.5 测试隔离

- 单例类：使用 @Execution(SAME_THREAD) 或 setInstance(null)
- PluginManager：注意 StampedLock 状态重置
- @VisibleForTesting 约束内部方法可见性

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
