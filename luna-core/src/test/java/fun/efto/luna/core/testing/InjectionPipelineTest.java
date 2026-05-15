package fun.efto.luna.core.testing;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import fun.efto.luna.core.transformer.TransformerResult;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/02 10:00
 */
public class InjectionPipelineTest {

    private static final String TEST_CLASS = "com.example.TestSubject";
    private static final String TEST_METHOD = "greet";
    private static final String TEST_DESCRIPTOR = "(Ljava/lang/String;)V";

    private byte[] generateTestClassBytecode(String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className.replace('.', '/'), null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "greet", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private InjectionPoint createInjectionPoint(MethodInjectionType injectionType, String code) {
        MethodTarget target = new MethodTarget(injectionType, TEST_CLASS, TEST_METHOD, TEST_DESCRIPTOR);
        InjectableCode injectableCode = new InjectableCode() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };
        return new InjectionPoint(target, injectableCode);
    }

    private void assertBytecodeValid(byte[] bytecode, String message) {
        try {
            ClassReader cr = new ClassReader(bytecode);
            cr.accept(new ClassVisitor(Opcodes.ASM9) {}, 0);
        } catch (Exception e) {
            fail(message + ": " + e.getMessage());
        }
    }

    @Test
    public void testLogExpressionBytecodeGeneration() {
        byte[] originalBytecode = generateTestClassBytecode(TEST_CLASS);
        InjectionPoint injectionPoint = createInjectionPoint(MethodInjectionType.ENTER, "log:hello");

        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult result = transformer.transform(injectionPoint, TEST_CLASS, originalBytecode);

        assertTrue(result.isTransformed(), "Transformation should succeed");
        assertNotNull(result.getBytecode(), "Transformed bytecode should not be null");
        assertNotEquals(originalBytecode.length, result.getBytecode().length,
                "Transformed bytecode size should differ from original");
    }

    @Test
    public void testEnterMethodVisitorBytecodeValid() {
        byte[] originalBytecode = generateTestClassBytecode(TEST_CLASS);
        InjectionPoint injectionPoint = createInjectionPoint(MethodInjectionType.ENTER, "log:hello");

        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult result = transformer.transform(injectionPoint, TEST_CLASS, originalBytecode);

        assertTrue(result.isTransformed(), "Transformation should succeed");
        assertBytecodeValid(result.getBytecode(), "Transformed bytecode should be valid and readable by ClassReader");
    }

    @Test
    public void testDefaultClassTransformerFlow() {
        byte[] originalBytecode = generateTestClassBytecode(TEST_CLASS);
        InjectionPoint injectionPoint = createInjectionPoint(MethodInjectionType.ENTER, "log:test-flow");

        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult result = transformer.transform(injectionPoint, TEST_CLASS, originalBytecode);

        assertTrue(result.isTransformed(), "Transformation should succeed");
        assertNotNull(result.getBytecode(), "Bytecode should not be null");
        assertTrue(result.getBytecode().length > 0, "Bytecode should have content");
        assertNotNull(result.getMessage(), "Message should not be null");
        assertTrue(result.getMessage().contains("转换成功"), "Message should indicate success");
    }

    @Test
    public void testMultipleInjectionOnSameClass() {
        byte[] originalBytecode = generateTestClassBytecode(TEST_CLASS);

        InjectionPoint enterPoint = createInjectionPoint(MethodInjectionType.ENTER, "log:enter-msg");
        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult enterResult = transformer.transform(enterPoint, TEST_CLASS, originalBytecode);

        assertTrue(enterResult.isTransformed(), "ENTER transformation should succeed");
        assertBytecodeValid(enterResult.getBytecode(), "Bytecode after ENTER injection should be valid");

        InjectionPoint exitPoint = createInjectionPoint(MethodInjectionType.EXIT, "log:exit-msg");
        TransformerResult exitResult = transformer.transform(exitPoint, TEST_CLASS, enterResult.getBytecode());

        assertTrue(exitResult.isTransformed(), "EXIT transformation should succeed");
        assertBytecodeValid(exitResult.getBytecode(), "Bytecode after EXIT injection should be valid");
    }

    @Test
    public void testExpressionWithColon() {
        byte[] originalBytecode = generateTestClassBytecode(TEST_CLASS);
        InjectionPoint injectionPoint = createInjectionPoint(MethodInjectionType.ENTER, "log:time=12:30:00");

        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult result = transformer.transform(injectionPoint, TEST_CLASS, originalBytecode);

        assertTrue(result.isTransformed(), "Transformation with colon in expression should succeed");
        assertNotNull(result.getBytecode(), "Bytecode should not be null");
        assertBytecodeValid(result.getBytecode(), "Bytecode with colon expression should be valid");
    }

    @Test
    public void testChineseExpressionBytecodeEncoding() {
        byte[] originalBytecode = generateTestClassBytecode(TEST_CLASS);
        String chineseMessage = "执行方法: com.example.TestSubject.greet";
        InjectionPoint injectionPoint = createInjectionPoint(MethodInjectionType.ENTER, "log:" + chineseMessage);

        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult result = transformer.transform(injectionPoint, TEST_CLASS, originalBytecode);

        assertTrue(result.isTransformed(), "Transformation with Chinese expression should succeed");
        assertBytecodeValid(result.getBytecode(), "Bytecode with Chinese expression should be valid");

        String constantPoolContent = extractStringConstants(result.getBytecode());
        System.out.println("String constants in bytecode: " + constantPoolContent);
        assertTrue(constantPoolContent.contains(chineseMessage),
                "Bytecode constant pool should contain the Chinese string. Got: " + constantPoolContent);
    }

    private String extractStringConstants(byte[] bytecode) {
        StringBuilder sb = new StringBuilder();
        ClassReader cr = new ClassReader(bytecode);
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                                                              String signature, Object value) {
                if (value instanceof String) {
                    sb.append(value).append("\n");
                }
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                              String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof String) {
                            sb.append((String) value).append("\n");
                        }
                    }
                };
            }
        }, 0);

        return sb.toString();
    }
}
