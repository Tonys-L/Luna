class PluginRegistry {
  constructor() {
    this._manifest = null
    this._cachedManifest = null
  }

  async init() {
    await this.refresh()
  }

  async refresh() {
    try {
      const resp = await fetch('/api/plugins/ui-manifest')
      if (resp.ok) {
        const data = await resp.json()
        const manifest = data.data || data
        const converted = {
          injectionTypes: this._convertInjectionLocations(manifest.injectionLocations),
          probeHandlers: this._convertToProbeTypes(manifest.probeTypes || []),
          codeEngines: manifest.codeEngines || []
        }
        if (converted.injectionTypes.length > 0 && converted.probeHandlers.length > 0) {
          this._manifest = converted
          this._cachedManifest = converted
          return
        }
      }
    } catch (e) {
      console.warn('Failed to load plugin UI manifest:', e)
    }

    // API 不可用时使用上次成功缓存的 manifest，无缓存则为空
    this._manifest = this._cachedManifest || { injectionTypes: [], probeHandlers: [], codeEngines: [] }
  }

  _convertInjectionLocations(locations) {
    if (!locations || locations.length === 0) return []
    return locations.map(loc => {
      const name = loc.name || loc
      // 优先使用后端传的 category，缺失时从 name 推导
      const category = loc.category || (name.startsWith('line_') ? 'line'
        : name.startsWith('method_') || name === 'invoke' || name.startsWith('exception_') ? 'method'
        : 'other')
      return {
        name,
        displayName: loc.displayName || name,
        category,
        categoryLabel: loc.categoryLabel || category,
        color: loc.color || '#6b7280',
        canonicalName: name.toLowerCase()
      }
    })
  }

  _convertToProbeTypes(probeTypes) {
    return probeTypes.map(p => ({
      probeType: p.probeType,
      displayName: p.displayName || p.probeType,
      syntax: p.syntax || '',
      icon: p.icon || '',
      category: p.category || 'injection',
      usesCode: p.usesCode,
      codeType: p.codeType || null,
      supportedInjectionLocations: p.supportedInjectionLocations || [],
      quickActionBehavior: p.quickActionBehavior || 'FORM',
      glyphColor: p.glyphColor || '',
      configSchema: p.configSchema || []
    }))
  }

  get injectionTypes() { return this._manifest?.injectionTypes || [] }
  get probeHandlers() { return this._manifest?.probeHandlers || [] }
  get codeEngines() { return this._manifest?.codeEngines || [] }
}

export const pluginRegistry = new PluginRegistry()
