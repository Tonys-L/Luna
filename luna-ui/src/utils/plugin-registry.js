class PluginRegistry {
  constructor() {
    this._manifest = null
  }

  async init() {
    await this.refresh()
  }

  async refresh() {
    try {
      const resp = await fetch('/api/plugins/ui-manifest')
      if (resp.ok) {
        this._manifest = await resp.json()
        if (this._manifest.injectionTypes && this._manifest.injectionTypes.length > 0) {
          return
        }
      }
    } catch (e) {
      console.warn('Failed to load plugin UI manifest, using built-in defaults:', e)
    }
    this._manifest = this._builtInDefaults()
  }

  _builtInDefaults() {
    return {
      injectionTypes: [
        { name: 'ENTER_METHOD', displayName: '方法进入', category: 'method' },
        { name: 'EXIT_METHOD', displayName: '方法退出', category: 'method' },
        { name: 'AROUND_METHOD', displayName: '方法环绕', category: 'method' },
        { name: 'LINE_BEFORE', displayName: '行号前注入', category: 'line' },
        { name: 'LINE_AFTER', displayName: '行号后注入', category: 'line' }
      ],
      expressionProtocols: [
        { protocol: 'log', displayName: '日志表达式', codeType: 'EXPRESSION', syntax: '使用 {} 占位符，如: User ID is {}' },
        { protocol: 'snapshot', displayName: '内存快照', codeType: 'SNAPSHOT', syntax: '自动捕获当前作用域内所有局部变量' },
        { protocol: 'trace', displayName: '方法耗时', codeType: 'EXPRESSION', syntax: '格式: start | end:阈值ms | alert:阈值ms' }
      ],
      templates: []
    }
  }

  get injectionTypes() { return this._manifest?.injectionTypes || [] }
  get expressionProtocols() { return this._manifest?.expressionProtocols || [] }
  get templates() { return this._manifest?.templates || [] }
}

export const pluginRegistry = new PluginRegistry()
