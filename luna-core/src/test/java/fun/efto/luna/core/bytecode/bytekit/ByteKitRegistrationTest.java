/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:00
 */
package fun.efto.luna.core.bytecode.bytekit;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitAroundInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitEnterInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitExceptionExitInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitExitInjector;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitInvokeInjector;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;
import fun.efto.luna.core.plugin.builtin.line.AfterLineInjector;
import fun.efto.luna.core.plugin.builtin.line.BeforeLineInjector;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import fun.efto.luna.core.plugin.builtin.method.ExceptionExitInjectionType;
import fun.efto.luna.core.plugin.builtin.method.InvokeInjectionType;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionType;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ByteKitRegistrationTest {

    @BeforeEach
    void setUp() {
        BytecodeInjectorRegistry.getInstance().getRegistry().clear();
        InjectionTypeRegistry.getInstance().clear();
        RuleConverterRegistry.getInstance().clear();
        CoreCapabilityRegistry.getInstance().clear();
    }

    @Test
    @DisplayName("ENTER 注入器应为 ByteKitEnterInjector 实例")
    void testEnterInjectorIsByteKit() {
        CoreModuleInitializer.initialize();
        Optional<BytecodeInjector> injector = BytecodeInjectorRegistry.getInstance().get(MethodInjectionType.ENTER);
        assertTrue(injector.isPresent());
        assertInstanceOf(ByteKitEnterInjector.class, injector.get());
    }

    @Test
    @DisplayName("EXIT 注入器应为 ByteKitExitInjector 实例")
    void testExitInjectorIsByteKit() {
        CoreModuleInitializer.initialize();
        Optional<BytecodeInjector> injector = BytecodeInjectorRegistry.getInstance().get(MethodInjectionType.EXIT);
        assertTrue(injector.isPresent());
        assertInstanceOf(ByteKitExitInjector.class, injector.get());
    }

    @Test
    @DisplayName("AROUND 注入器应为 ByteKitAroundInjector 实例")
    void testAroundInjectorIsByteKit() {
        CoreModuleInitializer.initialize();
        Optional<BytecodeInjector> injector = BytecodeInjectorRegistry.getInstance().get(MethodInjectionType.AROUND);
        assertTrue(injector.isPresent());
        assertInstanceOf(ByteKitAroundInjector.class, injector.get());
    }

    @Test
    @DisplayName("EXCEPTION_EXIT 注入器应为 ByteKitExceptionExitInjector 实例")
    void testExceptionExitInjectorIsByteKit() {
        CoreModuleInitializer.initialize();
        Optional<BytecodeInjector> injector = BytecodeInjectorRegistry.getInstance().get(ExceptionExitInjectionType.EXCEPTION_EXIT);
        assertTrue(injector.isPresent());
        assertInstanceOf(ByteKitExceptionExitInjector.class, injector.get());
    }

    @Test
    @DisplayName("INVOKE 注入器应为 ByteKitInvokeInjector 实例")
    void testInvokeInjectorIsByteKit() {
        CoreModuleInitializer.initialize();
        Optional<BytecodeInjector> injector = BytecodeInjectorRegistry.getInstance().get(InvokeInjectionType.INVOKE);
        assertTrue(injector.isPresent());
        assertInstanceOf(ByteKitInvokeInjector.class, injector.get());
    }

    @Test
    @DisplayName("BEFORE 行号注入器仍使用 ASM 实现")
    void testBeforeLineInjectorIsAsm() {
        CoreModuleInitializer.initialize();
        Optional<BytecodeInjector> injector = BytecodeInjectorRegistry.getInstance().get(LineNumberInjectionType.BEFORE);
        assertTrue(injector.isPresent());
        assertInstanceOf(BeforeLineInjector.class, injector.get());
    }

    @Test
    @DisplayName("AFTER 行号注入器仍使用 ASM 实现")
    void testAfterLineInjectorIsAsm() {
        CoreModuleInitializer.initialize();
        Optional<BytecodeInjector> injector = BytecodeInjectorRegistry.getInstance().get(LineNumberInjectionType.AFTER);
        assertTrue(injector.isPresent());
        assertInstanceOf(AfterLineInjector.class, injector.get());
    }
}
