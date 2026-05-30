package fun.efto.luna.core.asm.injector;

import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.builtin.method.AbstractMethodInjector;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/01 12:00
 */
public class MethodTargetMatchTest {

    private AbstractMethodInjector injector;

    @BeforeEach
    public void setUp() {
        injector = new AbstractMethodInjector() {
            @Override
            protected MethodVisitor createMethodVisitor(AsmInjectionContext context, BytecodeAssembler bytecodeAssembler) {
                return context.getMethodVisitor();
            }
        };
    }

    private boolean invokeShouldInjectIntoMethod(String name, String descriptor, AsmInjectionContext context) throws Exception {
        Method method = AbstractMethodInjector.class.getDeclaredMethod("shouldInjectIntoMethod", String.class, String.class, AsmInjectionContext.class);
        method.setAccessible(true);
        return (boolean) method.invoke(injector, name, descriptor, context);
    }

    private AsmInjectionContext createContext(String className, String methodName, String methodDescriptor) {
        MethodTarget target = new MethodTarget(MethodInjectionType.ENTER, className, methodName, methodDescriptor);
        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "log:test";
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };
        InjectionPoint injectionPoint = new InjectionPoint(target, code);
        InjectionContext injectionContext = new InjectionContext(injectionPoint);
        return new AsmInjectionContext(injectionContext, new byte[0]);
    }

    @Test
    public void testMatchByNameOnly() throws Exception {
        AsmInjectionContext context = createContext("com.example.Test", "testMethod", null);
        assertTrue(invokeShouldInjectIntoMethod("testMethod", "()V", context),
                "Should match when methodDescriptor is null and name matches");
    }

    @Test
    public void testMatchByNameAndDescriptor() throws Exception {
        AsmInjectionContext context = createContext("com.example.Test", "testMethod", "()V");
        assertTrue(invokeShouldInjectIntoMethod("testMethod", "()V", context),
                "Should match when both name and descriptor match");
    }

    @Test
    public void testNoMatchByName() throws Exception {
        AsmInjectionContext context = createContext("com.example.Test", "testMethod", null);
        assertFalse(invokeShouldInjectIntoMethod("otherMethod", "()V", context),
                "Should not match when name differs");
    }

    @Test
    public void testNoMatchByDescriptor() throws Exception {
        AsmInjectionContext context = createContext("com.example.Test", "testMethod", "()V");
        assertFalse(invokeShouldInjectIntoMethod("testMethod", "(I)V", context),
                "Should not match when descriptor differs");
    }

    @Test
    public void testMatchWithEmptyDescriptor() throws Exception {
        AsmInjectionContext context = createContext("com.example.Test", "testMethod", "");
        assertTrue(invokeShouldInjectIntoMethod("testMethod", "()V", context),
                "Should match when methodDescriptor is empty and name matches");
    }

    @Test
    public void testMatchOverloadedMethod() throws Exception {
        AsmInjectionContext context = createContext("com.example.Test", "testMethod", "(I)V");
        assertFalse(invokeShouldInjectIntoMethod("testMethod", "()V", context),
                "Should not match overloaded method with different descriptor");
        assertTrue(invokeShouldInjectIntoMethod("testMethod", "(I)V", context),
                "Should match overloaded method with same descriptor");
    }
}
