/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:00
 */
package fun.efto.luna.core.bytecode.bytekit;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;
import fun.efto.luna.core.plugin.builtin.method.ExceptionExitInjectionType;
import fun.efto.luna.core.plugin.builtin.method.InvokeInjectionType;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NewInjectionTypeTest {

    @BeforeEach
    void setUp() {
        BytecodeInjectorRegistry.getInstance().getRegistry().clear();
        InjectionTypeRegistry.getInstance().clear();
        RuleConverterRegistry.getInstance().clear();
        CoreCapabilityRegistry.getInstance().clear();
        CoreModuleInitializer.initialize();
    }

    @Test
    @DisplayName("resolve('exception_exit') 返回 ExceptionExitInjectionType")
    void testResolveExceptionExit() {
        InjectionType type = InjectionTypeRegistry.getInstance().resolve("exception_exit");
        assertSame(ExceptionExitInjectionType.EXCEPTION_EXIT, type);
    }

    @Test
    @DisplayName("resolve('invoke') 返回 InvokeInjectionType")
    void testResolveInvoke() {
        InjectionType type = InjectionTypeRegistry.getInstance().resolve("invoke");
        assertSame(InvokeInjectionType.INVOKE, type);
    }

    @Test
    @DisplayName("ExceptionExitInjectionType.createTarget() 返回 MethodTarget")
    void testExceptionExitCreateTarget() {
        InjectionTarget target = ExceptionExitInjectionType.EXCEPTION_EXIT.createTarget(
                "com.example.Service", "process", "(I)V", null);
        assertInstanceOf(MethodTarget.class, target);
        assertEquals("com.example.Service", target.getTargetClass());
        assertEquals("process", ((MethodTarget) target).getMethodName());
        assertEquals("(I)V", ((MethodTarget) target).getMethodDescriptor());
    }

    @Test
    @DisplayName("InvokeInjectionType.createTarget() 返回 MethodTarget")
    void testInvokeCreateTarget() {
        InjectionTarget target = InvokeInjectionType.INVOKE.createTarget(
                "com.example.Service", "execute", "()V", null);
        assertInstanceOf(MethodTarget.class, target);
        assertEquals("com.example.Service", target.getTargetClass());
        assertEquals("execute", ((MethodTarget) target).getMethodName());
        assertEquals("()V", ((MethodTarget) target).getMethodDescriptor());
    }
}
