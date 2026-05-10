<template>
  <div v-if="visible" class="debugger-panel-overlay" @click.self="close">
    <div class="debugger-panel">
      <div class="debugger-header">
        <div class="header-left">
          <i class="fas fa-bug debugger-icon"></i>
          <span class="debugger-title">Virtual Breakpoint Snapshot</span>
        </div>
        <div class="header-right">
          <span class="thread-info">
            <i class="fas fa-microchip"></i> {{ snapshot.threadName }} (ID: {{ snapshot.threadId }})
          </span>
          <button class="close-btn" @click="close">&times;</button>
        </div>
      </div>
      
      <div class="debugger-body">
        <div class="debugger-sidebar">
          <div class="sidebar-section">
            <div class="section-header">
              <i class="fas fa-list-ol"></i> Call Stack
            </div>
            <div class="stack-list">
              <div 
                v-for="(frame, index) in snapshot.stackTrace" 
                :key="index"
                class="stack-frame"
                :class="{ active: index === 0 }"
              >
                <div class="frame-method">{{ frame.method }}</div>
                <div class="frame-location">{{ getSimpleClassName(frame.class) }}:{{ frame.line }}</div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="debugger-main">
          <div class="main-section">
            <div class="section-header">
              <div class="header-content">
                <i class="fas fa-cube"></i> Variables
                <div class="search-box">
                  <i class="fas fa-search"></i>
                  <input v-model="searchQuery" placeholder="Filter variables..." />
                </div>
              </div>
            </div>
            <div class="variables-container">
              <div class="variables-tree">
                <VariableTreeNode 
                  v-for="(value, name) in filteredVariables" 
                  :key="name"
                  :name="name"
                  :value="value"
                  :depth="0"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import VariableTreeNode from './VariableTreeNode.vue'

export default {
  name: 'DebuggerPanel',
  components: {
    VariableTreeNode
  },
  props: {
    visible: Boolean,
    snapshot: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      searchQuery: ''
    }
  },
  computed: {
    filteredVariables() {
      if (!this.snapshot.localVars) return {}
      if (!this.searchQuery) return this.snapshot.localVars
      
      const filtered = {}
      const query = this.searchQuery.toLowerCase()
      for (const [name, value] of Object.entries(this.snapshot.localVars)) {
        if (name.toLowerCase().includes(query)) {
          filtered[name] = value
        }
      }
      return filtered
    }
  },
  methods: {
    close() {
      this.$emit('close')
    },
    getSimpleClassName(className) {
      if (!className) return ''
      const parts = className.split('.')
      return parts[parts.length - 1]
    }
  }
}
</script>

<style scoped>
.debugger-panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.debugger-panel {
  width: 90%;
  height: 85%;
  background-color: #1e1e1e;
  border: 1px solid #333;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
}

.debugger-header {
  height: 40px;
  background-color: #2d2d2d;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #3c3c3c;
  flex-shrink: 0;
}

.header-left, .header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.debugger-icon {
  color: #f14c4c;
  font-size: 14px;
}

.debugger-title {
  color: #cccccc;
  font-size: 13px;
  font-weight: 600;
}

.thread-info {
  color: #888;
  font-size: 12px;
}

.close-btn {
  background: none;
  border: none;
  color: #888;
  font-size: 20px;
  cursor: pointer;
  line-height: 1;
}

.close-btn:hover {
  color: #fff;
}

.debugger-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.debugger-sidebar {
  width: 280px;
  background-color: #252526;
  border-right: 1px solid #333;
  display: flex;
  flex-direction: column;
}

.debugger-main {
  flex: 1;
  background-color: #1e1e1e;
  display: flex;
  flex-direction: column;
}

.sidebar-section, .main-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.section-header {
  height: 32px;
  background-color: #37373d;
  color: #bbbbbb;
  font-size: 11px;
  text-transform: uppercase;
  font-weight: 700;
  display: flex;
  align-items: center;
  padding: 0 12px;
  gap: 8px;
  letter-spacing: 0.5px;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 16px;
}

.search-box {
  display: flex;
  align-items: center;
  background-color: #3c3c3c;
  border-radius: 4px;
  padding: 2px 8px;
  flex: 1;
  max-width: 240px;
}

.search-box i {
  color: #888;
  font-size: 12px;
  margin-right: 6px;
}

.search-box input {
  background: none;
  border: none;
  color: #cccccc;
  font-size: 11px;
  outline: none;
  width: 100%;
}

.stack-list {
  flex: 1;
  overflow-y: auto;
}

.stack-frame {
  padding: 8px 12px;
  border-bottom: 1px solid #333;
  cursor: pointer;
}

.stack-frame:hover {
  background-color: #2a2d2e;
}

.stack-frame.active {
  background-color: #37373d;
  border-left: 3px solid #007acc;
}

.frame-method {
  color: #cccccc;
  font-size: 12px;
  font-family: var(--font-mono);
}

.frame-location {
  color: #888;
  font-size: 11px;
  margin-top: 2px;
}

.variables-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.variables-tree {
  font-family: var(--font-mono);
  font-size: 13px;
}
</style>
