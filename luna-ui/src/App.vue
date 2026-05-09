<template>
  <div id="app" class="industrial-tech">
    <el-config-provider namespace="el">
      <div class="app-container">
        <!-- 顶部导航栏 -->
        <header class="app-header">
          <div class="header-content">
            <!-- 品牌标识 -->
            <div class="brand">
              <div class="brand-logo">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                  <rect x="2" y="2" width="20" height="20" rx="1" stroke="#00d4ff" stroke-width="1.5"/>
                  <path d="M6 12L9 15L16 8" stroke="#00d4ff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  <rect x="4" y="4" width="16" height="16" rx="1" stroke="#00d4ff" stroke-width="0.5" stroke-dasharray="2 2"/>
                </svg>
              </div>
              <h1 class="brand-name">LUNA</h1>
              <div class="brand-tag">{{ t('app.tag') }}</div>
            </div>
            
            <!-- 导航选项卡 -->
            <nav class="main-nav">
              <button 
                :class="['nav-item', { active: activeTab === 'class-tree' }]"
                @click="activeTab = 'class-tree'"
              >
                <i class="fas fa-folder-tree nav-icon"></i>
                <span class="nav-label">{{ t('nav.class_tree') }}</span>
              </button>
              <button 
                :class="['nav-item', { active: activeTab === 'log' }]"
                @click="activeTab = 'log'"
              >
                <i class="fas fa-terminal nav-icon"></i>
                <span class="nav-label">日志监控</span>
              </button>
              <button 
                :class="['nav-item', { active: activeTab === 'dashboard' }]"
                @click="activeTab = 'dashboard'"
              >
                <i class="fas fa-tachometer-alt nav-icon"></i>
                <span class="nav-label">监控大盘</span>
              </button>
              <button 
                :class="['nav-item', { active: activeTab === 'thread-analyzer' }]"
                @click="activeTab = 'thread-analyzer'"
              >
                <i class="fas fa-microchip nav-icon"></i>
                <span class="nav-label">线程分析</span>
              </button>
              <button 
                :class="['nav-item', { active: activeTab === 'configuration' }]"
                @click="activeTab = 'configuration'"
              >
                <i class="fas fa-sliders-h nav-icon"></i>
                <span class="nav-label">{{ t('nav.configuration') }}</span>
              </button>
            </nav>
            
            <!-- 右侧操作区 -->
            <div class="header-right">
              <!-- 语言切换按钮 -->
              <button class="lang-switcher" @click="switchLang">
                {{ currentLocale === 'zh' ? 'EN' : '中文' }}
              </button>
            </div>
          </div>
        </header>
        
        <!-- 主内容区域 -->
        <main class="app-main">
          <ClassTreeViewer v-if="activeTab === 'class-tree'" @class-count-update="updateClassCount" @refresh-data="handleRefreshData" />
          <LogViewer v-else-if="activeTab === 'log'" />
          <Dashboard v-else-if="activeTab === 'dashboard'" />
          <ThreadAnalyzer v-else-if="activeTab === 'thread-analyzer'" />
          <ConfigurationViewer v-else-if="activeTab === 'configuration'" />
        </main>
        
        <!-- 底部状态栏 -->
        <footer class="app-footer">
          <div class="footer-content">
            <div class="footer-info">
              <span class="info-item">CLASSES: {{ classCount }}</span>
              <span class="info-item">{{ t('app.version') }}</span>
            </div>
            <div class="footer-actions">
              <button class="footer-button" @click="refreshData">
                <i class="fas fa-sync-alt"></i>
                <span>{{ t('app.refresh') }}</span>
              </button>
            </div>
          </div>
        </footer>
      </div>
    </el-config-provider>
  </div>
</template>

<script>
import { useI18n } from 'vue-i18n'
import ClassTreeViewer from './views/ClassTreeViewer.vue'
import ConfigurationViewer from './views/ConfigurationViewer.vue'
import LogViewer from './views/LogViewer.vue'
import Dashboard from './views/Dashboard.vue'
import ThreadAnalyzer from './views/ThreadAnalyzer.vue'

export default {
  name: 'App',
  components: {
    ClassTreeViewer,
    ConfigurationViewer,
    LogViewer,
    Dashboard,
    ThreadAnalyzer
  },
  setup() {
    const { t, locale } = useI18n()
    const currentLocale = locale

    const switchLang = () => {
      currentLocale.value = currentLocale.value === 'zh' ? 'en' : 'zh'
    }

    return { t, switchLang, currentLocale }
  },
  data() {
    return {
      activeTab: 'class-tree',
      loading: false,
      classCount: 0
    }
  },
  methods: {
    async refreshData() {
      this.loading = true
      try {
        this.$emit('refresh-data')
        await new Promise(resolve => setTimeout(resolve, 500))
      } finally {
        this.loading = false
      }
    },
    updateClassCount(count) {
      this.classCount = count
    },
    handleRefreshData() {
      this.refreshData()
    }
  }
}
</script>

<style>
@import "@fortawesome/fontawesome-free/css/all.css";

/* 全局重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 根变量 - VSCode Dark+ 风格 */
:root {
  --bg-primary: #1e1e1e;
  --bg-secondary: #252526;
  --bg-tertiary: #2d2d30;
  --bg-hover: #3c3c3c;
  
  --text-primary: #d4d4d4;
  --text-secondary: #9d9d9d;
  --text-tertiary: #6a6a6a;
  
  --border-color: #3c3c3c;
  --border-focus: #007acc;
  
  --accent-primary: #007acc;
  --accent-success: #6a9955;
  --accent-danger: #f14c4c;
  
  --radius-sm: 2px;
  --radius-md: 3px;
  
  --transition-fast: 0.1s ease;
  
  --font-mono: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
}

/* 基础样式 */
html, body {
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.4;
  color: var(--text-primary);
  background-color: var(--bg-primary);
  height: 100%;
  overflow: hidden;
}

#app {
  height: 100vh;
  overflow: hidden;
}

/* 应用容器 */
.app-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-primary);
}

/* 顶部导航栏 */
.app-header {
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  height: 40px;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 100%;
  gap: 24px;
}

/* 右侧操作区 */
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

/* 品牌标识 */
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

.brand-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.brand-tag {
  font-size: 10px;
  color: var(--text-tertiary);
}

/* 导航选项卡 */
.main-nav {
  display: flex;
  gap: 8px;
  flex: 1;
  justify-content: center;
}

.nav-item {
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
  border-bottom: 1px solid transparent;
}

.nav-item:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

.nav-item.active {
  color: var(--text-primary);
  background-color: var(--bg-hover);
  border-bottom-color: var(--accent-primary);
}

.nav-icon {
  flex-shrink: 0;
}

/* 主内容区域 */
.app-main {
  flex: 1;
  overflow: hidden;
  background-color: var(--bg-primary);
}

/* 底部状态栏 */
.app-footer {
  background-color: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  height: 22px;
  flex-shrink: 0;
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  height: 100%;
}

.footer-info {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 11px;
  color: var(--text-tertiary);
}

.info-item {
  position: relative;
}

.info-item::after {
  content: '';
  position: absolute;
  right: -8px;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 10px;
  background-color: var(--border-color);
}

.info-item:last-child::after {
  display: none;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background-color: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 11px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.footer-button:hover {
  background-color: var(--bg-hover);
  color: var(--text-primary);
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: var(--bg-primary);
}

::-webkit-scrollbar-thumb {
  background: var(--bg-hover);
  border-radius: 0;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--border-color);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .nav-label {
    display: none;
  }
  
  .status-text {
    display: none;
  }
  
  .footer-button span {
    display: none;
  }
}
</style>