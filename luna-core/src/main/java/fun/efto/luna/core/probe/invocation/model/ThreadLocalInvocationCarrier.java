package fun.efto.luna.core.probe.invocation.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/07/05 20:00
 */
public class ThreadLocalInvocationCarrier implements InvocationContextCarrier {

    private static final int MAX_STACK_DEPTH = 20;

    private static final ThreadLocal<Deque<ActiveInvocation>> STACK =
        ThreadLocal.withInitial(ArrayDeque::new);

    @Override
    public ActiveInvocation current() {
        return STACK.get().peek();
    }

    @Override
    public void push(ActiveInvocation invocation) {
        Deque<ActiveInvocation> stack = STACK.get();
        if (stack.size() < MAX_STACK_DEPTH) {
            stack.push(invocation);
        }
        // If max depth exceeded, silently skip push
        // Root EXIT will still work correctly
    }

    @Override
    public ActiveInvocation pop() {
        return STACK.get().poll();  // poll from front (ArrayDeque as stack: push adds at front, poll removes from front)
    }

    /**
     * Check if the stack is empty (used to detect root EXIT)
     */
    public boolean isEmpty() {
        return STACK.get().isEmpty();
    }

    /**
     * Get current stack depth
     */
    public int depth() {
        return STACK.get().size();
    }

    /**
     * Clean up the ThreadLocal for the current thread.
     * Should be called when root EXIT completes to prevent ThreadLocal leaks.
     */
    public void clear() {
        STACK.remove();
    }
}
