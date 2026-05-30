package fun.efto.luna.core.testing;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.probe.ProbeMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static fun.efto.luna.core.testing.LineInjectionTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/30 19:00
 */
@DisplayName("行号注入端到端集成测试")
public class LineNumberInjectionE2ETest {

    private static final String TARGET_CLASS = LineInjectionTestHelper.TestTargetService.class.getName();

    @BeforeAll
    static void setUp() {
        TestSetup.init();
    }

    @Nested
    @DisplayName("line_before 注入测试")
    class BeforeLineTests {

        @Test
        @DisplayName("line_before + log 注入，验证 BUFFER 输出 LOG 消息")
        void testLineBeforeLogInjection() throws Exception {
            clearProbeBuffer();

            byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
            int line = findFirstMethodLine(bytecode, "processWithLoop");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TARGET_CLASS, line, 0,
                    "processWithLoop", "(I)V");
            InjectableCode code = createCode("log:check loop");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                    .get(LineNumberInjectionType.BEFORE).get();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            Class<?> loaded = injectAndLoad(result, TARGET_CLASS);
            Object instance = loaded.getDeclaredConstructor().newInstance();
            Method method = loaded.getMethod("processWithLoop", int.class);
            method.invoke(instance, 5);

            List<ProbeMessage> messages = pollProbeMessages();
            assertFalse(messages.isEmpty());
            assertEquals("LOG", messages.get(0).getType());
            assertTrue(messages.get(0).getPayload().contains("check loop"));
        }

        @Test
        @DisplayName("line_before + snapshot 注入，验证 BUFFER 输出 SNAPSHOT 消息")
        void testLineBeforeSnapshotInjection() throws Exception {
            clearProbeBuffer();

            byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
            int line = findFirstMethodLine(bytecode, "processWithPrimitives");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TARGET_CLASS, line, 0,
                    "processWithPrimitives", "(BSIJFDCZ)V");
            InjectableCode code = createCode("snapshot:true");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                    .get(LineNumberInjectionType.BEFORE).get();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            Class<?> loaded = injectAndLoad(result, TARGET_CLASS);
            Object instance = loaded.getDeclaredConstructor().newInstance();
            Method method = loaded.getMethod("processWithPrimitives",
                    byte.class, short.class, int.class, long.class,
                    float.class, double.class, char.class, boolean.class);
            method.invoke(instance, (byte) 1, (short) 2, 3, 4L, 5.0f, 6.0, 'a', true);

