<template>
  <div class="class-tree-viewer">
    <!-- 左侧类树 -->
    <aside :class="['class-tree-aside', { collapsed: isTreeCollapsed }]">
      <!-- 树头部 -->
      <div class="tree-header">
        <!-- 搜索框 -->
        <div class="search-container">
          <el-input
            v-model="searchText"
            :placeholder="t('tree.search_placeholder')"
            clearable
            @input="handleSearch"
            class="search-input"
          >
            <template #prefix>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </template>
          </el-input>
        </div>
        
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-buttons">
            <!-- 刷新按钮 -->
            <button 
              :class="['toolbar-button', { loading: loading }]"
              @click="handleRefresh"
              :title="t('app.refresh')"
            >
              <i v-if="!loading" class="fas fa-sync-alt"></i>
              <div v-else class="loading-spinner"></div>
            </button>
            
            <!-- 过滤已注入按钮 -->
            <button 
              :class="['toolbar-button', { active: showOnlyInjected }]"
              @click="toggleShowOnlyInjected"
              :title="showOnlyInjected ? '显示全部类' : '仅显示已注入类'"
            >
              <i class="fas fa-syringe" :style="{ color: showOnlyInjected ? '#8b5cf6' : '' }"></i>
            </button>
            
            <!-- 全部展开按钮 -->
            <button 
              class="toolbar-button" 
              @click="handleExpandAll"
              :title="t('tree.expand_all')"
            >
              <i class="fas fa-folder-open"></i>
            </button>
            
            <!-- 全部折叠按钮 -->
            <button 
              class="toolbar-button" 
              @click="handleCollapseAll"
              :title="t('tree.collapse_all')"
            >
              <i class="fas fa-folder"></i>
            </button>
          </div>
          
          <!-- 统计信息 -->
          <div class="stats">
            <span class="stat-item">{{ t('tree.loaders') }}: {{ loaderCount }}</span>
            <span class="stat-item">{{ t('tree.classes') }}: {{ totalClassCount }}</span>
          </div>
        </div>
      </div>
      
      <!-- 树内容 -->
        <div class="tree-content">
          <el-tree
            :key="treeKey"
            ref="classTree"
            :data="treeData"
            :default-expanded-keys="expandedKeys"
            :expand-on-click-node="true"
            :filter-node-method="filterNode"
            :props="treeProps"
            class="class-tree"
            highlight-current
            node-key="id"
            @node-click="handleNodeClick"
          >
          <template #default="{ node, data }">
            <div class="tree-node">
                <span class="node-icon">
                  <i v-if="data.isClass" class="fas fa-file-code"></i>
                  <i v-else class="fas fa-folder"></i>
                </span>
                <span class="node-label">{{ node.label }}</span>
                <span v-if="data.injectionCount > 0" class="node-injection-badge">
                  {{ data.injectionCount }}
                </span>
              </div>
          </template>
        </el-tree>
      </div>
      
      <!-- 折叠按钮 -->
      <button class="collapse-toggle" @click="toggleTreeCollapse">
        <i :class="['fas fa-caret-left collapse-icon', { rotated: isTreeCollapsed }]"></i>
      </button>
    </aside>
    
    <!-- 右侧类详情 -->
    <main class="detail-main">
      <!-- 标签栏 -->
      <div v-if="openTabs.length > 0" class="tabs-bar">
        <div 
          v-for="tab in openTabs" 
          :key="tab.className"
          :class="['tab-item', { active: activeTabName === tab.className }]"
          @click="selectTab(tab)"
        >
          <div v-if="tab.loading" class="tab-loading-spinner"></div>
          <i v-else class="fas fa-file-code tab-icon"></i>
          
          <span class="tab-label" :title="tab.className">{{ getShortClassName(tab.className) }}</span>
          
          <span v-if="tab.injectionCount > 0" class="tab-injection-badge" title="Active Injections">
            {{ tab.injectionCount }}
          </span>

          <i class="fas fa-times tab-close" @click.stop="closeTab(tab.className)"></i>
        </div>
      </div>

      <ClassDetail 
        v-if="selectedClass" 
        :key="activeTabName"
        :class-info="selectedClass" 
        @sync-state="handleTabStateSync"
      />
      
      <!-- 无选择状态 -->
      <div v-else class="empty-state">
        <div class="empty-state-content">
          <i class="fas fa-code-branch"></i>
          <h3>开始分析</h3>
          <p>从左侧树中选择一个类，即可开启分析之旅</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { useI18n } from 'vue-i18n'
