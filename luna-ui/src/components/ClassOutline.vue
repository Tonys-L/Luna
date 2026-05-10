<template>
  <div class="outline-sidebar">
    <!-- 方法列表 -->
    <div class="sidebar-section">
      <div class="section-header">
        <i class="fas fa-code"></i>
        <span>METHODS</span>
        <span class="badge">{{ methods.length }}</span>
      </div>
      <div class="outline-list">
        <div 
          v-for="(method, index) in methods" 
          :key="index"
          class="outline-item method"
          @click="$emit('scroll-to', method)"
        >
          <div class="item-main">
            <span class="access-indicator" :class="getAccessClass(method.readableAccessFlags)"></span>
            <span class="item-name" :title="method.name">
              {{ formatMethodName(method.name) }}
            </span>
            <span class="item-params" v-if="method.descriptor">
              ({{ formatMethodParams(method.descriptor) }})
            </span>
          </div>
          <button class="quick-inject-btn" @click.stop="$emit('inject', method)" title="Quick Inject">
            <i class="fas fa-plus"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- 字段列表 -->
    <div class="sidebar-section">
      <div class="section-header">
        <i class="fas fa-table"></i>
        <span>FIELDS</span>
        <span class="badge">{{ fields.length }}</span>
      </div>
      <div class="outline-list">
        <div 
          v-for="(field, index) in fields" 
          :key="index"
          class="outline-item field"
        >
          <span class="access-indicator" :class="getAccessClass(field.readableAccessFlags)"></span>
          <span class="item-name" :title="field.name">{{ field.name }}</span>
          <span class="item-type">{{ getSimpleType(field.descriptor) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ClassOutline',
  props: {
    classInfo: { type: Object, required: true }
  },
  emits: ['scroll-to', 'inject'],
  computed: {
    methods() {
      return this.classInfo.convertedMethods || this.classInfo.methods || []
    },
    fields() {
      return this.classInfo.convertedFields || this.classInfo.fields || []
    }
  },
  methods: {
    getAccessClass(flags) {
      if (!flags) return 'public'
      const f = flags.toLowerCase()
      if (f.includes('private')) return 'private'
      if (f.includes('protected')) return 'protected'
      return 'public'
    },
    formatDescriptor(desc) {
      if (!desc) return ''
      const typeMap = {
        'Z': 'boolean', 'B': 'byte', 'C': 'char', 'S': 'short',
        'I': 'int', 'J': 'long', 'F': 'float', 'D': 'double',
        'V': 'void'
      }
      if (desc.startsWith('L') && desc.endsWith(';')) {
        return desc.substring(1, desc.length - 1).split('/').pop().split('.').pop()
      }
      if (desc.startsWith('[')) {
        return this.formatDescriptor(desc.substring(1)) + '[]'
      }
      return typeMap[desc] || desc
    },
    getSimpleType(desc) {
      return this.formatDescriptor(desc)
    },
    formatMethodName(name) {
      if (name === '<init>') {
        const full = this.classInfo.className || ''
        return full.split('.').pop()
      }
      return name
    },
    formatMethodParams(desc) {
      if (!desc || !desc.startsWith('(')) return ''
      
      const paramsPart = desc.substring(1, desc.indexOf(')'))
      if (!paramsPart) return ''
      
      const params = []
      let i = 0
      while (i < paramsPart.length) {
        let arrayDim = ''
        while (paramsPart[i] === '[') {
          arrayDim += '[]'
          i++
        }
        
        const char = paramsPart[i]
        if (char === 'L') {
          const end = paramsPart.indexOf(';', i)
          const fullClass = paramsPart.substring(i + 1, end)
          params.push(fullClass.split('/').pop().split('.').pop() + arrayDim)
          i = end + 1
        } else {
          const typeMap = {
            'Z': 'boolean', 'B': 'byte', 'C': 'char', 'S': 'short',
            'I': 'int', 'J': 'long', 'F': 'float', 'D': 'double'
          }
          params.push((typeMap[char] || char) + arrayDim)
          i++
        }
      }
      return params.join(', ')
    }
  }
}
</script>

<style scoped>
.outline-sidebar {
  width: 280px;
  background-color: #1e1e1e;
  border-left: 1px solid #333;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sidebar-section {
  padding: 16px;
  border-bottom: 1px solid #333;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 11px;
  font-weight: 800;
  color: #888;
  letter-spacing: 1px;
}

.section-header i {
  font-size: 10px;
}

.badge {
  background: #333;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 9px;
  margin-left: auto;
}

.outline-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.outline-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  gap: 10px;
}

.outline-item:hover {
  background-color: #2a2d2e;
}

.item-main {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  overflow: hidden;
}

.access-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.access-indicator.public { background-color: #10b981; }
.access-indicator.private { background-color: #ef4444; }
.access-indicator.protected { background-color: #f59e0b; }

.item-name {
  font-size: 12px;
  color: #ccc;
  white-space: nowrap;
}

.item-params {
  font-size: 11px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.item-type {
  font-size: 10px;
  color: #666;
  font-family: var(--font-mono);
  margin-left: auto;
}

.quick-inject-btn {
  background: none;
  border: none;
  color: #6366f1;
  cursor: pointer;
  opacity: 0;
  padding: 4px;
  transition: opacity 0.2s;
}

.outline-item:hover .quick-inject-btn {
  opacity: 1;
}

.quick-inject-btn:hover {
  transform: scale(1.2);
}
</style>
