# 消息格式与详情

> **文档定位**: 定义通信协议格式、主题、推送场景
> **更新时机**: 新增消息类型、修改消息格式时更新
> **读者**: 前后端开发者

---

## 1. 连接信息

| 参数 | 值 |
|------|-----|
| 协议 | WebSocket |
| 地址 | ws://localhost:8080/ws/log |

---

## 2. 消息格式

### 2.1 通用消息结构

WebSocket 推送的消息为文本帧，格式为 JSON 字符串。

---

## 3. 消息详情

### 3.1 日志消息

**触发场景**: 注入代码调用 `LunaSpy.onLog()` 时触发

**消息格式**:
```json
{
  "type": "log",
  "injectionId": "uuid-xxx",
  "message": "日志内容",
  "timestamp": 1718500000000
}
```

---

### 3.2 快照消息

**触发场景**: 注入代码调用 `LunaSpy.onSnapshot()` 时触发

**消息格式**:
```json
{
  "type": "snapshot",
  "injectionId": "uuid-xxx",
  "variables": {
    "param1": "value1",
    "localVar": "value2"
  },
  "variableTypes": {
    "param1": "java.lang.String",
    "localVar": "int"
  },
  "timestamp": 1718500000000
}
```

---

### 3.3 追踪消息

**触发场景**: 方法退出时调用 `LunaSpy.onTraceEnd()` 或 `LunaSpy.onTraceAlert()` 时触发

**消息格式**:
```json
{
  "type": "trace",
  "className": "com.example.Service",
  "methodName": "process",
  "duration": 150,
  "threshold": 100,
  "alert": true,
  "timestamp": 1718500000000
}
```

---

## 4. 消息处理流程

```text
业务线程执行注入代码
    → LunaSpy 静态方法调用
    → RingBuffer.offer() (MPSC 无锁队列)
    → LogDispatcher 后台线程轮询
    → RingBuffer.poll()
    → WebSocket Session.sendStringByFuture()
    → 浏览器接收并渲染
```

**关键设计**:
- 生产者（业务线程）不阻塞：RingBuffer 满时丢弃
- 消费者（LogDispatcher）轮询：无数据时 sleep(1) 避免 CPU 空转
- 无连接时仍消费数据（丢弃），防止 RingBuffer 积压导致 OOM

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|----------|--------|
| 2026/06/16 | 初始版本 | Tony.L |
