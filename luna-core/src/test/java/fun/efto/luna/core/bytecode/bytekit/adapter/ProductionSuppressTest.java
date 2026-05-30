/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 23:30
 */
package fun.efto.luna.core.bytecode.bytekit.adapter;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.deps.org.objectweb.asm.tree.TryCatchBlockNode;
import fun.efto.luna.core.bytecode.bytekit.interceptor.EnterInterceptor;
import fun.efto.luna.core.bytecode.bytekit.interceptor.ExitInterceptor;
import fun.efto.luna.core.bytecode.bytekit.interceptor.AroundInterceptor;
import fun.efto.luna.core.bytecode.bytekit.interceptor.ExceptionExitInterceptor;
import fun.efto.luna.core.bytecode.bytekit.interceptor.InvokeInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionSuppressTest {

    public static class BusinessTarget {
        public String normalBusiness() {
            return "success";
        }

        public void methodThatThrows() {
            throw new RuntimeException("test");
        }

        public void methodWithInvoke() {
            String s = "hello".toUpperCase();
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

    private byte[] enhanceWithInterceptor(Class<?> interceptorClass, byte[] originalBytecode,
                                           String methodName, String methodDesc) throws Exception {
        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = parser.parse(interceptorClass);

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

    private boolean hasTryCatchForThrowable(byte[] bytecode, String methodName, String methodDesc) {
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        new ClassReader(bytecode).accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            if (method.name.equals(methodName) && (methodDesc == null || method.desc.equals(methodDesc))) {
                if (method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty()) {
                    return false;
                }
                for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
                    if (tcb.type == null || tcb.type.equals("java/lang/Throwable") || tcb.type.equals("java/lang/Exception") || tcb.type.equals("java/lang/RuntimeException")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nested
    @DisplayName("生产 Interceptor suppress try-catch 保护验证")
    class ProductionSuppressTests {

        @Test
        @DisplayName("EnterInterceptor 增强后字节码包含 try-catch 异常保护")
        void testEnterInterceptorHasTryCatchProtection() throws Exception {
            byte[] original = getClassBytecode(BusinessTarget.class);
            byte[] enhanced = enhanceWithInterceptor(EnterInterceptor.class, original, "normalBusiness", "()Ljava/lang/String;");

            assertTrue(hasTryCatchForThrowable(enhanced, "normalBusiness", "()Ljava/lang/String;"),
                    "EnterInterceptor should generate try-catch block for suppress protection");
        }

        @Test
        @DisplayName("ExitInterceptor 增强后字节码包含 try-catch 异常保护")
        void testExitInterceptorHasTryCatchProtection() throws Exception {
            byte[] original = getClassBytecode(BusinessTarget.class);
            byte[] enhanced = enhanceWithInterceptor(ExitInterceptor.class, original, "normalBusiness", "()Ljava/lang/String;");

            assertTrue(hasTryCatchForThrowable(enhanced, "normalBusiness", "()Ljava/lang/String;"),
                    "ExitInterceptor should generate try-catch block for suppress protection");
        }

        @Test
        @DisplayName("AroundInterceptor 增强后字节码包含 try-catch 异常保护")
        void testAroundInterceptorHasTryCatchProtection() throws Exception {
            byte[] original = getClassBytecode(BusinessTarget.class);
            byte[] enhanced = enhanceWithInterceptor(AroundInterceptor.class, original, "normalBusiness", "()Ljava/lang/String;");

            assertTrue(hasTryCatchForThrowable(enhanced, "normalBusiness", "()Ljava/lang/String;"),
                    "AroundInterceptor should generate try-catch block for suppress protection");
        }

        @Test
        @DisplayName("ExceptionExitInterceptor 增强后字节码包含 try-catch 异常保护")
        void testExceptionExitInterceptorHasTryCatchProtection() throws Exception {
            byte[] original = getClassBytecode(BusinessTarget.class);
            byte[] enhanced = enhanceWithInterceptor(ExceptionExitInterceptor.class, original, "methodThatThrows", "()V");

            assertTrue(hasTryCatchForThrowable(enhanced, "methodThatThrows", "()V"),
                    "ExceptionExitInterceptor should generate try-catch block for suppress protection");
        }

        @Test
        @DisplayName("InvokeInterceptor 增强后字节码包含 try-catch 异常保护")
        void testInvokeInterceptorHasTryCatchProtection() throws Exception {
            byte[] original = getClassBytecode(BusinessTarget.class);
            byte[] enhanced = enhanceWithInterceptor(InvokeInterceptor.class, original, "methodWithInvoke", "()V");

            assertTrue(hasTryCatchForThrowable(enhanced, "methodWithInvoke", "()V"),
                    "InvokeInterceptor should generate try-catch block for suppress protection");
        }
    }
}
