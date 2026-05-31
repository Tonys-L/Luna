class PluginRegistry {
  constructor() {
    this._manifest = null
  }

  async init() {
    await this.refresh()
  }

  async refresh() {
    try {
      const [probesResp, enginesResp] = await Promise.all([
        fetch('/api/probes'),
        fetch('/api/probes/engines')
      ])

      if (probesResp.ok && enginesResp.ok) {
        const probes = await probesResp.json()
        const engines = await enginesResp.json()

        this._manifest = {
          injectionTypes: this._builtInInjectionTypes(),
          probeHandlers: probes.data || probes || [],
          codeEngines: engines.data || engines || [],
          templates: []
        }

        this._manifest.expressionProtocols = this._convertToExpressionProtocols(this._manifest.probeHandlers)
        return
      }
    } catch (e) {
      console.warn('Failed to load probe/engine capabilities from API, using built-in defaults:', e)
    }

    try {
      const resp = await fetch('/api/plugins/ui-manifest')
      if (resp.ok) {
        this._manifest = await resp.json()
        if (this._manifest.injectionTypes && this._manifest.injectionTypes.length > 0) {
          return
        }
      }
    } catch (e) {
      console.warn('Failed to load plugin UI manifest:', e)
    }

    this._manifest = this._builtInDefaults()
  }

  _convertToExpressionProtocols(probeHandlers) {
    return probeHandlers.map(p => ({
      protocol: p.probeType.toLowerCase(),
      displayName: this._probeDisplayName(p.probeType),
      codeType: p.usesCode ? 'EXPRESSION' : null,
      syntax: this._probeSyntax(p.probeType),
      probeType: p.probeType,
      usesCode: p.usesCode,
      supportedInjectionLocations: p.supportedInjectionLocations || []
    }))
  }

  _probeDisplayName(probeType) {
    const names = { LOG: '日志表达式', SNAPSHOT: '内存快照', TRACE: '方法耗时' }
    return names[probeType] || probeType
  }

  _probeSyntax(probeType) {
    const syntax = {
      LOG: '使用 {} 占位符，如: User ID is {}',
      SNAPSHOT: '自动捕获当前作用域内所有局部变量',
      TRACE: '格式: start | end:阈值ms | alert:阈值ms'
    }
    return syntax[probeType] || ''
  }

  _builtInInjectionTypes() {
    return [
      { name: 'ENTER_METHOD', displayName: '方法进入', category: 'method' },
      { name: 'EXIT_METHOD', displayName: '方法退出', category: 'method' },
      { name: 'AROUND_METHOD', displayName: '方法环绕', category: 'method' },
      { name: 'LINE_BEFORE', displayName: '行号前注入', category: 'line' },
      { name: 'LINE_AFTER', displayName: '行号后注入', category: 'line' }
    ]
  }

  _builtInDefaults() {
    return {
      injectionTypes: this._builtInInjectionTypes(),
      probeHandlers: [
        { probeType: 'LOG', usesCode: true, supportedInjectionLocations: ['method_enter', 'method_exit', 'method_around', 'line_before', 'line_after'] },
        { probeType: 'SNAPSHOT', usesCode: false, supportedInjectionLocations: ['line_before', 'line_after', 'method_enter', 'method_exit'] },
        { probeType: 'TRACE', usesCode: false, supportedInjectionLocations: ['method_enter', 'method_exit'] }
      ],
      codeEngines: [
        { codeType: 'EXPRESSION' }
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
  get probeHandlers() { return this._manifest?.probeHandlers || [] }
  get codeEngines() { return this._manifest?.codeEngines || [] }
  get templates() { return this._manifest?.templates || [] }
}

export const pluginRegistry = new PluginRegistry()
