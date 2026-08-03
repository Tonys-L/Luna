import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import TraceTreeViewer from '../TraceTreeViewer.vue'
import TraceTreeNode from '../TraceTreeNode.vue'

const sampleTrace = {
  traceId: 'test-trace-001',
  rootClassName: 'com.example.UserService',
  rootMethodName: 'getUser',
  totalDurationMs: 45,
  startTimestamp: Date.now(),
  spans: [
    {
      spanId: 'span-1',
      parentSpanId: null,
      className: 'com.example.UserService',
      methodName: 'getUser',
      durationMs: 45,
      args: '["userId"]',
      returnValue: '"User{id=1}"',
      threwException: false,
      exceptionMessage: null
    },
    {
      spanId: 'span-2',
      parentSpanId: 'span-1',
      className: 'com.example.UserDao',
      methodName: 'findById',
      durationMs: 12,
      args: '["id"]',
      returnValue: '"UserEntity{...}"',
      threwException: false,
      exceptionMessage: null
    },
    {
      spanId: 'span-3',
      parentSpanId: 'span-1',
      className: 'com.example.RoleService',
      methodName: 'getRoles',
      durationMs: 25,
      args: '["userId"]',
      returnValue: '"[ADMIN, USER]"',
      threwException: false,
      exceptionMessage: null
    }
  ]
}

