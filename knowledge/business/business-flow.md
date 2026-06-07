# 核心业务流程

> **更新日期**: 2026/06/07

---

## 1. API 实时注入流程

用户通过 Web UI 或 API 手动触发注入，对已加载的类进行 retransform。

```mermaid
sequenceDiagram
    participant UI as Luna-UI
    participant IC as InjectionController
    participant IS as InjectionService
    participant Repo as InjectionRepository
    participant Reg as InjectionRegistry
    participant PH as ProbeHandler
    participant RT as Retransformer
    participant GCT as GlobalClassFileTransformer
    participant DCT as DefaultClassTransformer

    UI->>IC: POST /api/injections (InjectRequest)
    IC->>PH: validate(request) — ProbeHandler 校验
    IC->>IS: inject(cmd)

    Note over IS: 1. 参数引用验证
    Note over IS: 2. 局部变量引用验证
    Note over IS: 3. 行号注入方法名校验

    IS->>IS: toPersistentInjection(cmd)
    IS->>Repo: save(persistentInjection)
    IS->>IS: InjectionPointFactory.create(pi)
    IS->>Reg: register(injectionPoint)
    IS->>RT: retransform(className)

    RT->>GCT: transform(loader, className, ..., bytecode)
    GCT->>Reg: getActivePointsForClass(className)
    Reg-->>GCT: List<InjectionPoint>

    loop 每个 InjectionPoint
        GCT->>DCT: transform(point, className, bytecode)
        DCT->>DCT: 查找 BytecodeInjector + CodeEngine
        DCT->>DCT: injector.inject(context, bytecode, engine)
        DCT-->>GCT: TransformerResult
    end

    GCT-->>RT: 修改后的字节码
    RT-->>IS: retransform 完成
    IS-->>IC: InjectResult.success(id)
    IC-->>UI: ApiResult
```

### 关键步骤说明

| 步骤 | 说明 | 失败处理 |
|------|------|---------|
| ProbeHandler 校验 | 验证 probeType 合法性、注入位置兼容性 | 返回 InjectResult.failure() |
| 参数引用验证 | 检查 `$N` 引用是否超出方法参数范围 | 返回 InjectResult.failure() |
| 局部变量验证 | 检查 `$varName` 在目标行号处是否可见 | 返回 InjectResult.failure() |
| Repository 存储 | 持久化 PersistentInjection | 异常捕获，记录日志 |
| Registry 注册 | 编译并注册 InjectionPoint | 异常捕获，记录日志 |
| Retransform | 触发 JVM 重新转换类 | 异常捕获，记录日志 |

---

## 2. 注入测试流程 (injectWithTest)

四步验证流程：dry-run → inject → verify → validate

```mermaid
flowchart TD
    A[InjectRequest] --> B{ProbeHandler 校验}
    B -->|失败| Z[返回 InjectTestResult.failure]
    B -->|通过| C{参数引用验证}
    C -->|失败| Z
    C -->|通过| D{局部变量验证}
    D -->|失败| Z
    D -->|通过| E[dry-run: preview 预览]
    E -->|未转换| Z
    E -->|已转换| F[inject: 实际注入]
    F -->|异常| Z
    F -->|成功| G[verify: 验证注入结果]
    G -->|验证失败| H[InjectTestResult 验证失败]
    G -->|验证成功| I[InjectTestResult 成功]
```

---

## 3. 规则自动注入流程

类首次加载时，`RuleClassFileTransformer` 自动匹配规则并注入。

```mermaid
sequenceDiagram
    participant JVM as JVM ClassLoader
    participant RCFT as RuleClassFileTransformer
    participant RM as RuleManager
    participant DCT as DefaultClassTransformer

    JVM->>RCFT: transform(loader, className, classBeingRedefined, ..., bytecode)

    Note over RCFT: classBeingRedefined == null 时才处理<br/>(仅新加载的类)

    RCFT->>RM: findRulesForClass(className)
    RM-->>RCFT: matchedRules

    loop 每个 InjectionRule
        RCFT->>RCFT: toPersistentInjection(rule)
        RCFT->>RuleConverterRegistry: convert(persistentInjection) → InjectionPoint
    end

    loop 每个 InjectionPoint
        RCFT->>DCT: transform(point, className, currentBytecode)
        DCT-->>RCFT: TransformerResult
    end

    RCFT-->>JVM: 返回修改后的字节码
```

---

## 4. 两条注入路径对比

| 特性 | API 实时注入 | 规则自动注入 |
|------|------------|------------|
| **触发方式** | 用户手动触发 | 类首次加载时自动触发 |
| **入口类** | `InjectionController` → `InjectionService` | `RuleClassFileTransformer` |
| **Transformer** | `GlobalClassFileTransformer`（全局注册） | `RuleClassFileTransformer`（全局注册） |
| **注入点来源** | `InjectRequest` → `PersistentInjection` → `InjectionPoint` | `RuleManager` → `InjectionRule` → `InjectionPoint` |
| **适用场景** | 对已加载的类进行 retransform | 对后续新加载的类自动注入 |
| **retransform** | 需要 `Instrumentation.retransformClasses()` | 不需要（类首次加载即转换） |

---

## 5. 注入移除流程

