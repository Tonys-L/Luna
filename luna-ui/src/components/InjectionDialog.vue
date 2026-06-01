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
          <select v-model="form.probeType" class="form-select" @change="onProbeTypeChange">
            <option v-for="handler in probeHandlers" :key="handler.probeType" :value="handler.probeType">
              {{ getProbeDisplayName(handler.probeType) }}
            </option>
          </select>
        </div>

        <!-- 注入位置 -->
        <div class="form-group">
          <label class="form-label">注入位置</label>
          <select v-model="form.injectionLocation" class="form-select">
            <optgroup v-for="group in groupedInjectionTypes" :key="group.category" :label="group.label">
              <option v-for="t in group.types" :key="t.name" :value="t.name">{{ t.displayName }}</option>
            </optgroup>
          </select>
        </div>

        <!-- 行号选择 (仅行注入可见) -->
        <div class="form-group" v-if="isLineInjection">
          <label class="form-label">源码行号</label>
          <select v-model.number="form.lineNumber" class="form-select">
            <option :value="null" disabled>请选择源码行号</option>
            <option v-for="line in availableLines" :key="line" :value="line">
              Line {{ line }}
            </option>
          </select>
          <div class="form-hint" v-if="availableLines.length === 0">
            <i class="fas fa-exclamation-triangle" style="color: #f59e0b;"></i>
            <span>未找到该方法的源码行号表，请确保源码已加载。</span>
          </div>
        </div>

        <!-- 局部变量辅助 (仅行注入且选中行号后可见) -->
        <div class="form-group" v-if="isLineInjection && form.lineNumber">
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

        <!-- 代码类型 (仅 usesCode=true 时可见) -->
        <div class="form-group" v-if="currentProbeUsesCode">
          <label class="form-label">动作类型</label>
          <select v-model="form.codeType" class="form-select">
            <option v-for="engine in codeEngines" :key="engine.codeType" :value="engine.codeType">
              {{ engine.codeType }}
            </option>
          </select>
        </div>

        <!-- 条件表达式 -->
        <div class="form-group">
          <label class="form-label">条件表达式 (可选)</label>
          <input 
            v-model="form.condition" 
            type="text" 
            class="form-input"
            placeholder="例如: params[0] != null"
          />
        </div>

        <!-- 日志模板 (仅 usesCode=true 时可见) -->
        <div class="form-group" v-if="currentProbeUsesCode">
          <label class="form-label">日志模板</label>
          <textarea 
            v-model="form.logContent" 
            class="form-textarea"
            placeholder="例如: User ID is {}"
            rows="3"
          ></textarea>
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
    initialInjectionType: { type: String, default: 'ENTER_METHOD' },
    initialCodeType: { type: String, default: 'EXPRESSION' },
    initialProbeType: { type: String, default: 'LOG' },
    availableLines: { type: Array, default: () => [] }
  },
  emits: ['close', 'submit'],
  data() {
    return {
      form: {
        injectionLocation: 'ENTER_METHOD',
        probeType: 'LOG',
        codeType: 'EXPRESSION',
        logContent: '',
        condition: '',
        lineNumber: null
      },
      localVariables: [],
      loadingVars: false
    }
  },
  computed: {
    isLineInjection() {
      return this.form.injectionLocation.startsWith('LINE_')
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
    expressionProtocols() {
      return pluginRegistry.expressionProtocols
    },
    groupedInjectionTypes() {
      const types = pluginRegistry.injectionTypes
      const groups = {}
      types.forEach(t => {
        const cat = t.category || 'other'
        if (!groups[cat]) groups[cat] = { category: cat, label: this.getCategoryLabel(cat), types: [] }
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
    resetForm() {
      const isLine = this.initialLineNumber !== null
      
      this.form = {
        injectionLocation: this.initialInjectionType,
        probeType: this.initialProbeType,
        codeType: this.initialCodeType,
        logContent: isLine ? `Line ${this.initialLineNumber} check` : `执行方法: ${this.method.name}`,
        condition: '',
        lineNumber: this.initialLineNumber
      }
      this.localVariables = []
      
      if (isLine) {
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
      this.form.logContent += ` $${name}`
    },
    onProbeTypeChange() {
      if (!this.currentProbeUsesCode) {
        this.form.logContent = ''
        this.form.codeType = null
      } else {
        if (!this.form.codeType) {
          this.form.codeType = 'EXPRESSION'
        }
      }
    },
    handleSubmit() {
      this.$emit('submit', { ...this.form })
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
