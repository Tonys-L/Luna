<template>
  <div class="configuration-viewer">
    <!-- 左侧列表 -->
    <div class="rules-sidebar">
      <div class="sidebar-header">
        <div class="title-group">
          <i class="fas fa-shield-halved sidebar-icon"></i>
          <h2>策略中心</h2>
        </div>
        <button class="add-btn-round" @click="addRule" title="新建规则">
          <i class="fas fa-plus"></i>
        </button>
      </div>

      <div class="rules-list">
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
    </div>

    <!-- 右侧编辑器 -->
    <RuleEditor 
      v-model="form"
      :rule-data="editingRule"
      @save="saveRule"
      @cancel="cancelEdit"
    />
  </div>
</template>

<script>
import RuleCard from '../components/RuleCard.vue'
import RuleEditor from '../components/RuleEditor.vue'

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
      form: null // 将被 RuleEditor 通过 v-model 使用
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
        injectionType: 'METHOD_ENTER',
        lineNumber: '',
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

/* 左侧列表 */
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

.rules-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 滚动条美化 */
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
</style>