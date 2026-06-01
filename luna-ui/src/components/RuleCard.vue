<template>
  <div 
    :class="['rule-card', { active: active, disabled: !rule.enabled }]"
    @click="$emit('select', rule)"
  >
    <div class="rule-card-header">
      <span class="rule-id">#{{ String(rule.id || '').slice(0, 4) }}</span>
      <div :class="['rule-type-tag', (rule.injectionLocation || '').toLowerCase()]">
        {{ formatInjectionTypeShort(rule.injectionLocation) }}
      </div>
      <div class="status-dot" :class="{ active: rule.enabled }"></div>
    </div>
    <div class="rule-card-body">
      <div class="target-info">
        <i class="fas fa-cube"></i>
        <span class="class-name">{{ getSimpleClassName(rule.targetClass) }}</span>
      </div>
      <div class="target-info">
        <i class="fas fa-code-branch"></i>
        <span class="method-name">{{ rule.targetMethod }}</span>
      </div>
    </div>
    <div class="rule-card-footer">
      <span class="code-type-indicator" :class="probeTypeClass">
        {{ probeTypeDisplay }}
      </span>
      <div class="card-actions">
        <button class="card-action-btn delete" @click.stop="$emit('delete', rule.id)">
          <i class="fas fa-trash-alt"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RuleCard',
  props: {
    rule: { type: Object, required: true },
    active: { type: Boolean, default: false }
  },
  emits: ['select', 'delete'],
  computed: {
    probeTypeClass() {
      const pt = (this.rule.probeType || '').toUpperCase()
      if (pt === 'SNAPSHOT') return 'SNAPSHOT'
      if (pt === 'TRACE') return 'TRACE'
      return 'EXPRESSION'
    },
    probeTypeDisplay() {
      const pt = (this.rule.probeType || '').toUpperCase()
      if (pt === 'SNAPSHOT') return '📷 快照'
      if (pt === 'TRACE') return '⏱ 耗时'
      return '📝 日志'
    }
  },
  methods: {
    formatInjectionTypeShort(type) {
      const types = {
        'METHOD_ENTER': 'Enter',
        'METHOD_EXIT': 'Exit',
        'METHOD_AROUND': 'Around',
        'LINE_BEFORE': 'Line',
        'LINE_AFTER': 'Line',
        'method_enter': 'Enter',
        'method_exit': 'Exit',
        'method_around': 'Around',
        'line_before': 'Line',
        'line_after': 'Line'
      }
      return types[type] || type
    },
    getSimpleClassName(className) {
      if (!className) return 'Any Class'
      const parts = className.split('.')
      return parts[parts.length - 1]
    }
  }
}
</script>

<style scoped>
.rule-card {
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}

.rule-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background-color: transparent;
  transition: background-color 0.2s;
}

.rule-card:hover {
  background-color: var(--bg-hover);
  transform: translateX(4px);
}

.rule-card.active {
  background-color: rgba(99, 102, 241, 0.08);
  border-color: var(--accent-primary);
}

.rule-card.active::before {
  background-color: var(--accent-primary);
}

.rule-card.disabled {
  opacity: 0.6;
  filter: grayscale(0.5);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4b5563;
  margin-left: 8px;
  transition: all 0.3s ease;
}

.status-dot.active {
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.rule-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.rule-id {
  font-size: 11px;
  color: var(--text-tertiary);
  font-weight: 700;
}

.rule-type-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background-color: rgba(255, 255, 255, 0.05);
  color: var(--text-secondary);
  text-transform: uppercase;
}

.rule-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.target-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.target-info i {
  width: 14px;
  color: var(--text-tertiary);
  font-size: 11px;
}

.class-name {
  color: var(--text-primary);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.method-name {
  color: var(--text-secondary);
}

.rule-card-footer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.code-type-indicator {
  font-size: 10px;
  font-weight: 600;
}

.code-type-indicator.SNAPSHOT { color: #ec4899; }
.code-type-indicator.EXPRESSION { color: #10b981; }
.code-type-indicator.TRACE { color: #f59e0b; }

.card-action-btn.delete {
  color: var(--text-tertiary);
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
}

.card-action-btn.delete:hover {
  color: #ef4444;
}
</style>
