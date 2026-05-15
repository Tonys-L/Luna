package fun.efto.luna.core.asm.injector;

import fun.efto.luna.core.asm.assmebler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.BytecodeAssemblerRegistry;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.plugin.ExpressionHandlerRegistry;
import fun.efto.luna.core.plugin.handler.LogExpressionHandler;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import fun.efto.luna.core.transformer.TransformerResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/03/29 02:30
 */
public class EnterMethodInjectorTest {

    @BeforeAll
    static void registerComponents() {
        BytecodeInjectorRegistry.getInstance().register(MethodInjectionType.ENTER, new EnterMethodInjector());
        BytecodeAssemblerRegistry.getInstance().register(CodeType.EXPRESSION, new ExpressionBytecodeAssembler());
        ExpressionHandlerRegistry.register(new LogExpressionHandler());
    }

    private byte[] generateTestClassBytecode() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestClass", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "testMethod", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    public void testInject() {
        byte[] originalBytecode = generateTestClassBytecode();

        MethodInjectionType injectionType = MethodInjectionType.ENTER;
        MethodTarget target = new MethodTarget(injectionType, "TestClass", "testMethod", "()V");

        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "log:hello";
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, code);

        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult result = transformer.transform(injectionPoint, "TestClass", originalBytecode);

        assertTrue(result.isTransformed(), "Transformation should succeed");
        assertNotNull(result.getBytecode(), "Transformed bytecode should not be null");
    }
}
