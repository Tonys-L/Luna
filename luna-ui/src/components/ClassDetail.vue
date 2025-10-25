<template>
  <div v-if="classInfo" class="class-detail">
    <el-card class="class-header">
      <template #header>
        <div class="card-header">
          <span>类信息: {{ classInfo.className }}</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="info-item"><strong>父类:</strong> {{ classInfo.superClass || '无' }}</div>
        </el-col>
        <el-col :span="12">
          <div class="info-item"><strong>访问标志:</strong> {{ classInfo.readableAccessFlags || classInfo.accessFlags }}</div>
        </el-col>
      </el-row>
      <div class="interfaces-section">
        <strong>实现接口:</strong>
        <div class="tags-container">
          <el-tag 
            v-for="iface in classInfo.interfaces" 
            :key="iface" 
            class="interface-tag"
          >
            {{ iface }}
          </el-tag>
          <div v-if="!classInfo.interfaces || classInfo.interfaces.length === 0" class="no-data">无</div>
        </div>
      </div>
    </el-card>
    
    <el-tabs class="detail-tabs" type="border-card">
      <el-tab-pane label="字段">
        <el-table :data="classInfo.convertedFields || classInfo.fields" class="data-table" size="small">
          <el-table-column label="访问标志" min-width="150">
            <template #default="scope">
              {{ `(${scope.row.accessFlags}) ${scope.row.readableAccessFlags}` }}
            </template>
          </el-table-column>
          <el-table-column label="字段名" min-width="150" prop="name"></el-table-column>
          <el-table-column label="描述符" min-width="200" prop="descriptor"></el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="方法">
        <el-table :data="classInfo.convertedMethods || classInfo.methods" class="data-table" size="small">
          <el-table-column label="访问标志" min-width="150">
            <template #default="scope">
              {{ `(${scope.row.accessFlags}) ${scope.row.readableAccessFlags}` }}
            </template>
          </el-table-column>
          <el-table-column label="方法名" min-width="150" prop="name"></el-table-column>
          <el-table-column label="描述符" min-width="200" prop="descriptor"></el-table-column>
          <el-table-column label="参数" min-width="200">
            <template #default="scope">
              <div class="parameter-tags">
                <el-tag 
                  v-for="param in scope.row.parameters" 
                  :key="param.name" 
                  class="parameter-tag"
                  size="small"
                >
                  {{ param.name }}: {{ param.descriptor }}
                </el-tag>
                <div v-if="!scope.row.parameters || scope.row.parameters.length === 0" class="no-data">无参数</div>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="反编译源码">
        <div class="decompile-section">
          <el-button class="load-button" type="primary" @click="loadDecompiledCode">加载反编译源码</el-button>
          <div v-if="decompiledCode" class="editor-wrapper">
            <div class="editor-container">
              <CodeEditor
                :options="editorOptions"
                :value="decompiledCode"
                class="code-editor"
                language="java"
                @change="handleEditorChange"
              />
            </div>
          </div>
          <div v-else-if="loadingDecompiled" class="loading-placeholder">
            <i class="el-icon-loading"></i> 正在加载反编译源码...
          </div>
          <div v-else class="empty-placeholder">
            <el-empty description="点击上方按钮加载反编译源码">
              <template #image>
                <i class="el-icon-document" style="font-size: 60px; color: #363637;"></i>
              </template>
            </el-empty>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
  <div v-else class="no-selection">
    <el-empty description="请选择一个类查看详细信息">
      <template #image>
        <i class="el-icon-document" style="font-size: 60px; color: #363637;"></i>
      </template>
    </el-empty>
  </div>
</template>

<script>
import {getDecompiledCode} from '../utils/api'
import {CodeEditor} from 'monaco-editor-vue3'

export default {
  name: 'ClassDetail',
  components: {
    CodeEditor
  },
  props: {
    classInfo: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
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
      }
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
      // 编辑器内容变化时的处理函数（只读模式下不会触发）
      console.log('Editor content changed:', value)
    }
  }
}
</script>

<style scoped>
.class-detail {
  padding: 20px;
  height: 100%;
  overflow: auto;
  background-color: #141414;
  display: flex;
  flex-direction: column;
}

.card-header {
  font-weight: bold;
  color: #e5eaf3;
  font-size: 16px;
}

.info-item {
  padding: 5px 0;
  color: #cfd3dc;
}

.interfaces-section {
  margin-top: 15px;
  color: #e5eaf3;
}

.interfaces-section > strong {
  display: block;
  margin-bottom: 8px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.interface-tag {
  background-color: #262727;
  border-color: #363637;
  color: #cfd3dc;
  margin: 0;
}

.no-data {
  color: #a3a6ad;
  font-style: italic;
  padding: 2px 0;
}

.detail-tabs {
  margin-top: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow: hidden;
  height: 100%;
}

.detail-tabs :deep(.el-tab-pane) {
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.data-table {
  width: 100%;
  height: 100%;
}

.parameter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  padding: 5px 0;
}

.parameter-tag {
  background-color: #262727;
  border-color: #363637;
  color: #cfd3dc;
  margin: 0;
}

.decompile-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  flex: 1;
  overflow: hidden;
}

.load-button {
  align-self: center;
  margin: 10px 0;
}

.editor-wrapper {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.editor-container {
  flex: 1;
  border: 1px solid #363637;
  border-radius: 4px;
  overflow: hidden;
  min-height: 500px;
}

.code-editor {
  height: 100%;
  min-height: 500px;
}

.loading-placeholder {
  text-align: center;
  padding: 30px;
  color: #a3a6ad;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-selection {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background-color: #141414;
}

:deep(.el-card) {
  background-color: #1d1e1f;
  border: 1px solid #363637;
  color: #e5eaf3;
  margin-bottom: 20px;
  flex-shrink: 0;
}

:deep(.el-card__header) {
  background-color: #262727;
  border-bottom: 1px solid #363637;
  color: #e5eaf3;
}

:deep(.el-tabs) {
  background-color: #1d1e1f;
  border: 1px solid #363637;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.el-tabs__header) {
  margin-bottom: 0;
  flex-shrink: 0;
}

:deep(.el-tabs__item) {
  color: #cfd3dc;
}

:deep(.el-tabs__item.is-active) {
  color: #409EFF;
}

:deep(.el-tabs__active-bar) {
  background-color: #409EFF;
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: #363637;
}

:deep(.el-table) {
  background-color: #1d1e1f;
  height: 100%;
}

:deep(.el-table__header) {
  background-color: #262727;
  color: #e5eaf3;
}

:deep(.el-table__body) {
  background-color: #1d1e1f;
  color: #cfd3dc;
}

:deep(.el-table__row) {
  background-color: #1d1e1f;
}

:deep(.el-table__row:hover) {
  background-color: #262727;
}

/* 隐藏表格内的滚动条 */
:deep(.el-table__body-wrapper::-webkit-scrollbar) {
  display: none;
}

:deep(.el-table__body-wrapper) {
  overflow: auto;
}
</style>