import ClassDetail from '../components/ClassDetail.vue'
import {getClassAnalysis, getClassTree} from '../utils/api';

export default {
  name: 'ClassTreeViewer',
  components: {
    ClassDetail
  },
  setup() {
    const { t } = useI18n()
    return { t }
  },
  data() {
    return {
      searchText: '',
      treeData: [],
      expandedKeys: [],
      treeProps: {
        children: 'children',
        label: 'label'
      },
      selectedClass: null,
      openTabs: [],
      activeTabName: '',
      rawClassData: {}, // 保存原始数据用于搜索
      loaderCount: 0,
      totalClassCount: 0,
      loading: false,
      isTreeCollapsed: false,
      showOnlyInjected: false,
      treeKey: 1
    }
  },
  mounted() {
    this.loadClassTree()
  },
  methods: {
    async refreshData() {
      this.loading = true
      try {
        this.expandedKeys = []
        this.treeKey++
        await this.loadClassTree()
      } finally {
        this.loading = false
      }
    },
    
    handleRefresh() {
      this.refreshData()
    },
    
    handleExpandAll() {
      this.expandAll()
    },
    
    handleCollapseAll() {
      this.collapseAll()
    },
    
    async loadClassTree() {
      try {
        // 使用封装的API方法获取类树数据
        const data = await getClassTree();
        this.rawClassData = data
        this.treeData = this.buildTreeData(data)
        
        // 更新统计信息
        this.loaderCount = Object.keys(data).length
        this.totalClassCount = this.countTotalClasses(data)
        
        // 通知父组件更新类数量
        this.$emit('class-count-update', this.totalClassCount)
      } catch (error) {
        console.error('加载类树失败:', error)
        this.$message.error('加载类树失败: ' + error.message)
      }
    },
    
    countTotalClasses(classData) {
      let count = 0
      for (const classes of Object.values(classData)) {
        count += classes.length
      }
      return count
    },
    
    buildTreeData(classData) {
      const result = []
      
      // 遍历每个类加载器
      for (const [loaderName, classes] of Object.entries(classData)) {
        const loaderNode = {
          id: `loader-${loaderName}`,
          label: loaderName,
          isClass: false,
          loaderName: loaderName,
          children: [],
          expanded: false
        }
        
        // 构建包层次结构
        const packageTree = {}
        classes.forEach(cls => {
          const className = cls.className
          const parts = className.split('.')
          let current = packageTree
          
          // 处理包路径
          for (let i = 0; i < parts.length - 1; i++) {
            const part = parts[i]
            if (!current[part]) {
              current[part] = {}
            }
            current = current[part]
          }
          
          // 添加类
          const classNamePart = parts[parts.length - 1]
          if (!current[classNamePart]) {
            current[classNamePart] = {
              className: className,
              isClass: true,
              injectionCount: cls.injectionCount || 0
            }
          }
        })
        
        // 转换为树节点
        loaderNode.children = this.convertPackageTreeToNodes(packageTree, loaderName)
        result.push(loaderNode)
      }
      
      return result
    },
    
    convertPackageTreeToNodes(packageTree, parentPath) {
      const nodes = []
      
      // 分离包和类
      const packages = []
      const classes = []
      
      for (const [name, content] of Object.entries(packageTree)) {
        if (content.isClass) {
          classes.push({
            name: name,
            content: content
          })
        } else {
          packages.push({
            name: name,
            content: content
          })
        }
      }
      
      // 递归处理包节点
      for (const pkg of packages) {
        const currentPath = parentPath ? `${parentPath}.${pkg.name}` : pkg.name
        const children = this.convertPackageTreeToNodes(pkg.content, currentPath)
        
        // 如果包下没有类，尝试合并层级
        if (children.length === 0) {
          // 包下没有子节点，跳过
          continue
        } else if (children.length === 1 && !children[0].isClass) {
          // 如果只有一个子包，合并层级
          const child = children[0]
          nodes.push({
            id: child.id,
            label: `${pkg.name}.${child.label}`,
            isClass: false,
            packageName: child.packageName,
            children: child.children,
            expanded: false
          })
        } else {
          // 正常添加包节点
          const pkgInjectionCount = this.sumChildrenInjection(children)
          nodes.push({
            id: `package-${currentPath}`,
            label: pkg.name,
            isClass: false,
            packageName: currentPath,
            children: children,
            injectionCount: pkgInjectionCount,
            expanded: false
          })
        }
      }
      
      // 添加类节点
      for (const cls of classes) {
        nodes.push({
          id: `class-${cls.content.className}`,
          label: cls.name,
          isClass: true,
          className: cls.content.className,
          injectionCount: cls.content.injectionCount || 0,
          expanded: false
        })
      }
      
      // 排序：包在前，类在后
      nodes.sort((a, b) => {
        if (a.isClass && !b.isClass) {
          return 1 // 类排在后面
        }
        if (!a.isClass && b.isClass) {
          return -1 // 包排在前面
        }
        return a.label.localeCompare(b.label) // 同类型按字母排序
      })
      
      return nodes
    },
    
    handleNodeClick(data) {
      if (data.isClass) {
        this.loadClassInfo(data.className)
      }
    },
    
    async loadClassInfo(className) {
      // 检查是否已经打开
      const existingTab = this.openTabs.find(t => t.className === className)
      if (existingTab) {
        this.selectTab(existingTab)
        return
      }

      try {
        const classInfo = await getClassAnalysis(className);
        // 初始化扩展状态
        const newTab = {
          ...classInfo,
          loading: false,
          injectionCount: 0
        }
        this.openTabs.push(newTab)
        this.selectTab(newTab)
      } catch (error) {
        console.error('加载类信息失败:', error)
        this.$message.error('加载类信息失败: ' + error.message)
      }
    },
    
    selectTab(tab) {
      this.selectedClass = tab
      this.activeTabName = tab.className
    },

    closeTab(className) {
      const index = this.openTabs.findIndex(t => t.className === className)
      if (index === -1) return

      this.openTabs.splice(index, 1)

      // 如果关闭的是当前选中的
      if (this.activeTabName === className) {
        if (this.openTabs.length > 0) {
          // 选中上一个或第一个
          const nextTab = this.openTabs[Math.max(0, index - 1)]
          this.selectTab(nextTab)
        } else {
          this.selectedClass = null
          this.activeTabName = ''
        }
      }
    },

    getShortClassName(fullName) {
      const parts = fullName.split('.')
      return parts[parts.length - 1]
    },

    handleTabStateSync(state) {
      const tab = this.openTabs.find(t => t.className === state.className)
      if (tab) {
        if (state.loading !== undefined) tab.loading = state.loading
        if (state.injectionCount !== undefined) tab.injectionCount = state.injectionCount
      }

      // 同步更新类树中对应节点的 injectionCount
      if (state.injectionCount !== undefined && state.className) {
        this.updateTreeNodeInjectionCount(state.className, state.injectionCount)
      }
    },

    updateTreeNodeInjectionCount(className, count) {
      const updateNode = (nodes) => {
        for (const node of nodes) {
          if (node.isClass && node.className === className) {
            node.injectionCount = count
            return true
          }
          if (node.children && node.children.length > 0) {
            if (updateNode(node.children)) {
              node.injectionCount = this.sumChildrenInjection(node.children)
              return true
            }
          }
        }
        return false
      }
      updateNode(this.treeData)
      this.treeKey++
    },
    
    handleSearch(value) {
      this.$refs.classTree.filter(value)
    },
    
    filterNode(value, data) {
      // 综合判定搜索文本和“仅显示注入”状态
      const matchesSearch = !this.searchText || data.label.toLowerCase().includes(this.searchText.toLowerCase())
      const matchesInjected = !this.showOnlyInjected || data.injectionCount > 0
      
      return matchesSearch && matchesInjected
    },
    
    toggleShowOnlyInjected() {
      this.showOnlyInjected = !this.showOnlyInjected
      this.$refs.classTree.filter(this.searchText)
    },
    
    sumChildrenInjection(nodes) {
      return nodes.reduce((sum, node) => sum + (node.injectionCount || 0), 0)
    },
    
    expandAll() {
      const keys = []
      const collectKeys = (nodes) => {
        nodes.forEach(node => {
          if (node.id) {
            keys.push(node.id)
          }
          if (node.children && node.children.length > 0) {
            collectKeys(node.children)
          }
        })
      }
      collectKeys(this.treeData)
      this.expandedKeys = [...new Set(keys)]
      this.treeKey++
    },
    
    collapseAll() {
      this.expandedKeys = []
      this.treeKey++
    },
    
    // 递归展开所有节点
    expandNodesRecursively(node) {
      if (node) {
        node.expanded = true
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => {
            this.expandNodesRecursively(child)
          })
        }
      }
    },
    
    // 递归折叠所有节点
    collapseNodesRecursively(node) {
      if (node) {
        node.expanded = false
        if (node.children && node.children.length > 0) {
          node.children.forEach(child => {
            this.collapseNodesRecursively(child)
          })
        }
      }
    },
    
    // 展开所有节点（数据级）
    expandAllNodesInData() {
      const expandNodes = (nodes) => {
        nodes.forEach(node => {
          node.expanded = true
          if (node.children && node.children.length > 0) {
            expandNodes(node.children)
          }
        })
      }
      expandNodes(this.treeData)
      this.treeKey++
    },
    
    // 折叠所有节点（数据级）
    collapseAllNodesInData() {
      const collapseNodes = (nodes) => {
        nodes.forEach(node => {
          node.expanded = false
          if (node.children && node.children.length > 0) {
            collapseNodes(node.children)
          }
        })
      }
      collapseNodes(this.treeData)
      this.treeKey++
    },
    
    toggleTreeCollapse() {
      this.isTreeCollapsed = !this.isTreeCollapsed;
    }
  }
}
</script>

