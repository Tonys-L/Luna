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
        <div class="auto-refresh">
          <input type="checkbox" id="thread-auto-refresh" v-model="autoRefresh" />
          <label for="thread-auto-refresh">自动刷新 (5s)</label>
        </div>
      </div>
    </div>

    <div v-if="deadlockedIds && deadlockedIds.length > 0" class="deadlock-alert">
      <i class="fas fa-exclamation-triangle"></i>
      <span>检测到 {{ deadlockedIds.length }} 个死锁线程！</span>
    </div>

    <!-- 线程状态分布 -->
    <div v-if="stateDistribution.length > 0" class="state-distribution">
      <div class="dist-title">线程状态分布</div>
      <div class="dist-bars">
        <div v-for="item in stateDistribution" :key="item.state" class="dist-item">
          <span class="dist-state" :class="getStateClass(item.state)">{{ item.state }}</span>
          <div class="dist-bar-container">
            <div
              class="dist-bar-fill"
              :style="{ width: item.percent + '%' }"
              :class="getStateClass(item.state)"
            ></div>
          </div>
          <span class="dist-count">{{ item.count }}</span>
        </div>
      </div>
    </div>

    <div class="analyzer-body">
      <ThreadList
        v-model="selectedThread"
        :threads="threads"
        :deadlockedIds="deadlockedIds"
      />
      <ThreadDetail
        :thread="selectedThread"
      />
    </div>
  </div>
</template>

<script>
import { getThreadDump } from '../utils/api'
import ThreadList from '../components/ThreadList.vue'
import ThreadDetail from '../components/ThreadDetail.vue'

export default {
  name: 'ThreadAnalyzer',
  components: {
    ThreadList,
    ThreadDetail
  },
  data() {
    return {
      threads: [],
      deadlockedIds: [],
      loading: false,
      autoRefresh: false,
      timer: null,
      selectedThread: null
    }
  },
  computed: {
    stateDistribution() {
      if (this.threads.length === 0) return []
      const counts = {}
      for (const t of this.threads) {
        counts[t.state] = (counts[t.state] || 0) + 1
      }
      const total = this.threads.length
      const order = ['RUNNABLE', 'BLOCKED', 'WAITING', 'TIMED_WAITING', 'NEW', 'TERMINATED']
      const result = []
      for (const state of order) {
        if (counts[state]) {
          result.push({
            state,
            count: counts[state],
            percent: Math.round((counts[state] / total) * 100)
          })
        }
      }
      for (const state of Object.keys(counts)) {
        if (!order.includes(state)) {
          result.push({
            state,
            count: counts[state],
            percent: Math.round((counts[state] / total) * 100)
          })
        }
      }
      return result
    }
  },
  mounted() {
    this.fetchThreadDump()
    this.timer = setInterval(() => {
      if (this.autoRefresh) {
        this.fetchThreadDump()
      }
    }, 5000)
  },
  beforeUnmount() {
    if (this.timer) {
      clearInterval(this.timer)
    }
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
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

.auto-refresh {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
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

/* 线程状态分布 */
.state-distribution {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.dist-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.dist-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dist-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dist-state {
  font-size: 11px;
  font-weight: 600;
  min-width: 110px;
  text-align: right;
  padding: 2px 6px;
  border-radius: 3px;
  color: var(--text-tertiary);
}

.dist-state.state-runnable { color: var(--accent-success); }
.dist-state.state-waiting { color: #e3c322; }
.dist-state.state-blocked { color: var(--accent-danger); }

.dist-bar-container {
  flex: 1;
  height: 8px;
  background-color: #333;
  border-radius: 4px;
  overflow: hidden;
}

.dist-bar-fill {
  height: 100%;
  transition: width 0.3s ease;
}

.dist-bar-fill.state-runnable { background-color: var(--accent-success); }
.dist-bar-fill.state-waiting { background-color: #e3c322; }
.dist-bar-fill.state-blocked { background-color: var(--accent-danger); }

.dist-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 30px;
  text-align: right;
}

.analyzer-body {
  flex: 1;
  display: flex;
  gap: 20px;
  overflow: hidden;
}
</style>
