/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 02:00
 */
package fun.efto.luna.core.bytecode.bytekit;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.bytecode.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.bootstrap.capability.CoreCapabilityRegistry;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionQuery;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionType;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import fun.efto.luna.core.plugin.builtin.log.LogExpressionHandler;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotExpressionHandler;
import fun.efto.luna.core.plugin.builtin.trace.TraceExpressionHandler;
import fun.efto.luna.core.transformer.GlobalClassFileTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ByteKitRetransformTest {

    private static final String TARGET_CLASS = "RetransformTarget";
    private static final String TARGET_CLASS_INTERNAL = "RetransformTarget";

    private List<InjectionPoint> activePoints;

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

        activePoints = new ArrayList<>();
    }

    private InjectionQuery createQuery() {
        return new InjectionQuery() {
            @Override
            public List<InjectionPoint> getActivePointsForClass(String className) {
                if (className.equals(TARGET_CLASS) && !activePoints.isEmpty()) {
                    return activePoints;
                }
                return Collections.emptyList();
            }

            @Override
            public int getInjectionCount(String className) {
                return getActivePointsForClass(className).size();
            }

            @Override
            public List<InjectionPoint> getInjectionPoints(String className) {
                return getActivePointsForClass(className);
            }
        };
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
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, TARGET_CLASS_INTERNAL, null, "java/lang/Object", null);

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

    @Nested
    @DisplayName("GlobalClassFileTransformer 与 ByteKit 集成")
    class GlobalTransformerTests {

        @Test
        @DisplayName("GlobalClassFileTransformer 可通过 InjectionQuery 实例化")
        void testTransformerInstantiation() {
            InjectionQuery query = createQuery();
            assertDoesNotThrow(() -> new GlobalClassFileTransformer(query));
        }

        @Test
        @DisplayName("无激活注入点时 transform 返回 null（零开销）")
        void testNoInjectionPointsReturnsNull() {
            GlobalClassFileTransformer transformer = new GlobalClassFileTransformer(createQuery());
            byte[] bytecode = generateSimpleTestClass();

            byte[] result = transformer.transform(
                    null, TARGET_CLASS_INTERNAL, null, null, bytecode);

            assertNull(result, "无注入点时 transform 应返回 null");
        }

        @Test
        @DisplayName("有激活注入点时 transform 返回增强字节码")
        void testActiveInjectionPointsReturnsEnhancedBytecode() {
            MethodTarget target = new MethodTarget(MethodInjectionType.ENTER, TARGET_CLASS, "doWork", "()V");
            InjectableCode code = createExpressionCode("log:test");
            InjectionPoint point = new InjectionPoint(target, code);
            activePoints.add(point);

            GlobalClassFileTransformer transformer = new GlobalClassFileTransformer(createQuery());
            byte[] bytecode = generateSimpleTestClass();

            byte[] result = transformer.transform(
                    null, TARGET_CLASS_INTERNAL, null, null, bytecode);

            assertNotNull(result, "有注入点时 transform 应返回增强字节码");
            assertTrue(result.length > 0, "增强字节码长度应大于 0");
            assertNotEquals(bytecode.length, result.length, "增强字节码长度应与原始不同");
        }
    }
}
