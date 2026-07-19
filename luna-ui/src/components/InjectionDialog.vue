<template>
  <div v-if="visible" class="dialog-overlay" @click="$emit('close')">
    <div class="dialog-content" @click.stop>
      <div class="dialog-header">
        <div class="dialog-icon">
          <i class="fas fa-plus"></i>
        </div>
        <h3 class="dialog-title">方法注入配置</h3>
        <button class="dialog-close" @click="$emit('close')">
          <i class="fas fa-times"></i>
        </button>
      </div>

      <div class="dialog-body">
        <!-- 探针类型 -->
        <div class="form-group">
          <label class="form-label">探针类型</label>
          <select v-if="!readonlyProbeType" v-model="form.probeType" class="form-select" @change="onProbeTypeChange">
            <option v-for="handler in probeHandlers" :key="handler.probeType" :value="handler.probeType">
              {{ getProbeDisplayName(handler.probeType) }}
            </option>
          </select>
          <div v-else class="form-readonly">
            <i :class="['fas', currentProbeIcon]" v-if="currentProbeIcon"></i>
            {{ getProbeDisplayName(form.probeType) }}
          </div>
        </div>

        <!-- 注入位置 -->
        <div class="form-group">
          <label class="form-label">注入位置</label>
          <select v-if="!isInjectionLocationReadonly" v-model="form.injectionLocation" class="form-select">
            <optgroup v-for="group in groupedInjectionTypes" :key="group.category" :label="group.label">
              <option v-for="t in group.types" :key="t.name" :value="t.name">{{ t.displayName }}</option>
            </optgroup>
          </select>
          <div v-else class="form-readonly">
            {{ getLocationDisplayName(form.injectionLocation) }}
          </div>
        </div>

        <!-- 行号选择 (仅行级上下文可见) -->
        <div class="form-group" v-if="injectionContext === 'line'">
          <label class="form-label">源码行号</label>
          <div class="form-readonly">
            Line {{ form.lineNumber }}
          </div>
          <div class="form-hint" v-if="availableLines.length === 0">
            <i class="fas fa-exclamation-triangle" style="color: #f59e0b;"></i>
            <span>未找到该方法的源码行号表，请确保源码已加载。</span>
          </div>
        </div>

        <!-- 局部变量辅助 (仅行级上下文且当前探针使用代码) -->
        <div class="form-group" v-if="injectionContext === 'line' && currentProbeUsesCode">
          <label class="form-label">可用变量 (点击插入)</label>
          <div class="local-vars-container" v-if="loadingVars">
            <div class="loading-spinner-small"></div>
            <span>加载变量中...</span>
          </div>
          <div class="local-vars-container" v-else-if="localVariables.length > 0">
            <span 
              v-for="v in localVariables" 
              :key="v.name + v.slot"
              class="local-var-tag"
              @click="insertVar(v.name)"
              :title="'点击插入 $' + v.name"
            >
              ${{ v.name }}
            </span>
          </div>
          <div class="local-vars-container" v-else>
            <span class="no-data">该行号处无可见局部变量</span>
          </div>
        </div>

        <!-- 动态表单字段（根据 configSchema 渲染） -->
        <div
          v-for="field in currentConfigSchema"
          :key="field.key"
          class="form-group"
        >
          <label class="form-label">
            {{ field.label }}
            <span v-if="field.required" class="required-mark">*</span>
          </label>

          <!-- text -->
          <input
            v-if="field.type === 'text'"
            v-model="form.configValues[field.key]"
            type="text"
            class="form-input"
            :placeholder="field.placeholder || ''"
          />

          <!-- number -->
          <input
            v-else-if="field.type === 'number'"
            v-model.number="form.configValues[field.key]"
            type="number"
            class="form-input"
            :placeholder="field.placeholder || ''"
          />

          <!-- select -->
          <select
            v-else-if="field.type === 'select'"
            v-model="form.configValues[field.key]"
            class="form-select"
          >
            <option v-for="opt in field.options" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>

          <!-- textarea -->
          <textarea
            v-else-if="field.type === 'textarea'"
            v-model="form.configValues[field.key]"
            class="form-textarea"
            :placeholder="field.placeholder || ''"
            rows="3"
          ></textarea>

          <div class="form-hint" v-if="field.key === 'code' && currentProbeSyntax">
            <i class="fas fa-info-circle"></i>
            <span>{{ currentProbeSyntax }}</span>
          </div>
        </div>

        <!-- 持久化选项 -->
        <div class="form-group">
          <label class="form-label">注入类型</label>
          <div class="toggle-group">
            <button 
              class="toggle-btn" 
              :class="{ active: form.ephemeral }" 
              @click="form.ephemeral = true"
            >
              <i class="fas fa-bolt"></i> 临时
            </button>
            <button 
              class="toggle-btn" 
              :class="{ active: !form.ephemeral }" 
              @click="form.ephemeral = false"
            >
              <i class="fas fa-thumbtack"></i> 持久
            </button>
          </div>
          <div class="form-hint">
            {{ form.ephemeral ? '临时注入：Agent 重启后自动失效' : '持久注入：Agent 重启后仍然生效' }}
          </div>
        </div>
      </div>

      <div class="dialog-footer">
        <button class="dialog-button cancel-button" @click="$emit('close')">取消</button>
        <button 
          class="dialog-button primary-button"
          @click="handleSubmit"
          :disabled="loading"
        >
          <div v-if="loading" class="loading-spinner-small"></div>
          <span>{{ loading ? '正在注入...' : '提交注入' }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { getLocalVariables } from '../utils/api'
import { pluginRegistry } from '../utils/plugin-registry'

export default {
  name: 'InjectionDialog',
  props: {
    visible: Boolean,
    loading: Boolean,
    className: String,
    method: Object,
    initialLineNumber: { type: Number, default: null },
    initialInjectionType: { type: String, default: '' },
    initialCodeType: { type: String, default: '' },
    initialProbeType: { type: String, default: '' },
    readonlyProbeType: { type: Boolean, default: false },
    injectionContext: { type: String, default: 'free', validator: v => ['method', 'line', 'free'].includes(v) },
    availableLines: { type: Array, default: () => [] }
  },
  emits: ['close', 'submit'],
  data() {
    return {
      form: {
        injectionLocation: '',
        probeType: '',
        codeType: '',
        configValues: {},
        lineNumber: null,
        ephemeral: true
      },
      localVariables: [],
      loadingVars: false
    }
  },
  computed: {
    isLineInjection() {
      const type = pluginRegistry.injectionTypes.find(t => t.name === this.form.injectionLocation)
      return type?.category === 'line'
    },
    // 探针只支持一个注入位置时，位置不可选
    isInjectionLocationReadonly() {
      const handler = this.probeHandlers.find(h => h.probeType === this.form.probeType)
      const supported = handler?.supportedInjectionLocations || []
      return supported.length <= 1
    },
    probeHandlers() {
      return pluginRegistry.probeHandlers
    },
    codeEngines() {
      return pluginRegistry.codeEngines
    },
    currentProbeUsesCode() {
      const handler = this.probeHandlers.find(h => h.probeType === this.form.probeType)
      return handler ? handler.usesCode : true
    },
    currentProbeIcon() {
      const handler = this.probeHandlers.find(h => h.probeType === this.form.probeType)
      return handler?.icon || ''
    },
    currentProbeSyntax() {
      const handler = this.probeHandlers.find(h => h.probeType === this.form.probeType)
      return handler?.syntax || ''
    },
    currentConfigSchema() {
      const handler = this.probeHandlers.find(h => h.probeType === this.form.probeType)
      return handler?.configSchema || []
    },
    groupedInjectionTypes() {
      const types = pluginRegistry.injectionTypes
      const currentHandler = this.probeHandlers.find(h => h.probeType === this.form.probeType)
      const supportedLocations = currentHandler?.supportedInjectionLocations || null

      // 按 injectionContext 过滤位置类别
      const contextCategories = this.injectionContext === 'line'
        ? ['line']
        : this.injectionContext === 'method'
          ? ['method']
          : ['method', 'line', 'field', 'other']

      const groups = {}
      types.forEach(t => {
        const canonicalName = t.canonicalName || t.name.toLowerCase()
        // 过滤1：探针支持的位置
        if (supportedLocations && !supportedLocations.includes(canonicalName)) return
        // 过滤2：上下文允许的类别
        const cat = t.category || 'other'
        if (!contextCategories.includes(cat)) return
        if (!groups[cat]) groups[cat] = { category: cat, label: t.categoryLabel || cat, types: [] }
        groups[cat].types.push(t)
      })
      return Object.values(groups)
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        if (val && this.method) {
          this.resetForm()
        }
      }
    },
    'form.lineNumber'() {
      if (this.isLineInjection && this.form.lineNumber) {
        this.fetchVars()
      }
    },
    'form.injectionLocation'() {
      this.applyDefaultCode()
    }
  },
  methods: {
    getProbeDisplayName(probeType) {
      const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === probeType)
      return handler?.displayName || probeType
    },
    getLocationDisplayName(location) {
      const type = pluginRegistry.injectionTypes?.find(t => t.name === location)
      return type?.displayName || location
    },
    applyDefaultCode() {
      const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === this.form.probeType)
      if (!handler) return

      // 初始化 configValues（从 configSchema 的 defaultValue），保留用户已输入的值
      const configValues = {}
      const schema = handler.configSchema || []
      schema.forEach(field => {
        // Preserve user-entered values when re-initializing (e.g., injectionLocation change)
        if (this.form.configValues && this.form.configValues.hasOwnProperty(field.key)
            && this.form.configValues[field.key] !== '') {
          configValues[field.key] = this.form.configValues[field.key]
        } else {
          configValues[field.key] = field.defaultValue != null ? field.defaultValue : ''
        }
      })

      this.form.configValues = configValues
    },
    resetForm() {
      let injectionLocation = this.initialInjectionType
      const probeType = this.initialProbeType

      // 校验初始注入位置是否被当前探针支持，不支持则选择第一个可用位置
      const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === probeType)
      const supportedLocations = handler?.supportedInjectionLocations || null
      if (supportedLocations && injectionLocation) {
        const currentType = pluginRegistry.injectionTypes.find(t => t.name === injectionLocation)
        const canonicalName = currentType?.canonicalName || injectionLocation.toLowerCase()
        if (!supportedLocations.includes(canonicalName)) {
          const firstAvailable = pluginRegistry.injectionTypes.find(t => {
            const cn = t.canonicalName || t.name.toLowerCase()
            return supportedLocations.includes(cn)
          })
          if (firstAvailable) {
            injectionLocation = firstAvailable.name
          }
        }
      }

      this.form = {
        injectionLocation,
        probeType,
        codeType: this.initialCodeType,
        configValues: {},
        lineNumber: this.initialLineNumber,
        ephemeral: true
      }
      this.localVariables = []

      this.applyDefaultCode()

      if (this.injectionContext === 'line' && this.initialLineNumber) {
        this.fetchVars()
      }
    },
    async fetchVars() {
      if (!this.className || !this.method || !this.form.lineNumber) return
      this.loadingVars = true
      try {
        const result = await getLocalVariables(
          this.className,
          this.method.name,
          this.method.descriptor,
          this.form.lineNumber
        )
        this.localVariables = result.variables || []
      } catch (e) {
        console.error('Failed to fetch vars:', e)
      } finally {
        this.loadingVars = false
      }
    },
    insertVar(name) {
      if (this.form.configValues.hasOwnProperty('code')) {
        this.form.configValues['code'] += ` $${name}`
      }
    },
    onProbeTypeChange() {
      this.applyDefaultCode()

      if (!this.currentProbeUsesCode) {
        this.form.codeType = null
      } else {
        if (!this.form.codeType) {
          const engines = pluginRegistry.codeEngines
          this.form.codeType = engines.length > 0 ? engines[0].codeType : ''
        }
      }

      // If current injection location is not supported by the new probe type, reset to first available
      const currentHandler = this.probeHandlers.find(h => h.probeType === this.form.probeType)
      const supportedLocations = currentHandler?.supportedInjectionLocations || null
      if (supportedLocations) {
        const currentType = pluginRegistry.injectionTypes.find(t => t.name === this.form.injectionLocation)
        const canonicalName = currentType?.canonicalName || this.form.injectionLocation.toLowerCase()
        if (!supportedLocations.includes(canonicalName)) {
          const firstAvailable = this.groupedInjectionTypes.flatMap(g => g.types)[0]
          if (firstAvailable) {
            this.form.injectionLocation = firstAvailable.name
          }
        }
      }
    },
    handleSubmit() {
      const configValues = this.form.configValues || {}
      const submitData = {
        injectionLocation: this.form.injectionLocation,
        probeType: this.form.probeType,
        codeType: this.form.codeType,
        code: configValues['code'] || '',
        configValues: { ...configValues },
        lineNumber: this.form.lineNumber,
        ephemeral: this.form.ephemeral !== undefined ? this.form.ephemeral : true
      }
      this.$emit('submit', submitData)
    }
  }
}
</script>