<style scoped>
.class-tree-viewer {
  height: 100%;
  display: flex;
  overflow: hidden;
}

/* 左侧类树 */
.class-tree-aside {
  width: 250px;
  height: 100%;
  background-color: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.class-tree-aside.collapsed {
  width: 0;
}

/* 树头部 */
.tree-header {
  padding: 8px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

/* 搜索容器 */
.search-container {
  margin-bottom: 8px;
}

/* 搜索输入框 */
.search-input :deep(.el-input__wrapper) {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 0;
  padding: 0 8px;
  height: 26px;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--border-focus);
}

.search-input :deep(.el-input__inner) {
  color: var(--text-primary);
  font-size: 12px;
}

.search-input :deep(.el-input__inner::placeholder) {
  color: var(--text-tertiary);
  font-size: 12px;
}

.search-input :deep(.el-input__prefix-inner) {
  color: var(--text-tertiary);
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-buttons {
  display: flex;
  gap: 2px;
}

/* 工具栏按钮 */
.toolbar-button {
  width: 24px;
  height: 24px;
  padding: 0;
  background-color: transparent;
  border: none;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.toolbar-button:hover {
  background-color: var(--bg-hover);
}

.toolbar-button.loading {
  color: var(--accent-primary);
}

/* 加载动画 */
.loading-spinner {
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

/* 统计信息 */
.stats {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: var(--text-tertiary);
}

.stat-item {
  position: relative;
}

.stat-item::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 1px;
  height: 10px;
  background-color: var(--border-color);
}

.stat-item:last-child::after {
  display: none;
}

/* 树内容 */
.tree-content {
  flex: 1;
  overflow: auto;
  padding: 2px;
}

/* 类树 */
.class-tree {
  background: transparent;
  height: 100%;
}

/* 树节点 */
.tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 4px;
  cursor: pointer;
}

.tree-node:hover {
  background-color: var(--bg-hover);
}

/* 节点图标 */
.node-icon {
  color: var(--text-secondary);
  flex-shrink: 0;
  font-size: 12px;
}

.tree-node:hover .node-icon {
  color: var(--text-primary);
}

/* 注入徽标 */
.node-injection-badge {
  background-color: #8b5cf6;
  color: white;
  font-size: 10px;
  padding: 0 5px;
  border-radius: 10px;
  height: 16px;
  line-height: 16px;
  min-width: 16px;
  text-align: center;
  font-weight: bold;
  box-shadow: 0 0 5px rgba(139, 92, 246, 0.5);
  margin-left: 4px;
}

.toolbar-button.active {
  background-color: var(--bg-hover);
}

/* 折叠按钮 */
.collapse-toggle {
  position: absolute;
  top: 50%;
  right: -1px;
  transform: translateY(-50%);
  width: 16px;
  height: 32px;
  background-color: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-left: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
}

.collapse-toggle:hover {
  background-color: var(--bg-hover);
}

.collapse-icon {
  color: var(--text-tertiary);
  font-size: 10px;
}

.collapse-icon.rotated {
  transform: rotate(180deg);
}

.collapse-toggle:hover .collapse-icon {
  color: var(--text-primary);
}

/* 右侧类详情 */
.detail-main {
  flex: 1;
  overflow: hidden;
  background-color: var(--bg-primary);
  display: flex;
  flex-direction: column;
}

/* 标签栏 */
.tabs-bar {
  display: flex;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  overflow-x: auto;
  scrollbar-width: none; /* Firefox */
}

.tabs-bar::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  height: 35px;
  min-width: 120px;
  max-width: 200px;
  border-right: 1px solid var(--border-color);
  cursor: pointer;
  background-color: var(--bg-secondary);
  color: var(--text-tertiary);
  font-size: 12px;
  transition: all 0.2s;
  user-select: none;
}

.tab-item:hover {
  background-color: var(--bg-hover);
  color: var(--text-secondary);
}

.tab-item.active {
  background-color: var(--bg-primary);
  color: var(--accent-primary);
  border-bottom: 2px solid var(--accent-primary);
  height: 34px; /* 为了不遮住下边框 */
}

.tab-icon {
  font-size: 11px;
  opacity: 0.7;
}

.tab-loading-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-top-color: var(--accent-primary);
  border-radius: 50%;
  animation: tab-spin 1s linear infinite;
}

