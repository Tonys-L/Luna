/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:00
 */
package fun.efto.luna.core.bytecode.bytekit;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;
import fun.efto.luna.core.plugin.builtin.method.ExceptionExitInjectionLocation;
import fun.efto.luna.core.plugin.builtin.method.InvokeInjectionLocation;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NewInjectionTypeTest {

    @BeforeEach
    void setUp() {
        BytecodeInjectorRegistry.getInstance().getRegistry().clear();
        InjectionTypeRegistry.getInstance().clear();
        CoreCapabilityRegistry.getInstance().clear();
        CoreModuleInitializer.initialize();
    }

    @Test
    @DisplayName("resolve('exception_exit') 返回 ExceptionExitInjectionLocation")
    void testResolveExceptionExit() {
        InjectionLocation location = InjectionTypeRegistry.getInstance().resolve("exception_exit");
        assertSame(ExceptionExitInjectionLocation.EXCEPTION_EXIT, location);
    }

    @Test
    @DisplayName("resolve('invoke') 返回 InvokeInjectionLocation")
    void testResolveInvoke() {
        InjectionLocation location = InjectionTypeRegistry.getInstance().resolve("invoke");
        assertSame(InvokeInjectionLocation.INVOKE, location);
    }

    @Test
    @DisplayName("ExceptionExitInjectionLocation.createTarget() 返回 MethodTarget")
    void testExceptionExitCreateTarget() {
        InjectionTarget target = ExceptionExitInjectionLocation.EXCEPTION_EXIT.createTarget(
                "com.example.Service", "process", "(I)V", null);
        assertInstanceOf(MethodTarget.class, target);
        assertEquals("com.example.Service", target.getTargetClass());
        assertEquals("process", ((MethodTarget) target).getMethodName());
        assertEquals("(I)V", ((MethodTarget) target).getMethodDescriptor());
    }

    @Test
    @DisplayName("InvokeInjectionLocation.createTarget() 返回 MethodTarget")
    void testInvokeCreateTarget() {
        InjectionTarget target = InvokeInjectionLocation.INVOKE.createTarget(
                "com.example.Service", "execute", "()V", null);
        assertInstanceOf(MethodTarget.class, target);
        assertEquals("com.example.Service", target.getTargetClass());
        assertEquals("execute", ((MethodTarget) target).getMethodName());
        assertEquals("()V", ((MethodTarget) target).getMethodDescriptor());
    }
}
