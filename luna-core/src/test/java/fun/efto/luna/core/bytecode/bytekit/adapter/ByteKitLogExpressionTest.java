/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 00:10
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
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.target.InjectionType;
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

public class ByteKitLogExpressionTest {

    public static class SimpleService {
        public String greet(String name) {
            return "Hello, " + name;
        }

        public int add(int a, int b) {
            return a + b;
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

    private InjectionPoint createEnterInjectionPoint(String className, String methodName, String methodDesc, String expression) {
        InjectableCode code = LineInjectionTestHelper.createCode(expression);
        MethodTarget target = new MethodTarget(MethodInjectionType.ENTER, className, methodName, methodDesc);
        return new InjectionPoint(target, code);
    }

    private InjectionPoint createExitInjectionPoint(String className, String methodName, String methodDesc, String expression) {
        InjectableCode code = LineInjectionTestHelper.createCode(expression);
        MethodTarget target = new MethodTarget(MethodInjectionType.EXIT, className, methodName, methodDesc);
        return new InjectionPoint(target, code);
    }

    @Nested
    @DisplayName("log: expression through ByteKit ENTER injection")
    class EnterLogExpressionTests {

        @Test
        @DisplayName("log:hello expression produces log output when method enters")
        void testLogExpressionThroughByteKitEnter() throws Exception {
            BytecodeInjector injector = new ByteKitEnterInjector();
            InjectionPoint point = createEnterInjectionPoint(
                    SimpleService.class.getName(), "greet", "(Ljava/lang/String;)Ljava/lang/String;", "log:hello");
            InjectionContext context = new InjectionContext(point);

            BytecodeAssembler assembler = BytecodeAssemblerRegistry.getInstance().get(CodeType.EXPRESSION).orElse(null);
            assertNotNull(assembler, "ExpressionBytecodeAssembler should be registered");

            byte[] original = getClassBytecode(SimpleService.class);
            byte[] enhanced = injector.inject(context, original, assembler);

            LineInjectionTestHelper.BytecodeClassLoader cl = new LineInjectionTestHelper.BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(SimpleService.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("greet", String.class);

            Object result = method.invoke(instance, "World");
            assertEquals("Hello, World", result, "Business method should return normally");

            List<ProbeMessage> messages = LineInjectionTestHelper.pollProbeMessages();
            assertFalse(messages.isEmpty(), "Should produce log output from log:hello expression");
            boolean found = messages.stream().anyMatch(m -> m.getPayload() != null && m.getPayload().contains("hello"));
            assertTrue(found, "Log output should contain 'hello', got: " + messages);
        }

        @Test
        @DisplayName("log:hello $1 expression produces log with parameter value")
        void testLogExpressionWithParameterRef() throws Exception {
            BytecodeInjector injector = new ByteKitEnterInjector();
            InjectionPoint point = createEnterInjectionPoint(
                    SimpleService.class.getName(), "greet", "(Ljava/lang/String;)Ljava/lang/String;", "log:hello $1");
            InjectionContext context = new InjectionContext(point);

            BytecodeAssembler assembler = BytecodeAssemblerRegistry.getInstance().get(CodeType.EXPRESSION).orElse(null);
            assertNotNull(assembler);

            byte[] original = getClassBytecode(SimpleService.class);
            byte[] enhanced = injector.inject(context, original, assembler);

            LineInjectionTestHelper.BytecodeClassLoader cl = new LineInjectionTestHelper.BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(SimpleService.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("greet", String.class);

            Object result = method.invoke(instance, "World");
            assertEquals("Hello, World", result);

            List<ProbeMessage> messages = LineInjectionTestHelper.pollProbeMessages();
            assertFalse(messages.isEmpty(), "Should produce log output");
            boolean found = messages.stream().anyMatch(m -> m.getPayload() != null && m.getPayload().contains("World"));
            assertTrue(found, "Log output should contain parameter value 'World', got: " + messages);
        }
    }

    @Nested
    @DisplayName("log: expression through ByteKit EXIT injection")
    class ExitLogExpressionTests {

        @Test
        @DisplayName("log:done expression produces log output when method exits")
        void testLogExpressionThroughByteKitExit() throws Exception {
            BytecodeInjector injector = new ByteKitExitInjector();
            InjectionPoint point = createExitInjectionPoint(
                    SimpleService.class.getName(), "greet", "(Ljava/lang/String;)Ljava/lang/String;", "log:done");
            InjectionContext context = new InjectionContext(point);

            BytecodeAssembler assembler = BytecodeAssemblerRegistry.getInstance().get(CodeType.EXPRESSION).orElse(null);
            assertNotNull(assembler);

            byte[] original = getClassBytecode(SimpleService.class);
            byte[] enhanced = injector.inject(context, original, assembler);

            LineInjectionTestHelper.BytecodeClassLoader cl = new LineInjectionTestHelper.BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(SimpleService.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("greet", String.class);

            Object result = method.invoke(instance, "World");
            assertEquals("Hello, World", result);

            List<ProbeMessage> messages = LineInjectionTestHelper.pollProbeMessages();
            assertFalse(messages.isEmpty(), "Should produce log output from log:done expression");
        }
    }
}
