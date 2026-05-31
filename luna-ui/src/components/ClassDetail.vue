<template>
  <div class="class-detail">
    <!-- 核心视图区域：IDE 风格布局 -->
    <div v-if="classInfo" class="main-viewer">
      <!-- 左侧：编辑器区域 -->
      <div class="editor-pane">
        <div class="editor-viewport">
          <div v-if="decompiledCode" class="editor-container">
            <CodeEditor
              :key="'decompile-' + classInfo?.className"
              :options="editorOptions"
              :value="decompiledCode"
              class="code-editor"
              language="java"
              @editorDidMount="onEditorMount"
            />
          </div>
          <div v-else-if="loadingDecompiled" class="loading-placeholder">
            <div class="loading-spinner"></div>
            <span>ANALYZING BYTECODE...</span>
          </div>
          <div v-else class="empty-placeholder">
            <div class="empty-icon"><i class="fas fa-terminal"></i></div>
            <p>源码未加载</p>
            <button class="load-btn-hero" @click="loadDecompiledCode">
              <i class="fas fa-cloud-download-alt"></i> 立即加载反编译源码
            </button>
          </div>
        </div>
      </div>

      <!-- 右侧：大纲侧边栏 -->
      <ClassOutline 
        :class-info="classInfo" 
        @scroll-to="scrollToMethod"
        @inject="showInjectDialog"
      />
    </div>
    
    <!-- 无选择状态 -->
    <div v-else class="no-selection">
      <div class="no-selection-icon"><i class="fas fa-file-code"></i></div>
      <span class="no-selection-text">{{ t('detail.no_selection') }}</span>
    </div>
    
    <!-- 注入详情悬浮层 -->
    <InjectionDetailOverlay 
      :visible="injectionDetailVisible"
      :marker="selectedInjectionMarker"
      @close="injectionDetailVisible = false"
      @delete="removeInjectionPoint"
    />

    <!-- 注入配置对话框 -->
    <InjectionDialog 
      v-if="injectDialogVisible"
      :visible="injectDialogVisible"
      :loading="injecting"
      :class-name="classInfo?.className"
      :method="currentMethod"
      :initial-line-number="initialLineNumber"
      :initial-injection-type="initialInjectionType"
      :initial-code-type="initialCodeType"
      :initial-probe-type="initialProbeType"
      :available-lines="Object.values(sourceLineMapping)"
      @close="injectDialogVisible = false"
      @submit="handleInjectLog"
    />

    <!-- 快捷操作菜单 -->
    <div 
      v-if="quickActionMenu.visible" 
      class="quick-action-popover"
      :style="{ top: quickActionMenu.y + 'px', left: quickActionMenu.x + 'px' }"
    >
      <div class="popover-item" @click="handleQuickAction('LINE_BEFORE')">
        <i class="fas fa-level-up-alt"></i> 行前注入 (Before)
      </div>
      <div class="popover-item" @click="handleQuickAction('LINE_AFTER')">
        <i class="fas fa-level-down-alt"></i> 行后注入 (After)
      </div>
      <div class="popover-divider"></div>
      <div class="popover-item snapshot" @click="handleQuickAction('LINE_BEFORE', 'SNAPSHOT')">
        <i class="fas fa-camera"></i> 快速快照 (Snapshot)
      </div>
    </div>
  </div>
</template>

<script>
import { useI18n } from 'vue-i18n'
import { markRaw } from 'vue'
import { getDecompiledCode, injectMethodLog, getLineNumbers, getInjectionList, removeInjection } from '../utils/api'
import { CodeEditor } from 'monaco-editor-vue3'
import * as monaco from 'monaco-editor'

// 子组件
import ClassHeader from './ClassHeader.vue'
import ClassOutline from './ClassOutline.vue'
import InjectionDialog from './InjectionDialog.vue'
import InjectionDetailOverlay from './InjectionDetailOverlay.vue'

