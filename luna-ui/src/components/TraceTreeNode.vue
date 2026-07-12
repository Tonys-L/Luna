<template>
  <div class="trace-node">
    <div class="node-header" @click="toggleDetail">
      <span class="node-indent" :style="{ width: depth * 20 + 'px' }"></span>
      <span class="toggle-icon" v-if="hasChildren" @click.stop="toggleChildren">
        <i :class="childrenExpanded ? 'fas fa-chevron-down' : 'fas fa-chevron-right'"></i>
      </span>
      <span class="toggle-spacer" v-else></span>

      <span class="method-name">{{ simpleClassName }}.{{ node.methodName }}</span>

      <span class="duration-badge" :class="durationClass">{{ node.durationMs }}ms</span>

      <span class="args-summary" v-if="argsSummary">{{ argsSummary }}</span>
      <span class="arrow-icon" v-if="argsSummary && returnSummary">→</span>
      <span class="return-summary" v-if="returnSummary">{{ returnSummary }}</span>

      <span class="exception-badge" v-if="node.threwException" :title="node.exceptionMessage">
        <i class="fas fa-exclamation-triangle"></i>
      </span>
    </div>

    <div v-if="detailExpanded" class="node-detail">
      <div class="detail-row" v-if="node.args">
        <span class="detail-label">Args:</span>
        <span class="detail-value">{{ node.args }}</span>
      </div>
      <div class="detail-row" v-if="node.returnValue != null">
        <span class="detail-label">Return:</span>
        <span class="detail-value">{{ node.returnValue }}</span>
      </div>
      <div class="detail-row exception-detail" v-if="node.threwException">
        <span class="detail-label">Exception:</span>
        <span class="detail-value">{{ node.exceptionMessage || '(no message)' }}</span>
      </div>
    </div>

    <div v-if="childrenExpanded && hasChildren" class="node-children">
      <TraceTreeNode
        v-for="child in node.children"
        :key="child.spanId"
        :node="child"
        :depth="depth + 1"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'TraceTreeNode',
  props: {
    node: {
      type: Object,
      required: true
    },
    depth: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      childrenExpanded: true,
      detailExpanded: false
    }
  },
  computed: {
    hasChildren() {
      return this.node.children && this.node.children.length > 0
    },
    simpleClassName() {
      if (!this.node.className) return ''
      const parts = this.node.className.split('.')
      return parts[parts.length - 1]
    },
    durationClass() {
      const ms = this.node.durationMs
      if (ms == null) return ''
      if (ms < 50) return 'dur-fast'
      if (ms <= 200) return 'dur-normal'
      return 'dur-slow'
    },
    argsSummary() {
      return this.truncate(this.node.args, 40)
    },
    returnSummary() {
      return this.truncate(this.node.returnValue, 40)
    }
  },
  methods: {
    toggleDetail() {
      this.detailExpanded = !this.detailExpanded
    },
    toggleChildren() {
      this.childrenExpanded = !this.childrenExpanded
    },
    truncate(val, maxLen) {
      if (val == null || val === '') return ''
      const str = String(val)
      if (str.length <= maxLen) return str
      return str.substring(0, maxLen) + '...'
    }
  }
}
</script>

<style scoped>
.trace-node {
  font-family: var(--font-mono);
  font-size: 12px;
}

.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 3px 8px;
  cursor: pointer;
  transition: background-color var(--transition-fast);
  white-space: nowrap;
}

.node-header:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.node-indent {
  flex-shrink: 0;
}

.toggle-icon {
  width: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  font-size: 9px;
  flex-shrink: 0;
  cursor: pointer;
}

.toggle-spacer {
  width: 14px;
  flex-shrink: 0;
}

.method-name {
  color: #9cdcfe;
  font-weight: 500;
  flex-shrink: 0;
}

.duration-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 0 5px;
  border-radius: 8px;
  flex-shrink: 0;
  line-height: 16px;
}

.dur-fast {
  color: #10b981;
  background: rgba(16, 185, 129, 0.12);
}

.dur-normal {
  color: #eab308;
  background: rgba(234, 179, 8, 0.12);
}

.dur-slow {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.12);
}

.args-summary {
  color: #ce9178;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.arrow-icon {
  color: var(--text-tertiary);
  font-size: 10px;
  flex-shrink: 0;
}

.return-summary {
  color: #b5cea8;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.exception-badge {
  color: #ef4444;
  font-size: 11px;
  flex-shrink: 0;
  cursor: help;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.node-detail {
  margin: 2px 0 2px 22px;
  padding: 6px 10px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  font-size: 11px;
}

.detail-row {
  display: flex;
  gap: 8px;
  padding: 2px 0;
  word-break: break-all;
  white-space: pre-wrap;
}

.detail-label {
  color: var(--text-tertiary);
  flex-shrink: 0;
  min-width: 60px;
  font-weight: 600;
}

.detail-value {
  color: var(--text-secondary);
}

.exception-detail .detail-label {
  color: #ef4444;
}

.exception-detail .detail-value {
  color: #f87171;
}

.node-children {
  border-left: 1px solid rgba(255, 255, 255, 0.06);
  margin-left: 22px;
}
</style>
