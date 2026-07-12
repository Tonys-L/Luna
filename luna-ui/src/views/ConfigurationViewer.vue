<template>
  <div class="configuration-viewer">
    <!-- 左侧列表 -->
    <div class="rules-sidebar">
      <div class="sidebar-header">
        <div class="title-group">
          <i class="fas fa-thumbtack sidebar-icon"></i>
          <h2>持久注入</h2>
        </div>
      </div>

      <div class="rules-list">
        <div v-for="inj in persistentInjections" :key="inj.id" class="persistent-card">
          <div class="persistent-header">
            <span class="persistent-probe-badge" :style="getProbeBadgeStyle(inj.probeType)">
              {{ getProbeDisplayName(inj.probeType) }}
            </span>
            <button class="persistent-delete" @click="deletePersistentInjection(inj.id)" title="删除">
              <i class="fas fa-trash-alt"></i>
            </button>
          </div>
          <div class="persistent-body">
            <div class="persistent-class">{{ inj.clazz }}</div>
            <div class="persistent-method">
              <i class="fas fa-code"></i> {{ inj.method }}()
              <span v-if="inj.lineNumber" class="persistent-line">:{{ inj.lineNumber }}</span>
            </div>
            <div class="persistent-location">
              <i class="fas fa-map-pin"></i> {{ getLocationDisplayName(inj.injectionLocation) }}
            </div>
            <div v-if="inj.code" class="persistent-code">
              <code>{{ inj.code }}</code>
            </div>
          </div>
        </div>

        <div v-if="persistentInjections.length === 0" class="empty-rules">
          <i class="fas fa-thumbtack"></i>
          <p>暂无持久注入</p>
        </div>
      </div>
    </div>

    <!-- 右侧预览 -->
    <div class="template-preview">
      <div class="preview-placeholder">
        <div class="pulse-logo">
          <i class="fas fa-thumbtack"></i>
        </div>
        <p>Agent 重启后仍然生效的注入点</p>
      </div>
    </div>
  </div>
</template>

<script>
import { getPersistentInjections, removeInjection } from '../utils/api'
import { pluginRegistry } from '../utils/plugin-registry'

export default {
  name: 'ConfigurationViewer',
  data() {
    return {
      persistentInjections: []
    }
  },
  mounted() {
    this.loadPersistentInjections()
  },
  methods: {
    async loadPersistentInjections() {
      try {
        const data = await getPersistentInjections()
        this.persistentInjections = data.injections || []
      } catch (error) {
        console.error('加载持久注入列表失败:', error)
        this.persistentInjections = []
      }
    },
    async deletePersistentInjection(id) {
      if (!confirm('确定删除此持久注入？')) return
      try {
        await removeInjection(id)
        await this.loadPersistentInjections()
      } catch (error) {
        console.error('删除持久注入失败:', error)
      }
    },
    getProbeDisplayName(probeType) {
      const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === probeType)
      return handler?.displayName || probeType || ''
    },
    getProbeBadgeStyle(probeType) {
      const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === probeType)
      const color = handler?.glyphColor || '#6b7280'
      return { background: color }
    },
    getLocationDisplayName(location) {
      const type = pluginRegistry.injectionTypes?.find(t => t.name === location)
      return type?.displayName || location || ''
    }
  }
}
</script>

<style scoped>
.configuration-viewer {
  display: flex;
  height: 100%;
  min-height: 400px;
  background-color: var(--bg-primary);
  overflow: hidden;
}

.rules-sidebar {
  width: 320px;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  height: 56px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
}

.title-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sidebar-icon {
  color: var(--accent-primary);
  font-size: 18px;
}

.sidebar-header h2 {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.rules-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rules-list::-webkit-scrollbar {
  width: 6px;
}

.rules-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.empty-rules {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--text-tertiary);
  opacity: 0.5;
}

.empty-rules i {
  font-size: 32px;
  margin-bottom: 12px;
}

.empty-rules p {
  font-size: 12px;
}

.persistent-card {
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 14px;
  transition: all 0.2s;
}

.persistent-card:hover {
  border-color: var(--accent-primary);
  background-color: rgba(99, 102, 241, 0.05);
}

.persistent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.persistent-probe-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fff;
}

.persistent-delete {
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  font-size: 12px;
  transition: color 0.2s;
}

.persistent-delete:hover {
  color: var(--accent-danger);
}

.persistent-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.persistent-class {
  font-size: 11px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.persistent-method {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.persistent-method i {
  color: var(--accent-primary);
  margin-right: 4px;
  font-size: 11px;
}

.persistent-line {
  color: var(--accent-secondary);
  font-size: 12px;
}

.persistent-location {
  font-size: 11px;
  color: var(--text-secondary);
}

.persistent-location i {
  margin-right: 4px;
  color: var(--accent-warning);
  font-size: 10px;
}

.persistent-code {
  margin-top: 4px;
  padding: 6px 8px;
  background: var(--bg-primary);
  border-radius: 4px;
  border: 1px solid var(--border-color);
}

.persistent-code code {
  font-size: 11px;
  color: var(--accent-secondary);
  font-family: var(--font-mono);
}

.template-preview {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-primary);
}

.preview-placeholder {
  text-align: center;
  max-width: 300px;
  color: var(--text-tertiary);
}

.pulse-logo {
  font-size: 48px;
  color: var(--accent-primary);
  margin-bottom: 20px;
  opacity: 0.3;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.1; }
  50% { transform: scale(1.1); opacity: 0.3; }
  100% { transform: scale(1); opacity: 0.1; }
}
</style>
