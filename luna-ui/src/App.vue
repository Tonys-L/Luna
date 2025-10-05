<template>
  <div id="app" class="dark" style="height: 100vh;">
    <el-config-provider namespace="el">
      <el-container style="height: 100%;">
        <el-header style="background-color: #1d1e1f; color: white; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #363637; padding: 0 20px;">
          <h1>Luna</h1>
          <div></div>
        </el-header>
        
        <el-main style="padding: 0; background-color: #141414;">
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