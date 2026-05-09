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
                :key="'decompile-' + classInfo?.className"
                :options="editorOptions"
                :value="decompiledCode"
                class="code-editor"
                language="java"
                @change="handleEditorChange"
                @editorWillMount="onEditorWillMount"
                @editorDidMount="onEditorMount"
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
    
    <!-- 注入详情弹窗 -->
    <div v-if="injectionDetailVisible && selectedInjectionMarker" class="injection-detail-overlay" @click.self="injectionDetailVisible = false">
      <div class="injection-detail-panel">
        <div class="injection-detail-header">
          <span class="injection-detail-type" :class="'type-' + selectedInjectionMarker.type.toLowerCase().replace('_', '-')">
            {{ getInjectionTypeLabel(selectedInjectionMarker.type) }}
          </span>
          <button class="injection-detail-close" @click="injectionDetailVisible = false">&times;</button>
        </div>
        <div class="injection-detail-body">
          <div class="injection-detail-row">
            <span class="injection-detail-label">Method:</span>
            <span class="injection-detail-value">{{ selectedInjectionMarker.method }}</span>
          </div>
          <div v-if="selectedInjectionMarker.lineNumber" class="injection-detail-row">
            <span class="injection-detail-label">Line:</span>
            <span class="injection-detail-value">{{ selectedInjectionMarker.lineNumber }}</span>
          </div>
          <div class="injection-detail-row">
            <span class="injection-detail-label">Condition:</span>
            <code class="injection-detail-code">{{ parseInjectionCondition(selectedInjectionMarker.code) || '无条件 (Always)' }}</code>
          </div>
          <div class="injection-detail-row">
            <span class="injection-detail-label">Log:</span>
            <code class="injection-detail-code">{{ parseInjectionLog(selectedInjectionMarker.code) }}</code>
          </div>
          <div class="injection-detail-row">
            <span class="injection-detail-label">Time:</span>
            <span class="injection-detail-value">{{ new Date(selectedInjectionMarker.timestamp).toLocaleString() }}</span>
          </div>
        </div>
        <div class="injection-detail-footer">
          <button class="injection-detail-delete" @click="removeInjectionPoint(selectedInjectionMarker)">
            Delete Injection
          </button>
        </div>
      </div>
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
              <option value="AROUND_METHOD">方法环绕 (Around)</option>
              <option value="LINE_BEFORE">行号前 (Before Line)</option>
              <option value="LINE_AFTER">行号后 (After Line)</option>
            </select>
          </div>
          <div class="form-group" v-if="injectForm.injectionType === 'LINE_BEFORE' || injectForm.injectionType === 'LINE_AFTER'">
            <label class="form-label">源码行号 (Source Line Number)</label>
            <select 
              v-model.number="injectForm.lineNumber" 
              class="form-select"
            >
              <option :value="null" disabled>请选择源码行号</option>
              <option 
                v-for="line in availableSourceLines" 
                :key="line" 
                :value="line"
              >
                Line {{ line }}
              </option>
            </select>
            <div class="form-hint" v-if="availableSourceLines.length === 0">
              <i class="fas fa-exclamation-triangle" style="color: #f59e0b;"></i>
              <span>未找到该方法的源码行号表，请先加载反编译代码</span>
            </div>
            <div class="form-hint" v-else>
              <i class="fas fa-info-circle"></i>
              <span>选择字节码中实际存在的源码行号 (共 {{ availableSourceLines.length }} 行)</span>
            </div>
          </div>
          <div class="form-group" v-if="isLineInjection && injectForm.lineNumber">
            <label class="form-label">可用局部变量 (Local Variables)</label>
            <div class="local-vars-container" v-if="loadingLocalVariables">
              <div class="loading-spinner-small"></div>
              <span>加载中...</span>
            </div>
            <div class="local-vars-container" v-else-if="localVariables.length > 0">
              <span 
                v-for="v in localVariables" 
                :key="v.name + v.slot"
                class="local-var-tag"
                @click="insertLocalVariableRef(v.name)"
                :title="'点击插入 $' + v.name"
              >
                ${{ v.name }} <span class="local-var-type">({{ formatDescriptor(v.descriptor) }})</span>
              </span>
            </div>
            <div class="local-vars-container" v-else>
              <span class="no-data">该行号处无可见局部变量</span>
            </div>
            <div class="form-hint">
              <i class="fas fa-info-circle"></i>
              <span>点击变量名可插入到表达式中，使用 $varName 引用局部变量</span>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('detail.code_type') }}</label>
            <select 
              v-model="injectForm.codeType" 
              class="form-select"
            >
              <option value="EXPRESSION">表达式 (Expression)</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">条件表达式 (Condition Expression)</label>
            <input 
              v-model="injectForm.condition" 
              type="text" 
              class="form-input"
              placeholder="例如: param[0] != null 或者 age >= 18 (留空表示无条件)"
            />
            <div class="form-hint">
              <i class="fas fa-info-circle"></i>
              <span>满足条件时才会执行注入。支持使用 $1, $2 或者 param[0], $varName 等引用变量。</span>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">{{ t('detail.log_content') }}</label>
            <textarea 
              v-model="injectForm.logContent" 
              class="form-textarea"
              :placeholder="t('detail.enter_log_content')"
              rows="4"
            ></textarea>
            <div class="form-hint">
              <i class="fas fa-info-circle"></i>
              <span>使用 $1, $2, $3... 引用方法参数，$varName 引用局部变量，例如: name=$1, result=$result</span>
            </div>
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
import { markRaw } from 'vue'
import {getDecompiledCode, injectMethodLog, getLineNumbers, getLocalVariables, getInjectionList, removeInjection} from '../utils/api'
import {CodeEditor} from 'monaco-editor-vue3'
import * as monaco from 'monaco-editor'

