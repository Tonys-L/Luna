package fun.efto.luna.core.bootstrap.capability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/29 00:00
 */
public class CoreCapabilityRegistryTest {

    private final CoreCapabilityRegistry registry = CoreCapabilityRegistry.getInstance();

    @AfterEach
    void tearDown() {
        registry.clear();
    }

    @Test
    void testRegisterAndGet() {
        CoreCapabilityRecord record = new CoreCapabilityRecord(
                "method-target", "方法注入目标", CapabilityKind.KERNEL,
                Arrays.asList("method_enter", "method_exit", "method_around"),
                ReadinessState.NOT_INITIALIZED, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        );
        registry.register(record);

        CoreCapabilityRecord fetched = registry.get("method-target");
        assertNotNull(fetched);
        assertEquals("method-target", fetched.getCapabilityId());
        assertEquals("方法注入目标", fetched.getDisplayName());
    }

    @Test
    void testGetAllReturnsAllRecords() {
        CoreCapabilityRecord r1 = new CoreCapabilityRecord(
                "method-target", "方法注入目标", CapabilityKind.KERNEL,
                Arrays.asList("method_enter", "method_exit", "method_around"),
                ReadinessState.NOT_INITIALIZED, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        );
        CoreCapabilityRecord r2 = new CoreCapabilityRecord(
                "line-target", "行号注入目标", CapabilityKind.KERNEL,
                Arrays.asList("line_before", "line_after"),
                ReadinessState.NOT_INITIALIZED, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        );
        registry.register(r1);
        registry.register(r2);

        List<CoreCapabilityRecord> all = registry.getAll();
        assertEquals(2, all.size());
    }

    @Test
    void testIsReady() {
        CoreCapabilityRecord record = new CoreCapabilityRecord(
                "method-target", "方法注入目标", CapabilityKind.KERNEL,
                Arrays.asList("method_enter", "method_exit", "method_around"),
                ReadinessState.NOT_INITIALIZED, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        );
        registry.register(record);

        assertFalse(registry.isReady("method-target"));

        record.markReady();
        assertTrue(registry.isReady("method-target"));
    }

    @Test
    void testGetProvidedEntries() {
        CoreCapabilityRecord record = new CoreCapabilityRecord(
                "method-target", "方法注入目标", CapabilityKind.KERNEL,
                Arrays.asList("method_enter", "method_exit", "method_around"),
                ReadinessState.NOT_INITIALIZED, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        );
        registry.register(record);

        List<String> entries = registry.getProvidedEntries("method-target");
        assertEquals(3, entries.size());
        assertTrue(entries.contains("method_enter"));
        assertTrue(entries.contains("method_exit"));
        assertTrue(entries.contains("method_around"));
    }

    @Test
    void testMethodTargetNotUnloadable() {
        CoreCapabilityRecord record = new CoreCapabilityRecord(
                "method-target", "方法注入目标", CapabilityKind.KERNEL,
                Arrays.asList("method_enter", "method_exit", "method_around"),
                ReadinessState.NOT_INITIALIZED, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        );
        registry.register(record);

        CoreCapabilityRecord fetched = registry.get("method-target");
        assertEquals(LifecyclePolicy.CORE_ONLY, fetched.getLifecyclePolicy());
    }

    @Test
    void testReadinessTransition() {
        CoreCapabilityRecord record = new CoreCapabilityRecord(
                "method-target", "方法注入目标", CapabilityKind.KERNEL,
                Arrays.asList("method_enter", "method_exit", "method_around"),
                ReadinessState.NOT_INITIALIZED, Collections.emptyList(), LifecyclePolicy.CORE_ONLY
        );

        assertEquals(ReadinessState.NOT_INITIALIZED, record.getReadinessState());

        record.markReady();
        assertEquals(ReadinessState.READY, record.getReadinessState());

        record.markDegraded();
        assertEquals(ReadinessState.DEGRADED, record.getReadinessState());

        record.markFailed();
        assertEquals(ReadinessState.FAILED, record.getReadinessState());
    }
}
