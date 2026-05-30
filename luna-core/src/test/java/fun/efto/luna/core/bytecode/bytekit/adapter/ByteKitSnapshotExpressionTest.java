/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 00:30
 */
package fun.efto.luna.core.bytecode.bytekit.adapter;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.bytecode.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionType;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import fun.efto.luna.core.testing.LineInjectionTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ByteKitSnapshotExpressionTest {

    public static class Calculator {
        public int add(int a, int b) {
            int sum = a + b;
            return sum;
        }
    }

    private static byte[] getClassBytecode(Class<?> clazz) throws Exception {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            assertNotNull(is, "Cannot read bytecode for: " + className);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
            return bos.toByteArray();
        }
    }

    @BeforeAll
    static void setUpAll() {
        TestSetup.init();
    }

    @BeforeEach
    void setUp() {
        LineInjectionTestHelper.clearProbeBuffer();
    }

    @AfterEach
    void tearDown() {
        LineInjectionTestHelper.clearProbeBuffer();
    }

    @Nested
    @DisplayName("snapshot: expression through ByteKit ENTER injection")
    class EnterSnapshotExpressionTests {

        @Test
        @DisplayName("snapshot:true expression captures method parameters")
        void testSnapshotExpressionThroughByteKitEnter() throws Exception {
            BytecodeInjector injector = new ByteKitEnterInjector();
            InjectableCode code = LineInjectionTestHelper.createCode("snapshot:true");
            MethodTarget target = new MethodTarget(MethodInjectionType.ENTER, Calculator.class.getName(), "add", "(II)I");
            InjectionPoint point = new InjectionPoint(target, code);
            InjectionContext context = new InjectionContext(point);

            BytecodeAssembler assembler = BytecodeAssemblerRegistry.getInstance().get(CodeType.EXPRESSION).orElse(null);
            assertNotNull(assembler);

            byte[] original = getClassBytecode(Calculator.class);
            byte[] enhanced = injector.inject(context, original, assembler);

            LineInjectionTestHelper.BytecodeClassLoader cl = new LineInjectionTestHelper.BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(Calculator.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("add", int.class, int.class);

            int result = (int) method.invoke(instance, 3, 7);
            assertEquals(10, result, "Business method should return normally");

            List<ProbeMessage> messages = LineInjectionTestHelper.pollProbeMessages();
            assertFalse(messages.isEmpty(), "Should produce snapshot output");
            boolean foundSnapshot = messages.stream()
                    .anyMatch(m -> m.getType() != null && m.getType().equals("SNAPSHOT"));
            assertTrue(foundSnapshot, "Should produce SNAPSHOT type message, got: " + messages);
        }
    }
}
