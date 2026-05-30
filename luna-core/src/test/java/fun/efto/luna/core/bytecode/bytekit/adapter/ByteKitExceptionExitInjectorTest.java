/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 00:00
 */
package fun.efto.luna.core.bytecode.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
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
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ByteKitExceptionExitInjectorTest {

    public static class ExceptionTarget {
        public void methodThatThrows() {
            throw new RuntimeException("test");
        }

        public String methodWithTryCatch() {
            try {
                throw new RuntimeException("inner");
            } catch (RuntimeException e) {
                return "caught";
            }
        }
    }

    public static class TestExceptionExitInterceptor {
        @AtExceptionExit(inline = true, suppress = Throwable.class)
        public static void onExceptionExit(
                @Binding.This Object target,
                @Binding.MethodName String methodName,
                @Binding.Throwable Throwable t) {
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

    private byte[] enhanceWithByteKitAtExceptionExit(byte[] originalBytecode, String methodName, String methodDesc) throws Exception {
        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = parser.parse(TestExceptionExitInterceptor.class);

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
    @DisplayName("ByteKit @AtExceptionExit 增强验证")
    class AtExceptionExitTests {

        @Test
        @DisplayName("抛异常方法 @AtExceptionExit 增强后字节码合法 (L1+L2)")
        void testExceptionMethodEnhancementProducesValidBytecode() throws Exception {
            byte[] original = getClassBytecode(ExceptionTarget.class);
            byte[] enhanced = enhanceWithByteKitAtExceptionExit(original, "methodThatThrows", "()V");

            assertNotNull(enhanced);
            assertTrue(enhanced.length > 0);

            org.objectweb.asm.ClassReader lunaCr = new org.objectweb.asm.ClassReader(enhanced);
            assertDoesNotThrow(() -> {
                lunaCr.accept(new ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {}, 0);
            });

            BytecodeClassLoader cl = new BytecodeClassLoader();
            assertDoesNotThrow(() -> {
                cl.defineClass(ExceptionTarget.class.getName(), enhanced);
            });
        }

        @Test
        @DisplayName("增强后抛异常方法可被外部捕获 (L3)")
        void testEnhancedExceptionMethodCanBeCaughtExternally() throws Exception {
            byte[] original = getClassBytecode(ExceptionTarget.class);
            byte[] enhanced = enhanceWithByteKitAtExceptionExit(original, "methodThatThrows", "()V");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(ExceptionTarget.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("methodThatThrows");

            InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () -> method.invoke(instance));
            assertTrue(thrown.getCause() instanceof RuntimeException);
            assertEquals("test", thrown.getCause().getMessage());
        }

        @Test
        @DisplayName("含 try-catch 方法 @AtExceptionExit 增强后正常返回 (L3)")
        void testMethodWithTryCatchStillReturnsNormally() throws Exception {
            byte[] original = getClassBytecode(ExceptionTarget.class);
            byte[] enhanced = enhanceWithByteKitAtExceptionExit(original, "methodWithTryCatch", "()Ljava/lang/String;");

            BytecodeClassLoader cl = new BytecodeClassLoader();
            Class<?> enhancedClass = cl.defineClass(ExceptionTarget.class.getName(), enhanced);
            Object instance = enhancedClass.getDeclaredConstructor().newInstance();
            Method method = enhancedClass.getMethod("methodWithTryCatch");

            String result = (String) method.invoke(instance);
            assertEquals("caught", result);
        }
    }
}
