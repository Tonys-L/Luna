/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 22:30
 */
package fun.efto.luna.core.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import fun.efto.luna.core.spy.LunaSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassVisitor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ByteKitAroundInjectorTest {

    public static class AroundTarget {
        public String process(String input) {
            return "processed: " + input;
        }

        public int compute(int a, int b) {
            return a + b;
        }
    }

    public static class TestAroundInterceptor {
        @AtEnter(inline = true, suppress = Throwable.class)
        public static void onEnter(
                @Binding.This Object target,
                @Binding.Args Object[] args,
                @Binding.MethodName String methodName) {
            LunaSpy.onLog("[AROUND-ENTER] " + methodName);
        }

        @AtExit(inline = true, suppress = Throwable.class)
        public static void onExit(
                @Binding.This Object target,
                @Binding.Args Object[] args,
                @Binding.MethodName String methodName,
                @Binding.Return Object returnValue) {
            LunaSpy.onLog("[AROUND-EXIT] " + methodName);
        }
    }

    public static class BytecodeClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    @BeforeEach
    void setUp() {
        while (ProbeOutput.BUFFER.poll() != null) {}
    }

    private List<ProbeMessage> drainBuffer() {
        List<ProbeMessage> messages = new ArrayList<>();
        ProbeMessage msg;
        while ((msg = ProbeOutput.BUFFER.poll()) != null) {
            messages.add(msg);
        }
        return messages;
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

    private byte[] enhanceWithByteKitAround(byte[] originalBytecode, String methodName, String methodDesc) throws Exception {
        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = parser.parse(TestAroundInterceptor.class);

        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        new ClassReader(originalBytecode).accept(classNode, ClassReader.SKIP_FRAMES);

        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals(methodName) && (methodDesc == null || methodNode.desc.equals(methodDesc))) {
                MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
                for (InterceptorProcessor processor : processors) {
                    processor.process(methodProcessor);
                }
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    @Nested
    @DisplayName("ByteKit @AtEnter + @AtExit 组合增强验证")
    class AroundTests {

        @Test
        @DisplayName("@AtEnter + @AtExit 组合增强产生合法字节码")
        void testAroundEnhancementProducesValidBytecode() throws Exception {
            byte[] original = getClassBytecode(AroundTarget.class);
            byte[] enhanced = enhanceWithByteKitAround(original, "process", "(Ljava/lang/String;)Ljava/lang/String;");

            assertNotNull(enhanced);
            assertTrue(enhanced.length > 0);

            org.objectweb.asm.ClassReader lunaCr = new org.objectweb.asm.ClassReader(enhanced);
            assertDoesNotThrow(() -> {
                lunaCr.accept(new ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {}, 0);
            });

            BytecodeClassLoader cl = new BytecodeClassLoader();
            assertDoesNotThrow(() -> {
                cl.defineClass(AroundTarget.class.getName(), enhanced);
            });
        }

        @Test
        @DisplayName("增强方法可被调用且 enter 和 exit 回调均触发")
        void testEnhancedMethodFiresBothCallbacks() throws Exception {
            byte[] original = getClassBytecode(AroundTarget.class);
            byte[] enhanced = enhanceWithByteKitAround(original, "process", "(Ljava/lang/String;)Ljava/lang/String;");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(AroundTarget.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("process", String.class);

            String result = (String) method.invoke(instance, "test");
            assertEquals("processed: test", result);

            List<ProbeMessage> messages = drainBuffer();
            long enterCount = messages.stream().filter(m -> m.getPayload().contains("[AROUND-ENTER]")).count();
            long exitCount = messages.stream().filter(m -> m.getPayload().contains("[AROUND-EXIT]")).count();
            assertEquals(1, enterCount, "Enter callback should fire once");
            assertEquals(1, exitCount, "Exit callback should fire once");
        }

        @Test
        @DisplayName("int 返回值方法 Around 增强后可正常调用")
        void testIntReturnMethodAroundEnhancement() throws Exception {
            byte[] original = getClassBytecode(AroundTarget.class);
            byte[] enhanced = enhanceWithByteKitAround(original, "compute", "(II)I");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(AroundTarget.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("compute", int.class, int.class);

            int result = (int) method.invoke(instance, 3, 7);
            assertEquals(10, result);

            List<ProbeMessage> messages = drainBuffer();
            long enterCount = messages.stream().filter(m -> m.getPayload().contains("[AROUND-ENTER]")).count();
            long exitCount = messages.stream().filter(m -> m.getPayload().contains("[AROUND-EXIT]")).count();
            assertEquals(1, enterCount);
            assertEquals(1, exitCount);
        }
    }
}
