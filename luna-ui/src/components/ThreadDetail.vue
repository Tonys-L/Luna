<template>
  <div class="thread-detail-panel">
    <div v-if="thread" class="detail-container">
      <div class="detail-header">
        <h3>{{ thread.name }}</h3>
        <span class="state-badge" :class="getStateClass(thread.state)">{{ thread.state }}</span>
      </div>
      <div class="detail-info">
        <div v-if="thread.lockName" class="info-row">
          <span class="label">Waiting on:</span>
          <span class="value">{{ thread.lockName }}</span>
        </div>
        <div v-if="thread.lockOwnerName" class="info-row">
          <span class="label">Owned by:</span>
          <span class="value">{{ thread.lockOwnerName }}</span>
        </div>
      </div>
      <div class="stack-trace-header">Stack Trace:</div>
      <pre class="stack-trace">{{ thread.stackTrace }}</pre>
    </div>
    <div v-else class="detail-placeholder">
      <i class="fas fa-mouse-pointer"></i>
      <p>请在左侧选择一个线程查看详情</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ThreadDetail',
  props: {
    thread: { type: Object, default: null }
  },
  methods: {
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

.state-runnable { background-color: var(--accent-success); }
.state-waiting { background-color: #e3c322; }
.state-blocked { background-color: var(--accent-danger); }

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
