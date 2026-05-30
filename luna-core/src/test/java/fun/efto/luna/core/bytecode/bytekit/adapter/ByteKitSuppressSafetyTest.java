/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 00:00
 */
package fun.efto.luna.core.bytecode.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.ExceptionHandler;
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

public class ByteKitSuppressSafetyTest {

    public static class BusinessTarget {
        public String normalBusiness() {
            return "success";
        }

        public int compute(int a, int b) {
            return a + b;
        }
    }

    public static class SuppressHandler {
        @ExceptionHandler(inline = true)
        public static void onSuppress(@Binding.Throwable Throwable t) {
        }
    }

    public static class BrokenInterceptor {
        @AtEnter(inline = true, suppress = Throwable.class, suppressHandler = SuppressHandler.class)
        public static void onEnter(
                @Binding.This Object target,
                @Binding.MethodName String methodName) {
            throw new RuntimeException("interceptor callback broken!");
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

    private byte[] enhanceWithBrokenInterceptor(byte[] originalBytecode, String methodName, String methodDesc) throws Exception {
        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = parser.parse(BrokenInterceptor.class);

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
    @DisplayName("ByteKit suppress 异常安全验证")
    class SuppressSafetyTests {

        @Test
        @DisplayName("拦截器回调抛异常时业务方法仍正常返回 (suppress 保护)")
        void testBusinessMethodReturnsNormallyWhenInterceptorThrows() throws Exception {
            byte[] original = getClassBytecode(BusinessTarget.class);
            byte[] enhanced = enhanceWithBrokenInterceptor(original, "normalBusiness", "()Ljava/lang/String;");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(BusinessTarget.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("normalBusiness");

            String result = (String) method.invoke(instance);
            assertEquals("success", result, "Business method should still return normally even when interceptor callback throws");
        }

        @Test
        @DisplayName("拦截器抛异常时增强后字节码合法 (L1+L2)")
        void testBrokenInterceptorEnhancementProducesValidBytecode() throws Exception {
            byte[] original = getClassBytecode(BusinessTarget.class);
            byte[] enhanced = enhanceWithBrokenInterceptor(original, "normalBusiness", "()Ljava/lang/String;");

            assertNotNull(enhanced);
            assertTrue(enhanced.length > 0);

            org.objectweb.asm.ClassReader lunaCr = new org.objectweb.asm.ClassReader(enhanced);
            assertDoesNotThrow(() -> {
                lunaCr.accept(new ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {}, 0);
            });

            BytecodeClassLoader cl = new BytecodeClassLoader();
            assertDoesNotThrow(() -> {
                cl.defineClass(BusinessTarget.class.getName(), enhanced);
            });
        }

        @Test
        @DisplayName("带参数方法拦截器抛异常时仍正常返回 (suppress 保护)")
        void testMethodWithArgsReturnsNormallyWhenInterceptorThrows() throws Exception {
            byte[] original = getClassBytecode(BusinessTarget.class);
            byte[] enhanced = enhanceWithBrokenInterceptor(original, "compute", "(II)I");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(BusinessTarget.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("compute", int.class, int.class);

            int result = (int) method.invoke(instance, 3, 7);
            assertEquals(10, result);
        }
    }
}