export default {
  name: 'ClassDetail',
  components: {
    CodeEditor
  },
  setup() {
    const { t } = useI18n()
    return { t }
  },
  computed: {
    editorOptions() {
      const self = this
      return {
        readOnly: true,
        automaticLayout: true,
        minimap: { enabled: true },
        scrollBeyondLastLine: false,
        fontSize: 14,
        theme: 'vs-dark',
        wordWrap: 'on',
        wrappingIndent: 'indent',
        fixedOverflowWidgets: true,
        glyphMargin: true,
        lineNumbers: (lineNumber) => {
          if (self.sourceLineMapping && self.sourceLineMapping[lineNumber]) {
            return String(self.sourceLineMapping[lineNumber])
          }
          return String(lineNumber)
        }
      }
    },
    availableSourceLines() {
      if (!this.lineNumberMap) return []
      const methodName = this.currentMethod ? this.currentMethod.name : null
      const methodDesc = this.currentMethod ? this.currentMethod.descriptor : null

      const normalizeDesc = (desc) => {
        return desc.replace(/\//g, '.').replace(/\./g, '')
      }

      if (methodName) {
        for (const [methodKey, lineNumbers] of Object.entries(this.lineNumberMap)) {
          const keyName = methodKey.split('(')[0]
          if (keyName === methodName) {
            if (!methodDesc) {
              return Array.isArray(lineNumbers) ? [...new Set(lineNumbers)].sort((a, b) => a - b) : []
            }
            const keyDesc = '(' + methodKey.split('(').slice(1).join('(')
            if (normalizeDesc(keyDesc) === normalizeDesc(methodDesc)) {
              return Array.isArray(lineNumbers) ? [...new Set(lineNumbers)].sort((a, b) => a - b) : []
            }
          }
        }
        for (const [methodKey, lineNumbers] of Object.entries(this.lineNumberMap)) {
          const keyName = methodKey.split('(')[0]
          if (keyName === methodName) {
            return Array.isArray(lineNumbers) ? [...new Set(lineNumbers)].sort((a, b) => a - b) : []
          }
        }
      }

      const allLines = new Set()
      for (const lineNumbers of Object.values(this.lineNumberMap)) {
        if (Array.isArray(lineNumbers)) {
          lineNumbers.forEach(l => allLines.add(l))
        }
      }
      return [...allLines].sort((a, b) => a - b)
    },
    isLineInjection() {
      return this.injectForm.injectionType === 'LINE_BEFORE' || this.injectForm.injectionType === 'LINE_AFTER'
    }
  },
  watch: {
    'injectForm.lineNumber'() {
      if (this.isLineInjection && this.injectForm.lineNumber) {
        this.fetchLocalVariables()
      } else {
        this.localVariables = []
      }
    },
    'injectForm.injectionType'() {
      if (this.isLineInjection && this.injectForm.lineNumber) {
        this.fetchLocalVariables()
      } else {
        this.localVariables = []
      }
    }
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
      injectDialogVisible: false,
      injecting: false,
      injectForm: {
        injectionType: 'ENTER_METHOD',
        codeType: 'EXPRESSION',
        logContent: '',
        condition: '',
        lineNumber: null
      },
      currentMethod: null,
      monacoEditor: null,
      lineNumberMap: {},
      localVariables: [],
      loadingLocalVariables: false,
      sourceLineMapping: {},
      injectionMarkers: [],
      injectionDecorations: null,
      sourceLineDecorations: null,
      loadedClassName: null,
      selectedInjectionMarker: null,
      injectionDetailVisible: false
    }
  },
  methods: {
    async loadDecompiledCode() {
      if (!this.classInfo) return

      this.loadingDecompiled = true
      try {
        const code = await getDecompiledCode(this.classInfo.className)

        if (!code || code.trim() === '') {
          this.decompiledCode = '// Decompilation returned empty result for class: ' + this.classInfo.className
          this.sourceLineMapping = {}
          this.lineNumberMap = {}
          this.loadingDecompiled = false
          return
        }

        const { cleanCode, mapping } = this.parseSourceLineComments(code)
        this.decompiledCode = cleanCode
        this.sourceLineMapping = mapping
        if (this.classInfo.className !== this.loadedClassName) {
          this.injectionMarkers = []
          this.loadedClassName = this.classInfo.className
        }
        if (this.monacoEditor) {
          this.injectionDecorations = markRaw(this.monacoEditor.createDecorationsCollection([]))
          this.sourceLineDecorations = markRaw(this.monacoEditor.createDecorationsCollection([]))
        } else {
          this.injectionDecorations = null
          this.sourceLineDecorations = null
        }
      } catch (error) {
        console.error('加载反编译代码失败:', error)
        this.decompiledCode = '// Failed to load decompiled code: ' + (error.message || error)
        this.sourceLineMapping = {}
      } finally {
        this.loadingDecompiled = false
      }

      getLineNumbers(this.classInfo.className).then(lineNumbers => {
        this.lineNumberMap = lineNumbers || {}
        if (this.monacoEditor && this.sourceLineDecorations) {
          this.$nextTick(() => {
            this.updateSourceLineDecorations()
          })
        }
      }).catch(err => {
        console.error('获取行号表失败:', err)
        this.lineNumberMap = {}
      })

      getInjectionList(this.classInfo.className).then(injectionData => {
        if (injectionData && injectionData.injections && injectionData.injections.length > 0) {
          const existingIds = new Set(this.injectionMarkers.map(m => m.id))
          for (const inj of injectionData.injections) {
            if (existingIds.has(inj.id)) continue
            let editorLine = null
            if (inj.lineNumber && this.sourceLineMapping) {
              for (const [eLine, sLine] of Object.entries(this.sourceLineMapping)) {
                if (sLine === inj.lineNumber) {
                  editorLine = parseInt(eLine)
                  break
                }
              }
            }
            if (!editorLine && inj.method && this.decompiledCode) {
              editorLine = this.findEditorLineForMethod(inj.method)
            }
            this.injectionMarkers.push({
              id: inj.id,
              type: inj.type,
              method: inj.method,
              lineNumber: inj.lineNumber,
              code: inj.code,
              editorLine: editorLine,
              timestamp: Date.now()
            })
          }
          if (this.monacoEditor && this.injectionDecorations) {
            this.$nextTick(() => {
              this.updateInjectionDecorations()
            })
          }
        }
      }).catch(err => {
        console.error('Failed to load injection list:', err)
      })
    },
    parseSourceLineComments(code) {
      if (typeof code !== 'string') {
        console.error('[Luna] parseSourceLineComments received non-string input:', typeof code, code)
        return { cleanCode: String(code || ''), mapping: {} }
      }

      const mapping = {}
      const lines = code.split('\n')
      const commentPattern = /^\/\*\s*(\d+)\s*\*\/\s?(.*)$/

      for (let i = 0; i < lines.length; i++) {
        const match = lines[i].match(commentPattern)
        if (match) {
          mapping[i + 1] = parseInt(match[1], 10)
        }
      }

      return { cleanCode: code, mapping }
    },
    getSourceLineForEditorLine(editorLine) {
      if (this.sourceLineMapping && this.sourceLineMapping[editorLine]) {
        return this.sourceLineMapping[editorLine]
      }
      if (this.sourceLineMapping) {
        let closest = null
        let minDist = Infinity
        for (const [eLine, sLine] of Object.entries(this.sourceLineMapping)) {
          const dist = Math.abs(parseInt(eLine) - editorLine)
          if (dist < minDist) {
            minDist = dist
            closest = sLine
          }
        }
        return closest
      }
      return null
    },
    handleEditorChange(value) {
      console.log('Editor content changed:', value)
    },
    onEditorWillMount() {
    },
    onEditorMount(editor) {
      this.monacoEditor = markRaw(editor)

      this.injectionDecorations = markRaw(editor.createDecorationsCollection([]))
      this.sourceLineDecorations = markRaw(editor.createDecorationsCollection([]))

      if (this.lineNumberMap && Object.keys(this.lineNumberMap).length > 0) {
        this.updateSourceLineDecorations()
      }

      if (this.injectionMarkers.length > 0) {
        this.$nextTick(() => {
          this.updateInjectionDecorations()
        })
      }

      editor.addAction({
        id: 'luna-inject-before-line',
        label: '在此行前注入 (Before Line)',
        keybindings: [],
        contextMenuGroupId: 'luna-injection',
        contextMenuOrder: 1,
        run: (ed) => {
          const position = ed.getPosition()
          if (position) {
            this.showLineInjectDialog(position.lineNumber, 'LINE_BEFORE')
          }
        }
      })

      editor.addAction({
        id: 'luna-inject-after-line',
        label: '在此行后注入 (After Line)',
        keybindings: [],
        contextMenuGroupId: 'luna-injection',
        contextMenuOrder: 2,
        run: (ed) => {
          const position = ed.getPosition()
          if (position) {
            this.showLineInjectDialog(position.lineNumber, 'LINE_AFTER')
          }
        }
      })

      editor.onMouseDown((e) => {
        if (
          e.target &&
          e.target.type &&
          (e.target.type === 2 || e.target.type === 3)
        ) {
          const lineNumber = e.target.position ? e.target.position.lineNumber : e.target.detail ? e.target.detail.lineNumber : null
          if (lineNumber) {
            const clickedMarker = this.injectionMarkers.find(m => m.editorLine === lineNumber)
            if (clickedMarker) {
              this.selectedInjectionMarker = clickedMarker
              this.injectionDetailVisible = true
            } else {
              this.showLineInjectDialog(lineNumber, 'LINE_BEFORE')
            }
          }
        }
      })
    },
    updateSourceLineDecorations() {
      if (!this.monacoEditor || !this.decompiledCode || !this.lineNumberMap) return
      if (!this.sourceLineDecorations) return

      const decorations = []
      const lines = this.decompiledCode.split('\n')

      for (const [methodKey, lineNumbers] of Object.entries(this.lineNumberMap)) {
        if (!Array.isArray(lineNumbers) || lineNumbers.length === 0) continue

        const methodName = methodKey.split('(')[0]

        for (let i = 0; i < lines.length; i++) {
          const line = lines[i]
          if (line.includes(methodName) && line.includes('(')) {
            const minLine = Math.min(...lineNumbers)
            const maxLine = Math.max(...lineNumbers)
            decorations.push({
              range: new monaco.Range(i + 1, 1, i + 1, 1),
              options: {
                isWholeLine: true,
                glyphMarginClassName: 'source-line-glyph',
                glyphMarginHoverMessage: {
                  value: `**${methodName}** source lines: ${minLine}-${maxLine} ([${lineNumbers.join(', ')}])`
                }
              }
            })
            break
          }
        }
      }

      this.sourceLineDecorations.set(decorations)
    },
    showLineInjectDialog(editorLineNumber, injectionType) {
      const methods = this.classInfo.convertedMethods || this.classInfo.methods || []
      const currentLineMethod = this.findMethodByLine(editorLineNumber)
      if (currentLineMethod) {
        this.currentMethod = currentLineMethod
      } else if (methods.length > 0 && !this.currentMethod) {
        this.currentMethod = methods[0]
      }
      this.injectForm.injectionType = injectionType
      const sourceLine = this.getSourceLineForEditorLine(editorLineNumber)
      if (sourceLine) {
        this.injectForm.lineNumber = sourceLine
      } else {
        this.injectForm.lineNumber = null
      }
      this.injectForm.logContent = `${injectionType === 'LINE_BEFORE' ? '行前' : '行后'}注入`
      this.injectForm.condition = ''
      this.injectDialogVisible = true
    },
    findMethodByLine(lineNumber) {
      if (!this.decompiledCode || !this.classInfo) return null
      const methods = this.classInfo.convertedMethods || this.classInfo.methods || []
      const lines = this.decompiledCode.split('\n')
      let bestMethod = null
      let bestDistance = Infinity
      for (const method of methods) {
        if (method.name === '<init>' || method.name === '<clinit>') continue
        const methodName = method.name
        for (let i = 0; i < lines.length; i++) {
          if (lines[i] && lines[i].includes(methodName) && lines[i].includes('(') && lines[i].includes(')')) {
            const dist = Math.abs(i + 1 - lineNumber)
            if (dist < bestDistance && i + 1 <= lineNumber) {
              bestDistance = dist
              bestMethod = method
            }
          }
        }
      }
      return bestMethod
    },
    showInjectDialog(method) {
      this.currentMethod = method
      this.injectForm.logContent = `执行方法: ${this.classInfo.className}.${method.name}`
      this.injectForm.condition = ''
      this.localVariables = []
      this.injectDialogVisible = true
    },
    closeDialog() {
      this.injectDialogVisible = false
      this.injecting = false
    },
    async fetchLocalVariables() {
      if (!this.classInfo || !this.currentMethod || !this.injectForm.lineNumber) {
        this.localVariables = []
        return
      }
      this.loadingLocalVariables = true
      try {
        const result = await getLocalVariables(
          this.classInfo.className,
          this.currentMethod.name,
          this.currentMethod.descriptor,
          this.injectForm.lineNumber
        )
        this.localVariables = result.variables || []
      } catch (error) {
        console.error('获取局部变量失败:', error)
        this.localVariables = []
      } finally {
        this.loadingLocalVariables = false
      }
    },
    insertLocalVariableRef(varName) {
      this.injectForm.logContent += '$' + varName
    },
    formatDescriptor(desc) {
      if (!desc) return ''
      const typeMap = {
        'Z': 'boolean', 'B': 'byte', 'C': 'char', 'S': 'short',
        'I': 'int', 'J': 'long', 'F': 'float', 'D': 'double',
        'V': 'void'
      }
      if (desc.startsWith('L') && desc.endsWith(';')) {
        return desc.substring(1, desc.length - 1).replace(/\//g, '.')
      }
      if (desc.startsWith('[')) {
        return this.formatDescriptor(desc.substring(1)) + '[]'
      }
      return typeMap[desc] || desc
    },
    addInjectionMarker(injectionData, injectionPointId) {
      const type = injectionData.injectionType
      const method = injectionData.method
      const lineNumber = injectionData.lineNumber
      const code = injectionData.code

      let editorLine = null
      if ((type === 'LINE_BEFORE' || type === 'LINE_AFTER') && lineNumber) {
        for (const [eLine, sLine] of Object.entries(this.sourceLineMapping)) {
          if (sLine === lineNumber) {
            editorLine = parseInt(eLine)
            break
          }
        }
        if (!editorLine && this.decompiledCode) {
          const lines = this.decompiledCode.split('\n')
          for (let i = 0; i < lines.length; i++) {
            if (lines[i].includes(method)) {
              for (let j = i; j < Math.min(i + 30, lines.length); j++) {
                const srcLine = this.sourceLineMapping[j + 1]
                if (srcLine === lineNumber) {
                  editorLine = j + 1
                  break
                }
              }
              if (editorLine) break
            }
          }
        }
      }

      if (!editorLine && type !== 'LINE_BEFORE' && type !== 'LINE_AFTER' && method) {
        editorLine = this.findEditorLineForMethod(method)
      }

      console.log('[Luna] addInjectionMarker:', { type, method, lineNumber, editorLine, sourceLineMapping: this.sourceLineMapping, decompiledCode: !!this.decompiledCode, monacoEditor: !!this.monacoEditor })

      this.injectionMarkers.push({
        id: injectionPointId,
        type,
        method,
        lineNumber,
        code,
        editorLine,
        timestamp: Date.now()
      })

      this.activeTab = 'decompile'

      const tryUpdate = (attempt) => {
        if (attempt > 20) {
          console.log('[Luna] gave up waiting for editor after 20 attempts')
          return
        }
        this.$nextTick(() => {
          if (this.monacoEditor && this.injectionDecorations) {
            this.updateInjectionDecorations()
          } else {
            console.log('[Luna] editor not ready, retry ' + attempt + '...')
            setTimeout(() => tryUpdate(attempt + 1), 300)
          }
        })
      }

      if (!this.decompiledCode) {
        this.loadDecompiledCode().then(() => {
          setTimeout(() => tryUpdate(1), 500)
        })
      } else {
        tryUpdate(1)
      }
    },
    updateInjectionDecorations() {
      if (!this.monacoEditor || !this.injectionDecorations) return

      const decorations = []

      for (const marker of this.injectionMarkers) {
        let targetLine = marker.editorLine

        if (!targetLine && marker.method) {
          targetLine = this.findEditorLineForMethod(marker.method)
        }

        console.log('[Luna] decoration for marker:', { method: marker.method, lineNumber: marker.lineNumber, editorLine: marker.editorLine, targetLine })

        if (!targetLine) continue

        const typeLabel = this.getInjectionTypeLabel(marker.type)

        let glyphClassName = 'injected-glyph'
        let lineBgClassName = 'injected-line-bg'
        if (marker.type === 'LINE_BEFORE') {
          glyphClassName = 'injected-glyph-before'
          lineBgClassName = 'injected-line-bg-before'
        } else if (marker.type === 'LINE_AFTER') {
          glyphClassName = 'injected-glyph-after'
          lineBgClassName = 'injected-line-bg-after'
        }

        decorations.push({
          range: new monaco.Range(targetLine, 1, targetLine, 1),
          options: {
            isWholeLine: true,
            className: lineBgClassName,
            glyphMarginClassName: glyphClassName,
            glyphMarginHoverMessage: {
              value: `**[${typeLabel}]**\n- **Condition:** ${this.parseInjectionCondition(marker.code) || '无条件 (Always)'}\n- **Log:** ${this.parseInjectionLog(marker.code)}\n\nInjection time: ${new Date(marker.timestamp).toLocaleTimeString()}`
            }
          }
        })
      }

      console.log('[Luna] applying decorations:', decorations.length, 'items')

      this.injectionDecorations.set(decorations)

      this.monacoEditor.layout()
    },
    findEditorLineForMethod(methodName) {
      if (!this.decompiledCode) return null
      const lines = this.decompiledCode.split('\n')
      for (let i = 0; i < lines.length; i++) {
        if (lines[i].includes(methodName) && lines[i].includes('(')) {
          return i + 1
        }
      }
      return null
    },
    getInjectionTypeLabel(type) {
      const labels = {
        'ENTER_METHOD': 'Method Enter',
        'EXIT_METHOD': 'Method Exit',
        'AROUND_METHOD': 'Around',
        'LINE_BEFORE': 'Before Line',
        'LINE_AFTER': 'After Line'
      }
      return labels[type] || type
    },
    parseInjectionCondition(code) {
      if (!code) return ''
      const match = code.match(/^\$\{(.+?)\}::/)
      return match ? match[1] : ''
    },
    parseInjectionLog(code) {
      if (!code) return ''
      const match = code.match(/^\$\{(.+?)\}::(.*)/)
      if (match) return match[2]
      return code.startsWith('log:') ? code.substring(4) : code
    },
    async removeInjectionPoint(marker) {
      if (!marker || !marker.id) {
        this.$message.error('No injection point ID')
        return
      }
      try {
        const result = await removeInjection(marker.id)
        if (result.success) {
          this.$message.success('Injection point removed')
          this.injectionMarkers = this.injectionMarkers.filter(m => m.id !== marker.id)
          this.injectionDetailVisible = false
          this.updateInjectionDecorations()
        } else {
          this.$message.error(result.error || 'Failed to remove')
        }
      } catch (error) {
        this.$message.error('Failed to remove: ' + error.message)
      }
    },
    async handleInjectLog() {
      if (!this.classInfo || !this.currentMethod) return

      this.injecting = true
      try {
        const injectionData = {
          clazz: this.classInfo.className,
          method: this.currentMethod.name,
          injectionType: this.injectForm.injectionType,
          codeType: this.injectForm.codeType,
          code: this.injectForm.condition ? `\${${this.injectForm.condition}}::log:${this.injectForm.logContent}` : `log:${this.injectForm.logContent}`,
          desc: this.currentMethod.descriptor
        }

        if (this.injectForm.injectionType === 'LINE_BEFORE' || this.injectForm.injectionType === 'LINE_AFTER') {
          if (!this.injectForm.lineNumber || this.injectForm.lineNumber < 1) {
            this.$message.error('请选择源码行号')
            this.injecting = false
            return
          }
          injectionData.lineNumber = this.injectForm.lineNumber
        }

        const result = await injectMethodLog(injectionData)

        if (result.success) {
          this.$message.success('日志注入成功')
          this.addInjectionMarker(injectionData, result.injectionPointId)
          this.injectDialogVisible = false
        } else {
          this.$message.error(result.error || '注入失败')
        }
      } catch (error) {
        console.error('方法注入失败:', error)
        this.$message.error(error.message || '注入失败')
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

.form-input {
  width: 100%;
  padding: 6px 8px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  font-size: 12px;
}

.form-input:focus {
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

.form-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-tertiary, #888);
}

.form-hint i {
  font-size: 11px;
  color: var(--accent-color, #6366f1);
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

/* 局部变量容器 */
.local-vars-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
  min-height: 28px;
  padding: 4px;
  background-color: var(--bg-primary);
  border: 1px solid var(--border-color);
}

/* 局部变量标签 */
.local-var-tag {
  padding: 2px 8px;
  background-color: var(--bg-hover);
  font-size: 11px;
  color: var(--accent-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
}

.local-var-tag:hover {
  background-color: var(--border-color);
  color: var(--text-primary);
}

.local-var-type {
  color: var(--text-tertiary);
  font-size: 10px;
}

.injection-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.injection-detail-panel {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  min-width: 360px;
  max-width: 480px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.injection-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
}

.injection-detail-type {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.type-line-before {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.type-line-after {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.type-enter-method,
.type-exit-method,
.type-around-method {
  background: rgba(99, 102, 241, 0.15);
  color: #6366f1;
}

.injection-detail-close {
  background: none;
  border: none;
  color: var(--text-tertiary);
  font-size: 18px;
  cursor: pointer;
  padding: 0 4px;
}

.injection-detail-close:hover {
  color: var(--text-primary);
}

.injection-detail-body {
  padding: 16px;
}

.injection-detail-row {
  margin-bottom: 10px;
}

.injection-detail-label {
  font-size: 11px;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  display: block;
  margin-bottom: 2px;
}

.injection-detail-value {
  font-size: 13px;
  color: var(--text-primary);
}

.injection-detail-code {
  font-size: 12px;
  color: #e2e8f0;
  background: rgba(0, 0, 0, 0.3);
  padding: 8px 12px;
  border-radius: 4px;
  display: block;
  word-break: break-all;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.injection-detail-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
}

.injection-detail-delete {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.injection-detail-delete:hover {
  background: rgba(239, 68, 68, 0.25);
}
</style>

<!-- 非 scoped 样式，用于 Monaco Editor 的 glyph margin 装饰 -->
<style>
.source-line-glyph {
  background: #4caf50;
  border-radius: 50%;
  margin-left: 4px;
  width: 8px !important;
  height: 8px !important;
  margin-top: 5px;
  cursor: pointer;
}

.injected-glyph {
  background: #f59e0b;
  margin-left: 3px;
  width: 10px !important;
  height: 10px !important;
  margin-top: 4px;
  cursor: pointer;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
}

.injected-glyph-before {
  background: #22c55e;
  margin-left: 3px;
  width: 0 !important;
  height: 0 !important;
  margin-top: 4px;
  cursor: pointer;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-bottom: 8px solid #22c55e;
}

.injected-glyph-after {
  background: #f59e0b;
  margin-left: 3px;
  width: 0 !important;
  height: 0 !important;
  margin-top: 4px;
  cursor: pointer;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 8px solid #f59e0b;
}

.injected-line-bg {
  background: rgba(245, 158, 11, 0.12);
  border-left: 3px solid #f59e0b;
}

.injected-line-bg-before {
  background: rgba(34, 197, 94, 0.08);
  border-left: 3px solid #22c55e;
}

.injected-line-bg-after {
  background: rgba(245, 158, 11, 0.08);
  border-left: 3px solid #f59e0b;
}
</style>