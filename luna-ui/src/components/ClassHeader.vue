<template>
  <div class="slim-class-header">
    <div class="header-left">
      <div class="header-main">
        <div class="class-icon">
          <i class="fas fa-file-code"></i>
        </div>
        <h2 class="class-name" :title="classInfo.className">{{ classInfo.className }}</h2>
      </div>
      
      <div class="header-meta">
        <div class="meta-item" v-if="classInfo.superClass && classInfo.superClass !== 'java.lang.Object'">
          <span class="meta-label">extends</span>
          <span class="meta-value">{{ classInfo.superClass }}</span>
        </div>

        <div class="meta-item" v-if="classInfo.interfaces && classInfo.interfaces.length > 0">
          <span class="meta-label">implements</span>
          <div class="interface-list">
            <span v-for="iface in classInfo.interfaces" :key="iface" class="iface-tag">
              {{ iface }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <div class="header-actions">
      <div class="injection-counter" v-if="injectionCount > 0" title="Active Injections">
        <i class="fas fa-syringe"></i> {{ injectionCount }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ClassHeader',
  props: {
    classInfo: { type: Object, required: true },
    injectionCount: { type: Number, default: 0 },
    loading: { type: Boolean, default: false }
  }
}
</script>

<style scoped>
.slim-class-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  gap: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.injection-counter {
  font-size: 11px;
  font-weight: 700;
  color: var(--accent-primary);
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 8px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.refresh-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
  border-color: var(--text-tertiary);
}

.refresh-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-spinner-tiny {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255,255,255,0.1);
  border-top-color: var(--text-secondary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
