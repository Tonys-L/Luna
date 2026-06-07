# 经验教训索引

> **更新日期**: 2026/06/07

---

## 使用方式

每个经验教训以独立文件存储，命名格式: `YYYY-MM-DD-问题名称.md`

模板:

```markdown
## 问题名称

### 现象
发生了什么

### 影响范围
影响哪些功能/模块

### 原因分析
根因是什么

### 错误决策
为什么会出现这个问题

### 正确方案
最终如何解决

### 预防措施
以后如何避免

### 关联模块
涉及哪些模块

### 关联文件
涉及哪些文件

### 标签
#架构设计 #并发问题 #字节码 #性能优化
```

---

## 已记录的经验教训

### 2026-05: VerifyError 修复

**文件**: 待创建

**现象**: `line_before` 行号级注入触发 `java.lang.VerifyError: null`

**根因**: 4 个相互作用的 Bug:
1. `LocalVariableScanner` 未排除未初始化变量
2. Frame 计算冲突（SKIP_FRAMES 未使用）
3. `RuleClassFileTransformer` retransform 时双重注入
4. `contextVarIndex` slot 冲突

**标签**: `#字节码` `#ASM` `#VerifyError` `#行号注入`

---

### 2026-05: Log4j2 ClassCastException

**文件**: 待创建

**现象**: Agent 启动后 `StrLookup.asSubclass()` 抛出 ClassCastException

**根因**: 整个 Agent JAR 被注入 Bootstrap CL，Log4j2 的 lazy PluginType.getPluginClass() 导致类被 Bootstrap CL 加载，而 Interpolator 的 StrLookup.class 由 LunaAgentClassLoader 加载

**正确方案**:
1. Logger 初始化必须在 `appendToBootstrapClassLoaderSearch()` 之前
2. 仅注入精简 Bootstrap JAR（不含 Log4j2）

**标签**: `#类加载` `#BootstrapClassLoader` `#Log4j2` `#Shade`

---

### 2026-05: LineNumberInjectionType 并发 Bug

**文件**: 待创建

**现象**: `LineNumberInjectionType.withLineNumber()` 在并发下修改静态常量

**根因**: `BEFORE`/`AFTER` 是静态常量，`withLineNumber()` 直接修改其 lineNumber 字段

**正确方案**: 每次返回新实例，不修改静态常量

**标签**: `#并发问题` `#不可变对象` `#类型系统`

---

### 2026-05: BytecodeCache 内存泄漏

**文件**: 待创建

**现象**: BytecodeCache 无上限，长时间运行后内存持续增长

**根因**: `ConcurrentHashMap` 无驱逐策略

**正确方案**: 引入 LRU 驱逐或最大容量限制

**标签**: `#内存泄漏` `#缓存` `#性能优化`

---

## 待补充

以下场景发现新问题时必须记录:

- 设计缺陷导致的大规模重构
- 并发问题导致的线上故障
- 性能问题导致的用户体验下降
- 字节码操作导致的 VerifyError
- 类加载冲突导致的 ClassCastException/ClassNotFoundException
- 插件卸载后的资源泄漏
