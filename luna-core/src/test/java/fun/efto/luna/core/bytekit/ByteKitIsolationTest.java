/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 20:00
 */
package fun.efto.luna.core.bytekit;

import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassVisitor;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ByteKit ASM 隔离验证测试。
 * 验证 ByteKit shade ASM 与 Luna ASM 可同时工作，无类冲突。
 */
public class ByteKitIsolationTest {

    @Test
    @DisplayName("ByteKit InterceptorProcessor 类可被加载")
    void testInterceptorProcessorCanBeLoaded() throws ClassNotFoundException {
        assertDoesNotThrow(() -> {
            Class.forName("com.alibaba.bytekit.asm.interceptor.InterceptorProcessor");
        });
    }

    @Test
    @DisplayName("ByteKit shade ASM ClassReader 与 Luna ASM ClassReader 可同时加载且无类冲突")
    void testBothAsmClassReadersCanBeLoaded() throws ClassNotFoundException {
        Class<?> bytekitCr = Class.forName("com.alibaba.deps.org.objectweb.asm.ClassReader");
        Class<?> lunaCr = Class.forName("org.objectweb.asm.ClassReader");

        assertNotSame(bytekitCr, lunaCr,
                "ByteKit shade ASM 和 Luna ASM 的 ClassReader 应该是不同的类（包名不同）");
        assertNotEquals(bytekitCr.getName(), lunaCr.getName(),
                "两个 ClassReader 的全限定名应该不同");
    }

    @Test
    @DisplayName("ByteKit 可对简单类执行 @AtEnter 增强并输出合法字节码")
    void testByteKitAtEnterEnhancementProducesValidBytecode() throws Exception {
        byte[] targetBytecode = getClassBytecode(SimpleTarget.class);

        DefaultInterceptorClassParser parser = new DefaultInterceptorClassParser();
        List<InterceptorProcessor> processors = parser.parse(SimpleEnterInterceptor.class);

        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        new ClassReader(targetBytecode).accept(classNode, ClassReader.SKIP_FRAMES);

        boolean processed = false;
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("simpleMethod")) {
                MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
                for (InterceptorProcessor processor : processors) {
                    processor.process(methodProcessor);
                }
                processed = true;
                break;
            }
        }

        assertTrue(processed, "应该找到并处理 simpleMethod");

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(cw);
        byte[] enhancedBytecode = cw.toByteArray();

        assertNotNull(enhancedBytecode, "增强后的字节码不应为 null");
        assertTrue(enhancedBytecode.length > 0, "增强后的字节码长度应大于 0");

        org.objectweb.asm.ClassReader lunaCr = new org.objectweb.asm.ClassReader(enhancedBytecode);
        assertDoesNotThrow(() -> {
            lunaCr.accept(new ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {}, 0);
        }, "增强后的字节码应可被 Luna ASM ClassReader 解析");
    }

    private byte[] getClassBytecode(Class<?> clazz) throws Exception {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(className)) {
            assertNotNull(is, "无法读取类字节码: " + className);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                bos.write(buffer, 0, n);
            }
            return bos.toByteArray();
        }
    }

    public static class SimpleTarget {
        public String simpleMethod() {
            return "hello";
        }
    }

    public static class SimpleEnterInterceptor {
        @AtEnter(inline = true, suppress = Throwable.class)
        public static void onEnter(
                @Binding.This Object target,
                @Binding.MethodName String methodName) {
        }
    }
}
