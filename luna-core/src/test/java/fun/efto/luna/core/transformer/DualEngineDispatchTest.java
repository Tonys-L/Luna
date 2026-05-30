/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:00
 */
package fun.efto.luna.core.transformer;

import fun.efto.luna.core.bytecode.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.bytecode.bytekit.adapter.ByteKitEnterInjector;
import fun.efto.luna.core.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;
import fun.efto.luna.core.plugin.builtin.line.BeforeLineInjector;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionType;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import fun.efto.luna.core.plugin.builtin.log.LogExpressionHandler;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotExpressionHandler;
import fun.efto.luna.core.plugin.builtin.trace.TraceExpressionHandler;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.*;

public class DualEngineDispatchTest {

    @BeforeEach
    void setUp() {
        BytecodeInjectorRegistry.getInstance().getRegistry().clear();
        InjectionTypeRegistry.getInstance().clear();
        RuleConverterRegistry.getInstance().clear();
        CoreCapabilityRegistry.getInstance().clear();
        BytecodeAssemblerRegistry.getInstance().register(CodeType.EXPRESSION, new ExpressionBytecodeAssembler());
        ExpressionHandlerRegistry.getInstance().register(new LogExpressionHandler());
        ExpressionHandlerRegistry.getInstance().register(new SnapshotExpressionHandler());
        ExpressionHandlerRegistry.getInstance().register(new TraceExpressionHandler());
        CoreModuleInitializer.initialize();
    }

    @Test
    @DisplayName("方法级注入点 (ENTER) 使用 ByteKit 注入器")
    void testMethodLevelUsesByteKitInjector() {
        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance().get(MethodInjectionType.ENTER).orElse(null);
        assertNotNull(injector);
        assertInstanceOf(ByteKitEnterInjector.class, injector);

        byte[] bytecode = generateSimpleTestClass();
        MethodTarget target = new MethodTarget(MethodInjectionType.ENTER, "TestService", "doWork", "()V");
        InjectableCode code = createExpressionCode("log:test");
        InjectionPoint point = new InjectionPoint(target, code);

        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult result = transformer.transform(point, "TestService", bytecode);
        assertTrue(result.isTransformed());
    }

    @Test
    @DisplayName("行级注入点 (BEFORE) 使用 ASM 注入器")
    void testLineLevelUsesAsmInjector() {
        BytecodeInjector injector = BytecodeInjectorRegistry.getInstance().get(LineNumberInjectionType.BEFORE).orElse(null);
        assertNotNull(injector);
        assertInstanceOf(BeforeLineInjector.class, injector);

        byte[] bytecode = generateTestClassWithLineNumber();
        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionType.BEFORE, "TestService", 12, 0, "doWork", "()V");
        InjectableCode code = createExpressionCode("log:test");
        InjectionPoint point = new InjectionPoint(target, code);

        DefaultClassTransformer transformer = new DefaultClassTransformer();
        TransformerResult result = transformer.transform(point, "TestService", bytecode);
        assertTrue(result.isTransformed());
    }

    private InjectableCode createExpressionCode(String expression) {
        return new InjectableCode() {
            @Override
            public String getCode() {
                return expression;
            }
            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };
    }

    private byte[] generateSimpleTestClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestService", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private byte[] generateTestClassWithLineNumber() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "TestService", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "doWork", "()V", null, null);
        mv.visitCode();
        mv.visitLineNumber(12, new Label());
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
