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
      >
        <span class="log-content">{{ log }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LogViewer',
  data() {
    return {
      logs: [],
      autoScroll: true,
      maxLogs: 1000, // 最大保留日志行数
      ws: null,
      status: 'disconnected' // connected, disconnected, connecting
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
      this.logs.push(message)
      if (this.logs.length > this.maxLogs) {
        this.logs.shift()
      }
      this.scrollToBottom()
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
}

.log-line:hover {
  background-color: rgba(255, 255, 255, 0.05);
}
</style>
