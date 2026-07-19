<template>
  <div v-if="visible && marker" class="injection-detail-overlay" @click.self="$emit('close')">
    <div class="injection-detail-panel">
      <div class="injection-detail-header">
        <span class="injection-detail-type" :style="getTypeStyle(marker.injectionLocation)">
          {{ getLabel(marker.injectionLocation, marker.probeType) }}
        </span>
        <span v-if="marker.ephemeral !== undefined" class="injection-detail-badge" :class="marker.ephemeral ? 'badge-ephemeral' : 'badge-persistent'">
          {{ marker.ephemeral ? '临时' : '持久' }}
        </span>
        <button class="injection-detail-close" @click="$emit('close')">&times;</button>
      </div>
      <div class="injection-detail-body">
        <div class="injection-detail-row">
          <span class="injection-detail-label">Probe:</span>
          <span class="injection-detail-value">{{ getProbeDisplayName(marker.probeType) }}</span>
        </div>
        <div class="injection-detail-row">
          <span class="injection-detail-label">Method:</span>
          <span class="injection-detail-value">{{ marker.method }}</span>
        </div>
        <div v-if="marker.lineNumber" class="injection-detail-row">
          <span class="injection-detail-label">Line:</span>
          <span class="injection-detail-value">{{ marker.lineNumber }}</span>
        </div>
        <div class="injection-detail-row">
          <span class="injection-detail-label">Code:</span>
          <code class="injection-detail-code">{{ getCodeDisplay(marker.code, marker.probeType) }}</code>
        </div>
      </div>
      <div class="injection-detail-footer">
        <button class="injection-detail-delete" @click="handleDelete">
          Delete Injection
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { pluginRegistry } from '../utils/plugin-registry'

export default {
  name: 'InjectionDetailOverlay',
  props: {
    visible: Boolean,
    marker: Object
  },
  emits: ['close', 'delete'],
  methods: {
    getProbeDisplayName(probeType) {
      const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === probeType)
      return handler?.displayName || probeType || ''
    },
    getTypeStyle(injectionLocation) {
      const type = pluginRegistry.injectionTypes?.find(t => t.name === injectionLocation)
      const color = type?.color || '#6b7280'
      return { background: color }
    },
    getLabel(loc, probeType) {
      if (!loc) return 'UNKNOWN'
      const injectionType = pluginRegistry.injectionTypes?.find(t => t.name === loc)
      return injectionType?.displayName || loc.toUpperCase()
    },
    getCodeDisplay(code, probeType) {
      if (!code) return '-'
      const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === probeType)
      // 如果探针不使用代码（如 TRACE），显示 configSchema 的 label
      if (handler && !handler.usesCode) {
        const schema = handler.configSchema || []
        const codeField = schema.find(f => f.key === 'code')
        if (codeField) {
          return code === '0' ? '全部输出' : `≥ ${code} ms`
        }
      }
      return code
    },
    handleDelete() {
      if (!confirm('Delete this injection?')) return
      this.$emit('delete', this.marker)
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
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #444;
}

.injection-detail-type {
  font-size: 10px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 10px;
  color: white;
}

.injection-detail-badge {
  font-size: 9px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 8px;
  color: white;
}

.badge-ephemeral { background: #6b7280; }
.badge-persistent { background: #059669; }

.injection-detail-close {
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  font-size: 20px;
  margin-left: auto;
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
