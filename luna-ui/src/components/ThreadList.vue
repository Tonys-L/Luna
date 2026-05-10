<template>
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
        :class="{ active: modelValue?.id === thread.id, 'is-deadlocked': isDeadlocked(thread.id) }"
        @click="$emit('update:modelValue', thread)"
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
</template>

<script>
export default {
  name: 'ThreadList',
  props: {
    threads: { type: Array, default: () => [] },
    deadlockedIds: { type: Array, default: () => [] },
    modelValue: { type: Object, default: null }
  },
  emits: ['update:modelValue'],
  data() {
    return {
      searchQuery: ''
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
  methods: {
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
</style>
