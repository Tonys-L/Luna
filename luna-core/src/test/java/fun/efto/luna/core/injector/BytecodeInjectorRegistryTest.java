/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/01 12:00
 */
package fun.efto.luna.core.injector;

import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BytecodeInjectorRegistryTest {

    @Test
    public void testGetInstance() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        assertNotNull(registry, "Registry instance should not be null");
    }

    @Test
    public void testGetEnterInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(MethodInjectionType.ENTER);
        assertTrue(injector.isPresent(), "ENTER injector should be present");
    }

    @Test
    public void testGetExitInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(MethodInjectionType.EXIT);
        assertTrue(injector.isPresent(), "EXIT injector should be present");
    }

    @Test
    public void testGetAroundInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(MethodInjectionType.AROUND);
        assertTrue(injector.isPresent(), "AROUND injector should be present");
    }

    @Test
    public void testGetBeforeLineInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(LineNumberInjectionType.BEFORE);
        assertTrue(injector.isPresent(), "BEFORE line injector should be present");
    }

    @Test
    public void testGetAfterLineInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(LineNumberInjectionType.AFTER);
        assertTrue(injector.isPresent(), "AFTER line injector should be present");
    }
}
