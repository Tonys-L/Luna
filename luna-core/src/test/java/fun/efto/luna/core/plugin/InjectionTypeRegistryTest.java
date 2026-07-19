package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class InjectionTypeRegistryTest {

    @AfterEach
    void tearDown() {
        InjectionTypeRegistry.getInstance().clear();
    }

    @Test
    void testRegisterAndResolve() {
        InjectionLocation location = InjectionLocation.of("method_enter", "test", "method");

        InjectionTypeRegistry.getInstance().register(location);
        assertSame(location, InjectionTypeRegistry.getInstance().resolve("method_enter"));
    }

    @Test
    void testResolveUnknown() {
        assertThrows(IllegalArgumentException.class, () -> InjectionTypeRegistry.getInstance().resolve("unknown"));
    }

    @Test
    void testAliasSupport() {
        InjectionLocation location = InjectionLocation.of("method_enter", "test", "method", "enter", "ENTER");

        InjectionTypeRegistry.getInstance().register(location);
        assertSame(location, InjectionTypeRegistry.getInstance().resolve("enter"));
        assertSame(location, InjectionTypeRegistry.getInstance().resolve("ENTER"));
    }

    @Test
    void testUnregisterAll() {
        InjectionLocation location = InjectionLocation.of("method_enter", "test", "method");

        InjectionTypeRegistry.getInstance().register(location);
        InjectionTypeRegistry.getInstance().unregisterAll(Collections.singletonList(location));
        assertThrows(IllegalArgumentException.class, () -> InjectionTypeRegistry.getInstance().resolve("method_enter"));
    }
}
