---
trigger: always_on
---

## 📏 实现保证 (Implementation Guarantees)
1. **非侵入性**：Agent 必须与业务完全隔离，支持 `retransform` 实现无残留卸载。
2. **性能红线**：单次插桩判定耗时必须 **< 0.1ms**。
3. **线程安全**：严禁阻塞业务线程，必须使用无锁队列（如 Disruptor/RingBuffer）进行数据传输。

## 📜 工程协议 (Engineering Protocols) - 必须严格执行

### 1. 文档与计划先行 (Design First)
在编写任何功能代码前，必须先输出：
- **【设计文档】**：包含 Mermaid 逻辑流图、接口定义、关键算法及所选设计模式。文档必须保存到 doc 目录下，最好有索引，以便后续参考
- **【开发计划】**：明确每一步的验证方案。

### 2. 测试驱动 (TDD Mode)
- 必须先编写失败的测试用例（JUnit/Mockito/Vitest），验证通过后方可交付功能代码。

### 3. 字节码透明化 (ASM Transparency)
- 编写 ASM 代码时，**必须在代码块下方同步输出对应的 Java 伪代码对照**，确保逻辑直观。

### 4. 源码规范 (Coding Standards)
- **类头声明**：所有新文件必须包含以下头部：
  ```java
  /**
   * @author : Tony.L(<286269159@qq.com>)
   * @since  : {{YYYY/MM/DD HH:mm}}
   */