/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 22:00
 */
package fun.efto.luna.core.bytecode.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassVisitor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ByteKitEnterInjectorTest {

    public static class SimpleTarget {
        public String simpleMethod() {
            return "hello";
        }
        public String methodWithArgs(String name, int count) {
            return name + ":" + count;
        }
        public static int staticMethod(int x) {
            return x * 2;
        }
    }

    public static class EnterInterceptor {
        @AtEnter(inline = true, suppress = Throwable.class)
        public static void onEnter(
                @Binding.This Object target,
                @Binding.Args Object[] args,
                @Binding.MethodName String methodName) {
        }
    }

    public static class BytecodeClassLoader extends ClassLoader {
        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
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

    private byte[] enhanceWithByteKitAtEnter(byte[] originalBytecode, String methodName, String methodDesc) throws Exception {
        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = parser.parse(EnterInterceptor.class);

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
    @DisplayName("ByteKit @AtEnter 增强验证")
    class AtEnterTests {

        @Test
        @DisplayName("简单方法 @AtEnter 增强后字节码合法")
        void testSimpleMethodEnhancementProducesValidBytecode() throws Exception {
            byte[] original = getClassBytecode(SimpleTarget.class);
            byte[] enhanced = enhanceWithByteKitAtEnter(original, "simpleMethod", "()Ljava/lang/String;");

            assertNotNull(enhanced);
            assertTrue(enhanced.length > 0);

            org.objectweb.asm.ClassReader lunaCr = new org.objectweb.asm.ClassReader(enhanced);
            assertDoesNotThrow(() -> {
                lunaCr.accept(new ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {}, 0);
            });

            BytecodeClassLoader cl = new BytecodeClassLoader();
            assertDoesNotThrow(() -> {
                cl.defineClass(SimpleTarget.class.getName(), enhanced);
            });
        }

        @Test
        @DisplayName("增强后方法可被反射调用")
        void testEnhancedMethodCanBeInvoked() throws Exception {
            byte[] original = getClassBytecode(SimpleTarget.class);
            byte[] enhanced = enhanceWithByteKitAtEnter(original, "simpleMethod", "()Ljava/lang/String;");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(SimpleTarget.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("simpleMethod");

            String result = (String) method.invoke(instance);
            assertEquals("hello", result, "Enhanced method should still return original value");
        }

        @Test
        @DisplayName("带参数方法 @AtEnter 增强后可正常调用")
        void testMethodWithArgsEnhancement() throws Exception {
            byte[] original = getClassBytecode(SimpleTarget.class);
            byte[] enhanced = enhanceWithByteKitAtEnter(original, "methodWithArgs", "(Ljava/lang/String;I)Ljava/lang/String;");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(SimpleTarget.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("methodWithArgs", String.class, int.class);

            String result = (String) method.invoke(instance, "test", 42);
            assertEquals("test:42", result);
        }

        @Test
        @DisplayName("静态方法 @AtEnter 增强后可正常调用")
        void testStaticMethodEnhancement() throws Exception {
            byte[] original = getClassBytecode(SimpleTarget.class);
            byte[] enhanced = enhanceWithByteKitAtEnter(original, "staticMethod", "(I)I");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(SimpleTarget.class.getName(), enhanced);
            Method method = enhancedClass.getMethod("staticMethod", int.class);

            int result = (int) method.invoke(null, 5);
            assertEquals(10, result);
        }
    }
}
