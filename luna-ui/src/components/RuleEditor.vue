<template>
  <div class="rules-editor">
    <div v-if="rule" class="editor-container">
      <div class="editor-header">
        <div class="header-main">
          <i class="fas fa-edit"></i>
          <h3>{{ rule.id ? '编辑策略' : '新建策略' }}</h3>
        </div>
        <div class="header-actions">
          <div class="status-toggle" @click="rule.enabled = !rule.enabled">
            <span class="status-label">{{ rule.enabled ? '已启用' : '已禁用' }}</span>
            <div class="toggle-switch" :class="{ active: rule.enabled }"></div>
          </div>
          <div class="action-divider"></div>
          <button class="editor-btn cancel" @click="$emit('cancel')">取消</button>
          <button class="editor-btn save" @click="$emit('save', rule)">
            <i class="fas fa-save"></i> 保存策略
          </button>
        </div>
      </div>

      <div class="editor-scroll">
        <!-- Section: 匹配定位 -->
        <div class="config-section">
          <div class="section-title">
            <span class="step-num">01</span>
            <h4>匹配定位 (Matcher)</h4>
          </div>
          <div class="section-content">
            <div class="form-row">
              <div class="form-item">
                <label>目标类名 (Class Pattern)</label>
                <div class="input-with-icon">
                  <i class="fas fa-box"></i>
                  <input v-model="rule.targetClass" placeholder="e.g. com.example.Service" />
                </div>
              </div>
              <div class="form-item">
                <label>目标方法 (Method Pattern)</label>
                <div class="input-with-icon">
                  <i class="fas fa-bolt"></i>
                  <input v-model="rule.targetMethod" placeholder="e.g. login*" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Section: 注入配置 -->
        <div class="config-section">
          <div class="section-title">
            <span class="step-num">02</span>
            <h4>处理器配置 (Handler)</h4>
          </div>
          <div class="section-content">
            <div class="form-row">
              <div class="form-item">
                <label>注入位置</label>
                <div class="type-selector">
                  <div 
                    v-for="type in injectionTypes" 
                    :key="type.value"
                    :class="['type-option', { active: rule.injectionType === type.value }]"
                    @click="rule.injectionType = type.value"
                  >
                    <i :class="type.icon"></i>
                    <span>{{ type.label }}</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div class="form-row" v-if="rule.injectionType?.startsWith('LINE_')">
              <div class="form-item">
                <label>行号</label>
                <input type="number" v-model="rule.lineNumber" class="premium-input" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-item">
                <label>探针类型</label>
                <div class="mode-selector">
                  <button 
                    v-for="handler in probeHandlers"
                    :key="handler.probeType"
                    :class="{ active: rule.probeType === handler.probeType }"
                    @click="selectProbeType(handler)"
                  >
                    <i class="fas fa-file-code"></i> {{ getProbeDisplayName(handler.probeType) }}
                  </button>
                </div>
              </div>
            </div>

            <div class="form-row" v-if="currentProbeUsesCode">
              <div class="form-item">
                <label>代码引擎</label>
                <div class="mode-selector">
                  <button 
                    v-for="engine in codeEngines"
                    :key="engine.codeType"
                    :class="{ active: rule.codeType === engine.codeType }"
                    @click="rule.codeType = engine.codeType"
                  >
                    {{ engine.codeType }}
                  </button>
                </div>
              </div>
            </div>

            <div class="form-row" v-if="selectedProbeSyntax">
              <div class="form-item">
                <label>语法提示</label>
                <div class="syntax-hint">{{ selectedProbeSyntax }}</div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-item">
                <label>条件表达式 (可选)</label>
                <textarea v-model="rule.expression" placeholder="e.g. params[0] != null" class="premium-textarea"></textarea>
              </div>
            </div>
            
            <div class="form-row" v-if="currentProbeUsesCode">
              <div class="form-item">
                <label>日志模板</label>
                <textarea v-model="rule.logContent" :placeholder="logContentPlaceholder" class="premium-textarea"></textarea>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="editor-placeholder">
      <div class="placeholder-content">
        <div class="pulse-logo">
          <i class="fas fa-shield-halved"></i>
        </div>
        <p>请从左侧选择一个策略进行编辑，或点击“+”创建一个新策略</p>
      </div>
    </div>
  </div>
</template>

<script>
import { pluginRegistry } from '../utils/plugin-registry'

