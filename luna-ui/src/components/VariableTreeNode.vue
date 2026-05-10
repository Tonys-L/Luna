<template>
  <div class="variable-node" :style="{ paddingLeft: depth * 16 + 'px' }">
    <div 
      class="node-content" 
      :class="{ 'is-expandable': isExpandable }"
      @click="toggleExpand"
    >
      <span class="toggle-icon" v-if="isExpandable">
        <i :class="expanded ? 'fas fa-chevron-down' : 'fas fa-chevron-right'"></i>
      </span>
      <span class="toggle-spacer" v-else></span>
      
      <span class="node-name">{{ name }}</span>
      <span class="node-separator">:</span>
      
      <template v-if="!expanded || !isExpandable">
        <span class="node-value" :class="valueClass">{{ formatValue(value) }}</span>
      </template>
    </div>
    
    <div v-if="expanded && isExpandable" class="node-children">
      <!-- 数组或集合 -->
      <template v-if="Array.isArray(value)">
        <VariableTreeNode 
          v-for="(item, index) in value" 
          :key="index"
          :name="'[' + index + ']'"
          :value="item"
          :depth="depth + 1"
        />
      </template>
      <!-- 对象 -->
      <template v-else-if="typeof value === 'object' && value !== null">
        <VariableTreeNode 
          v-for="(val, key) in value" 
          :key="key"
          :name="key"
          :value="val"
          :depth="depth + 1"
        />
      </template>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VariableTreeNode',
  props: {
    name: String,
    value: [String, Number, Boolean, Object, Array],
    depth: Number
  },
  data() {
    return {
      expanded: false
    }
  },
  computed: {
    isExpandable() {
      if (this.value === null) return false
      if (typeof this.value === 'string' && this.value.startsWith('[MAX_DEPTH]')) return false
      if (typeof this.value === 'string' && this.value === '[CIRCULAR_REF]') return false
      return typeof this.value === 'object'
    },
    valueClass() {
      if (this.value === null) return 'val-null'
      const type = typeof this.value
      if (type === 'string') return 'val-string'
      if (type === 'number') return 'val-number'
      if (type === 'boolean') return 'val-boolean'
      return 'val-object'
    }
  },
  methods: {
    toggleExpand() {
      if (this.isExpandable) {
        this.expanded = !this.expanded
      }
    },
    formatValue(val) {
      if (val === null) return 'null'
      if (typeof val === 'string') {
        if (val.startsWith('[MAX_DEPTH]') || val === '[CIRCULAR_REF]' || val.startsWith('[ERROR_SERIALIZING')) {
          return val
        }
        return `"${val}"`
      }
      if (typeof val === 'object') {
        if (Array.isArray(val)) return `Array(${val.length})`
        return '{...}'
      }
      return String(val)
    }
  }
}
</script>

<style scoped>
.variable-node {
  user-select: none;
}

.node-content {
  display: flex;
  align-items: center;
  padding: 2px 4px;
  border-radius: 3px;
  cursor: default;
}

.node-content.is-expandable {
  cursor: pointer;
}

.node-content:hover {
  background-color: #2a2d2e;
}

.toggle-icon {
  width: 16px;
  display: flex;
  justify-content: center;
  color: #888;
  font-size: 10px;
}

.toggle-spacer {
  width: 16px;
}

.node-name {
  color: #9cdcfe;
  margin-right: 4px;
}

.node-separator {
  color: #cccccc;
  margin-right: 8px;
}

.node-value {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.val-string { color: #ce9178; }
.val-number { color: #b5cea8; }
.val-boolean { color: #569cd6; }
.val-null { color: #569cd6; font-style: italic; }
.val-object { color: #888; }

.node-children {
  margin-top: 1px;
}
</style>
