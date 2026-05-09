<template>
  <div class="thread-analyzer">
    <div class="analyzer-header">
      <div class="header-info">
        <i class="fas fa-microchip"></i>
        <h2>线程分析与死锁检测</h2>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="fetchThreadDump" :disabled="loading">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i> 获取线程快照
        </button>
      </div>
    </div>

    <div v-if="deadlockedIds && deadlockedIds.length > 0" class="deadlock-alert">
      <i class="fas fa-exclamation-triangle"></i>
      <span>检测到 {{ deadlockedIds.length }} 个死锁线程！</span>
    </div>

    <div class="analyzer-body">
      <div class="thread-list-panel">
        <div class="panel-header">
          <div class="search-box">
            <i class="fas fa-search"></i>
            <input v-model="searchQuery" placeholder="Filter threads..." />
          </div>
          <span class="count-tag">{{ filteredThreads.length }}</span>
        </div>
        <div class="thread-list">
          <div 
            v-for="thread in filteredThreads" 
            :key="thread.id"
            class="thread-item"
            :class="{ active: selectedThread?.id === thread.id, 'is-deadlocked': isDeadlocked(thread.id) }"
            @click="selectedThread = thread"
          >
            <div class="thread-item-header">
              <span class="thread-state-dot" :class="getStateClass(thread.state)"></span>
              <span class="thread-name">{{ thread.name }}</span>
            </div>
            <div class="thread-item-footer">
              <span class="thread-id">ID: {{ thread.id }}</span>
              <span class="thread-state-text">{{ thread.state }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="thread-detail-panel">
        <div v-if="selectedThread" class="detail-container">
          <div class="detail-header">
            <h3>{{ selectedThread.name }}</h3>
            <span class="state-badge" :class="getStateClass(selectedThread.state)">{{ selectedThread.state }}</span>
          </div>
          <div class="detail-info">
            <div v-if="selectedThread.lockName" class="info-row">
              <span class="label">Waiting on:</span>
              <span class="value">{{ selectedThread.lockName }}</span>
            </div>
            <div v-if="selectedThread.lockOwnerName" class="info-row">
              <span class="label">Owned by:</span>
              <span class="value">{{ selectedThread.lockOwnerName }}</span>
            </div>
          </div>
          <div class="stack-trace-header">Stack Trace:</div>
          <pre class="stack-trace">{{ selectedThread.stackTrace }}</pre>
        </div>
        <div v-else class="detail-placeholder">
          <i class="fas fa-mouse-pointer"></i>
          <p>请在左侧选择一个线程查看详情</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getThreadDump } from '../utils/api'

export default {
  name: 'ThreadAnalyzer',
  data() {
    return {
      threads: [],
      deadlockedIds: [],
      loading: false,
      searchQuery: '',
      selectedThread: null
    }
  },
  computed: {
    filteredThreads() {
      if (!this.searchQuery) return this.threads
      const query = this.searchQuery.toLowerCase()
      return this.threads.filter(t => 
        t.name.toLowerCase().includes(query) || 
        String(t.id).includes(query) ||
        t.state.toLowerCase().includes(query)
      )
    }
  },
  mounted() {
    this.fetchThreadDump()
  },
  methods: {
    async fetchThreadDump() {
      this.loading = true
      try {
        const data = await getThreadDump()
        this.threads = data.threads || []
        this.deadlockedIds = data.deadlockedIds || []
        if (this.threads.length > 0 && !this.selectedThread) {
          this.selectedThread = this.threads[0]
        }
      } catch (e) {
        console.error('Fetch thread dump failed:', e)
      } finally {
        this.loading = false
      }
    },
    isDeadlocked(id) {
      return this.deadlockedIds.includes(id)
    },
    getStateClass(state) {
      if (state === 'RUNNABLE') return 'state-runnable'
      if (state === 'WAITING' || state === 'TIMED_WAITING') return 'state-waiting'
      if (state === 'BLOCKED') return 'state-blocked'
      return ''
    }
  }
}
</script>

<style scoped>
.thread-analyzer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-primary);
  padding: 24px;
}

.analyzer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-info h2 {
  font-size: 20px;
  margin: 0;
}

.header-info i {
  color: var(--accent-primary);
  font-size: 24px;
}

.refresh-btn {
  background-color: var(--bg-hover);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.deadlock-alert {
  background-color: rgba(241, 76, 76, 0.1);
  border: 1px solid var(--accent-danger);
  color: var(--accent-danger);
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 600;
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.analyzer-body {
  flex: 1;
  display: flex;
  gap: 20px;
  overflow: hidden;
}

.thread-list-panel {
  width: 320px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 4px 8px;
}

.search-box input {
  background: none;
  border: none;
  color: var(--text-primary);
  font-size: 12px;
  width: 100%;
  outline: none;
}

.count-tag {
  background-color: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  color: var(--text-tertiary);
}

.thread-list {
  flex: 1;
  overflow-y: auto;
}

.thread-item {
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: background-color 0.2s;
}

.thread-item:hover {
  background-color: var(--bg-hover);
}

.thread-item.active {
  background-color: rgba(0, 122, 204, 0.1);
  border-left: 3px solid var(--accent-primary);
}

.thread-item.is-deadlocked {
  background-color: rgba(241, 76, 76, 0.05);
  border-left: 3px solid var(--accent-danger);
}

.thread-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.thread-state-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #888;
}

.state-runnable { background-color: var(--accent-success); }
.state-waiting { background-color: #e3c322; }
.state-blocked { background-color: var(--accent-danger); }

.thread-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.thread-item-footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-tertiary);
}

.thread-detail-panel {
  flex: 1;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-container {
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.detail-header h3 {
  margin: 0;
  font-size: 18px;
}

.state-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 700;
  color: white;
}

.detail-info {
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  gap: 12px;
  font-size: 13px;
}

.info-row .label {
  color: var(--text-tertiary);
  width: 100px;
}

.info-row .value {
  color: var(--accent-danger);
  font-weight: 600;
}

.stack-trace-header {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-secondary);
}

.stack-trace {
  flex: 1;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 16px;
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.6;
  overflow: auto;
  color: var(--text-secondary);
}

.detail-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.detail-placeholder i {
  font-size: 48px;
  margin-bottom: 16px;
}
</style>
