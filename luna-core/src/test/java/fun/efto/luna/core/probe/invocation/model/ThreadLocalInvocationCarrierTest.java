package fun.efto.luna.core.probe.invocation.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 22:00
 */
public class ThreadLocalInvocationCarrierTest {

    private ThreadLocalInvocationCarrier carrier;

    @BeforeEach
    void setUp() {
        carrier = new ThreadLocalInvocationCarrier();
        // Ensure clean ThreadLocal state
        carrier.clear();
    }

    @AfterEach
    void tearDown() {
        carrier.clear();
    }

    @Test
    void testPushAndPop() {
        ActiveInvocation invocation = createInvocation("trace1", "span1", null);
        carrier.push(invocation);

        ActiveInvocation popped = carrier.pop();
        assertSame(invocation, popped, "Popped invocation should be the same as pushed");
    }

    @Test
    void testCurrent() {
        ActiveInvocation invocation = createInvocation("trace1", "span1", null);
        carrier.push(invocation);

        ActiveInvocation current = carrier.current();
        assertSame(invocation, current, "current() should return the top of stack");
    }

    @Test
    void testPopWhenEmpty() {
        ActiveInvocation popped = carrier.pop();
        assertNull(popped, "Pop from empty stack should return null");
    }

    @Test
    void testCurrentWhenEmpty() {
        ActiveInvocation current = carrier.current();
        assertNull(current, "Current from empty stack should return null");
    }

    @Test
    void testNestedPushPop() {
        ActiveInvocation first = createInvocation("trace1", "span1", null);
        ActiveInvocation second = createInvocation("trace1", "span2", "span1");

        carrier.push(first);
        carrier.push(second);

        // LIFO order: second should be popped first
        ActiveInvocation popped2 = carrier.pop();
        assertSame(second, popped2, "First pop should return second (LIFO)");

        ActiveInvocation popped1 = carrier.pop();
        assertSame(first, popped1, "Second pop should return first (LIFO)");
    }

    @Test
    void testMaxStackDepth() {
        // MAX_STACK_DEPTH is 20; push 20 items, then push one more
        for (int i = 0; i < 20; i++) {
            carrier.push(createInvocation("trace1", "span" + i, "parent"));
        }

        assertEquals(20, carrier.depth(), "Stack should be at max depth");

        // Pushing beyond max should be silently rejected
        carrier.push(createInvocation("trace1", "span20", "parent"));
        assertEquals(20, carrier.depth(), "Stack should remain at max depth after overflow push");

        // Verify the stack still works correctly - top item should still be the 20th
        ActiveInvocation top = carrier.current();
        assertEquals("span19", top.getSpanId(), "Top of stack should be the 20th item (span19)");
    }

    @Test
    void testClear() {
        carrier.push(createInvocation("trace1", "span1", null));
        carrier.push(createInvocation("trace1", "span2", "span1"));

        assertFalse(carrier.isEmpty(), "Stack should not be empty before clear");

        carrier.clear();

        assertTrue(carrier.isEmpty(), "Stack should be empty after clear");
        assertNull(carrier.current(), "current() should return null after clear");
        assertEquals(0, carrier.depth(), "depth() should return 0 after clear");
    }

    @Test
    void testIsEmpty() {
        assertTrue(carrier.isEmpty(), "New carrier should be empty");

        carrier.push(createInvocation("trace1", "span1", null));
        assertFalse(carrier.isEmpty(), "Carrier with pushed item should not be empty");

        carrier.pop();
        assertTrue(carrier.isEmpty(), "Carrier after popping all items should be empty");
    }

    @Test
    void testDepth() {
        assertEquals(0, carrier.depth(), "Initial depth should be 0");

        carrier.push(createInvocation("trace1", "span1", null));
        assertEquals(1, carrier.depth(), "Depth after one push should be 1");

        carrier.push(createInvocation("trace1", "span2", "span1"));
        assertEquals(2, carrier.depth(), "Depth after two pushes should be 2");

        carrier.pop();
        assertEquals(1, carrier.depth(), "Depth after one pop should be 1");
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        carrier.push(createInvocation("trace1", "mainSpan", null));

        Thread t = new Thread(() -> {
            // Different thread should have its own empty stack
            assertNull(carrier.current(), "Other thread should see empty stack");

            ActiveInvocation otherInvocation = createInvocation("trace2", "otherSpan", null);
            carrier.push(otherInvocation);
            assertSame(otherInvocation, carrier.current(), "Other thread should see its own pushed item");
            carrier.clear();
        });

        t.start();
        t.join();

        // Main thread stack should be unaffected
        assertEquals("mainSpan", carrier.current().getSpanId(), "Main thread stack should be unaffected");
    }

    private ActiveInvocation createInvocation(String traceId, String spanId, String parentSpanId) {
        return new ActiveInvocation(traceId, spanId, parentSpanId,
            "com.example.TestClass", "testMethod",
            System.nanoTime(), "null");
    }
}
