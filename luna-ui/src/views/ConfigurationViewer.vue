<template>
  <div class="configuration-viewer">
    <!-- 头部 -->
    <div class="config-header">
      <h2>{{ t('config.title') }}</h2>
      <button class="add-button" @click="addRule">
        <i class="fas fa-plus"></i>
        <span>{{ t('config.add_rule') }}</span>
      </button>
    </div>

    <!-- 规则列表 -->
    <div class="rules-container">
      <table class="rules-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>{{ t('config.class') }}</th>
            <th>{{ t('config.method') }}</th>
            <th>{{ t('config.type') }}</th>
            <th>{{ t('config.expression') }}</th>
            <th>{{ t('config.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.id" class="rule-row">
            <td>{{ rule.id }}</td>
            <td>{{ rule.targetClass }}</td>
            <td>{{ rule.targetMethod }}</td>
            <td>{{ formatInjectionType(rule.injectionType) }}</td>
            <td>{{ rule.expression }}</td>
            <td>
              <div class="actions">
                <button class="action-btn edit-btn" @click="editRule(rule)">
                  <i class="fas fa-edit"></i>
                </button>
                <button class="action-btn delete-btn" @click="deleteRule(rule.id)">
                  <i class="fas fa-trash"></i>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 对话框 -->
    <div v-if="dialogVisible" class="dialog-overlay" @click="closeDialog">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
          <h3>{{ dialogTitle }}</h3>
          <button class="dialog-close" @click="closeDialog">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="dialog-body">
          <div class="form-group">
            <label>{{ t('config.class') }}</label>
            <input v-model="form.targetClass" :placeholder="t('config.enter_class')" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ t('config.method') }}</label>
            <input v-model="form.targetMethod" :placeholder="t('config.enter_method')" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ t('config.type') }}</label>
            <select v-model="form.injectionType" class="form-select">
              <option value="" disabled>{{ t('config.select_type') }}</option>
              <option value="METHOD_ENTER">Method Enter</option>
              <option value="METHOD_EXIT">Method Exit</option>
              <option value="METHOD_AROUND">Method Around</option>
              <option value="LINE_BEFORE">Line Before</option>
              <option value="LINE_AFTER">Line After</option>
            </select>
          </div>
          <div v-if="form.injectionType === 'LINE_BEFORE' || form.injectionType === 'LINE_AFTER'" class="form-group">
            <label>{{ t('config.line_number') }}</label>
            <input type="number" v-model="form.lineNumber" :placeholder="t('config.enter_line_number')" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ t('config.expression') }}</label>
            <input v-model="form.expression" :placeholder="t('config.enter_expression')" class="form-input" />
          </div>
          <div class="form-group">
            <label>{{ t('config.log_content') }}</label>
            <textarea v-model="form.logContent" :placeholder="t('config.enter_log_content')" class="form-textarea"></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-btn cancel-btn" @click="closeDialog">{{ t('detail.cancel') }}</button>
          <button class="dialog-btn primary-btn" @click="saveRule">{{ t('detail.save') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useI18n } from 'vue-i18n'

export default {
  name: 'ConfigurationViewer',
  components: {},
  setup() {
    const { t } = useI18n()
    return { t }
  },
  data() {
    return {
      rules: [],
      dialogVisible: false,
      dialogTitle: 'ADD RULE',
      form: {
        id: '',
        targetClass: '',
        targetMethod: '',
        injectionType: '',
        lineNumber: '',
        expression: '',
        logContent: ''
      }
    }
  },
  mounted() {
    this.loadRules()
  },
  methods: {
    formatInjectionType(type) {
      const types = {
        'METHOD_ENTER': 'Enter',
        'METHOD_EXIT': 'Exit',
        'METHOD_AROUND': 'Around',
        'LINE_BEFORE': 'Line Before',
        'LINE_AFTER': 'Line After'
      }
      return types[type] || type
    },
    async loadRules() {
      try {
        this.rules = [
          {
            id: 1,
            targetClass: "com.example.User",
            targetMethod: "getName",
            injectionType: "METHOD_ENTER",
            expression: "true",
            logContent: "Entering method: getName"
          },
          {
            id: 2,
            targetClass: "com.example.User",
            targetMethod: "setName",
            injectionType: "METHOD_EXIT",
            expression: "name != null",
            logContent: "Exiting method: setName with name: ${name}"
          },
          {
            id: 3,
            targetClass: "com.example.service.UserService",
            targetMethod: "saveUser",
            injectionType: "METHOD_AROUND",
            expression: "user != null",
            logContent: "Processing user: ${user.getName()}"
          }
        ]
      } catch (error) {
        console.error('加载规则失败:', error)
      }
    },
    addRule() {
      this.dialogTitle = 'ADD RULE'
      this.form = {
        id: '',
        targetClass: '',
        targetMethod: '',
        injectionType: '',
        lineNumber: '',
        expression: '',
        logContent: ''
      }
      this.dialogVisible = true
    },
    closeDialog() {
      this.dialogVisible = false
    },
    editRule(rule) {
      this.dialogTitle = 'EDIT RULE'
      this.form = { ...rule }
      this.dialogVisible = true
    },
    async saveRule() {
      try {
        if (this.form.id) {
          const index = this.rules.findIndex(r => r.id === this.form.id)
          if (index !== -1) {
            this.rules[index] = { ...this.form }
          }
        } else {
          this.form.id = Date.now()
          this.rules.push({ ...this.form })
        }
        this.dialogVisible = false
      } catch (error) {
        console.error('保存规则失败:', error)
      }
    },
    async deleteRule(id) {
      try {
        this.rules = this.rules.filter(r => r.id !== id)
      } catch (error) {
        console.error('删除规则失败:', error)
      }
    }
  }
}
</script>

<style scoped>
:root {
  --bg-primary: #1e1e1e;
  --bg-secondary: #252526;
  --bg-tertiary: #2d2d30;
  --bg-hover: #3c3c3c;
  --text-primary: #d4d4d4;
  --text-secondary: #9d9d9d;
  --text-tertiary: #6a6a6a;
  --border-color: #3c3c3c;
  --accent-primary: #007acc;
  --accent-success: #6a9955;
  --accent-danger: #f14c4c;
  --radius-sm: 2px;
  --transition-fast: 0.1s ease;
}

.configuration-viewer {
  padding: 8px;
  height: 100%;
  overflow: auto;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 8px;
}

.config-header h2 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.add-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 11px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.add-button:hover {
  background-color: var(--bg-hover);
  border-color: var(--text-tertiary);
  color: var(--text-primary);
}

.rules-container {
  flex: 1;
  overflow: auto;
  border: 1px solid var(--border-color);
}

.rules-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.rules-table thead th {
  background-color: var(--bg-secondary);
  color: var(--text-secondary);
  text-align: left;
  padding: 6px 10px;
  font-weight: 500;
  font-size: 11px;
  letter-spacing: 0.3px;
  border-bottom: 1px solid var(--border-color);
}

.rules-table tbody tr {
  border-bottom: 1px solid var(--border-color);
}

.rules-table tbody tr:hover {
  background-color: var(--bg-hover);
}

.rules-table tbody td {
  padding: 6px 10px;
  color: var(--text-primary);
}

.actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  width: 22px;
  height: 22px;
  padding: 0;
  background-color: transparent;
  border: none;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.delete-btn:hover {
  color: var(--accent-danger);
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
  z-index: 1000;
}

.dialog-content {
  width: 500px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color);
}

.dialog-header h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.dialog-close {
  width: 22px;
  height: 22px;
  padding: 0;
  background-color: transparent;
  border: none;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.dialog-close:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.dialog-body {
  padding: 12px;
}

.form-group {
  margin-bottom: 10px;
}

.form-group label {
  display: block;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 4px;
  letter-spacing: 0.3px;
}

.form-input,
.form-select {
  width: 100%;
  padding: 6px 8px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 12px;
  box-sizing: border-box;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: var(--accent-primary);
}

.form-input::placeholder {
  color: var(--text-tertiary);
}

.form-textarea {
  width: 100%;
  padding: 6px 8px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 12px;
  min-height: 80px;
  resize: vertical;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--accent-primary);
}

.form-textarea::placeholder {
  color: var(--text-tertiary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid var(--border-color);
}

.dialog-btn {
  padding: 5px 12px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid var(--border-color);
}

.cancel-btn {
  background-color: var(--bg-primary);
  color: var(--text-secondary);
}

.cancel-btn:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.primary-btn {
  background-color: var(--accent-primary);
  color: white;
  border-color: var(--accent-primary);
}

.primary-btn:hover {
  background-color: #0066b3;
}
</style>