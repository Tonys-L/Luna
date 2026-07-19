# 代码规范

> **文档定位**: 定义命名规范、文件组织、代码风格、注释规范
> **更新时机**: 规范调整时更新
> **读者**: 所有开发者

---

## 1. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 包名 | 全小写，点分隔 | `fun.efto.luna.core.injection` |
| 类名 | PascalCase | `InjectionPoint`, `DefaultClassTransformer` |
| 接口名 | PascalCase，无 I 前缀 | `ClassTransformer`, `BytecodeInjector` |
| 方法名 | camelCase | `getInjectionPoints()`, `retransformClasses()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 枚举值 | UPPER_SNAKE_CASE | `METHOD_ENTER`, `LINE_BEFORE` |

---

## 2. 类头声明

所有新文件必须包含以下头部，**必须在类名上方**：

```java
/**
  * @author : Tony.L(286269159@qq.com)
  * @since : {{YYYY/MM/DD HH:mm}}
  */
```

---

## 3. 文件组织

```text
luna-core/
├── src/main/java/fun/efto/luna/core/
│   ├── injection/       # 注入域模型
│   ├── transformer/     # 字节码转换器
│   ├── expression/      # 表达式引擎
│   ├── plugin/          # 插件框架
│   ├── probe/           # 探针运行时
│   ├── infra/           # 基础设施
│   └── bootstrap/       # Bootstrap 相关
├── src/main/resources/
│   └── META-INF/services/  # SPI 配置
└── src/test/java/       # 测试代码
```

---

## 4. 导入顺序

```java
// 1. Java 标准库
import java.util.*;
// 2. 第三方库（ASM, Jetty 等）
import org.objectweb.asm.*;
// 3. Luna 内部模块
import fun.efto.luna.core.injection.*;
```

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
