package fun.efto.luna.core.injector;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionLocation;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/01 12:00
 */
public class BytecodeInjectorRegistryTest {

    @BeforeAll
    static void setUp() {
        TestSetup.init();
    }

    @Test
    public void testGetInstance() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        assertNotNull(registry, "Registry instance should not be null");
    }

    @Test
    public void testGetEnterInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(MethodInjectionLocation.ENTER);
        assertTrue(injector.isPresent(), "ENTER injector should be present");
    }

    @Test
    public void testGetExitInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(MethodInjectionLocation.EXIT);
        assertTrue(injector.isPresent(), "EXIT injector should be present");
    }

    @Test
    public void testGetAroundInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(MethodInjectionLocation.AROUND);
        assertTrue(injector.isPresent(), "AROUND injector should be present");
    }

    @Test
    public void testGetBeforeLineInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(LineNumberInjectionLocation.BEFORE);
        assertTrue(injector.isPresent(), "BEFORE line injector should be present");
    }

    @Test
    public void testGetAfterLineInjector() {
        BytecodeInjectorRegistry registry = BytecodeInjectorRegistry.getInstance();
        Optional<BytecodeInjector> injector = registry.get(LineNumberInjectionLocation.AFTER);
        assertTrue(injector.isPresent(), "AFTER line injector should be present");
    }
}
