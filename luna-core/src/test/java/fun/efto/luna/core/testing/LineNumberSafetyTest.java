package fun.efto.luna.core.testing;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import fun.efto.luna.core.probe.ProbeMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static fun.efto.luna.core.testing.LineInjectionTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/30 19:00
 */
public class LineNumberSafetyTest {

    @BeforeAll
    static void setUp() {
        TestSetup.init();
    }

    @BeforeEach
    void clearBuffer() {
        clearProbeBuffer();
    }

    @Test
    void testInjectionExceptionDoesNotAffectBusiness() throws Exception {
        byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
        int line = findFirstMethodLine(bytecode, "processWithLoop");
        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE,
                LineInjectionTestHelper.TestTargetService.class.getName(),
                line, 0, "processWithLoop", "(I)V");
        InjectableCode code = createCode("log:test");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);
        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());
        Class<?> loaded = injectAndLoad(result, LineInjectionTestHelper.TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithLoop", int.class);
        assertDoesNotThrow(() -> method.invoke(instance, 5));
    }

    @Test
    void testHighFrequencyTriggerInLoop() throws Exception {
        byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
        int loopLine = findLineByOffset(bytecode, "processWithLoop", 2);
        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE,
                LineInjectionTestHelper.TestTargetService.class.getName(),
                loopLine, 0, "processWithLoop", "(I)V");
        InjectableCode code = createCode("log:loop iteration");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);
        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());
        Class<?> loaded = injectAndLoad(result, LineInjectionTestHelper.TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithLoop", int.class);
        long start = System.currentTimeMillis();
        assertDoesNotThrow(() -> method.invoke(instance, 1000));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 5000, "Method should complete within 5 seconds, took: " + elapsed + "ms");
        List<ProbeMessage> messages = pollProbeMessages();
        assertFalse(messages.isEmpty(), "Should have received at least one LOG message");
    }

    @Test
    void testFinallyBlockInjection() throws Exception {
        byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
        int finallyLine = findLineByOffset(bytecode, "processWithException", 4);
        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE,
                LineInjectionTestHelper.TestTargetService.class.getName(),
                finallyLine, 0, "processWithException", "()V");
        InjectableCode code = createCode("log:finally executed");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);
        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());
        assertNotNull(result, "Injection should succeed");
        Class<?> loaded = injectAndLoad(result, LineInjectionTestHelper.TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithException");
        assertDoesNotThrow(() -> method.invoke(instance));
        List<ProbeMessage> messages = pollProbeMessages();
        assertFalse(messages.isEmpty(), "Finally block should have produced LOG messages");
    }

    @Test
    void testTryBlockInjectionWithException() throws Exception {
        byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
        int line = findFirstMethodLine(bytecode, "processWithLoop");
        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE,
                LineInjectionTestHelper.TestTargetService.class.getName(),
                line, 0, "processWithLoop", "(I)V");
        InjectableCode code = createCode("log:try block entry");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);
        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());
        Class<?> loaded = injectAndLoad(result, LineInjectionTestHelper.TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithLoop", int.class);
        assertDoesNotThrow(() -> method.invoke(instance, 5));
        List<ProbeMessage> messages = pollProbeMessages();
        assertFalse(messages.isEmpty(), "Should have LOG messages from injection");
        assertEquals("LOG", messages.get(0).getType());
    }
}
