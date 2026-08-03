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
      :readonly-probe-type="readonlyProbeType"
      :injection-context="injectionContext"
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
      <!-- 方法定义行：显示支持方法级注入的探针 -->
      <template v-if="quickActionMenu.isMethodDefinitionLine">
        <div v-for="action in methodQuickActions" :key="action.key"
             class="popover-item"
             @click="action.handler">
          <i :class="action.icon" :style="{ color: action.iconColor }"></i> {{ action.label }}
        </div>
      </template>

      <!-- 方法体内行：显示支持行级注入的探针 -->
      <template v-else>
        <div v-for="action in lineQuickActions" :key="action.key"
             class="popover-item"
             @click="action.handler">
          <i :class="action.icon" :style="{ color: action.iconColor }"></i> {{ action.label }}
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { useI18n } from 'vue-i18n'
import { markRaw } from 'vue'
import { getDecompiledCode, injectMethodLog, getLineNumbers, getInjectionList, removeInjection } from '../utils/api'
import { pluginRegistry } from '../utils/plugin-registry'
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
      initialInjectionType: '',
      initialCodeType: '',
      initialProbeType: '',
      readonlyProbeType: false,
      injectionContext: 'free',
      quickActionMenu: {
        visible: false,
        x: 0,
        y: 0,
        line: null,
        method: null,
        isMethodDefinitionLine: false
      }
    }
  },
  computed: {
    // 方法定义行快捷菜单：只显示支持方法级注入的探针
    methodQuickActions() {
      const actions = []
      const probeHandlers = pluginRegistry.probeHandlers || []
      const methodLocationNames = pluginRegistry.injectionTypes
        .filter(t => t.category === 'method').map(t => t.canonicalName || t.name.toLowerCase())

      for (const handler of probeHandlers) {
        const supported = handler.supportedInjectionLocations || []
        const supportsMethod = supported.some(loc => methodLocationNames.includes(loc.toLowerCase()))
        if (!supportsMethod) continue

        actions.push(this.buildQuickAction(handler))
      }
      return actions
    },
    lineQuickActions() {
      const actions = []
      const probeHandlers = pluginRegistry.probeHandlers || []
      const lineLocationNames = pluginRegistry.injectionTypes
        .filter(t => t.category === 'line').map(t => t.canonicalName || t.name.toLowerCase())

      for (const handler of probeHandlers) {
        const supported = handler.supportedInjectionLocations || []
        const supportsLine = supported.some(loc => lineLocationNames.includes(loc.toLowerCase()))
        if (!supportsLine) continue

        actions.push(this.buildQuickAction(handler, true))
      }
      return actions
    },
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
        const isMethodTarget = inj.targetType === 'MethodTarget' || inj.targetType === 'ConstructorTarget';
        const isLineTarget = inj.targetType === 'LineNumberTarget';
        
        // 方法级注入：定位到方法定义行
        if (isMethodTarget) {
          editorLine = this.findEditorLineForMethod(inj.method);
        }
        
        // 行号级注入：通过 sourceLineMapping 反查编辑器行
        if (isLineTarget && inj.lineNumber) {
          editorLine = Object.keys(this.sourceLineMapping).find(k => this.sourceLineMapping[k] === inj.lineNumber);
        }
        
        // 兜底：按方法名找
        if (!editorLine && inj.method) {
          editorLine = this.findEditorLineForMethod(inj.method);
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
            // 判断当前行是否为方法定义行（而非方法体内的行）
            const isMethodDefinitionLine = this.isMethodDefinitionLine(line, method)
            this.quickActionMenu = {
              visible: true,
              x: e.event.posx,
              y: e.event.posy,
              line: this.sourceLineMapping[line],
              method: method,
              isMethodDefinitionLine: isMethodDefinitionLine
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
    isMethodDefinitionLine(editorLine, method) {
      if (!method || !this.decompiledCode) return false
      const decompileLines = this.decompiledCode.split('\n')
      const lineText = decompileLines[editorLine - 1] || ''
      const simpleName = method.name === '<init>' ? this.classInfo.className.split('.').pop() : method.name
      // 方法定义行特征：包含方法名 + ( 并且有访问修饰符
      return lineText.includes(simpleName + '(') && /public|private|protected/.test(lineText)
    },
    /**
     * 构建快捷菜单项
     * @param {Object} handler - 探针处理器
     * @param {boolean} isLineContext - 是否在行级上下文中
     */
    buildQuickAction(handler, isLineContext = false) {
      return {
        key: handler.probeType,
        label: handler.displayName,
        icon: handler.icon,
        iconColor: handler.glyphColor || '#6366f1',
        handler: () => this.handleFormAction(handler, isLineContext)
      }
    },
    /**
     * 统一的对话框入口，所有参数显式传入
     * @param {Object} opts
     * @param {Object} opts.method - 目标方法
     * @param {number} opts.lineNumber - 行号（行注入时传入）
     * @param {string} opts.injectionType - 注入位置，如 'method_enter'/'line_before'
     * @param {string} opts.probeType - 探针类型，如 'LOG'/'SNAPSHOT'/'TRACE'
     * @param {boolean} opts.readonlyProbeType - 探针类型是否只读
     * @param {string} opts.injectionContext - 上下文：'method'/'line'/'free'
     */
    openInjectDialog({ method, lineNumber = null, injectionType, probeType, readonlyProbeType = false, injectionContext = 'free' }) {
      this.currentMethod = method
      this.initialLineNumber = lineNumber
      this.initialInjectionType = injectionType || this.getDefaultInjectionType(injectionContext)
      this.initialProbeType = probeType || this.getDefaultProbeType()
      this.initialCodeType = this.getDefaultCodeType(this.initialProbeType)
      this.readonlyProbeType = readonlyProbeType
      this.injectionContext = injectionContext
      this.injectDialogVisible = true
      this.quickActionMenu.visible = false
    },
    // 大纲/按钮入口：完全自由选择
    showInjectDialog(method, lineNumber = null) {
      this.openInjectDialog({ method, lineNumber, injectionContext: 'free' })
    },
    // 快捷菜单入口：探针类型只读，上下文由点击位置决定
    handleFormAction(handler, isLineContext = false) {
      this.quickActionMenu.visible = false
      const method = this.quickActionMenu.method
      if (!method) return

      this.openInjectDialog({
        method,
        lineNumber: this.quickActionMenu.line,
        injectionType: this.getDefaultInjectionType(isLineContext ? 'line' : 'method'),
        probeType: handler.probeType,
        readonlyProbeType: true,
        injectionContext: isLineContext ? 'line' : 'method'
      })
    },
    ensureGlyphStyles() {
      if (document.getElementById('luna-glyph-styles')) return
      const handlers = pluginRegistry.probeHandlers || []
      let css = ''
      handlers.forEach(h => {
        const key = (h.probeType || '').toLowerCase()
        const color = h.glyphColor || '#6366f1'
        css += `.injected-glyph-${key} { background: ${color}; box-shadow: 0 0 8px ${color}66; }\n`
      })
      const style = document.createElement('style')
      style.id = 'luna-glyph-styles'
      style.textContent = css
      document.head.appendChild(style)
    },
    getDefaultInjectionType(context = 'free') {
      const types = pluginRegistry.injectionTypes
      if (context === 'line') {
        const lineType = types.find(t => t.category === 'line')
        return lineType?.name || ''
      }
      const methodType = types.find(t => t.category === 'method')
      return methodType?.name || ''
    },
    getDefaultProbeType() {
      const handlers = pluginRegistry.probeHandlers
      return handlers.length > 0 ? handlers[0].probeType : ''
    },
    getDefaultCodeType(probeType) {
      const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === probeType)
      if (!handler?.usesCode) return null
      return handler.codeType || ''
    },
    async handleInjectLog(formData) {
      this.injecting = true
      try {
        const payload = {
          clazz: this.classInfo.className,
          method: this.currentMethod.name,
          desc: this.currentMethod.descriptor,
          injectionLocation: formData.injectionLocation,
          probeType: formData.probeType,
          codeType: formData.codeType,
          code: formData.code || '',
          lineNumber: formData.lineNumber,
          ephemeral: formData.ephemeral !== undefined ? formData.ephemeral : true
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
      const decoratedLines = new Set()
      
      // 1. 遍历所有建立了映射的行（即所有带有 /* XX */ 注释的行）
      Object.keys(this.sourceLineMapping).forEach(editorLine => {
        const line = parseInt(editorLine)
        decoratedLines.add(line)
        decorations.push({
          range: new monaco.Range(line, 1, line, 1),
          options: { 
            isWholeLine: true, 
            glyphMarginClassName: 'source-line-glyph' 
          }
        })
      })
      
      // 2. 给方法定义行也加上绿点（即使没有行号映射）
      if (this.classInfo && this.classInfo.methods) {
        for (const method of this.classInfo.methods) {
          const editorLine = this.findEditorLineForMethod(method.name)
          if (editorLine && !decoratedLines.has(editorLine)) {
            decoratedLines.add(editorLine)
            decorations.push({
              range: new monaco.Range(editorLine, 1, editorLine, 1),
              options: { 
                isWholeLine: true, 
                glyphMarginClassName: 'source-line-glyph' 
              }
            })
          }
        }
      }
      
      this.sourceLineDecorations.set(decorations)
    },
    updateInjectionDecorations() {
      if (!this.monacoEditor || !this.injectionDecorations) return
      this.ensureGlyphStyles()
      
      const decorations = this.injectionMarkers
        .filter(m => m.editorLine)
        .map(m => {
          const probeType = (m.probeType || '').toUpperCase()
          const colorKey = probeType.toLowerCase()

          // 从 plugin-registry 获取探针声明的颜色和显示名
          const handler = pluginRegistry.probeHandlers?.find(h => h.probeType === probeType)
          const displayName = handler?.displayName || probeType

          const glyphClasses = `injected-glyph injected-glyph-${colorKey}`

          return {
            range: new monaco.Range(m.editorLine, 1, m.editorLine, 1),
            options: {
              isWholeLine: true,
              glyphMarginClassName: glyphClasses,
              glyphMarginHoverMessage: { value: `**${displayName}** 注入点` },
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
  border-radius: 50%;
  margin-left: 3px;
  width: 10px !important;
  height: 10px !important;
  margin-top: 4px;
  cursor: pointer;
}

.injected-glyph-default {
  background: #6366f1;
  box-shadow: 0 0 8px rgba(99, 102, 241, 0.6);
}

.injected-line-bg {
  background: rgba(99, 102, 241, 0.08);
  border-left: 2px solid #6366f1;
}
</style>