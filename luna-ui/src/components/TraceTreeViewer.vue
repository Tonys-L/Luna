<template>
  <div class="trace-tree-viewer">
    <div class="trace-header" @click="expanded = !expanded">
      <span class="toggle-icon">
        <i :class="expanded ? 'fas fa-chevron-down' : 'fas fa-chevron-right'"></i>
      </span>
      <span class="trace-icon"><i class="fas fa-project-diagram"></i></span>
      <span class="trace-root-name">{{ rootLabel }}</span>
      <span class="trace-duration" :class="durationClass(trace.totalDurationMs)">{{ trace.totalDurationMs }}ms</span>
      <span class="trace-id">#{{ shortId }}</span>
      <span class="trace-time" v-if="timestamp">{{ formatTime(timestamp) }}</span>
    </div>
    <div v-if="expanded" class="trace-tree">
      <TraceTreeNode
        :node="rootNode"
        :depth="0"
      />
    </div>
  </div>
</template>

<script>
import TraceTreeNode from './TraceTreeNode.vue'

export default {
  name: 'TraceTreeViewer',
  components: { TraceTreeNode },
  props: {
    trace: {
      type: Object,
      required: true
    },
    timestamp: {
      type: Number,
      default: null
    }
  },
  data() {
    return {
      expanded: false
    }
  },
  computed: {
    shortId() {
      const id = this.trace.traceId || ''
      return id.length > 8 ? id.substring(0, 8) : id
    },
    rootLabel() {
      const root = this.trace.rootClassName
        ? this.getSimpleName(this.trace.rootClassName) + '.' + this.trace.rootMethodName
        : 'Unknown'
      return root
    },
    rootNode() {
      return this.buildTree(this.trace.spans || [])
    }
  },
  methods: {
    buildTree(spans) {
      const spanMap = {}
      const rootSpan = { children: [] }

      spans.forEach(span => {
        spanMap[span.spanId] = { ...span, children: [] }
      })

      spans.forEach(span => {
        const node = spanMap[span.spanId]
        if (span.parentSpanId && spanMap[span.parentSpanId]) {
          spanMap[span.parentSpanId].children.push(node)
        } else {
          rootSpan.children.push(node)
        }
      })

      if (rootSpan.children.length === 1) {
        return rootSpan.children[0]
      }
      return rootSpan
    },
    durationClass(ms) {
      if (ms == null) return ''
      if (ms < 50) return 'dur-fast'
      if (ms <= 200) return 'dur-normal'
      return 'dur-slow'
    },
    getSimpleName(className) {
      if (!className) return ''
      const parts = className.split('.')
      return parts[parts.length - 1]
    },
    formatTime(ts) {
      if (!ts) return ''
      const d = new Date(ts)
      const h = String(d.getHours()).padStart(2, '0')
      const m = String(d.getMinutes()).padStart(2, '0')
      const s = String(d.getSeconds()).padStart(2, '0')
      const ms = String(d.getMilliseconds()).padStart(3, '0')
      return `${h}:${m}:${s}.${ms}`
    }
  }
}
</script>

<style scoped>
.trace-tree-viewer {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  margin: 6px 0;
  overflow: hidden;
  background-color: var(--bg-secondary);
}

.trace-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  background-color: var(--bg-tertiary);
  transition: background-color var(--transition-fast);
  user-select: none;
}

.trace-header:hover {
  background-color: var(--bg-hover);
}

.toggle-icon {
  width: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
  font-size: 10px;
  flex-shrink: 0;
}

.trace-icon {
  color: #10b981;
  font-size: 12px;
  flex-shrink: 0;
}

.trace-root-name {
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
}

.trace-duration {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
  flex-shrink: 0;
}

.dur-fast {
  color: #10b981;
  background: rgba(16, 185, 129, 0.15);
}

.dur-normal {
  color: #eab308;
  background: rgba(234, 179, 8, 0.15);
}

.dur-slow {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.15);
}

.trace-id {
  color: var(--text-tertiary);
  font-size: 10px;
  font-family: var(--font-mono);
  flex-shrink: 0;
}

.trace-time {
  color: var(--text-tertiary);
  font-size: 10px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  margin-left: auto;
  flex-shrink: 0;
}

.trace-tree {
  padding: 4px 0;
  border-top: 1px solid var(--border-color);
}
</style>