export default {
  name: 'ClassDetail',
  components: {
    CodeEditor,
    ClassHeader,
    ClassOutline,
    InjectionDialog,
    InjectionDetailOverlay
  },
  setup() {
    const { t } = useI18n()
    return { t }
  },
  props: {
    classInfo: { type: Object, default: null }
  },
  data() {
    return {
      decompiledCode: '',
      loadingDecompiled: false,
      injectDialogVisible: false,
      injecting: false,
      currentMethod: null,
      monacoEditor: null,
      lineNumberMap: {},
      sourceLineMapping: {},
      injectionMarkers: [],
      injectionDecorations: null,
      sourceLineDecorations: null,
      loadedClassName: null,
      selectedInjectionMarker: null,
      injectionDetailVisible: false,
      initialLineNumber: null,
      initialInjectionType: 'ENTER_METHOD',
      initialCodeType: 'EXPRESSION',
      initialProbeType: 'LOG',
      quickActionMenu: {
        visible: false,
        x: 0,
        y: 0,
        line: null,
        method: null
      }
    }
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
        glyphMargin: true,
        lineNumbers: (lineNumber) => {
          return (self.sourceLineMapping && self.sourceLineMapping[lineNumber]) 
            ? String(self.sourceLineMapping[lineNumber]) 
            : String(lineNumber)
        }
      }
    },
    availableSourceLines() {
      if (!this.lineNumberMap || !this.currentMethod) return []
      const methodName = this.currentMethod.name
      const methodDesc = this.currentMethod.descriptor

      const normalizeDesc = (desc) => {
        return desc.replace(/\//g, '.').replace(/\./g, '')
      }

      if (methodName) {
        // 优先匹配方法名 + 描述符
        for (const [key, lines] of Object.entries(this.lineNumberMap)) {
          const keyName = key.split('(')[0]
          if (keyName === methodName) {
            const keyDesc = '(' + key.split('(').slice(1).join('(')
            if (normalizeDesc(keyDesc) === normalizeDesc(methodDesc || '')) {
              return Array.isArray(lines) ? [...new Set(lines)].sort((a, b) => a - b) : []
            }
          }
        }
        // 退而求其次，仅匹配方法名
        for (const [key, lines] of Object.entries(this.lineNumberMap)) {
          if (key.startsWith(methodName + '(')) {
            return Array.isArray(lines) ? [...new Set(lines)].sort((a, b) => a - b) : []
          }
        }
      }
      return []
    }
  },
  watch: {
    classInfo: {
      immediate: true,
      handler(newVal) {
        if (newVal && newVal.className !== this.loadedClassName) {
          this.loadDecompiledCode()
        }
      }
    },
    loadingDecompiled(val) {
      this.$emit('sync-state', { className: this.classInfo?.className, loading: val })
    },
    injectionMarkers: {
      deep: true,
      handler(val) {
        this.$emit('sync-state', { className: this.classInfo?.className, injectionCount: val.length })
      }
    }
  },
  methods: {
    async loadDecompiledCode() {
      if (!this.classInfo) return
      this.loadingDecompiled = true
      try {
        const code = await getDecompiledCode(this.classInfo.className)
        const { cleanCode, mapping } = this.parseSourceLineComments(code || '')
        this.decompiledCode = cleanCode
        this.sourceLineMapping = mapping
        
        this.injectionMarkers = []
        this.loadedClassName = this.classInfo.className
        
        this.refreshDecorations()
        this.fetchAuxiliaryData()
      } catch (error) {
        console.error('Failed to load code:', error)
        this.decompiledCode = '// Error: ' + error.message
      } finally {
        this.loadingDecompiled = false
      }
    },
    parseSourceLineComments(code) {
      const mapping = {}
      const lines = code.split('\n')
      // 允许行首有空格
      const pattern = /^\s*\/\*\s*(\d+)\s*\*\/\s?(.*)$/
      lines.forEach((line, i) => {
        const match = line.match(pattern)
        if (match) mapping[i + 1] = parseInt(match[1], 10)
      })
      return { cleanCode: code, mapping }
    },
    async fetchAuxiliaryData() {
      // 并行获取行号映射和已有注入点
      Promise.all([
        getLineNumbers(this.classInfo.className),
        getInjectionList(this.classInfo.className)
      ]).then(([lineNumbers, injectionData]) => {
        this.lineNumberMap = lineNumbers || {}
        this.updateSourceLineDecorations()
        
        if (injectionData?.injections) {
          this.syncInjectionMarkers(injectionData.injections)
        }
      })
    },
    syncInjectionMarkers(injections) {
      this.injectionMarkers = injections.map(inj => {
        let editorLine = null;
        
        // 如果是方法注入，优先寻找方法定义的行号
        if (inj.targetType === 'MethodTarget' || inj.targetType === 'METHOD') {
          editorLine = this.findEditorLineForMethod(inj.targetMethod || inj.method);
        }
        
        // 如果没找到（或者不是方法注入），尝试根据原始行号映射
        if (!editorLine && inj.lineNumber) {
          editorLine = Object.keys(this.sourceLineMapping).find(k => this.sourceLineMapping[k] === inj.lineNumber);
        }
        
        // 最后兜底：再次尝试按方法名找
        if (!editorLine) {
          editorLine = this.findEditorLineForMethod(inj.targetMethod || inj.method);
        }
        
        return { ...inj, editorLine: parseInt(editorLine) || null };
      });
      this.updateInjectionDecorations();
    },
    onEditorMount(editor) {
      this.monacoEditor = markRaw(editor)
      this.injectionDecorations = markRaw(editor.createDecorationsCollection([]))
      this.sourceLineDecorations = markRaw(editor.createDecorationsCollection([]))
      
      editor.onMouseDown((e) => {
        // TargetType 2 是 Glyph Margin (图标区), 3 是 Line Numbers (行号区)
        if (e.target?.type === 2 || e.target?.type === 3) {
          const line = e.target.position?.lineNumber
          if (!line) return

          // 1. 优先查找现有的注入点（紫点）
          const marker = this.injectionMarkers.find(m => m.editorLine === line)
          if (marker) {
            this.selectedInjectionMarker = marker
            this.injectionDetailVisible = true
            return
          }

          // 2. 如果没点到注入点，看看是不是点到了可注入的方法行（绿点）
          const method = this.findMethodByEditorLine(line)
          if (method) {
            this.quickActionMenu = {
              visible: true,
              x: e.event.posx,
              y: e.event.posy,
              line: this.sourceLineMapping[line],
              method: method
            }
          }
        } else {
          this.quickActionMenu.visible = false
        }
      })
    },
    scrollToMethod(method) {
      const line = this.findEditorLineForMethod(method.name)
      if (line && this.monacoEditor) {
        this.monacoEditor.revealLineInCenter(line)
        this.monacoEditor.setPosition({ lineNumber: line, column: 1 })
        this.monacoEditor.focus()
      }
    },
    findEditorLineForMethod(name) {
      if (!name) return null
      const lines = this.decompiledCode.split('\n')
      
      // 处理构造函数：字节码里叫 <init>，源码里叫类名
      let searchName = name
      if (name === '<init>') {
        const fullClassName = this.classInfo.className
        const parts = fullClassName.split('.')
        searchName = parts[parts.length - 1]
      }
      
      // 更加精准的启发式搜索：寻找方法定义或构造函数的特征
      const methodDefPattern = new RegExp(`\\b(public|private|protected|static|final|synchronized|native)\\s+.*\\b${searchName}\\s*\\(`)
      
      let idx = lines.findIndex(l => methodDefPattern.test(l))
      
      if (idx === -1) {
        idx = lines.findIndex(l => l.includes(searchName + '(') && !l.includes('=') && !l.trim().startsWith('this.'))
      }
      
      return idx !== -1 ? idx + 1 : null
    },

    findMethodByEditorLine(line) {
      // 1. 通过行号映射找原始行号
      const originalLine = this.sourceLineMapping[line]
      
      // 2. 在 lineNumberMap 中查找包含该原始行号的方法
      // lineNumberMap 结构: { "methodName(desc)": [origLine1, origLine2, ...] }
      for (const [methodKey, origLines] of Object.entries(this.lineNumberMap)) {
        if (origLines.includes(originalLine)) {
          const name = methodKey.split('(')[0]
          // 返回方法对象（模拟大纲里的对象结构）
          return (this.classInfo.methods || []).find(m => m.name === name)
        }
      }

      // 3. 兜底逻辑：如果这一行刚好是方法的定义行（哪怕没有字节码映射）
      const decompileLines = this.decompiledCode.split('\n')
      const currentLineText = decompileLines[line - 1] || ''
      return (this.classInfo.methods || []).find(m => {
        const simpleName = m.name === '<init>' ? this.classInfo.className.split('.').pop() : m.name
        return currentLineText.includes(simpleName + '(') && 
               /public|private|protected/.test(currentLineText)
      })
    },
    showInjectDialog(method, lineNumber = null, type = 'LINE_BEFORE') {
      this.currentMethod = method
      this.initialLineNumber = lineNumber
      this.injectDialogVisible = true
      this.quickActionMenu.visible = false
      
      // 注意：这里需要等弹窗 mount 后修改内部状态，或者通过 prop 传递更多初始状态
      // 稍后我会修改 InjectionDialog 支持 initialType
    },
    handleQuickAction(type, probeType = 'LOG') {
      this.initialInjectionType = type
      this.initialProbeType = probeType
      this.showInjectDialog(this.quickActionMenu.method, this.quickActionMenu.line)
    },
    async handleInjectLog(formData) {
      this.injecting = true
      try {
        const payload = {
          clazz: this.classInfo.className,
          method: this.currentMethod.name,
          desc: this.currentMethod.descriptor,
          injectionLocation: formData.injectionType,
          probeType: formData.probeType || 'LOG',
          codeType: formData.codeType,
          code: formData.logContent || '',
          lineNumber: formData.lineNumber,
          condition: formData.condition
        }
        const result = await injectMethodLog(payload)
        this.injectDialogVisible = false
        this.loadDecompiledCode()
      } catch (e) {
        alert('注入失败: ' + (e.response?.data?.message || e.message))
      } finally {
        this.injecting = false
      }
    },
    async removeInjectionPoint(marker) {
      if (!confirm('Delete this injection?')) return
      try {
        await removeInjection(marker.id)
        this.injectionDetailVisible = false
        this.loadDecompiledCode()
      } catch (e) {
        alert('删除失败')
      }
    },
    refreshDecorations() {
      if (this.injectionDecorations) this.injectionDecorations.set([])
      if (this.sourceLineDecorations) this.sourceLineDecorations.set([])
    },
    updateSourceLineDecorations() {
      if (!this.monacoEditor || !this.sourceLineDecorations) return
      const decorations = []
      
      // 遍历所有建立了映射的行（即所有带有 /* XX */ 注释的行）
      Object.keys(this.sourceLineMapping).forEach(editorLine => {
        const line = parseInt(editorLine)
        decorations.push({
          range: new monaco.Range(line, 1, line, 1),
          options: { 
            isWholeLine: true, 
            glyphMarginClassName: 'source-line-glyph' 
          }
        })
      })
      
      this.sourceLineDecorations.set(decorations)
    },
    updateInjectionDecorations() {
      if (!this.monacoEditor || !this.injectionDecorations) return
      const decorations = this.injectionMarkers
        .filter(m => m.editorLine)
        .map(m => {
          let glyphClass = 'injected-glyph-default'
          const type = (m.injectionType || m.type || '').toUpperCase()
          const codeType = (m.codeType || '').toUpperCase()
          
          if (codeType === 'SNAPSHOT') {
            glyphClass = 'injected-glyph-snapshot'
          } else if (type.includes('ENTER') || type.includes('BEFORE')) {
            glyphClass = 'injected-glyph-before'
          } else if (type.includes('EXIT') || type.includes('AFTER')) {
            glyphClass = 'injected-glyph-after'
          }
          
          return {
            range: new monaco.Range(m.editorLine, 1, m.editorLine, 1),
            options: { 
              isWholeLine: true, 
              glyphMarginClassName: `injected-glyph ${glyphClass}`,
              className: 'injected-line-bg'
            }
          }
        })
      this.injectionDecorations.set(decorations)
    }
  }
}
</script>

