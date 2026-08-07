# 状态流转

> **文档定位**: 定义服务状态机、流程时序图
> **更新时机**: 修改状态流转、业务流程时更新
> **读者**: 开发者、架构师

---

## 1. InjectionStatus 状态图

```mermaid
stateDiagram-v2
    [*] --> Created: addInjection()
    Created --> Active: enabled=true
    Created --> [*]: addInjection() retransform 失败 → 回滚删除
    Active --> Suspended: suspendInjectionsByLocation()
    Suspended --> Active: resumeInjectionsByLocation()
    Active --> Disabled: toggleEnabled(false)
    Disabled --> Active: toggleEnabled(true)
    Active --> Removed: removeInjection()
    Disabled --> Removed: removeInjection()
    Suspended --> Removed: removeInjection()
    Removed --> [*]
```

> **回滚路径说明**: `addInjection()` 在 retransform 失败时，会回滚已持久化的 `PersistentInjection`（repository.delete）并从注册表 unregister，抛出 `RuntimeException`。因此 Created 状态的注入不会停留在 ACTIVE 状态，而是被删除回到初始态。

---

## 2. PluginState 状态图

```mermaid
stateDiagram-v2
    [*] --> Loading: load()
    Loading --> Active: initialize() success
    Loading --> Unloaded: initialize() error
    Active --> Disabled: disable()
    Disabled --> Active: enable()
    Active --> Unloading: unload()
    Disabled --> Unloading: unload()
    Unloading --> Unloaded: cleanup complete
    Unloaded --> [*]
```

---

## 3. AgentRuntime 启动流程

```mermaid
flowchart TD
    A[AgentRuntime.start] --> B[初始化 CoreModuleInitializer]
    B --> C[创建 InjectionRegistry 空实例]
    C --> D[创建 InjectionRepository 从 luna-injections.json 加载]
    D --> E[组装 InjectionService]
    E --> F[恢复持久注入到 Registry]
    F --> G[初始化内置插件]
    G --> H[启动 Web 服务器]
    H --> I[注册 GlobalClassFileTransformer]
    I --> J[applyActiveInjectionsToLoadedClasses retransform]
    J --> K[启动完成]
```

---

## 4. API 实时注入流程时序图

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

**关键步骤说明**:

| 步骤 | 说明 | 失败处理 |
|------|------|---------|
| ProbeHandler 校验 | 验证 probeType 合法性、注入位置兼容性 | 返回 InjectResult.failure() |
| 参数引用验证 | 检查 `$N` 引用是否超出方法参数范围 | 返回 InjectResult.failure() |
| 局部变量验证 | 检查 `$varName` 在目标行号处是否可见 | 返回 InjectResult.failure() |
| Repository 存储 | 持久化 PersistentInjection | 异常向上抛出（inject 外层捕获返回 failure） |
| Registry 注册 | 编译并注册 InjectionPoint | 失败时回滚（unregister + repository.delete），抛出 RuntimeException |
| Retransform | 触发 JVM 重新转换类 | 失败时回滚（unregister + repository.delete），抛出 RuntimeException |

**回滚语义说明**（与 `InjectionService` 实际行为一致）:

| 方法 | 失败处理 |
|------|---------|
| `addInjection()` | retransform 失败 → 回滚持久化和注册表（unregister + delete）→ 抛出 `RuntimeException` |
| `updateInjection()` | retransform 失败 → 回滚到旧状态（恢复旧 `PersistentInjection`、重建旧 `InjectionPoint`、retransform 恢复旧字节码） |
| `toggleEnabled()` enable | retransform 失败 → 回滚 enabled 标志为 false + unregister registry 条目 |
| `suspendInjectionsByLocation()` | retransform 失败 → 回滚状态为 ACTIVE + 重新 register + 不加入 suspendedIds |
| `removeInjection()` | retransform 失败 → 仅记录日志（删除操作不需要回滚） |

---

## 5. 注入测试流程 (injectWithTest)

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

## 6. 注入移除流程时序图

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

## 7. 插件加载流程

```mermaid
flowchart TD
    A[PluginManager.load pluginId] --> B{依赖检查}
    B -->|缺失依赖| C[PluginLoadResult.failure]
    B -->|通过| D[创建 PluginClassLoader]
    D --> E[ServiceLoader 发现 LunaPlugin]
    E --> F[plugin.initialize context]
    F --> G[注册 ProbeHandler]
    G --> H[注册 InjectionType]
    H --> I[注册 CodeEngine]
    I --> J[注册 LunaController]
    J --> K[PluginLoadResult.success]
```

---

## 8. 插件卸载流程

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

**卸载关键顺序**: 必须先清空 Registry 强引用，再关 ClassLoader，否则类无法被 GC。

---

## 9. 插件热更新流程

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

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
| 2026/06/16 | 对照代码补充：InjectionService 实际方法、PluginManagerImpl 实际接口、AgentRuntime 10步流程、三级索引、CoreCapabilityRegistry | Tony.L |
| 2026/06/17 | 迁移补充：注入流程时序图(3个)、三层服务架构、接口隔离classDiagram、PluginManager完整接口、插件生命周期流程图(3个)、PluginLifecycleListener事件体系、Agent启动恢复流程图 | Tony.L |
| 2026/06/17 | 对照代码修正：InjectionQuery补充contains、InjectionRepository补充findByGroupId、PluginState删除FAILED、AgentRuntime修正为11步、CoreCapabilityRegistry修正为6个KERNEL能力、PluginLifecycleListener修正7个方法及参数、ProbeHandler补充onDelete、InjectionService依赖修正、code-compiler-dispatch kind修正为KERNEL | Tony.L |
| 2026/08/02 | 更新时序图失败处理描述，与 InjectionService 回滚语义一致 | Tony.L |