```mermaid
sequenceDiagram
    participant UI as Luna-UI
    participant IC as InjectionController
    participant IS as InjectionService
    participant Repo as InjectionRepository
    participant Reg as InjectionRegistry
    participant RT as Retransformer

    UI->>IC: DELETE /api/injections/{id}
    IC->>IS: removeInjection(id)
    IS->>Repo: findById(id)
    Repo-->>IS: PersistentInjection
    IS->>Repo: delete(id)
    IS->>Reg: unregister(id)
    IS->>RT: retransform(className)

    Note over RT: retransform 时，GlobalClassFileTransformer<br/>查询 Registry，该注入点已不存在<br/>若该类无其他注入点，返回 null（恢复原始字节码）

    RT-->>IS: 完成
    IS-->>IC: 成功
    IC-->>UI: ApiResult
```

---

## 6. 规则管理流程

### 6.1 创建规则

```mermaid
flowchart TD
    A[POST /api/rules] --> B[RuleManager.addRule]
    B --> C[生成 AtomicLong ID]
    C --> D[ConcurrentHashMap.put]
    D --> E[dirty = true]
    E --> F[retransformMatchedClasses]
    F --> G[5秒后定时刷盘到 JSON 文件]
```

### 6.2 模板应用

```mermaid
flowchart TD
    A[POST /api/templates/apply] --> B[TemplateService.applyTemplate]
    B --> C[TemplateRegistry.get template]
    C --> D[TemplateEngine.apply 参数替换]
    D --> E[生成 List InjectionRule]
    E --> F[逐条 RuleManager.addRule]
```

**6 个内置模板**:

| 模板 | 规则数 | 说明 |
|------|--------|------|
| `method-timing` | 2 | ENTER: trace:start + EXIT: trace:end:0 |
| `method-timing-threshold` | 2 | ENTER: trace:start + EXIT: trace:end:${threshold} |
| `method-access-log` | 2 | ENTER: log:→ call: $0 + EXIT: log:← return |
| `slow-method-alert` | 2 | ENTER: trace:start + EXIT: trace:alert:${threshold} |
| `line-snapshot` | 1 | LINE_BEFORE: snapshot:true |
| `conditional-breakpoint` | 1 | LINE_BEFORE: snapshot:true + condition |

---

## 7. 数据消费流程

```mermaid
flowchart LR
    subgraph "业务线程（生产者）"
        Biz[业务代码] -->|调用| Spy[LunaSpy]
    end

    subgraph "探针处理"
        Spy --> LogP[LogProbe.onLog]
        Spy --> SnapP[SnapshotProbe.onSnapshot]
        Spy --> TraceP[TraceProbe.onTraceEnd/Alert]
    end

    subgraph "数据传输"
        LogP --> RB[RingBuffer ProbeMessage 4096]
        SnapP --> RB
        TraceP --> RB
    end

    subgraph "消费线程"
        RB -->|poll| LD[LogDispatcher 守护线程]
        LD -->|JSON 序列化| WS[WebSocket Sessions]
    end

    subgraph "前端"
        WS -->|onMessage| UI[Luna-UI LogViewer]
    end
```

**关键设计决策**:
- 所有数据统一投递到同一个 `RingBuffer<ProbeMessage>`
- 满队列丢弃，保护业务线程
- 无 WebSocket 连接时仍消费（丢弃），防止 RingBuffer 积压 OOM
- `LogDispatcher` 无数据时 `Thread.sleep(1)` 避免 CPU 空转

---

## 8. 插件生命周期流程

### 8.1 加载

```mermaid
flowchart TD
    A[PluginManager.load pluginId] --> B{依赖检查}
    B -->|缺失依赖| C[PluginLoadResult.failure]
    B -->|通过| D[创建 PluginClassLoader]
    D --> E[ServiceLoader 发现 LunaPlugin]
    E --> F[plugin.initialize context]
    F --> G[注册 ProbeHandler]
    G --> H[注册 InjectionType]
    H --> I[注册 RuleConverter]
    I --> J[注册 CodeEngine]
    J --> K[注册 LunaController]
    K --> L[PluginLoadResult.success]
```

### 8.2 卸载

```mermaid
flowchart TD
    A[PluginManager.unload pluginId] --> B{checkUnloadable}
    B -->|不可卸载| C[PluginUnloadResult.failure]
    B -->|可卸载| D[suspendInjectionsByLocation]
    D --> E[StampedLock.writeLock]
    E --> F[plugin.destroy]
    F --> G[清理所有注册表]
    G --> H[retransform 受影响的类]
    H --> I[StampedLock.writeUnlock]
    I --> J[关闭 PluginClassLoader]
    J --> K[GC ELIGIBLE]
```

### 8.3 热更新

```mermaid
flowchart TD
    A[PluginManager.update pluginId] --> B[预验证新版本]
    B -->|验证失败| C[PluginUpdateResult.failure]
    B -->|验证通过| D[StampedLock.writeLock]
    D --> E[卸载旧版本]
    E -->|失败| F[Rollback 恢复旧版本]
    E -->|成功| G[加载新版本]
    G -->|失败| F
    G -->|成功| H[StampedLock.writeUnlock]
    H --> I[PluginUpdateResult.success]
```