<style scoped>
.class-detail {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-primary);
  overflow: hidden;
}

.main-viewer {
  flex: 1;
  display: flex;
  background-color: var(--bg-primary);
  overflow: hidden;
}

.editor-pane {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.editor-viewport { flex: 1; position: relative; }
.editor-container { height: 100%; width: 100%; }

.loading-placeholder, .empty-placeholder, .no-selection {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #555;
  gap: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255,255,255,0.05);
  border-top-color: #6366f1;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.no-selection-icon { font-size: 64px; opacity: 0.1; }

/* 快捷菜单样式 */
.quick-action-popover {
  position: fixed;
  z-index: 3000;
  background: #2d2d2d;
  border: 1px solid #444;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.5);
  padding: 6px;
  min-width: 160px;
  animation: pop-in 0.15s ease-out;
}

@keyframes pop-in {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.popover-item {
  padding: 8px 12px;
  color: #ccc;
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.popover-item:hover {
  background: #3e3e3f;
  color: #fff;
}

.popover-item i {
  width: 14px;
  text-align: center;
  font-size: 11px;
  color: #6366f1;
}

.popover-divider {
  height: 1px;
  background: #444;
  margin: 4px;
}

.popover-item.snapshot i {
  color: #f43f5e;
}
</style>

<style>
/* 全局装饰样式 - Monaco Editor 必须是非 scoped */
.source-line-glyph {
  background: #10b981;
  border-radius: 50%;
  margin-left: 4px;
  width: 8px !important;
  height: 8px !important;
  margin-top: 5px;
  cursor: pointer;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.6);
}

.injected-glyph {
  margin-left: 3px;
  width: 10px !important;
  height: 10px !important;
  margin-top: 4px;
  cursor: pointer;
  box-shadow: 0 0 8px rgba(99, 102, 241, 0.6);
}

.injected-glyph-before {
  background: #6366f1;
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%); /* 向上箭头 */
}

.injected-glyph-after {
  background: #8b5cf6;
  clip-path: polygon(50% 100%, 0% 0%, 100% 0%); /* 向下箭头 */
}

.injected-glyph-snapshot {
  background: #f43f5e;
  border-radius: 2px;
  clip-path: polygon(0% 20%, 20% 20%, 20% 0%, 80% 0%, 80% 20%, 100% 20%, 100% 100%, 0% 100%); /* 相机轮廓 */
}

.injected-glyph-default {
  background: #6366f1;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
}

.injected-line-bg {
  background: rgba(99, 102, 241, 0.08);
  border-left: 2px solid #6366f1;
}
</style>