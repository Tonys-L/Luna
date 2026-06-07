# 设计模式与核心机制

> **更新日期**: 2026/06/07

---

## 1. 核心设计模式

### 1.1 模板方法模式 (Template Method)

**应用场景**: 字节码注入器体系

```
BytecodeInjector (接口)
├── AbstractMethodInjector (抽象基类 - 模板方法)
│   ├── inject()          ← 骨架流程：ClassReader → ClassVisitor → ClassWriter
│   └── createMethodVisitor() ← 钩子方法，子类实现
│       ├── EnterMethodInjector
│       ├── ExitMethodInjector
│       └── AroundMethodInjector
├── BeforeLineInjector (Tree API，不走模板方法)
└── AfterLineInjector  (Tree API，不走模板方法)
```

**骨架流程** (`AbstractMethodInjector.inject()`):
1. 创建 `ClassLoaderAwareClassWriter`
2. 创建 `ClassReader` 读取字节码
3. 创建 `ClassVisitor`，委托子类的 `createMethodVisitor()`
4. `ClassReader.accept(visitor, 0)` 执行转换
5. 返回 `ClassWriter.toByteArray()`

### 1.2 策略模式 (Strategy)

**应用场景 1**: ProbeHandler — 探针行为策略

```java
public interface ProbeHandler {
    String getProbeType();                          // 策略标识
    boolean usesCode();                             // 是否需要代码
    Set<String> supportedInjectionLocations();      // 支持的注入位置
    ValidationResult validate(InjectRequest request); // 验证
    void handle(CompiledCode code, GenerateContext ctx); // 执行
}
```

3 个内置策略:
- `LogProbeHandler` → probeType = "LOG"
- `SnapshotProbeHandler` → probeType = "SNAPSHOT"
- `TraceProbeHandler` → probeType = "TRACE"

> **注意**: ConditionalBreakpointPlugin 未注册 ProbeHandler，仅注册模板。

**应用场景 2**: CodeEngine — 代码编译策略

```java
public interface CodeEngine {
    String getCodeType();
    CompiledCode compile(PersistentInjection persistent);
}
```

**应用场景 3**: InjectionRuleConverter — 规则转换策略

```java
// 每个插件可注册自己的 InjectionRuleConverter
// 两种注册方式：
// 1. 按 probeType 注册（无位置参数，自动匹配所有位置）
RuleConverterRegistry.getInstance().register(new MethodRuleConverter());
// 2. 按 injectionLocation 注册（精确匹配特定位置）
RuleConverterRegistry.getInstance().register(InjectionLocation.METHOD_ENTER, new MethodRuleConverter());
```

### 1.3 注册表模式 (Registry)

**统一接口**: 所有注册表均使用 `ConcurrentHashMap` 保证线程安全

| 注册表 | Key | Value | 初始化位置 |
|--------|-----|-------|-----------|
| `ProbeHandlerRegistry` | probeType (String) | ProbeHandler | 插件 onLoad |
| `InjectionTypeRegistry` | locationName (String) | InjectionLocation | CoreModuleInitializer |
| `RuleConverterRegistry` | InjectionLocation | InjectionRuleConverter | 插件 onLoad |
| `BytecodeInjectorRegistry` | InjectionLocation | BytecodeInjector | CoreModuleInitializer |
| `CodeEngineRegistry` | codeType (String) | CodeEngine | 插件 onLoad |
| `TemplateRegistry` | name (String) | RuleTemplate | 构造 + ServiceLoader |
| `CoreCapabilityRegistry` | capabilityId | CoreCapabilityRecord | 启动时 |

### 1.4 六角架构 / 端口-适配器 (Hexagonal / Ports & Adapters)

**应用场景**: InjectionService 的端口定义

```
InjectionService (核心)
├── port/Retransformer        ← 适配器: Agent 中的 Instrumentation 封装
├── port/BytecodeLoader       ← 适配器: ClassResourceHelper.loadClassBytes()
├── port/BytecodePreviewer    ← 适配器: InjectionService.preview() 内部实现
├── port/LocalVarValidator    ← 适配器: LocalVariableScanner 封装
├── port/InjectionVerifier    ← 适配器: InjectionTestHarnessAdapter
└── port/InjectionStore       ← 适配器: DefaultInjectionRepository
```

**核心优势**: InjectionService 不依赖任何具体技术实现，仅依赖端口接口。

### 1.5 观察者模式 (Observer)

**应用场景**: 插件生命周期事件

```java
public interface PluginLifecycleListener {
    default void onLoaded(PluginInfo info) {}
    default void onUnloaded(PluginInfo info) {}
    default void onUpdated(PluginInfo info, String oldVersion, String newVersion) {}
    default void onLoadFailed(String pluginId, String errorMessage) {}
    default void onUnloadFailed(String pluginId, String errorMessage) {}
    default void onDisabled(PluginInfo info) {}
    default void onEnabled(PluginInfo info) {}
}
```

`PluginManagerImpl` 维护监听器列表，状态变更时通知所有监听者。所有方法均为 default，监听者按需实现。

