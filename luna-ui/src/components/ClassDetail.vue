<template>
  <div v-if="classInfo" class="class-detail">
    <el-card class="class-header">
      <template #header>
        <div class="card-header">
          <span>类信息: {{ classInfo.className }}</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="12">
          <div><strong>父类:</strong> {{ classInfo.superClass || '无' }}</div>
        </el-col>
        <el-col :span="12">
          <div><strong>访问标志:</strong> {{ classInfo.accessFlags }}</div>
        </el-col>
      </el-row>
      <div style="margin-top: 10px;">
        <strong>实现接口:</strong>
        <el-tag 
          v-for="iface in classInfo.interfaces" 
          :key="iface" 
          style="margin-right: 5px; margin-top: 5px;"
        >
          {{ iface }}
        </el-tag>
        <div v-if="!classInfo.interfaces || classInfo.interfaces.length === 0">无</div>
      </div>
    </el-card>
    
    <el-tabs class="detail-tabs" type="border-card">
      <el-tab-pane label="字段">
        <el-table :data="classInfo.fields" size="small" style="width: 100%">
          <el-table-column label="字段名" prop="name" width="200"></el-table-column>
          <el-table-column label="描述符" prop="descriptor" width="200"></el-table-column>
          <el-table-column label="访问标志" prop="accessFlags" width="100"></el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="方法">
        <el-table :data="classInfo.methods" size="small" style="width: 100%">
          <el-table-column label="方法名" prop="name" width="200"></el-table-column>
          <el-table-column label="描述符" prop="descriptor" width="200"></el-table-column>
          <el-table-column label="访问标志" prop="accessFlags" width="100"></el-table-column>
          <el-table-column label="参数">
            <template #default="scope">
              <el-tag 
                v-for="param in scope.row.parameters" 
                :key="param.name" 
                size="mini" 
                style="margin-right: 3px;"
              >
                {{ param.name }}: {{ param.descriptor }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="反编译源码">
        <div style="text-align: center; padding: 20px;">
          <el-button type="primary" @click="loadDecompiledCode">加载反编译源码</el-button>
        </div>
        <div v-if="decompiledCode" class="decompiled-code">
          <pre><code>{{ decompiledCode }}</code></pre>
        </div>
        <div v-else-if="loadingDecompiled" class="loading-placeholder">
          <i class="el-icon-loading"></i> 正在加载反编译源码...
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
  <div v-else class="no-selection">
    <el-empty description="请选择一个类查看详细信息">
      <template #image>
        <i class="el-icon-document" style="font-size: 60px; color: #363637;"></i>
      </template>
    </el-empty>
  </div>
</template>

<script>
import {getDecompiledCode} from '../utils/api'

export default {
  name: 'ClassDetail',
  props: {
    classInfo: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      decompiledCode: '',
      loadingDecompiled: false
    }
  },
  watch: {
    classInfo: {
      handler() {
        // 当选中的类改变时，重置反编译代码
        this.decompiledCode = ''
      },
      deep: true
    }
  },
  methods: {
    async loadDecompiledCode() {
      if (!this.classInfo) return
      
      this.loadingDecompiled = true
      try {
        const code = await getDecompiledCode(this.classInfo.className)
        this.decompiledCode = code
      } catch (error) {
        console.error('加载反编译代码失败:', error)
        this.$message.error('加载反编译代码失败')
      } finally {
        this.loadingDecompiled = false
      }
    }
  }
}
</script>

<style scoped>
.class-detail {
  padding: 20px;
  height: 100%;
  overflow: auto;
  background-color: #141414;
}

.card-header {
  font-weight: bold;
  color: #e5eaf3;
}

.detail-tabs {
  margin-top: 20px;
}

.decompiled-code {
  margin-top: 10px;
  padding: 15px;
  background-color: #1d1e1f;
  border-radius: 4px;
  overflow-x: auto;
  border: 1px solid #363637;
}

.decompiled-code pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #cfd3dc;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace;
}

.loading-placeholder {
  text-align: center;
  padding: 20px;
  color: #a3a6ad;
}

.no-selection {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background-color: #141414;
}

:deep(.el-card) {
  background-color: #1d1e1f;
  border: 1px solid #363637;
  color: #e5eaf3;
}

:deep(.el-card__header) {
  background-color: #262727;
  border-bottom: 1px solid #363637;
  color: #e5eaf3;
}

:deep(.el-tabs) {
  background-color: #1d1e1f;
  border: 1px solid #363637;
}

:deep(.el-tabs__item) {
  color: #cfd3dc;
}

:deep(.el-tabs__item.is-active) {
  color: #409EFF;
}

:deep(.el-tabs__active-bar) {
  background-color: #409EFF;
}

:deep(.el-tabs__nav-wrap::after) {
  background-color: #363637;
}

:deep(.el-table) {
  background-color: #1d1e1f;
}

:deep(.el-table__header) {
  background-color: #262727;
  color: #e5eaf3;
}

:deep(.el-table__body) {
  background-color: #1d1e1f;
  color: #cfd3dc;
}

:deep(.el-table__row) {
  background-color: #1d1e1f;
}

:deep(.el-table__row:hover) {
  background-color: #262727;
}

:deep(.el-tag) {
  background-color: #262727;
  border-color: #363637;
  color: #cfd3dc;
}
</style>