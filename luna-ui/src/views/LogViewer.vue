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
        :class="{ 'snapshot-line': log.type === 'SNAPSHOT' }"
      >
        <template v-if="log.type === 'SNAPSHOT'">
          <span class="log-tag tag-debug">DEBUG</span>
          <span class="log-content snapshot-link" @click="openDebugger(log.data)">
            <i class="fas fa-bug"></i> 触发虚拟断点快照: {{ log.data.pointId }} (点击查看详情)
          </span>
        </template>
        <template v-else>
          <span class="log-content">{{ log.text }}</span>
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

export default {
  name: 'LogViewer',
  components: {
    DebuggerPanel
  },
  data() {
    return {
      logs: [],
      autoScroll: true,
      maxLogs: 1000, // 最大保留日志行数
      ws: null,
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
    if (this.ws) {
      this.ws.close()
    }
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
          // 自动重连逻辑可在此添加
          setTimeout(this.connectWebSocket, 5000)
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
      console.log('[Luna] Appending log:', message)
      let logObj = { type: 'TEXT', text: message }
      
      // 尝试解析为快照数据
      if (message.startsWith('{') && message.includes('"type":"SNAPSHOT"')) {
        try {
          const data = JSON.parse(message)
          if (data.type === 'SNAPSHOT') {
            logObj = { type: 'SNAPSHOT', data: data }
          }
        } catch (e) {
          // 如果不是合法的 JSON，按普通文本处理
        }
      }

      this.logs.push(logObj)
      if (this.logs.length > this.maxLogs) {
        this.logs.shift()
      }
      this.scrollToBottom()
    },
    openDebugger(snapshot) {
      this.currentSnapshot = snapshot
      this.debuggerVisible = true
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
  padding: 8px 16px;
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
  background: var(--bg-hover);
  color: var(--text-primary);
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
  padding: 12px;
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.5;
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
  padding: 2px 0;
  word-break: break-all;
  white-space: pre-wrap;
  display: flex;
  align-items: flex-start;
  gap: 8px;
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
  background-color: #f14c4c;
  color: white;
}

.snapshot-line {
  background-color: rgba(241, 76, 76, 0.05);
}

.snapshot-link {
  color: #4fc1ff;
  text-decoration: underline;
  cursor: pointer;
  font-weight: 500;
}

.snapshot-link:hover {
  color: #9cdcfe;
}

.snapshot-link i {
  font-size: 11px;
  margin-right: 4px;
}
</style>
