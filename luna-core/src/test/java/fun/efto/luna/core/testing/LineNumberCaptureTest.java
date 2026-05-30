package fun.efto.luna.core.testing;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.asm.injector.BytecodeInjector;
import fun.efto.luna.core.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import fun.efto.luna.core.probe.ProbeMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static fun.efto.luna.core.testing.LineInjectionTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/30 19:00
 */
class LineNumberCaptureTest {

    @BeforeAll
    static void setUp() {
        TestSetup.init();
    }

    @BeforeEach
    void clearBuffer() {
        clearProbeBuffer();
    }

    @Test
    void testPrimitiveVariableCapture() throws Exception {
        byte[] bytecode = getClassBytecode(TestTargetService.class);
        int line = findFirstMethodLine(bytecode, "processWithPrimitives");

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, TestTargetService.class.getName(), line, 0,
                "processWithPrimitives", "(BSIJFDCZ)V");
        InjectableCode code = createCode("snapshot:true");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

        Class<?> loaded = injectAndLoad(result, TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithPrimitives",
                byte.class, short.class, int.class, long.class,
                float.class, double.class, char.class, boolean.class);
        method.invoke(instance, (byte) 1, (short) 2, 3, 4L, 5.0f, 6.0, 'a', true);

        List<ProbeMessage> messages = pollProbeMessages();
        assertFalse(messages.isEmpty());
        assertEquals("SNAPSHOT", messages.get(0).getType());
        assertTrue(messages.get(0).getPayload().startsWith("{"));
    }

    @Test
    void testReferenceVariableCapture() throws Exception {
        byte[] bytecode = getClassBytecode(TestTargetService.class);
        int line = findFirstMethodLine(bytecode, "processWithReferences");

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, TestTargetService.class.getName(), line, 0,
                "processWithReferences", "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/String;)V");
        InjectableCode code = createCode("snapshot:true");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

        Class<?> loaded = injectAndLoad(result, TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithReferences",
                String.class, Object.class, String[].class);
        method.invoke(instance, "hello", new Object(), new String[]{"a", "b"});

        List<ProbeMessage> messages = pollProbeMessages();
        assertFalse(messages.isEmpty());
        assertEquals("SNAPSHOT", messages.get(0).getType());
        assertTrue(messages.get(0).getPayload().startsWith("{"));
    }

    @Test
    void testLoopVariableCapture() throws Exception {
        byte[] bytecode = getClassBytecode(TestTargetService.class);
        int line = findLineByOffset(bytecode, "processWithLoop", 2);

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, TestTargetService.class.getName(), line, 0,
                "processWithLoop", "(I)V");
        InjectableCode code = createCode("snapshot:true");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

        Class<?> loaded = injectAndLoad(result, TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithLoop", int.class);
        method.invoke(instance, 3);

        List<ProbeMessage> messages = pollProbeMessages();
        assertTrue(messages.size() >= 2);
        for (ProbeMessage msg : messages) {
            assertEquals("SNAPSHOT", msg.getType());
            assertTrue(msg.getPayload().startsWith("{"));
        }
    }

    @Test
    void testBranchVariableCapture() throws Exception {
        byte[] bytecode = getClassBytecode(TestTargetService.class);
        int line = findLineByOffset(bytecode, "processWithBranch", 2);

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, TestTargetService.class.getName(), line, 0,
                "processWithBranch", "(I)V");
        InjectableCode code = createCode("snapshot:true");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

        Class<?> loaded = injectAndLoad(result, TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithBranch", int.class);
        method.invoke(instance, 5);

        List<ProbeMessage> messages = pollProbeMessages();
        assertFalse(messages.isEmpty());
        assertEquals("SNAPSHOT", messages.get(0).getType());
        assertTrue(messages.get(0).getPayload().startsWith("{"));
    }

    @Test
    @Disabled("processWithException 的 try 块不会抛出异常，catch 块不会执行，无法触发注入点")
    void testCatchBlockVariableCapture() throws Exception {
        byte[] bytecode = getClassBytecode(TestTargetService.class);
        int line = findLineByOffset(bytecode, "processWithException", 3);

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, TestTargetService.class.getName(), line, 0,
                "processWithException", "()V");
        InjectableCode code = createCode("snapshot:true");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

        Class<?> loaded = injectAndLoad(result, TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithException");
        method.invoke(instance);

        List<ProbeMessage> messages = pollProbeMessages();
        assertFalse(messages.isEmpty());
        assertEquals("SNAPSHOT", messages.get(0).getType());
        assertTrue(messages.get(0).getPayload().startsWith("{"));
    }

    @Test
    void testLongDoubleSlotCapture() throws Exception {
        byte[] bytecode = getClassBytecode(TestTargetService.class);
        int line = findLineByOffset(bytecode, "processWithLongDouble", 4);

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, TestTargetService.class.getName(), line, 0,
                "processWithLongDouble", "()V");
        InjectableCode code = createCode("snapshot:true");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                .get(LineNumberInjectionType.BEFORE).get();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

        Class<?> loaded = injectAndLoad(result, TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithLongDouble");
        method.invoke(instance);

        List<ProbeMessage> messages = pollProbeMessages();
        assertFalse(messages.isEmpty());
        assertEquals("SNAPSHOT", messages.get(0).getType());
        assertTrue(messages.get(0).getPayload().startsWith("{"));
    }
}
