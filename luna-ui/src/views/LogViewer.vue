<template>
  <div class="log-viewer">
    <div class="log-toolbar">
      <div class="toolbar-left">
        <button class="tool-btn" @click="clearLogs">
          <i class="fas fa-ban"></i> 清空
        </button>
        <button class="tool-btn" :class="{ active: autoScroll }" @click="toggleAutoScroll">
          <i class="fas fa-arrow-down"></i> 自动滚动
        </button>
      </div>
      <div class="toolbar-right">
        <div class="connection-status" :class="statusClass">
          <span class="status-dot"></span>
          {{ statusText }}
        </div>
      </div>
    </div>
    
    <div class="log-container" ref="logContainer" @scroll="handleScroll">
      <div v-if="logs.length === 0" class="empty-state">
        <i class="fas fa-terminal empty-icon"></i>
        <p>暂无日志数据</p>
      </div>
      <div
        v-for="(log, index) in logs"
        :key="index"
        class="log-line"
        :class="getLineClass(log.type)"
      >
        <template v-if="log.type === 'SNAPSHOT'">
          <span class="log-tag tag-debug">DEBUG</span>
          <span v-if="log.data && log.data.timestamp" class="log-time">{{ formatTime(log.data.timestamp) }}</span>
          <span class="log-content snapshot-link" @click="openDebugger(log.data)">
            <i class="fas fa-bug"></i> 触发虚拟断点快照: {{ (log.data.payload && log.data.payload.pointId) || log.data.pointId }} (点击查看详情)
          </span>
        </template>
        <template v-else-if="log.type === 'INVOCATION'">
          <span class="log-tag tag-invocation">TRACE</span>
          <span v-if="log.timestamp" class="log-time">{{ formatTime(log.timestamp) }}</span>
          <div class="log-content invocation-content">
            <TraceTreeViewer :trace="log.trace" :timestamp="log.timestamp" />
          </div>
        </template>
        <template v-else>
          <span class="log-tag" :class="getTypeConfig(log.type).tagClass">{{ getTypeConfig(log.type).tag || log.type }}</span>
          <span v-if="log.timestamp" class="log-time">{{ formatTime(log.timestamp) }}</span>
          <span class="log-content" :class="getTypeConfig(log.type).contentClass">{{ log.text }}</span>
        </template>
      </div>
    </div>

    <!-- 调试面板 -->
    <DebuggerPanel 
      :visible="debuggerVisible"
      :snapshot="currentSnapshot"
      @close="debuggerVisible = false"
    />
  </div>
</template>

<script>
import DebuggerPanel from '../components/DebuggerPanel.vue'
import TraceTreeViewer from '../components/TraceTreeViewer.vue'

const messageHandlers = {
    SNAPSHOT: (parsed) => ({ type: 'SNAPSHOT', data: parsed }),
    TRACE: (parsed) => ({ type: 'TRACE', text: parsed.payload, timestamp: parsed.timestamp }),
    LOG: (parsed) => ({ type: 'LOG', text: parsed.payload, timestamp: parsed.timestamp }),
    INVOCATION: (parsed) => ({
        type: 'INVOCATION',
        trace: parsed.structuredPayload || {},
        timestamp: parsed.timestamp
    }),
}

const typeConfig = {
    SNAPSHOT: { tag: 'DEBUG', tagClass: 'tag-debug', lineClass: 'snapshot-line' },
    TRACE: { tag: 'TRACE', tagClass: 'tag-trace', contentClass: 'trace-content', lineClass: 'trace-line' },
    LOG: { tag: 'LOG', tagClass: 'tag-log', contentClass: 'log-text', lineClass: '' },
    INVOCATION: { tag: 'TRACE', tagClass: 'tag-invocation', lineClass: 'invocation-line' },
}