export default {
  name: 'RuleEditor',
  props: {
    modelValue: { type: Object, default: null }
  },
  emits: ['update:modelValue', 'save', 'cancel'],
  data() {
    return {
      selectedProbeType: null
    }
  },
  computed: {
    rule: {
      get() { return this.modelValue },
      set(val) { this.$emit('update:modelValue', val) }
    },
    injectionTypes() {
      return pluginRegistry.injectionTypes.map(t => ({
        label: t.displayName,
        value: t.name,
        icon: t.icon || 'fas fa-circle',
        category: t.category || 'other'
      }))
    },
    groupedInjectionTypes() {
      const groups = {}
      this.injectionTypes.forEach(t => {
        const cat = t.category
        if (!groups[cat]) groups[cat] = { label: this.getCategoryLabel(cat), types: [] }
        groups[cat].types.push(t)
      })
      return Object.values(groups)
    },
    probeHandlers() {
      return pluginRegistry.probeHandlers
    },
    codeEngines() {
      return pluginRegistry.codeEngines
    },
    currentProbeUsesCode() {
      const handler = this.probeHandlers.find(h => h.probeType === this.selectedProbeType)
      return handler ? handler.usesCode : true
    },
    selectedProbeSyntax() {
      if (!this.selectedProbeType) return ''
      const proto = pluginRegistry.expressionProtocols.find(p => p.probeType === this.selectedProbeType)
      return proto ? proto.syntax : ''
    },
    logContentPlaceholder() {
      if (!this.selectedProbeType) return ''
      return this.selectedProbeType.toLowerCase() + ':<expression>'
    }
  },
  mounted() {
    const handlers = pluginRegistry.probeHandlers
    if (handlers.length > 0 && !this.selectedProbeType) {
      this.selectedProbeType = handlers[0].probeType
    }
  },
  methods: {
    getCategoryLabel(cat) {
      const labels = { method: '方法注入', line: '行号注入', field: '字段注入', other: '其他' }
      return labels[cat] || cat
    },
    getProbeDisplayName(probeType) {
      const names = { LOG: '日志表达式', SNAPSHOT: '内存快照', TRACE: '方法耗时' }
      return names[probeType] || probeType
    },
    selectProbeType(handler) {
      this.selectedProbeType = handler.probeType
      if (this.rule) {
        this.rule.probeType = handler.probeType
        if (handler.usesCode) {
          this.rule.codeType = 'EXPRESSION'
        } else {
          this.rule.codeType = null
        }
      }
    }
  }
}
</script>

<style scoped>
.rules-editor {
  flex: 1;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.editor-header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-secondary);
}

.header-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  user-select: none;
}

.status-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
}

.status-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
}

.toggle-switch {
  width: 32px;
  height: 18px;
  background: #4b5563;
  border-radius: 9px;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toggle-switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  background: white;
  border-radius: 50%;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toggle-switch.active {
  background: var(--accent-primary);
  box-shadow: 0 0 10px rgba(99, 102, 241, 0.2);
}

.toggle-switch.active::after {
  left: 16px;
}

.action-divider {
  width: 1px;
  height: 24px;
  background: var(--border-color);
}

.editor-btn {
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.editor-btn.cancel {
  background: none;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
}

.editor-btn.save {
  background: var(--accent-primary);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.editor-btn.save:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
}

.editor-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  padding-bottom: 80px !important;
}

.config-section {
  background-color: var(--bg-secondary);
  border-radius: 12px;
  border: 1px solid var(--border-color);
  padding: 24px;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.step-num {
  font-size: 18px;
  font-weight: 900;
  color: var(--accent-primary);
  opacity: 0.5;
}

.section-title h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-tertiary);
  text-transform: uppercase;
}

.input-with-icon {
  position: relative;
}

.input-with-icon i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  font-size: 12px;
}

.input-with-icon input {
  width: 100%;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 12px 10px 36px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
}

.input-with-icon input:focus {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.type-selector {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}

.type-option {
  padding: 12px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.type-option i {
  font-size: 16px;
  color: var(--text-tertiary);
}

.type-option span {
  font-size: 11px;
  font-weight: 600;
}

.type-option.active {
  background-color: rgba(99, 102, 241, 0.1);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.mode-selector {
  display: flex;
  gap: 12px;
}

.mode-selector button {
  flex: 1;
  padding: 10px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.mode-selector button.active {
  background-color: rgba(99, 102, 241, 0.1);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.syntax-hint {
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 12px;
  color: var(--text-tertiary);
  font-size: 12px;
  font-family: var(--font-mono);
  line-height: 1.5;
}

.premium-input, .premium-textarea {
  width: 100%;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 12px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
}

.premium-textarea {
  min-height: 80px;
  resize: vertical;
  font-family: var(--font-mono);
}

.editor-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.placeholder-content {
  text-align: center;
  max-width: 300px;
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

/* 滚动条 */
.editor-scroll::-webkit-scrollbar {
  width: 6px;
}

.editor-scroll::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 10px;
}

.editor-scroll::-webkit-scrollbar-thumb:hover {
  background: var(--accent-primary);
}
</style>
