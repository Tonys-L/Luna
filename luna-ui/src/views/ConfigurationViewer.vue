<template>
  <div class="configuration-viewer">
    <!-- 左侧列表 -->
    <div class="rules-sidebar">
      <div class="sidebar-header">
        <div class="title-group">
          <i class="fas fa-shield-halved sidebar-icon"></i>
          <h2>策略中心</h2>
        </div>
        <button v-if="activeSubTab === 'rules'" class="add-btn-round" @click="addRule" title="新建规则">
          <i class="fas fa-plus"></i>
        </button>
      </div>

      <div class="sub-tabs">
        <button :class="['sub-tab', { active: activeSubTab === 'rules' }]" @click="activeSubTab = 'rules'">
          <i class="fas fa-list"></i> 规则列表
        </button>
        <button :class="['sub-tab', { active: activeSubTab === 'templates' }]" @click="activeSubTab = 'templates'">
          <i class="fas fa-puzzle-piece"></i> 模板库
        </button>
      </div>

      <div v-if="activeSubTab === 'rules'" class="rules-list">
        <RuleCard 
          v-for="rule in rules" 
          :key="rule.id"
          :rule="rule"
          :active="activeRuleId === rule.id"
          @select="selectRule"
          @delete="deleteRule"
        />
        
        <div v-if="rules.length === 0" class="empty-rules">
          <i class="fas fa-ghost"></i>
          <p>暂无活跃策略</p>
        </div>
      </div>

      <div v-else class="templates-list">
        <div v-for="tpl in templates" :key="tpl.id" class="template-card" @click="openTemplateDialog(tpl)">
          <div class="tpl-icon">
            <i :class="tpl.icon || 'fas fa-file-code'"></i>
          </div>
          <div class="tpl-info">
            <div class="tpl-name">{{ tpl.displayName }}</div>
            <div class="tpl-desc">{{ tpl.description }}</div>
          </div>
        </div>
        <div v-if="templates.length === 0" class="empty-rules">
          <i class="fas fa-box-open"></i>
          <p>暂无可用模板</p>
        </div>
      </div>
    </div>

    <!-- 右侧编辑器 -->
    <RuleEditor 
      v-if="activeSubTab === 'rules'"
      v-model="form"
      :rule-data="editingRule"
      @save="saveRule"
      @cancel="cancelEdit"
    />

    <div v-else class="template-preview">
      <div class="preview-placeholder">
        <div class="pulse-logo">
          <i class="fas fa-puzzle-piece"></i>
        </div>
        <p>从左侧选择一个模板，快速生成规则</p>
      </div>
    </div>

    <!-- 模板参数弹窗 -->
    <div v-if="templateDialogVisible" class="dialog-overlay" @click="templateDialogVisible = false">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
          <div class="dialog-icon">
            <i class="fas fa-file-code"></i>
          </div>
          <h3 class="dialog-title">{{ currentTemplate?.displayName }}</h3>
          <button class="dialog-close" @click="templateDialogVisible = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="dialog-body">
          <p class="template-desc">{{ currentTemplate?.description }}</p>
          <div v-for="param in currentTemplate?.parameters" :key="param.name" class="form-group">
            <label class="form-label">{{ param.displayName }}</label>
            <input 
              v-model="templateParams[param.name]" 
              :placeholder="param.defaultValue || param.displayName"
              class="form-input"
            />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-button cancel-button" @click="templateDialogVisible = false">取消</button>
          <button class="dialog-button primary-button" @click="applyTemplate" :disabled="templateLoading">
            <div v-if="templateLoading" class="loading-spinner-small"></div>
            <span>{{ templateLoading ? '生成中...' : '生成规则' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import RuleCard from '../components/RuleCard.vue'
import RuleEditor from '../components/RuleEditor.vue'
import { pluginRegistry } from '../utils/plugin-registry'

export default {
  name: 'ConfigurationViewer',
  components: {
    RuleCard,
    RuleEditor
  },
  data() {
    return {
      rules: [],
      activeRuleId: null,
      editingRule: null,
      form: null,
      activeSubTab: 'rules',
      templateDialogVisible: false,
      currentTemplate: null,
      templateParams: {},
      templateLoading: false
    }
  },
  computed: {
    templates() {
      return pluginRegistry.templates
    }
  },
  mounted() {
    this.loadRules()
  },
  methods: {
    async loadRules() {
      try {
        const { getRules } = await import('../utils/api')
        const data = await getRules()
        this.rules = Array.isArray(data) ? data : []
      } catch (error) {
        console.error('加载规则失败:', error)
        this.rules = []
      }
    },
    selectRule(rule) {
      this.activeRuleId = rule.id
      this.editingRule = rule
      this.form = { 
        ...rule, 
        probeType: rule.probeType || 'LOG',
        codeType: rule.codeType || 'EXPRESSION',
        enabled: rule.enabled !== false
      }
    },
    addRule() {
      this.activeRuleId = null
      this.editingRule = { id: '' }
      this.form = {
        id: '',
        targetClass: '',
        targetMethod: '',
        injectionLocation: 'METHOD_ENTER',
        lineNumber: '',
        probeType: 'LOG',
        codeType: 'EXPRESSION',
        expression: '',
        logContent: '',
        enabled: true
      }
    },
    cancelEdit() {
      this.editingRule = null
      this.activeRuleId = null
      this.form = null
    },
    async saveRule() {
      if (!this.form) return
      try {
        const { addRule, updateRule } = await import('../utils/api')
        if (this.form.id) {
          await updateRule(this.form.id, this.form)
        } else {
          await addRule(this.form)
        }
        await this.loadRules()
        this.cancelEdit()
      } catch (error) {
        console.error('保存规则失败:', error)
      }
    },
    async deleteRule(id) {
      if (!confirm('确定删除此策略？')) return
      try {
        const { deleteRule } = await import('../utils/api')
        await deleteRule(id)
        if (this.activeRuleId === id) {
          this.cancelEdit()
        }
        await this.loadRules()
      } catch (error) {
        console.error('删除规则失败:', error)
      }
    },
    openTemplateDialog(tpl) {
      this.currentTemplate = tpl
      this.templateParams = {}
      if (tpl.parameters) {
        tpl.parameters.forEach(p => {
          this.templateParams[p.name] = p.defaultValue || ''
        })
      }
      this.templateDialogVisible = true
    },
    async applyTemplate() {
      if (!this.currentTemplate) return
      this.templateLoading = true
      try {
        const { addRule } = await import('../utils/api')
        const rule = {
          ...this.currentTemplate.ruleTemplate,
          ...this.templateParams
        }
        await addRule(rule)
        await this.loadRules()
        this.templateDialogVisible = false
        this.activeSubTab = 'rules'
      } catch (error) {
        console.error('应用模板失败:', error)
      } finally {
        this.templateLoading = false
      }
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

.add-btn-round {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: var(--accent-primary);
  border: none;
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.4);
}

.add-btn-round:hover {
  transform: scale(1.1) rotate(90deg);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.6);
}

.sub-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
}

.sub-tab {
  flex: 1;
  padding: 10px 0;
  background: none;
  border: none;
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-bottom: 2px solid transparent;
}

.sub-tab:hover {
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.03);
}

.sub-tab.active {
  color: var(--accent-primary);
  border-bottom-color: var(--accent-primary);
}

.rules-list, .templates-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rules-list::-webkit-scrollbar,
.templates-list::-webkit-scrollbar {
  width: 6px;
}

.rules-list::-webkit-scrollbar-thumb,
.templates-list::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.template-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.template-card:hover {
  border-color: var(--accent-primary);
  background-color: rgba(99, 102, 241, 0.05);
  transform: translateY(-1px);
}

.tpl-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-primary);
  font-size: 14px;
  flex-shrink: 0;
}

.tpl-info {
  flex: 1;
  min-width: 0;
}

.tpl-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.tpl-desc {
  font-size: 11px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.template-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 20px;
  line-height: 1.6;
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

.form-input {
  width: 100%;
  background-color: #1e1e1e;
  border: 1px solid #333;
  border-radius: 6px;
  padding: 10px 12px;
  color: #eee;
  font-size: 13px;
  outline: none;
}

.form-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
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
