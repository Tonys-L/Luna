<template>
  <div v-if="visible && marker" class="injection-detail-overlay" @click.self="$emit('close')">
    <div class="injection-detail-panel">
      <div class="injection-detail-header">
        <span class="injection-detail-type" :class="'type-' + marker.type.toLowerCase().replace('_', '-')">
          {{ getLabel(marker.type) }}
        </span>
        <button class="injection-detail-close" @click="$emit('close')">&times;</button>
      </div>
      <div class="injection-detail-body">
        <div class="injection-detail-row">
          <span class="injection-detail-label">Method:</span>
          <span class="injection-detail-value">{{ marker.method }}</span>
        </div>
        <div v-if="marker.lineNumber" class="injection-detail-row">
          <span class="injection-detail-label">Line:</span>
          <span class="injection-detail-value">{{ marker.lineNumber }}</span>
        </div>
        <div class="injection-detail-row">
          <span class="injection-detail-label">Condition:</span>
          <code class="injection-detail-code">{{ parseCondition(marker.code) || '无条件 (Always)' }}</code>
        </div>
        <div class="injection-detail-row">
          <span class="injection-detail-label">Log:</span>
          <code class="injection-detail-code">{{ parseLog(marker.code) }}</code>
        </div>
      </div>
      <div class="injection-detail-footer">
        <button class="injection-detail-delete" @click="$emit('delete', marker)">
          Delete Injection
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'InjectionDetailOverlay',
  props: {
    visible: Boolean,
    marker: Object
  },
  emits: ['close', 'delete'],
  methods: {
    getLabel(type) {
      const labels = {
        'ENTER_METHOD': 'BEFORE METHOD',
        'EXIT_METHOD': 'AFTER METHOD',
        'AROUND_METHOD': 'AROUND METHOD',
        'LINE_BEFORE': 'BEFORE LINE',
        'LINE_AFTER': 'AFTER LINE'
      }
      return labels[type] || type
    },
    parseCondition(code) {
      try {
        const json = JSON.parse(code)
        return json.condition
      } catch (e) { return null }
    },
    parseLog(code) {
      try {
        const json = JSON.parse(code)
        return json.logContent
      } catch (e) { return code }
    }
  }
}
</script>

<style scoped>
.injection-detail-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.2);
}

.injection-detail-panel {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 320px;
  background: #2d2d2d;
  border: 1px solid #444;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.5);
  overflow: hidden;
}

.injection-detail-header {
  padding: 12px 16px;
  background: #383838;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #444;
}

.injection-detail-type {
  font-size: 10px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 10px;
  color: white;
}

.type-enter-method { background: #10b981; }
.type-exit-method { background: #3b82f6; }
.type-line-before { background: #6366f1; }

.injection-detail-close {
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  font-size: 20px;
}

.injection-detail-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.injection-detail-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.injection-detail-label {
  font-size: 10px;
  font-weight: 700;
  color: #666;
  text-transform: uppercase;
}

.injection-detail-value {
  font-size: 12px;
  color: #ccc;
}

.injection-detail-code {
  font-size: 11px;
  background: #1e1e1e;
  padding: 6px;
  border-radius: 4px;
  color: #9cdcfe;
  word-break: break-all;
  font-family: var(--font-mono);
}

.injection-detail-footer {
  padding: 12px 16px;
  border-top: 1px solid #444;
}

.injection-detail-delete {
  width: 100%;
  padding: 8px;
  background: #442222;
  border: 1px solid #663333;
  color: #ff8888;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
}

.injection-detail-delete:hover {
  background: #663333;
}
</style>
