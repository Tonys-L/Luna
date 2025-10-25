<template>
  <div id="app" class="dark">
    <el-config-provider namespace="el">
      <el-container class="app-container">
        <el-header class="app-header">
          <div class="header-content">
            <h1>Luna</h1>
            <div class="header-actions">
              
            </div>
          </div>
        </el-header>
        
        <el-main class="app-main">
          <ClassTreeViewer @class-count-update="updateClassCount" @refresh-data="handleRefreshData" />
        </el-main>
      </el-container>
    </el-config-provider>
  </div>
</template>

<script>
import ClassTreeViewer from './views/ClassTreeViewer.vue'

export default {
  name: 'App',
  components: {
    ClassTreeViewer
  },
  data() {
    return {
      loading: false,
      classCount: 0
    }
  },
  methods: {
    async refreshData() {
      this.loading = true
      try {
        // 触发全局事件，通知所有组件刷新数据
        this.$emit('refresh-data')
        // 等待一段时间以显示加载状态
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
#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #e5eaf3;
  height: 100vh;
  background-color: #141414;
  overflow: hidden;
}

.app-container {
  height: 100vh;
  overflow: hidden;
}

.app-header {
  background-color: #1d1e1f;
  color: white;
  border-bottom: 1px solid #363637;
  padding: 0;
  height: auto;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.refresh-button {
  background-color: #262727;
  border-color: #363637;
  color: #cfd3dc;
}

.refresh-button:hover {
  background-color: #363737;
  border-color: #464747;
  color: #e5eaf3;
}

.class-count {
  color: #a3a6ad;
  font-size: 14px;
}

.app-main {
  padding: 0;
  background-color: #141414;
  overflow: hidden;
  flex: 1;
}

/* 隐藏滚动条 */
::-webkit-scrollbar {
  display: none;
}

/* 确保整个页面没有滚动条 */
html, body {
  overflow: hidden;
  height: 100%;
  margin: 0;
  padding: 0;
}

/* 暗黑主题覆盖 */
html.dark {
  --el-bg-color: #141414;
  --el-bg-color-page: #0a0a0a;
  --el-bg-color-overlay: #1d1e1f;
  --el-text-color-primary: #e5eaf3;
  --el-text-color-regular: #cfd3dc;
  --el-text-color-secondary: #a3a6ad;
  --el-border-color: #363637;
  --el-border-color-light: #2d2d2d;
  --el-border-color-extra-light: #232323;
  --el-fill-color: #1d1e1f;
  --el-fill-color-light: #262727;
  --el-fill-color-blank: #141414;
  --el-mask-color: rgba(0, 0, 0, 0.8);
}
</style>