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
      selectedThread: null
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
</style>
