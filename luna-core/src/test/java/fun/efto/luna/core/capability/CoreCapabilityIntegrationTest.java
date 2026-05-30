package fun.efto.luna.core.capability;

import fun.efto.luna.core.init.DefaultInitializer;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/30 00:00
 */
public class CoreCapabilityIntegrationTest {

    private CoreCapabilityRegistry registry;

    @BeforeEach
    void setUp() {
        registry = CoreCapabilityRegistry.getInstance();
        registry.clear();
    }

    @AfterEach
    void tearDown() {
        registry.clear();
    }

    @Test
    void testCoreModuleInitializerRegistersCapabilityRecords() {
        CoreModuleInitializer.initialize();

        CoreCapabilityRecord methodTarget = registry.get("method-target");
        assertNotNull(methodTarget);
        assertEquals("方法注入目标", methodTarget.getDisplayName());

        CoreCapabilityRecord lineTarget = registry.get("line-target");
        assertNotNull(lineTarget);
        assertEquals("行号注入目标", lineTarget.getDisplayName());
    }

    @Test
    void testAllSixCoreCapabilitiesRegistered() {
        new DefaultInitializer().initialize();

        List<CoreCapabilityRecord> all = registry.getAll();
        assertEquals(6, all.size());
    }

    @Test
    void testMethodTargetProvidedEntries() {
        CoreModuleInitializer.initialize();

        List<String> entries = registry.getProvidedEntries("method-target");
        assertEquals(5, entries.size());
        assertTrue(entries.contains("method_enter"));
        assertTrue(entries.contains("method_exit"));
        assertTrue(entries.contains("method_around"));
        assertTrue(entries.contains("exception_exit"));
        assertTrue(entries.contains("invoke"));
    }

    @Test
    void testAllCoreCapabilitiesAreCoreOnly() {
        new DefaultInitializer().initialize();

        for (CoreCapabilityRecord record : registry.getAll()) {
            assertEquals(LifecyclePolicy.CORE_ONLY, record.getLifecyclePolicy(),
                    "Capability " + record.getCapabilityId() + " should be CORE_ONLY");
        }
    }
}
