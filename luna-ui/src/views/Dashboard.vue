<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div class="header-info">
        <i class="fas fa-tachometer-alt"></i>
        <h2>JVM 实时监控大盘</h2>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="fetchMetrics" :disabled="loading">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i> 立即刷新
        </button>
        <div class="auto-refresh">
          <input type="checkbox" id="auto-refresh-check" v-model="autoRefresh" />
          <label for="auto-refresh-check">自动刷新 (5s)</label>
        </div>
      </div>
    </div>

    <div v-if="metrics" class="dashboard-grid">
      <!-- 内存面板 -->
      <div class="stat-card">
        <div class="card-title">
          <i class="fas fa-memory"></i> 内存占用 (Heap)
        </div>
        <div class="card-content">
          <div class="memory-bar-container">
            <div class="memory-bar">
              <div 
                class="memory-bar-fill" 
                :style="{ width: heapPercentage + '%' }"
                :class="getHeapColor(heapPercentage)"
              ></div>
            </div>
            <div class="memory-labels">
              <span>{{ formatMB(metrics.memory.heapUsed) }} MB Used</span>
              <span>{{ formatMB(metrics.memory.heapMax) }} MB Max</span>
            </div>
          </div>
          <div class="small-stats">
            <div class="small-stat">
              <span class="label">Non-Heap:</span>
              <span class="value">{{ formatMB(metrics.memory.nonHeapUsed) }} MB</span>
            </div>
          </div>
        </div>
      </div>

      <!-- GC 面板 -->
      <div class="stat-card">
        <div class="card-title">
          <i class="fas fa-broom"></i> 垃圾回收 (GC)
        </div>
        <div class="card-content">
          <div class="gc-list">
            <div v-for="gc in metrics.gc" :key="gc.name" class="gc-item">
              <div class="gc-name">{{ gc.name }}</div>
              <div class="gc-stats">
                <span class="gc-count">{{ gc.count }} 次</span>
                <span class="gc-time">{{ gc.time }} ms</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 线程面板 -->
      <div class="stat-card">
        <div class="card-title">
          <i class="fas fa-threads"></i> 线程状态
        </div>
        <div class="card-content">
          <div class="thread-stats">
            <div class="thread-main-stat">
              <span class="big-value">{{ metrics.threads.count }}</span>
              <span class="big-label">当前活跃线程</span>
            </div>
            <div class="thread-details">
              <div class="detail-item">
                <span class="label">Daemon:</span>
                <span class="value">{{ metrics.threads.daemonCount }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Peak:</span>
                <span class="value">{{ metrics.threads.peakCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 类加载面板 -->
      <div class="stat-card">
        <div class="card-title">
          <i class="fas fa-boxes"></i> 类加载统计
        </div>
        <div class="card-content">
          <div class="class-stats">
            <div class="class-stat-item">
              <span class="label">当前加载:</span>
              <span class="value">{{ metrics.classes.loadedCount }}</span>
            </div>
            <div class="class-stat-item">
              <span class="label">累计加载:</span>
              <span class="value">{{ metrics.classes.totalLoadedCount }}</span>
            </div>
            <div class="class-stat-item">
              <span class="label">卸载数量:</span>
              <span class="value">{{ metrics.classes.unloadedCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>正在采集 JVM 数据...</p>
    </div>
    
    <div v-else class="error-state">
      <i class="fas fa-exclamation-circle"></i>
      <p>无法连接到 Agent 数据接口，请检查 Agent 是否运行正常</p>
    </div>
  </div>
</template>

<script>
import { getJvmMetrics } from '../utils/api'

export default {
  name: 'Dashboard',
  data() {
    return {
      metrics: null,
      loading: false,
      autoRefresh: true,
      timer: null
    }
  },
  computed: {
    heapPercentage() {
      if (!this.metrics) return 0
      const { heapUsed, heapMax } = this.metrics.memory
      if (heapMax <= 0) return 0
      return Math.round((heapUsed / heapMax) * 100)
    }
  },
  mounted() {
    this.fetchMetrics()
    this.timer = setInterval(() => {
      if (this.autoRefresh) {
        this.fetchMetrics()
      }
    }, 5000)
  },
  beforeUnmount() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  },
  methods: {
    async fetchMetrics() {
      this.loading = true
      try {
        const data = await getJvmMetrics()
        this.metrics = data
      } catch (e) {
        console.error('Fetch metrics failed:', e)
      } finally {
        this.loading = false
      }
    },
    formatMB(bytes) {
      return Math.round(bytes / 1024 / 1024)
    },
    getHeapColor(percent) {
      if (percent > 85) return 'bar-danger'
      if (percent > 65) return 'bar-warning'
      return 'bar-success'
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 24px;
  background-color: var(--bg-primary);
  height: 100%;
  overflow-y: auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-info h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.header-info i {
  color: var(--accent-primary);
  font-size: 24px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.refresh-btn {
  background-color: var(--bg-hover);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.refresh-btn:hover:not(:disabled) {
  background-color: var(--border-color);
}

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.stat-card {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title i {
  color: var(--accent-primary);
}

.memory-bar-container {
  margin-bottom: 16px;
}

.memory-bar {
  height: 12px;
  background-color: #333;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.memory-bar-fill {
  height: 100%;
  transition: width 0.5s ease;
}

.bar-success { background-color: var(--accent-success); }
.bar-warning { background-color: #e3c322; }
.bar-danger { background-color: var(--accent-danger); }

.memory-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-tertiary);
}

.small-stats {
  border-top: 1px solid var(--border-color);
  padding-top: 12px;
}

.small-stat {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.gc-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gc-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.gc-name {
  font-size: 13px;
  color: var(--text-primary);
}

.gc-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
}

.gc-count {
  color: var(--accent-primary);
  font-weight: 600;
}

.gc-time {
  color: var(--text-tertiary);
}

.thread-stats {
  display: flex;
  align-items: center;
  gap: 32px;
}

.thread-main-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.big-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--accent-primary);
}

.big-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.thread-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.class-stats {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.class-stat-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 4px 0;
}

.class-stat-item .label {
  color: var(--text-secondary);
}

.class-stat-item .value {
  font-weight: 600;
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--text-tertiary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-state i {
  font-size: 40px;
  color: var(--accent-danger);
  margin-bottom: 16px;
}
</style>