export default {
  name: 'LogViewer',
  components: {
    DebuggerPanel,
    TraceTreeViewer
  },
  data() {
    return {
      logs: [],
      autoScroll: true,
      maxLogs: 1000, // 最大保留日志行数
      ws: null,
      reconnectTimer: null,
      status: 'disconnected', // connected, disconnected, connecting
      debuggerVisible: false,
      currentSnapshot: {}
    }
  },
  computed: {
    statusClass() {
      return {
        'status-connected': this.status === 'connected',
        'status-disconnected': this.status === 'disconnected',
        'status-connecting': this.status === 'connecting'
      }
    },
    statusText() {
      switch (this.status) {
        case 'connected': return '已连接'
        case 'connecting': return '连接中...'
        default: return '未连接'
      }
    }
  },
  mounted() {
    this.connectWebSocket()
  },
  beforeUnmount() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
    }
  },
  activated() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      this.connectWebSocket()
    }
  },
  deactivated() {
    // 被缓存时，不做任何操作，保持连接
  },
  methods: {
    connectWebSocket() {
      if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
        return
      }
      
      this.status = 'connecting'
      
      // 构建 WebSocket URL
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      // 在开发环境下，Vue CLI 默认代理并不处理 ws，但为了简便先假设同源
      // 注意：实际应用中可能需要根据环境配置 WS URL
      let host = window.location.host
      if (process.env.NODE_ENV === 'development') {
        // 开发模式下假设 Agent 跑在 8080 端口（根据实际情况调整）
        // 这里提供一个 fallback
        host = 'localhost:8080'
      }
      const wsUrl = `${protocol}//${host}/ws/log`
      
      try {
        this.ws = new WebSocket(wsUrl)
        
        this.ws.onopen = () => {
          this.status = 'connected'
          this.appendLog('[System] WebSocket 连接成功')
        }
        
        this.ws.onmessage = (event) => {
          console.log('[Luna] Received WS message:', event.data)
          this.appendLog(event.data)
        }
        
        this.ws.onclose = () => {
          this.status = 'disconnected'
          this.appendLog('[System] WebSocket 连接断开')
          this.reconnectTimer = setTimeout(this.connectWebSocket, 5000)
        }
        
        this.ws.onerror = (error) => {
          this.status = 'disconnected'
          console.error('WebSocket Error:', error)
        }
      } catch (e) {
        this.status = 'disconnected'
        this.appendLog(`[System] WebSocket 连接失败: ${e.message}`)
      }
    },
    appendLog(message) {
      let logObj = { type: 'TEXT', text: message }

      let parsed = null
      try {
        parsed = JSON.parse(message)
      } catch (e) {
        console.warn('[Luna] JSON parse failed:', e.message, 'raw:', message.substring(0, 100))
        parsed = null
      }

      if (parsed && parsed.type) {
        const handler = messageHandlers[parsed.type]
        logObj = handler ? handler(parsed) : { type: parsed.type, text: parsed.payload || message, timestamp: parsed.timestamp }
      } else {
        console.warn('[Luna] No parsed.type, raw type:', typeof message, 'value:', message.substring(0, 80))
      }

      this.logs.push(logObj)
      if (this.logs.length > this.maxLogs) {
        this.logs.shift()
      }
      this.scrollToBottom()
    },
    openDebugger(snapshot) {
      // ProbeMessage.toJson() 将快照数据嵌套在 payload 字段中
      // DebuggerPanel 期望直接访问 localVars/stackTrace/threadName 等
      this.currentSnapshot = snapshot.payload || snapshot
      this.debuggerVisible = true
    },
    formatTime(timestamp) {
      if (!timestamp) return ''
      const d = new Date(timestamp)
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      const s = String(d.getSeconds()).padStart(2, '0')
      const ms = String(d.getMilliseconds()).padStart(3, '0')
      return `${h}:${m}:${s}.${ms}`
    },
    clearLogs() {
      this.logs = []
    },
    toggleAutoScroll() {
      this.autoScroll = !this.autoScroll
      if (this.autoScroll) {
        this.scrollToBottom()
      }
    },
    scrollToBottom() {
      if (!this.autoScroll) return
      
      this.$nextTick(() => {
        const container = this.$refs.logContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    handleScroll() {
      const container = this.$refs.logContainer
      if (!container) return
      
      // 如果用户向上滚动，则暂停自动滚动
      const isAtBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 10
      this.autoScroll = isAtBottom
    },
    getTypeConfig(type) {
      return typeConfig[type] || {}
    },
    getLineClass(type) {
      const config = typeConfig[type]
      return config && config.lineClass ? { [config.lineClass]: true } : {}
    }
  }
}
</script>

<style scoped>
.log-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-primary);
  color: var(--text-primary);
}

.log-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.toolbar-left, .toolbar-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.tool-btn {
  background: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tool-btn:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-primary);
  border-color: var(--text-secondary);
}

.tool-btn.active {
  background: rgba(0, 122, 204, 0.2);
  color: var(--accent-primary);
  border-color: var(--accent-primary);
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--text-tertiary);
}

.status-connected .status-dot {
  background-color: var(--accent-success);
  box-shadow: 0 0 4px var(--accent-success);
}

.status-connecting .status-dot {
  background-color: #e3c322;
  animation: pulse 1s infinite alternate;
}

.status-disconnected .status-dot {
  background-color: var(--accent-danger);
}

@keyframes pulse {
  from { opacity: 0.5; }
  to { opacity: 1; }
}

.log-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.6;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-tertiary);
  opacity: 0.7;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.log-line {
  padding: 6px 0;
  word-break: break-all;
  white-space: pre-wrap;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.02);
}

.log-line:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.log-tag {
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 2px;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 2px;
}

.tag-debug {
  background: linear-gradient(135deg, #f14c4c 0%, #b91c1c 100%);
  color: white;
  box-shadow: 0 0 8px rgba(241, 76, 76, 0.3);
}

.tag-log {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: white;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.3);
}

.log-time {
  color: var(--text-tertiary);
  font-size: 11px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

.log-text {
  color: var(--text-primary);
}

.snapshot-line {
  background-color: rgba(241, 76, 76, 0.05);
}

.snapshot-link {
  background: rgba(99, 102, 241, 0.1);
  border: 1px solid rgba(99, 102, 241, 0.2);
  color: #fff;
  padding: 4px 14px;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all var(--transition-normal);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.snapshot-link:hover {
  background: rgba(99, 102, 241, 0.2);
  border-color: var(--accent-primary);
  transform: translateX(4px);
  box-shadow: 0 0 12px rgba(99, 102, 241, 0.3);
}

.snapshot-link i {
  color: var(--accent-primary);
  font-size: 12px;
}

.trace-line {
  background-color: rgba(234, 179, 8, 0.05);
}

.tag-trace {
  background: linear-gradient(135deg, #eab308 0%, #a16207 100%);
  color: white;
  box-shadow: 0 0 8px rgba(234, 179, 8, 0.3);
}

.trace-content {
  color: #eab308;
}

.tag-invocation {
  background: linear-gradient(135deg, #10b981 0%, #047857 100%);
  color: white;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.3);
}

.invocation-line {
  background-color: rgba(16, 185, 129, 0.03);
}

.invocation-content {
  flex: 1;
  min-width: 0;
}
</style>