### 1.6 组合模式 (Composite)

**应用场景**: AroundMethodVisitor = EnterMethodVisitor + ExitMethodVisitor

```java
public class AroundMethodVisitor extends MethodVisitor {
    private final EnterMethodVisitor enterVisitor;
    private final ExitMethodVisitor exitVisitor;
    // 组合了进入和退出两个 Visitor 的行为
}
```

### 1.7 单例模式 (Singleton)

**应用场景**:

| 类 | 获取方式 |
|------|---------|
| `RuleManager` | `RuleManager.getInstance()` |
| `TemplateRegistry` | `TemplateRegistry.getInstance()` |
| `ProbeHandlerRegistry` | `ProbeHandlerRegistry.getInstance()` |
| `InitializerManager` | `InitializerManager.getInstance()` |
| `CoreCapabilityRegistry` | `CoreCapabilityRegistry.getInstance()` |

---

## 2. 核心机制

### 2.1 注入点三级索引 (DefaultInjectionRegistry)

```
查询 getActivePointsForClass(className)
│
├── 1. exactIndex: ConcurrentHashMap<String, List<InjectionPoint>>
│       精确匹配: "com.example.Service" → [point1, point2]
│       时间复杂度: O(1)
│
├── 2. prefixTrie: PackageTrie
│       前缀匹配: "com.example.*" → Trie 树遍历
│       时间复杂度: O(k)，k = 包名段数
│
└── 3. regexFallback: CopyOnWriteArrayList<RegexEntry>
        正则匹配: "com\.example\..*ServiceImpl" → Pattern.matcher()
        时间复杂度: O(n)，n = 正则规则数
```

**匹配优先级**: exactIndex → prefixTrie → regexFallback

### 2.2 条件表达式求值流程

```
"${param[1] > 0}::log:参数值: $1"
         │                │
         ▼                ▼
   ConditionRegistry    ExpressionCodeEngine
         │                │
         ▼                ▼
   Tokenizer → Parser → AST (预编译缓存)
         │
         ▼
   EvaluationContext (ThreadLocal)
   └── bind("param[1]", $1)
         │
         ▼
   AST.evaluate(context) → boolean
         │
         ▼ (true)
   执行注入代码
```

### 2.3 插件热加载安全机制

```
PluginManager.load(pluginId)
│
├── 1. SAFETY CHECK: checkUnloadable() → 依赖检查
├── 2. RULE SUSPEND: suspendInjectionsByLocation() → 暂停相关注入
├── 3. WRITE LOCK: StampedLock.writeLock()
├── 4. DESTROY: plugin.destroy() → 清理资源
├── 5. REGISTRY CLEAN: unregisterAll() → 清理注册表
├── 6. RETRANSFORM: 恢复原始字节码
├── 7. WRITE UNLOCK
├── 8. CLOSE ClassLoader
└── 9. GC ELIGIBLE
```

### 2.4 ReadyGate 启动屏障

```java
// 确保所有内置插件初始化完成后，才允许外部请求
public class ReadyGate {
    private volatile boolean ready = false;

    public void awaitReady() {
        while (!ready) { Thread.onSpinWait(); }
    }

    public void signalReady() { ready = true; }
}
```

### 2.5 规则脏标记 + 定时刷盘

```
RuleManager
├── ConcurrentHashMap<Long, InjectionRule>  ← 内存存储
├── AtomicLong idGenerator                  ← ID 生成
├── volatile boolean dirty                  ← 脏标记
├── ScheduledExecutorService (5s)           ← 定时检查
└── RulePersistenceService                  ← JSON 文件持久化

写操作: addRule/updateRule/deleteRule → dirty = true
定时任务: flushIfDirty() → dirty == true → persistenceService.saveAll()
```

---

## 3. ASM 透明化对照

### 3.1 EnterMethodVisitor — 方法入口注入

**ASM 字节码**:
```java
visitCode() {
    // 调用 assembler.assemble(context, mv)
    // 生成: LunaSpy.onLog(String.format("消息 %s", $1))
}
```

**等价 Java 伪代码**:
```java
void method(Object param0) {
    LunaSpy.onLog(String.format("消息 %s", param0)); // ← 注入
    // ... 原始方法体 ...
}
```

### 3.2 ExitMethodVisitor — 方法退出注入

**ASM 字节码**:
```java
visitInsn(opcode) {
    if (IRETURN <= opcode <= RETURN) {
        // 在 return 指令前注入代码
    }
}
```

**等价 Java 伪代码**:
```java
Object method() {
    Object result = ...;
    LunaSpy.onLog("返回: " + result); // ← 注入
    return result;
}
```

### 3.3 LineNumberVisitor — 行号注入

**ASM 字节码**:
```java
visitLineNumber(line, start) {
    if (line == targetLineNumber && !injected) {
        // 在目标行号处注入代码
        injected = true;
    }
}
```

**等价 Java 伪代码**:
```java
void method() {
    int x = 10;           // line 15
    LunaSpy.onLog("到达行16"); // ← 注入 (line 16)
    int y = x + 1;        // line 16
}
```
