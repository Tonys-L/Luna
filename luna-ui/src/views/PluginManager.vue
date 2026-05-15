<template>
  <div class="plugin-manager">
    <div class="page-header">
      <div class="header-info">
        <i class="fas fa-puzzle-piece"></i>
        <h2>插件管理</h2>
      </div>
      <div class="header-actions">
        <button class="refresh-btn" @click="loadPlugins" :disabled="loading">
          <i class="fas fa-sync-alt" :class="{ 'fa-spin': loading }"></i> 刷新
        </button>
        <button class="market-btn" @click="showMarket = !showMarket">
          <i class="fas fa-store"></i> {{ showMarket ? '返回列表' : '插件市场' }}
        </button>
      </div>
    </div>

    <div v-if="!showMarket" class="plugin-content">
      <div v-if="loading && plugins.length === 0" class="loading-state">
        <div class="spinner"></div>
        <p>加载插件列表...</p>
      </div>

      <div v-else class="plugin-table-wrap">
        <table class="plugin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>版本</th>
              <th>状态</th>
              <th>分类</th>
              <th>描述</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in plugins" :key="p.id" @click="openDetail(p)" class="plugin-row">
              <td class="mono">{{ p.id }}</td>
              <td>{{ p.displayName }}</td>
              <td class="mono">{{ p.version }}</td>
              <td>
                <span :class="['state-tag', stateClass(p.state)]">{{ p.state }}</span>
              </td>
              <td>{{ p.category || '-' }}</td>
              <td class="desc-cell">{{ p.description || '-' }}</td>
              <td class="action-cell" @click.stop>
                <button v-if="p.state === 'ACTIVE' && !p.builtin" class="action-btn warn" @click="disablePlugin(p.id)">禁用</button>
                <button v-if="p.state === 'DISABLED'" class="action-btn success" @click="enablePlugin(p.id)">启用</button>
                <button v-if="!p.builtin" class="action-btn danger" @click="unloadPlugin(p.id)">卸载</button>
                <button v-if="!p.builtin" class="action-btn info" @click="checkUpdate(p.id)" :disabled="updatingPlugins[p.id]">
                  <i class="fas fa-arrow-up"></i> {{ updatingPlugins[p.id] ? '更新中...' : '检查更新' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="plugins.length === 0" class="empty-state">
          <i class="fas fa-box-open"></i>
          <p>暂无已安装插件</p>
        </div>
      </div>
    </div>

    <div v-else class="market-content">
      <div class="market-search">
        <div class="search-input-wrap">
          <i class="fas fa-search"></i>
          <input 
            v-model="marketKeyword" 
            placeholder="搜索插件..." 
            @keyup.enter="searchMarket"
            class="search-input"
          />
        </div>
        <button class="search-btn" @click="searchMarket" :disabled="marketLoading">
          <i class="fas fa-search"></i> 搜索
        </button>
      </div>

      <div v-if="marketLoading" class="loading-state">
        <div class="spinner"></div>
        <p>搜索中...</p>
      </div>

      <div v-else-if="marketResults.length > 0" class="market-grid">
        <div v-for="item in marketResults" :key="item.id" class="market-card">
          <div class="market-card-header">
            <div class="market-card-icon">
              <i :class="item.icon || 'fas fa-puzzle-piece'"></i>
            </div>
            <div class="market-card-info">
              <div class="market-card-name">{{ item.displayName }}</div>
              <div class="market-card-id mono">{{ item.id }}</div>
            </div>
          </div>
          <div class="market-card-desc">{{ item.description }}</div>
          <div class="market-card-footer">
            <span class="market-card-version mono">v{{ item.version }}</span>
            <button class="install-btn" @click="installPlugin(item.id)" :disabled="item.installing">
              <i class="fas fa-download"></i> {{ item.installing ? '安装中...' : '安装' }}
            </button>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <i class="fas fa-store"></i>
        <p>输入关键词搜索插件</p>
      </div>
    </div>

    <div v-if="detailVisible" class="dialog-overlay" @click="detailVisible = false">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
          <div class="dialog-icon">
            <i class="fas fa-puzzle-piece"></i>
          </div>
          <h3 class="dialog-title">{{ detailData?.displayName }}</h3>
          <button v-if="detailData?.state === 'ACTIVE'" class="dialog-action-btn" @click="openConfig(detailData.id)">
            <i class="fas fa-cog"></i> 配置
          </button>
          <button class="dialog-close" @click="detailVisible = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="dialog-body">
          <div class="detail-section">
            <div class="detail-row">
              <span class="detail-label">插件 ID</span>
              <span class="detail-value mono">{{ detailData?.id }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">版本</span>
              <span class="detail-value mono">{{ detailData?.version }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">状态</span>
              <span :class="['state-tag', stateClass(detailData?.state)]">{{ detailData?.state }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">分类</span>
              <span class="detail-value">{{ detailData?.category || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">描述</span>
              <span class="detail-value">{{ detailData?.description || '-' }}</span>
            </div>
          </div>

          <div v-if="detailData?.extensionPoints?.length" class="detail-section">
            <h4 class="section-heading">注册的扩展点</h4>
            <div class="ext-list">
              <div v-for="ep in detailData.extensionPoints" :key="ep.name" class="ext-item">
                <span class="ext-type">{{ ep.type }}</span>
                <span class="ext-name mono">{{ ep.name }}</span>
              </div>
            </div>
          </div>

          <div v-if="detailData?.dependencies?.length" class="detail-section">
            <h4 class="section-heading">依赖</h4>
            <div class="dep-list">
              <span v-for="dep in detailData.dependencies" :key="dep" class="dep-tag mono">{{ dep }}</span>
            </div>
          </div>

          <div v-if="detailData?.config" class="detail-section">
            <h4 class="section-heading">配置</h4>
            <pre class="config-block">{{ JSON.stringify(detailData.config, null, 2) }}</pre>
          </div>
        </div>
      </div>
    </div>

    <div v-if="configVisible" class="dialog-overlay" @click="configVisible = false">
      <div class="dialog-content" @click.stop>
        <div class="dialog-header">
          <div class="dialog-icon">
            <i class="fas fa-cog"></i>
          </div>
          <h3 class="dialog-title">插件配置</h3>
          <button class="dialog-close" @click="configVisible = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="dialog-body">
          <div v-if="configLoading" class="loading-state">
            <div class="spinner"></div>
            <p>加载配置...</p>
          </div>
          <div v-else-if="configEntries.length > 0" class="config-form">
            <div v-for="entry in configEntries" :key="entry.key" class="config-field">
              <label class="config-label">{{ entry.key }}</label>
              <input
                v-model="entry.value"
                class="config-input"
                :placeholder="entry.key"
              />
            </div>
            <div class="config-actions">
              <button class="config-save-btn" @click="saveConfig" :disabled="configSaving">
                <i class="fas fa-save"></i> {{ configSaving ? '保存中...' : '保存配置' }}
              </button>
              <button class="config-cancel-btn" @click="configVisible = false">取消</button>
            </div>
          </div>
          <div v-else class="empty-state">
            <i class="fas fa-sliders-h"></i>
            <p>该插件暂无可配置项</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getPlugins, getPluginDetail, disablePlugin as disablePluginApi, enablePlugin as enablePluginApi, unloadPlugin as unloadPluginApi, searchPlugins, installPlugin as installPluginApi, getPluginConfig, savePluginConfig as savePluginConfigApi, checkPluginUpdate } from '../utils/api'
import { pluginRegistry } from '../utils/plugin-registry'

export default {
  name: 'PluginManager',
  data() {
    return {
      plugins: [],
      loading: false,
      showMarket: false,
      marketKeyword: '',
      marketResults: [],
      marketLoading: false,
      detailVisible: false,
      detailData: null,
      configVisible: false,
      configPluginId: null,
      configEntries: [],
      configLoading: false,
      configSaving: false,
      updatingPlugins: {}
    }
  },
  mounted() {
    this.loadPlugins()
  },
  methods: {
    async loadPlugins() {
      this.loading = true
      try {
        const data = await getPlugins()
        this.plugins = Array.isArray(data) ? data : []
      } catch (e) {
        console.error('加载插件列表失败:', e)
        this.plugins = []
      } finally {
        this.loading = false
      }
    },
    stateClass(state) {
      if (state === 'ACTIVE') return 'state-active'
      if (state === 'DISABLED') return 'state-disabled'
      return 'state-other'
    },
    async openDetail(plugin) {
      try {
        this.detailData = await getPluginDetail(plugin.id)
      } catch (e) {
        this.detailData = plugin
      }
      this.detailVisible = true
    },
    async disablePlugin(pluginId) {
      try {
        await disablePluginApi(pluginId)
        await this.loadPlugins()
        await pluginRegistry.refresh()
      } catch (e) {
        console.error('禁用插件失败:', e)
      }
    },
    async enablePlugin(pluginId) {
      try {
        await enablePluginApi(pluginId)
        await this.loadPlugins()
        await pluginRegistry.refresh()
      } catch (e) {
        console.error('启用插件失败:', e)
      }
    },
    async unloadPlugin(pluginId) {
      if (!confirm('确定卸载此插件？卸载后需重新安装。')) return
      try {
        await unloadPluginApi(pluginId)
        await this.loadPlugins()
        await pluginRegistry.refresh()
      } catch (e) {
        console.error('卸载插件失败:', e)
      }
    },
    async searchMarket() {
      if (!this.marketKeyword.trim()) return
      this.marketLoading = true
      try {
        const data = await searchPlugins(this.marketKeyword)
        this.marketResults = Array.isArray(data) ? data : []
      } catch (e) {
        console.error('搜索插件失败:', e)
        this.marketResults = []
      } finally {
        this.marketLoading = false
      }
    },
    async installPlugin(pluginId) {
      const item = this.marketResults.find(r => r.id === pluginId)
      if (item) item.installing = true
      try {
        await installPluginApi(pluginId)
        await this.loadPlugins()
        await pluginRegistry.refresh()
      } catch (e) {
        console.error('安装插件失败:', e)
      } finally {
        if (item) item.installing = false
      }
    },
    async openConfig(pluginId) {
      this.configPluginId = pluginId
      this.configLoading = true
      this.configVisible = true
      try {
        const config = await getPluginConfig(pluginId)
        this.configEntries = Object.entries(config || {}).map(([key, value]) => ({ key, value }))
      } catch (e) {
        console.error('加载插件配置失败:', e)
        this.configEntries = []
      } finally {
        this.configLoading = false
      }
    },
    async saveConfig() {
      this.configSaving = true
      try {
        const config = {}
        for (const entry of this.configEntries) {
          config[entry.key] = entry.value
        }
        await savePluginConfigApi(this.configPluginId, config)
        this.configVisible = false
      } catch (e) {
        console.error('保存插件配置失败:', e)
      } finally {
        this.configSaving = false
      }
    },
    async checkUpdate(pluginId) {
      this.$set(this.updatingPlugins, pluginId, true)
      try {
        const result = await checkPluginUpdate(pluginId)
        if (result && result.success) {
          alert(`插件 ${pluginId} 更新成功！${result.oldVersion || ''} → ${result.newVersion || ''}`)
          await this.loadPlugins()
          await pluginRegistry.refresh()
        } else {
          alert(`插件 ${pluginId} 暂无可用更新`)
        }
      } catch (e) {
        console.error('检查插件更新失败:', e)
        alert('检查更新失败: ' + (e.message || '未知错误'))
      } finally {
        this.$set(this.updatingPlugins, pluginId, false)
      }
    }
  }
}
</script>

<style scoped>
.plugin-manager {
  padding: 24px;
  background-color: var(--bg-primary);
  height: 100%;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-info h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.header-info i {
  color: var(--accent-primary);
  font-size: 24px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.refresh-btn, .market-btn {
  background-color: var(--bg-hover);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.refresh-btn:hover:not(:disabled), .market-btn:hover {
  background-color: var(--border-color);
}

.market-btn {
  background-color: rgba(99, 102, 241, 0.1);
  border-color: rgba(99, 102, 241, 0.3);
  color: var(--accent-primary);
}

.market-btn:hover {
  background-color: rgba(99, 102, 241, 0.2);
}

.plugin-table-wrap {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.plugin-table {
  width: 100%;
  border-collapse: collapse;
}

.plugin-table th {
  text-align: left;
  padding: 12px 16px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background-color: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-color);
}

.plugin-table td {
  padding: 12px 16px;
  font-size: 13px;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-color);
}

.plugin-row {
  cursor: pointer;
  transition: background-color 0.15s;
}

.plugin-row:hover {
  background-color: rgba(255, 255, 255, 0.03);
}

.mono {
  font-family: var(--font-mono);
  font-size: 12px;
}

.desc-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
}

.state-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.state-active {
  background-color: rgba(16, 185, 129, 0.15);
  color: var(--accent-success);
}

.state-disabled {
  background-color: rgba(245, 158, 11, 0.15);
  color: var(--accent-warning);
}

.state-other {
  background-color: rgba(107, 107, 123, 0.15);
  color: var(--text-tertiary);
}

.action-cell {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.action-btn.warn {
  background-color: rgba(245, 158, 11, 0.15);
  color: var(--accent-warning);
}

.action-btn.warn:hover {
  background-color: rgba(245, 158, 11, 0.3);
}

.action-btn.success {
  background-color: rgba(16, 185, 129, 0.15);
  color: var(--accent-success);
}

.action-btn.success:hover {
  background-color: rgba(16, 185, 129, 0.3);
}

.action-btn.danger {
  background-color: rgba(239, 68, 68, 0.15);
  color: var(--accent-danger);
}

.action-btn.danger:hover {
  background-color: rgba(239, 68, 68, 0.3);
}

.action-btn.info {
  background-color: rgba(99, 102, 241, 0.15);
  color: var(--accent-primary);
}

.action-btn.info:hover:not(:disabled) {
  background-color: rgba(99, 102, 241, 0.3);
}

.action-btn.info:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--text-tertiary);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state i {
  font-size: 40px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.market-content {
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 24px;
}

.market-search {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input-wrap {
  flex: 1;
  position: relative;
}

.search-input-wrap i {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  font-size: 13px;
}

.search-input {
  width: 100%;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 10px 12px 10px 36px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
}

.search-input:focus {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.search-btn {
  background-color: var(--accent-primary);
  border: none;
  color: #fff;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.search-btn:hover:not(:disabled) {
  filter: brightness(1.1);
}

.market-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.market-card {
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 20px;
  transition: all 0.2s;
}

.market-card:hover {
  border-color: var(--accent-primary);
  transform: translateY(-2px);
}

.market-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.market-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(99, 102, 241, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-primary);
  font-size: 16px;
  flex-shrink: 0;
}

.market-card-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.market-card-id {
  font-size: 11px;
  color: var(--text-tertiary);
}

.market-card-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 16px;
}

.market-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.market-card-version {
  font-size: 12px;
  color: var(--text-tertiary);
}

.install-btn {
  background-color: rgba(99, 102, 241, 0.15);
  border: 1px solid rgba(99, 102, 241, 0.3);
  color: var(--accent-primary);
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.install-btn:hover:not(:disabled) {
  background-color: rgba(99, 102, 241, 0.3);
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
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.dialog-content {
  width: 560px;
  background-color: #252526;
  border-radius: 12px;
  border: 1px solid #444;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0,0,0,0.4);
  animation: modal-show 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes modal-show {
  from { opacity: 0; transform: scale(0.95) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.dialog-header {
  padding: 20px;
  background-color: #2d2d2d;
  border-bottom: 1px solid #333;
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-icon {
  width: 32px;
  height: 32px;
  background-color: rgba(99, 102, 241, 0.2);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6366f1;
}

.dialog-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  flex: 1;
}

.dialog-close {
  background: none;
  border: none;
  color: #888;
  cursor: pointer;
  font-size: 18px;
}

.dialog-action-btn {
  background-color: rgba(99, 102, 241, 0.15);
  border: 1px solid rgba(99, 102, 241, 0.3);
  color: var(--accent-primary);
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.dialog-action-btn:hover {
  background-color: rgba(99, 102, 241, 0.3);
}

.dialog-body {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.detail-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
}

.detail-value {
  font-size: 13px;
  color: var(--text-primary);
}

.section-heading {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.ext-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ext-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background-color: var(--bg-tertiary);
  border-radius: 6px;
}

.ext-type {
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-primary);
  background: rgba(99, 102, 241, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.ext-name {
  font-size: 12px;
  color: var(--text-primary);
}

.dep-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dep-tag {
  font-size: 11px;
  color: var(--accent-secondary);
  background: rgba(6, 182, 212, 0.1);
  padding: 4px 10px;
  border-radius: 4px;
}

.config-block {
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  overflow-x: auto;
  font-family: var(--font-mono);
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.config-input {
  width: 100%;
  background-color: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 10px 12px;
  color: var(--text-primary);
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
}

.config-input:focus {
  border-color: var(--accent-primary);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

.config-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.config-save-btn {
  background-color: var(--accent-primary);
  border: none;
  color: #fff;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.config-save-btn:hover:not(:disabled) {
  filter: brightness(1.1);
}

.config-save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.config-cancel-btn {
  background-color: var(--bg-hover);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.config-cancel-btn:hover {
  background-color: var(--border-color);
}
</style>
