<template>
  <div class="class-detail">
    <!-- 类信息卡片 -->
    <div v-if="classInfo" class="class-info-card">
      <div class="card-header">
        <div class="card-icon">
          <i class="fas fa-file-code"></i>
        </div>
        <h2 class="card-title">{{ classInfo.className }}</h2>
      </div>
      <div class="card-body">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">{{ t('detail.super_class') }}</span>
            <span class="info-value">{{ classInfo.superClass || 'NONE' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('detail.access') }}</span>
            <span class="info-value">{{ classInfo.readableAccessFlags || classInfo.accessFlags }}</span>
          </div>
        </div>
        <div class="interfaces-section">
          <span class="section-label">{{ t('detail.interfaces') }}</span>
          <div class="interface-tags">
            <span 
              v-for="iface in classInfo.interfaces" 
              :key="iface"
              class="interface-tag"
            >
              {{ iface }}
            </span>
            <span v-if="!classInfo.interfaces || classInfo.interfaces.length === 0" class="no-data">{{ t('detail.no_interfaces') }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 详情标签页 -->
    <div v-if="classInfo" class="detail-tabs">
      <div class="tabs-header">
        <button 
          :class="['tab-item', { active: activeTab === 'fields' }]"
          @click="activeTab = 'fields'"
        >
          <i class="fas fa-table"></i>
          <span>{{ t('detail.fields') }}</span>
        </button>
        <button 
          :class="['tab-item', { active: activeTab === 'methods' }]"
          @click="activeTab = 'methods'"
        >
          <i class="fas fa-code"></i>
          <span>{{ t('detail.methods') }}</span>
        </button>
        <button 
          :class="['tab-item', { active: activeTab === 'decompile' }]"
          @click="activeTab = 'decompile'"
        >
          <i class="fas fa-file-alt"></i>
          <span>{{ t('detail.source') }}</span>
        </button>
      </div>
      <div class="tabs-content">
        <!-- 字段标签页 -->
        <div v-show="activeTab === 'fields'" class="tab-panel">
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>ACCESS</th>
                  <th>NAME</th>
                  <th>DESCRIPTOR</th>
                </tr>
              </thead>
              <tbody>
                <tr 
                  v-for="(field, index) in (classInfo.convertedFields || classInfo.fields)" 
                  :key="index"
                  class="table-row"
                >
                  <td>{{ field.readableAccessFlags || field.accessFlags }}</td>
                  <td>{{ field.name }}</td>
                  <td>{{ field.descriptor }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        
        <!-- 方法标签页 -->
        <div v-show="activeTab === 'methods'" class="tab-panel">
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th>{{ t('detail.access') }}</th>
                  <th>NAME</th>
                  <th>DESCRIPTOR</th>
                  <th>{{ t('detail.params') }}</th>
                  <th>ACTION</th>
                </tr>
              </thead>
              <tbody>
                <tr 
                  v-for="(method, index) in (classInfo.convertedMethods || classInfo.methods)" 
                  :key="index"
                  class="table-row"
                >
                  <td>{{ method.readableAccessFlags || method.accessFlags }}</td>
                  <td>{{ method.name }}</td>
                  <td>{{ method.descriptor }}</td>
                  <td>
                    <div class="parameter-tags">
                      <span 
                        v-for="(param, paramIndex) in (method.parameters || [])" 
                        :key="paramIndex"
                        class="parameter-tag"
                      >
                        {{ param.name }}: {{ param.descriptor }}
                      </span>
                      <span v-if="!method.parameters || method.parameters.length === 0" class="no-data">{{ t('detail.no_params') }}</span>
                    </div>
                  </td>
                  <td>
                    <button 
                      class="inject-button"
                      @click="showInjectDialog(method)"
                    >
                      <i class="fas fa-plus"></i>
                      <span>{{ t('detail.inject') }}</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        
        <!-- 反编译源码标签页 -->
        <div v-show="activeTab === 'decompile'" class="tab-panel">
          <div class="decompile-section">
            <button 
              class="load-button"
              @click="loadDecompiledCode"
              :disabled="loadingDecompiled"
            >
              <i v-if="!loadingDecompiled" class="fas fa-sync-alt"></i>
              <div v-else class="loading-spinner-small"></div>
              <span>{{ loadingDecompiled ? t('detail.loading') : t('detail.load_source') }}</span>
            </button>
            <div v-if="decompiledCode" class="editor-container">
              <CodeEditor
                :options="editorOptions"
                :value="decompiledCode"
                class="code-editor"
                language="java"
                @change="handleEditorChange"
              />
            </div>
            <div v-else-if="loadingDecompiled" class="loading-placeholder">
              <div class="loading-spinner"></div>
              <span>LOADING DECOMPILED CODE...</span>
            </div>
            <div v-else class="empty-placeholder">
              <div class="empty-icon">
                <i class="fas fa-file-alt"></i>
              </div>
              <span class="empty-text">{{ t('detail.loading_source') }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 无选择状态 -->
    <div v-else class="no-selection">
      <div class="no-selection-icon">
        <i class="fas fa-file-code"></i>
      </div>
      <span class="no-selection-text">{{ t('detail.no_selection') }}</span>
    </div>
    
    <!-- 注入日志对话框 -->
    <div v-if="injectDialogVisible" class="dialog-overlay" @click="closeDialog">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
            <div class="dialog-icon">
              <i class="fas fa-plus"></i>
            </div>
            <h3 class="dialog-title">{{ t('detail.inject_log') }}</h3>
            <button class="dialog-close" @click="closeDialog">
              <i class="fas fa-times"></i>
            </button>
          </div>
        <div class="dialog-body">
          <div class="form-group">
            <label class="form-label">{{ t('detail.injection_type') }}</label>
            <select 
              v-model="injectForm.injectionType" 
              class="form-select"
            >
              <option value="ENTER_METHOD">{{ t('detail.before_method') }}</option>
              <option value="EXIT_METHOD">{{ t('detail.after_method') }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('detail.log_content') }}</label>
            <textarea 
              v-model="injectForm.logContent" 
              class="form-textarea"
              :placeholder="t('detail.enter_log_content')"
              rows="4"
            ></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="dialog-button cancel-button" @click="closeDialog">{{ t('detail.cancel') }}</button>
          <button 
            class="dialog-button primary-button"
            @click="handleInjectLog"
            :disabled="injecting"
          >
            <div v-if="injecting" class="loading-spinner-small"></div>
            <span>{{ injecting ? t('detail.injecting') : t('detail.inject') }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useI18n } from 'vue-i18n'
import {getDecompiledCode, injectMethodLog} from '../utils/api'
import {CodeEditor} from 'monaco-editor-vue3'

export default {
  name: 'ClassDetail',
  components: {
    CodeEditor
  },
  setup() {
    const { t } = useI18n()
    return { t }
  },
  props: {
    classInfo: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      activeTab: 'fields',
      decompiledCode: '',
      loadingDecompiled: false,
      editorOptions: {
        readOnly: true,
        automaticLayout: true,
        minimap: {
          enabled: true
        },
        scrollBeyondLastLine: false,
        fontSize: 14,
        theme: 'vs-dark',
        wordWrap: 'on',
        wrappingIndent: 'indent',
        fixedOverflowWidgets: true
      },
      injectDialogVisible: false,
      injecting: false,
      injectForm: {
        injectionType: 'ENTER_METHOD',
        logContent: ''
      },
      currentMethod: null
    }
  },
  methods: {
    async loadDecompiledCode() {
      if (!this.classInfo) return

      this.loadingDecompiled = true
      try {
        const code = await getDecompiledCode(this.classInfo.className)
        this.decompiledCode = code
      } catch (error) {
        console.error('加载反编译代码失败:', error)
        this.$message.error('加载反编译代码失败')
      } finally {
        this.loadingDecompiled = false
      }
    },
    handleEditorChange(value) {
      console.log('Editor content changed:', value)
    },
    showInjectDialog(method) {
      this.currentMethod = method
      this.injectForm.logContent = `执行方法: ${this.classInfo.className}.${method.name}`
      this.injectDialogVisible = true
    },
    closeDialog() {
      this.injectDialogVisible = false
      this.injecting = false
    },
    async handleInjectLog() {
      if (!this.classInfo || !this.currentMethod) return

      this.injecting = true
      try {
        const injectionData = {
          class: this.classInfo.className,
          method: this.currentMethod.name,
          injectionType: this.injectForm.injectionType,
          codeType: 'EXPRESSION',
          code: `LOG:${this.injectForm.logContent}`,
          desc: this.currentMethod.descriptor
        }

        const result = await injectMethodLog(injectionData)

        if (result.success) {
          this.$message.success('日志注入成功')
          this.injectDialogVisible = false
        } else {
          this.$message.error(result.error || '注入失败')
        }
      } catch (error) {
        console.error('方法注入失败:', error)
        this.$message.error('方法注入失败: ' + error.message)
      } finally {
        this.injecting = false
      }
    }
  }
}
</script>

<style scoped>
.class-detail {
  height: 100%;
  overflow: auto;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

/* 类信息卡片 */
.class-info-card {
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

/* 卡片头部 */
.card-header {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: var(--bg-tertiary);
}

.card-icon {
  color: var(--accent-primary);
  flex-shrink: 0;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

/* 卡片内容 */
.card-body {
  padding: 8px 12px;
}

/* 信息网格 */
.info-grid {
  display: flex;
  gap: 24px;
  margin-bottom: 8px;
}

/* 信息项 */
.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 11px;
  color: var(--text-tertiary);
}

.info-value {
  font-size: 12px;
  color: var(--text-primary);
}

/* 接口部分 */
.interfaces-section {
  margin-top: 8px;
}

.section-label {
  display: block;
  font-size: 11px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}

/* 接口标签 */
.interface-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.interface-tag {
  padding: 2px 8px;
  background-color: var(--bg-hover);
  font-size: 11px;
  color: var(--text-primary);
}

.interface-tag:hover {
  background-color: var(--border-color);
}

/* 详情标签页 */
.detail-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
}

/* 标签页头部 */
.tabs-header {
  display: flex;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

/* 标签项 */
.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab-item:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.tab-item.active {
  color: var(--text-primary);
  border-bottom: 1px solid var(--accent-primary);
}

/* 标签页内容 */
.tabs-content {
  flex: 1;
  overflow: hidden;
}

/* 标签面板 */
.tab-panel {
  height: 100%;
  padding: 8px;
  overflow: auto;
}

/* 表格容器 */
.table-container {
  height: 100%;
  overflow: auto;
}

/* 数据表格 */
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.data-table th {
  background-color: var(--bg-tertiary);
  color: var(--text-tertiary);
  font-weight: 500;
  text-align: left;
  padding: 4px 8px;
  border-bottom: 1px solid var(--border-color);
  font-size: 11px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table td {
  padding: 4px 8px;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
}

/* 表格行 */
.table-row:hover {
  background-color: var(--bg-hover);
}

/* 参数标签 */
.parameter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.parameter-tag {
  padding: 1px 6px;
  background-color: var(--bg-hover);
  font-size: 11px;
  color: var(--text-primary);
}

/* 注入按钮 */
.inject-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background-color: transparent;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 11px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.inject-button:hover {
  background-color: var(--bg-hover);
  border-color: var(--text-tertiary);
  color: var(--text-primary);
}

/* 反编译部分 */
.decompile-section {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 加载按钮 */
.load-button {
  display: flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  padding: 4px 12px;
  background-color: var(--bg-hover);
  border: none;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.load-button:hover {
  background-color: var(--border-color);
  color: var(--text-primary);
}

.load-button:disabled {
  background-color: var(--bg-tertiary);
  color: var(--text-tertiary);
  cursor: not-allowed;
}

/* 编辑器容器 */
.editor-container {
  flex: 1;
  border: 1px solid var(--border-color);
  overflow: hidden;
  background-color: var(--bg-primary);
}

.code-editor {
  height: 100%;
}

/* 加载占位符 */
.loading-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  color: var(--text-tertiary);
  font-size: 12px;
}

/* 空状态占位符 */
.empty-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  color: var(--text-tertiary);
}

.empty-icon {
  color: var(--text-tertiary);
}

.empty-text {
  font-size: 12px;
}

/* 无选择状态 */
.no-selection {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: var(--text-tertiary);
}

.no-selection-icon {
  color: var(--text-tertiary);
}

.no-selection-text {
  font-size: 13px;
}

/* 对话框覆盖层 */
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

/* 对话框内容 */
.dialog-content {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  width: 90%;
  max-width: 480px;
}

/* 对话框头部 */
.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
}

.dialog-icon {
  color: var(--accent-primary);
  flex-shrink: 0;
}

.dialog-title {
  flex: 1;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

/* 对话框关闭按钮 */
.dialog-close {
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
}

.dialog-close:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

/* 对话框内容 */
.dialog-body {
  padding: 12px;
}

/* 表单组 */
.form-group {
  margin-bottom: 12px;
}

.form-label {
  display: block;
  font-size: 11px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}

/* 表单选择 */
.form-select {
  width: 100%;
  padding: 6px 8px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 12px;
}

.form-select:focus {
  outline: none;
  border-color: var(--border-focus);
}

/* 表单文本域 */
.form-textarea {
  width: 100%;
  padding: 6px 8px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 12px;
  resize: vertical;
}

.form-textarea:focus {
  outline: none;
  border-color: var(--border-focus);
}

/* 对话框底部 */
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 12px;
  border-top: 1px solid var(--border-color);
  background-color: var(--bg-tertiary);
}

/* 对话框按钮 */
.dialog-button {
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.cancel-button {
  background-color: var(--bg-hover);
  border: none;
  color: var(--text-secondary);
}

.cancel-button:hover {
  background-color: var(--border-color);
  color: var(--text-primary);
}

.primary-button {
  background-color: var(--accent-primary);
  border: none;
  color: white;
}

.primary-button:hover {
  background-color: #0066b3;
}

.primary-button:disabled {
  background-color: var(--bg-tertiary);
  color: var(--text-tertiary);
  cursor: not-allowed;
}

/* 加载动画 */
.loading-spinner {
  width: 20px;
  height: 20px;
  border: 1px solid var(--border-color);
  border-top: 1px solid var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-spinner-small {
  width: 14px;
  height: 14px;
  border: 1px solid var(--border-color);
  border-top: 1px solid var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 无数据状态 */
.no-data {
  color: var(--text-tertiary);
  font-size: 11px;
}
</style>