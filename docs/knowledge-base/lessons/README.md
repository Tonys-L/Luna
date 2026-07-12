# 经验教训库

> 开发过程中遇到的问题、踩坑记录和解决方案。避免重复犯错。

---

## 检索指引

查询经验教训时：

1. 先分析当前任务涉及的业务分类
2. 在下方索引中匹配"业务分类"列
3. 阅读匹配的教训文件

---

## 索引

| 文件 | 业务分类 | 条目数 | 最近更新 |
|------|----------|--------|----------|
| bytecode-injection.md | 字节码注入 | 5 | 2026/06/17 |
| plugin-lifecycle.md | 插件生命周期 | 7 | 2026/06/17 |

---

## 写作规范

每条教训包含以下字段：

```text
### N.M 标题

**问题**: 简述遇到的现象
**原因**: 根因分析
**解决方案**: 具体修复方式（含代码示例）
**影响文件**: 涉及的源码文件
**日期**: YYYY-MM-DD
```

新增教训时：
1. 按业务分类归入对应文件（如无对应文件则新建，文件名以业务分类命名）
2. 编号接续当前最大编号
3. 更新本索引的条目数和最近更新日期
4. 填写业务分类（对应 business-capabilities.md 中的能力名称）

---

## 已迁移的经验教训

以下经验教训已从 `knowledge/lessons/README.md` 和 `doc/VerifyError修复设计规约.md` 迁移至对应分类文件：

- **bytecode-injection.md**（5 条）：VerifyError 修复、Log4j2 ClassCastException、LineNumberInjectionType 并发 Bug、BytecodeCache 内存泄漏、LocalVariableScanner 算法漏洞
- **plugin-lifecycle.md**（7 条）：TRACE 探针非法组合、TRACE 注入不生效、ByteKitInjectorBase 路由错误、插件 update() 时序错误、ClassLoader 泄漏、TOCTOU 竞态、PluginManager 自我监听重复调用