describe('TraceTreeViewer', () => {
  describe('buildTree', () => {
    it('should build a correct tree from flat spans with parentSpanId relationships', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      const rootNode = wrapper.vm.rootNode

      // Root node should be span-1 (UserService.getUser)
      expect(rootNode.spanId).toBe('span-1')
      expect(rootNode.methodName).toBe('getUser')
      expect(rootNode.className).toBe('com.example.UserService')

      // Root should have 2 children
      expect(rootNode.children).toHaveLength(2)

      // Children should be span-2 and span-3
      expect(rootNode.children[0].spanId).toBe('span-2')
      expect(rootNode.children[0].methodName).toBe('findById')
      expect(rootNode.children[0].className).toBe('com.example.UserDao')

      expect(rootNode.children[1].spanId).toBe('span-3')
      expect(rootNode.children[1].methodName).toBe('getRoles')
      expect(rootNode.children[1].className).toBe('com.example.RoleService')

      // Children should have no further children
      expect(rootNode.children[0].children).toHaveLength(0)
      expect(rootNode.children[1].children).toHaveLength(0)
    })

    it('should handle a single root span returning it directly', () => {
      const singleSpanTrace = {
        traceId: 'single-001',
        rootClassName: 'com.example.Service',
        rootMethodName: 'ping',
        totalDurationMs: 5,
        spans: [
          {
            spanId: 'span-a',
            parentSpanId: null,
            className: 'com.example.Service',
            methodName: 'ping',
            durationMs: 5,
            args: null,
            returnValue: '"pong"',
            threwException: false,
            exceptionMessage: null
          }
        ]
      }

      const wrapper = mount(TraceTreeViewer, {
        props: { trace: singleSpanTrace },
        global: { components: { TraceTreeNode } }
      })

      const rootNode = wrapper.vm.rootNode
      expect(rootNode.spanId).toBe('span-a')
      expect(rootNode.methodName).toBe('ping')
      expect(rootNode.children).toHaveLength(0)
    })

    it('should handle multiple root spans (no parent) by wrapping in a virtual root', () => {
      const multiRootTrace = {
        traceId: 'multi-root-001',
        rootClassName: 'com.example.App',
        rootMethodName: 'run',
        totalDurationMs: 100,
        spans: [
          {
            spanId: 'span-x',
            parentSpanId: null,
            className: 'com.example.App',
            methodName: 'init',
            durationMs: 40,
            args: null,
            returnValue: null,
            threwException: false,
            exceptionMessage: null
          },
          {
            spanId: 'span-y',
            parentSpanId: null,
            className: 'com.example.App',
            methodName: 'run',
            durationMs: 60,
            args: null,
            returnValue: null,
            threwException: false,
            exceptionMessage: null
          }
        ]
      }

      const wrapper = mount(TraceTreeViewer, {
        props: { trace: multiRootTrace },
        global: { components: { TraceTreeNode } }
      })

      const rootNode = wrapper.vm.rootNode
      // Multiple roots get wrapped in a virtual root node with children
      expect(rootNode.children).toHaveLength(2)
      expect(rootNode.children[0].spanId).toBe('span-x')
      expect(rootNode.children[1].spanId).toBe('span-y')
    })

    it('should handle empty spans gracefully', () => {
      const emptyTrace = {
        traceId: 'empty-001',
        rootClassName: 'com.example.Service',
        rootMethodName: 'noop',
        totalDurationMs: 0,
        spans: []
      }

      const wrapper = mount(TraceTreeViewer, {
        props: { trace: emptyTrace },
        global: { components: { TraceTreeNode } }
      })

      const rootNode = wrapper.vm.rootNode
      // Empty spans produce a root with no children
      expect(rootNode.children).toHaveLength(0)
    })
  })

  describe('durationClass - color coding', () => {
    it('should return dur-fast (green) for duration < 50ms', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      expect(wrapper.vm.durationClass(0)).toBe('dur-fast')
      expect(wrapper.vm.durationClass(1)).toBe('dur-fast')
      expect(wrapper.vm.durationClass(49)).toBe('dur-fast')
    })

    it('should return dur-normal (yellow) for duration 50-200ms', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      expect(wrapper.vm.durationClass(50)).toBe('dur-normal')
      expect(wrapper.vm.durationClass(100)).toBe('dur-normal')
      expect(wrapper.vm.durationClass(200)).toBe('dur-normal')
    })

    it('should return dur-slow (red) for duration > 200ms', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      expect(wrapper.vm.durationClass(201)).toBe('dur-slow')
      expect(wrapper.vm.durationClass(500)).toBe('dur-slow')
      expect(wrapper.vm.durationClass(10000)).toBe('dur-slow')
    })

    it('should return empty string for null/undefined duration', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      expect(wrapper.vm.durationClass(null)).toBe('')
      expect(wrapper.vm.durationClass(undefined)).toBe('')
    })

    it('should apply dur-fast class to trace header for totalDurationMs < 50', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace }, // totalDurationMs = 45
        global: { components: { TraceTreeNode } }
      })

      const durationEl = wrapper.find('.trace-duration')
      expect(durationEl.classes()).toContain('dur-fast')
    })

    it('should apply dur-normal class to trace header for totalDurationMs 50-200', () => {
      const normalTrace = { ...sampleTrace, totalDurationMs: 120 }
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: normalTrace },
        global: { components: { TraceTreeNode } }
      })

      const durationEl = wrapper.find('.trace-duration')
      expect(durationEl.classes()).toContain('dur-normal')
    })

    it('should apply dur-slow class to trace header for totalDurationMs > 200', () => {
      const slowTrace = { ...sampleTrace, totalDurationMs: 350 }
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: slowTrace },
        global: { components: { TraceTreeNode } }
      })

      const durationEl = wrapper.find('.trace-duration')
      expect(durationEl.classes()).toContain('dur-slow')
    })
  })

  describe('expandCollapse', () => {
    it('should toggle trace tree visibility when header is clicked', async () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      // Initially expanded (data.expanded defaults to true)
      expect(wrapper.vm.expanded).toBe(true)
      expect(wrapper.find('.trace-tree').exists()).toBe(true)

      // Click header to collapse
      await wrapper.find('.trace-header').trigger('click')
      expect(wrapper.vm.expanded).toBe(false)
      expect(wrapper.find('.trace-tree').exists()).toBe(false)

      // Click header again to expand
      await wrapper.find('.trace-header').trigger('click')
      expect(wrapper.vm.expanded).toBe(true)
      expect(wrapper.find('.trace-tree').exists()).toBe(true)
    })

    it('should render TraceTreeNode components when expanded', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      // When expanded, should render tree nodes
      const nodes = wrapper.findAllComponents(TraceTreeNode)
      expect(nodes.length).toBeGreaterThan(0)
    })

    it('should not render TraceTreeNode components when collapsed', async () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      // Collapse the tree
      await wrapper.find('.trace-header').trigger('click')

      // No TraceTreeNode components should be rendered
      const nodes = wrapper.findAllComponents(TraceTreeNode)
      expect(nodes).toHaveLength(0)
    })

    it('should toggle node detail panel when node header is clicked', async () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      // Find the first TraceTreeNode
      const node = wrapper.findComponent(TraceTreeNode)
      expect(node.exists()).toBe(true)

      // Detail panel should be hidden initially
      expect(node.vm.detailExpanded).toBe(false)
      expect(node.find('.node-detail').exists()).toBe(false)

      // Click node header to show detail
      await node.find('.node-header').trigger('click')
      expect(node.vm.detailExpanded).toBe(true)
      expect(node.find('.node-detail').exists()).toBe(true)

      // Click again to hide detail
      await node.find('.node-header').trigger('click')
      expect(node.vm.detailExpanded).toBe(false)
      expect(node.find('.node-detail').exists()).toBe(false)
    })
  })

  describe('computed properties', () => {
    it('should compute shortId by truncating traceId to 8 characters', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      // 'test-trace-001' is 14 chars, should be truncated to 8
      expect(wrapper.vm.shortId).toBe('test-tra')
    })

    it('should keep shortId as-is when traceId <= 8 characters', () => {
      const shortIdTrace = { ...sampleTrace, traceId: 'abc' }
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: shortIdTrace },
        global: { components: { TraceTreeNode } }
      })

      expect(wrapper.vm.shortId).toBe('abc')
    })

    it('should compute rootLabel from rootClassName and rootMethodName', () => {
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: sampleTrace },
        global: { components: { TraceTreeNode } }
      })

      // getSimpleName('com.example.UserService') = 'UserService'
      expect(wrapper.vm.rootLabel).toBe('UserService.getUser')
    })

    it('should display "Unknown" when rootClassName is missing', () => {
      const noClassTrace = { ...sampleTrace, rootClassName: null, rootMethodName: null }
      const wrapper = mount(TraceTreeViewer, {
        props: { trace: noClassTrace },
        global: { components: { TraceTreeNode } }
      })

      expect(wrapper.vm.rootLabel).toBe('Unknown')
    })
  })
})