<style scoped>
.dialog-overlay {
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

.dialog-content {
  width: 500px;
  background-color: #252526;
  border-radius: 12px;
  border: 1px solid #444;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0,0,0,0.4);
  animation: modal-show 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes modal-show {
  from { opacity: 0; transform: scale(0.95) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.dialog-header {
  padding: 20px;
  background-color: #2d2d2d;
  border-bottom: 1px solid #333;
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-icon {
  width: 32px;
  height: 32px;
  background-color: rgba(99, 102, 241, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366f1;
}

.dialog-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  flex: 1;
}

.dialog-close {
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  font-size: 18px;
}

.dialog-body {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #888;
  text-transform: uppercase;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.form-select, .form-input, .form-textarea {
  width: 100%;
  background-color: #1e1e1e;
  border: 1px solid #333;
  border-radius: 6px;
  padding: 10px 12px;
  color: #eee;
  font-size: 13px;
  outline: none;
}

.form-select:focus, .form-input:focus, .form-textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.local-vars-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  background: #1e1e1e;
  padding: 12px;
  border-radius: 6px;
  border: 1px dashed #444;
}

.local-var-tag {
  background: #333;
  color: #6366f1;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
}

.local-var-tag:hover {
  background: #444;
}

.dialog-footer {
  padding: 20px 24px;
  background-color: #2d2d2d;
  border-top: 1px solid #333;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-button {
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.cancel-button {
  background: none;
  border: 1px solid #444;
  color: #aaa;
}

.primary-button {
  background: #6366f1;
  border: none;
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.primary-button:hover {
  background: #4f46e5;
  transform: translateY(-1px);
}

.form-hint {
  font-size: 11px;
  color: #666;
  margin-top: 6px;
}

.required-mark {
  color: #ef4444;
  margin-left: 2px;
}

.form-readonly {
  background-color: #1e1e1e;
  border: 1px solid #333;
  border-radius: 6px;
  padding: 10px 12px;
  color: #eee;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.toggle-group {
  display: flex;
  gap: 0;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #333;
}

.toggle-btn {
  flex: 1;
  padding: 8px 12px;
  background: #1e1e1e;
  border: none;
  color: #888;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s;
}

.toggle-btn:first-child {
  border-right: 1px solid #333;
}

.toggle-btn.active {
  background: #6366f1;
  color: #fff;
}

.toggle-btn:not(.active):hover {
  background: #2a2a2a;
  color: #ccc;
}

.loading-spinner-small {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