@keyframes tab-spin { to { transform: rotate(360deg); } }

.tab-label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-injection-badge {
  background-color: var(--accent-primary);
  color: white;
  font-size: 9px;
  font-weight: 800;
  padding: 0 5px;
  height: 14px;
  line-height: 14px;
  border-radius: 7px;
  margin-right: 2px;
}

.tab-close {
  font-size: 10px;
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  opacity: 0.5;
  transition: all 0.2s;
}

.tab-close:hover {
  background-color: rgba(255, 255, 255, 0.1);
  opacity: 1;
  color: #f87171;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.empty-state-content {
  text-align: center;
}

.empty-state-content i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.2;
}

.empty-state-content h3 {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.empty-state-content p {
  font-size: 13px;
}

/* Element Plus 树样式覆盖 */
:deep(.el-tree) {
  background: transparent;
  color: var(--text-primary);
}

:deep(.el-tree-node) {
  padding: 0;
}

:deep(.el-tree-node__content) {
  height: 22px;
  padding: 0;
}

:deep(.el-tree-node__content:hover) {
  background: transparent;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--bg-hover);
}

:deep(.el-tree-node.is-current > .el-tree-node__content .node-label) {
  color: var(--accent-primary);
}

:deep(.el-tree-node__expand-icon) {
  color: var(--text-tertiary);
  font-size: 10px;
}

:deep(.el-tree-node__expand-icon:hover) {
  color: var(--text-primary);
}

:deep(.el-tree-node__expand-icon.is-leaf) {
  color: transparent;
}
</style>