            List<ProbeMessage> messages = pollProbeMessages();
            assertFalse(messages.isEmpty());
            assertEquals("SNAPSHOT", messages.get(0).getType());
        }

        @Test
        @DisplayName("方法第一行注入，验证能捕获方法参数")
        void testFirstLineInjection() throws Exception {
            clearProbeBuffer();

            byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
            int line = findFirstMethodLine(bytecode, "processWithPrimitives");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TARGET_CLASS, line, 0,
                    "processWithPrimitives", "(BSIJFDCZ)V");
            InjectableCode code = createCode("snapshot:true");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                    .get(LineNumberInjectionType.BEFORE).get();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            Class<?> loaded = injectAndLoad(result, TARGET_CLASS);
            Object instance = loaded.getDeclaredConstructor().newInstance();
            Method method = loaded.getMethod("processWithPrimitives",
                    byte.class, short.class, int.class, long.class,
                    float.class, double.class, char.class, boolean.class);
            method.invoke(instance, (byte) 10, (short) 20, 30, 40L, 5.5f, 6.6, 'x', false);

            List<ProbeMessage> messages = pollProbeMessages();
            assertFalse(messages.isEmpty());
            assertEquals("SNAPSHOT", messages.get(0).getType());
            assertTrue(messages.get(0).getPayload().contains("param["));
        }

        @Test
        @DisplayName("方法最后一行（return 前）注入")
        void testLastLineBeforeReturnInjection() throws Exception {
            clearProbeBuffer();

            byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
            List<Integer> lines = getAllMethodLines(bytecode, "processWithMultiReturn");
            assertFalse(lines.isEmpty());
            int lastLine = lines.get(lines.size() - 1);

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TARGET_CLASS, lastLine, 0,
                    "processWithMultiReturn", "(I)I");
            InjectableCode code = createCode("log:before return");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                    .get(LineNumberInjectionType.BEFORE).get();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            Class<?> loaded = injectAndLoad(result, TARGET_CLASS);
            Object instance = loaded.getDeclaredConstructor().newInstance();
            Method method = loaded.getMethod("processWithMultiReturn", int.class);
            method.invoke(instance, 0);

            List<ProbeMessage> messages = pollProbeMessages();
            assertFalse(messages.isEmpty());
            assertEquals("LOG", messages.get(0).getType());
            assertTrue(messages.get(0).getPayload().contains("before return"));
        }

        @Test
        @DisplayName("同一行注入两次 log，验证收到两条 LOG 消息")
        void testMultipleInjectionPointsOnSameLine() throws Exception {
            clearProbeBuffer();

            byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
            int line = findFirstMethodLine(bytecode, "processWithLoop");

            LineNumberTarget target1 = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TARGET_CLASS, line, 0,
                    "processWithLoop", "(I)V");
            InjectableCode code1 = createCode("log:first injection");
            InjectionPoint ip1 = new InjectionPoint(target1, code1);
            InjectionContext ctx1 = new InjectionContext(ip1);

            BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                    .get(LineNumberInjectionType.BEFORE).get();
            byte[] result1 = injector.inject(ctx1, bytecode, new ExpressionBytecodeAssembler());

            LineNumberTarget target2 = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TARGET_CLASS, line, 0,
                    "processWithLoop", "(I)V");
            InjectableCode code2 = createCode("log:second injection");
            InjectionPoint ip2 = new InjectionPoint(target2, code2);
            InjectionContext ctx2 = new InjectionContext(ip2);

            byte[] result2 = injector.inject(ctx2, result1, new ExpressionBytecodeAssembler());

            Class<?> loaded = injectAndLoad(result2, TARGET_CLASS);
            Object instance = loaded.getDeclaredConstructor().newInstance();
            Method method = loaded.getMethod("processWithLoop", int.class);
            method.invoke(instance, 5);

            List<ProbeMessage> messages = pollProbeMessages();
            long logCount = messages.stream().filter(m -> "LOG".equals(m.getType())).count();
            assertEquals(2, logCount);
        }

        @Test
        @DisplayName("条件表达式注入 - 字节码生成无 VerifyError")
        void testConditionalLogInjection() throws Exception {
            clearProbeBuffer();

            byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
            int line = findFirstMethodLine(bytecode, "processWithPrimitives");

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE, TARGET_CLASS, line, 0,
                    "processWithPrimitives", "(BSIJFDCZ)V");
            InjectableCode code = createCode("${param[2] >= 18}::log:Adult user");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                    .get(LineNumberInjectionType.BEFORE).get();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            Class<?> loaded = injectAndLoad(result, TARGET_CLASS);
            assertNotNull(loaded);
        }
    }

    @Nested
    @DisplayName("line_after 注入测试")
    class AfterLineTests {

        @Test
        @DisplayName("line_after + log 注入，验证消息前缀为 line after")
        void testLineAfterLogInjection() throws Exception {
            clearProbeBuffer();

            byte[] bytecode = getClassBytecode(LineInjectionTestHelper.TestTargetService.class);
            int line = findLineByOffset(bytecode, "processWithLoop", 1);

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.AFTER, TARGET_CLASS, line, 0,
                    "processWithLoop", "(I)V");
            InjectableCode code = createCode("log:after execution");
            InjectionPoint ip = new InjectionPoint(target, code);
            InjectionContext ctx = new InjectionContext(ip);

            BytecodeInjector injector = BytecodeInjectorRegistry.getInstance()
                    .get(LineNumberInjectionType.AFTER).get();
            byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

            Class<?> loaded = injectAndLoad(result, TARGET_CLASS);
            Object instance = loaded.getDeclaredConstructor().newInstance();
            Method method = loaded.getMethod("processWithLoop", int.class);
            method.invoke(instance, 5);

            List<ProbeMessage> messages = pollProbeMessages();
            assertFalse(messages.isEmpty());
            assertEquals("LOG", messages.get(0).getType());
            assertTrue(messages.get(0).getPayload().startsWith("line after: "));
        }
    }
}
