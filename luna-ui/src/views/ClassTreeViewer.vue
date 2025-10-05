<template>
  <el-container style="height: 100%;">
    <!-- 左侧类树 -->
    <el-aside class="class-tree-aside" style="border-right: 1px solid #363637; height: 100%; background-color: #1d1e1f;" width="400px">
      <div style="padding: 10px; border-bottom: 1px solid #363637;">
        <el-input
          v-model="searchText"
          placeholder="搜索类名或包名"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <i class="el-icon-search"></i>
          </template>
        </el-input>
      </div>
      
      <div style="padding: 10px; border-bottom: 1px solid #363637; display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; gap: 5px;">
          <el-tooltip content="刷新数据" placement="top">
            <el-button :loading="loading" circle class="toolbar-button" size="small" @click="refreshData">
              <template #default>
                <i :class="loading ? 'el-icon-loading' : 'el-icon-refresh'"></i>
              </template>
            </el-button>
          </el-tooltip>
          <el-tooltip content="全部展开" placement="top">
            <el-button circle class="toolbar-button" size="small" @click="expandAll">
              <template #default>
                <i class="el-icon-folder-opened"></i>
              </template>
            </el-button>
          </el-tooltip>
          <el-tooltip content="全部折叠" placement="top">
            <el-button circle class="toolbar-button" size="small" @click="collapseAll">
              <template #default>
                <i class="el-icon-folder"></i>
              </template>
            </el-button>
          </el-tooltip>
        </div>
        <div style="display: flex; align-items: center; color: #a3a6ad; font-size: 12px;">
          <span>类加载器: {{ loaderCount }}</span>
          <span style="margin-left: 10px;">类总数: {{ totalClassCount }}</span>
        </div>
      </div>
      
      <el-tree
        ref="classTree"
        :data="treeData"
        :expand-on-click-node="false"
        :props="treeProps"
        :default-expanded-keys="expandedKeys"
        :filter-node-method="filterNode"
        class="class-tree"
        highlight-current
        @node-click="handleNodeClick"
        node-key="id"
      >
        <template #default="{ node, data }">
          <span class="custom-tree-node">
            <span>
              <i 
                :class="data.isClass ? 'el-icon-document' : 'el-icon-folder'"
                :style="{ color: data.isClass ? '#409EFF' : '#E6A23C' }"
              ></i>
              <span style="margin-left: 5px;">{{ node.label }}</span>
            </span>
          </span>
        </template>
      </el-tree>
    </el-aside>
    
    <!-- 右侧类详情 -->
    <el-main style="background-color: #141414; padding: 0;">
      <ClassDetail :class-info="selectedClass" />
    </el-main>
  </el-container>
</template>

<script>
import ClassDetail from '../components/ClassDetail.vue'
import {getClassInfo, getClassList} from '../utils/api'

export default {
  name: 'ClassTreeViewer',
  components: {
    ClassDetail
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
      rawClassData: {}, // 保存原始数据用于搜索
      loaderCount: 0,
      totalClassCount: 0,
      loading: false
    }
  },
  mounted() {
    this.loadClassTree()
  },
  methods: {
    async refreshData() {
      this.loading = true
      try {
        await this.loadClassTree()
      } finally {
        this.loading = false
      }
    },
    
    async loadClassTree() {
      try {
        const data = await getClassList()
        this.rawClassData = data
        this.treeData = this.buildTreeData(data)
        
        // 更新统计信息
        this.loaderCount = Object.keys(data).length
        this.totalClassCount = this.countTotalClasses(data)
        
        // 通知父组件更新类数量
        this.$emit('class-count-update', this.totalClassCount)
      } catch (error) {
        console.error('加载类树失败:', error)
        this.$message.error('加载类树失败')
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
          children: []
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
              isClass: true
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
            children: child.children
          })
        } else {
          // 正常添加包节点
          nodes.push({
            id: `package-${currentPath}`,
            label: pkg.name,
            isClass: false,
            packageName: currentPath,
            children: children
          })
        }
      }
      
      // 添加类节点
      for (const cls of classes) {
        nodes.push({
          id: `class-${cls.content.className}`,
          label: cls.name,
          isClass: true,
          className: cls.content.className
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
      try {
        const classInfo = await getClassInfo(className)
        this.selectedClass = classInfo
      } catch (error) {
        console.error('加载类信息失败:', error)
        this.$message.error('加载类信息失败')
      }
    },
    
    handleSearch(value) {
      this.$refs.classTree.filter(value)
    },
    
    filterNode(value, data) {
      if (!value) return true
      return data.label.toLowerCase().includes(value.toLowerCase())
    },
    
    expandAll() {
      this.expandAllNodes(this.treeData)
    },
    
    expandAllNodes(nodes) {
      nodes.forEach(node => {
        this.expandedKeys.push(node.id)
        if (node.children && node.children.length > 0) {
          this.expandAllNodes(node.children)
        }
      })
    },
    
    collapseAll() {
      this.expandedKeys = []
    }
  }
}
</script>

<style scoped>
.class-tree-aside {
  background-color: #1d1e1f;
  border-right: 1px solid #363637;
}

.class-tree {
  padding: 10px;
  background-color: #1d1e1f;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
  color: #e5eaf3;
}

.custom-tree-node:hover {
  background-color: #262727;
  border-radius: 4px;
}

.toolbar-button {
  background-color: #262727;
  border-color: #363637;
  color: #cfd3dc;
  width: 28px;
  height: 28px;
  padding: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toolbar-button:hover {
  background-color: #363737;
  border-color: #464747;
  color: #e5eaf3;
}

.toolbar-button:active {
  background-color: #1a1b1b;
  border-color: #262727;
}

.toolbar-button i {
  font-size: 14px;
}

:deep(.el-tree) {
  background-color: #1d1e1f;
  color: #e5eaf3;
}

:deep(.el-tree-node__content:hover) {
  background-color: #262727;
}

:deep(.el-tree-node.is-current > .el-tree-node__content) {
  background-color: #1a2c42;
}

:deep(.el-input__wrapper) {
  background-color: #141414;
  box-shadow: 0 0 0 1px #363637 inset;
}

:deep(.el-button [class*="el-icon"] + span) {
  margin-left: 0;
}
</style>