package fun.efto.luna.core.testing;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.line.AfterLineInjector;
import fun.efto.luna.core.plugin.builtin.line.BeforeLineInjector;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionLocation;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.probe.ProbeOutput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

import static fun.efto.luna.core.testing.LineInjectionTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/30 19:00
 */
@DisplayName("行号注入边界条件测试")
public class LineNumberEdgeCaseTest {

    @BeforeAll
    static void setUp() {
        TestSetup.init();
    }

    @BeforeEach
    void clearBuffer() {
        LineInjectionTestHelper.clearProbeBuffer();
    }

    @Test
    @DisplayName("超出方法行范围的行号应抛出 RuntimeException")
    void testLineNumberOutOfRange() {
        byte[] bytecode = LineInjectionTestHelper.getClassBytecode(TestTargetService.class);
        int outOfRangeLine = 99999;

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionLocation.BEFORE, TestTargetService.class.getName(),
                outOfRangeLine, 0, "processWithLoop", "(I)V");
        InjectableCode code = LineInjectionTestHelper.createCode("log:test");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BeforeLineInjector injector = new BeforeLineInjector();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler()));
        assertTrue(ex.getMessage().contains("not found"),
                "异常消息应包含 'not found'，实际为: " + ex.getMessage());
    }

    @Test
    @DisplayName("无 LineNumberTable 的类应抛出异常")
    void testNoDebugInfoClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/test/NoDebugClass", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "test", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();

        cw.visitEnd();
        byte[] noDebugBytecode = cw.toByteArray();

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionLocation.BEFORE, "com.test.NoDebugClass",
                1, 0, "test", "()V");
        InjectableCode code = LineInjectionTestHelper.createCode("log:test");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BeforeLineInjector injector = new BeforeLineInjector();
        assertThrows(RuntimeException.class,
                () -> injector.inject(ctx, noDebugBytecode, new ExpressionBytecodeAssembler()));
    }

    @Test
    @DisplayName("多 return 方法 line_after 注入应不抛异常")
    void testMultiReturnLineAfter() throws Exception {
        byte[] bytecode = LineInjectionTestHelper.getClassBytecode(TestTargetService.class);
        int returnLine = LineInjectionTestHelper.findLineByOffset(bytecode, "processWithMultiReturn", 3);

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionLocation.AFTER, TestTargetService.class.getName(),
                returnLine, 0, "processWithMultiReturn", "(I)I");
        InjectableCode code = LineInjectionTestHelper.createCode("log:test");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        AfterLineInjector injector = new AfterLineInjector();
        byte[] result = assertDoesNotThrow(
                () -> injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler()));
        assertNotNull(result);
    }

    @Test
    @DisplayName("synchronized 块内 line_before 注入应成功且方法正常执行")
    void testSynchronizedBlockInjection() throws Exception {
        byte[] bytecode = LineInjectionTestHelper.getClassBytecode(TestTargetService.class);
        int line = LineInjectionTestHelper.findFirstMethodLine(bytecode, "processWithSync");

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionLocation.BEFORE, TestTargetService.class.getName(),
                line, 0, "processWithSync", "()V");
        InjectableCode code = LineInjectionTestHelper.createCode("log:sync-test");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BeforeLineInjector injector = new BeforeLineInjector();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

        Class<?> loaded = LineInjectionTestHelper.injectAndLoad(result, TestTargetService.class.getName());
        Object instance = loaded.getDeclaredConstructor().newInstance();
        Method method = loaded.getMethod("processWithSync");
        assertDoesNotThrow(() -> method.invoke(instance));
    }

    @Test
    @DisplayName("静态方法 line_before 注入应成功")
    void testStaticMethodInjection() throws Exception {
        byte[] bytecode = LineInjectionTestHelper.getClassBytecode(TestTargetService.class);
        int line = LineInjectionTestHelper.findFirstMethodLine(bytecode, "staticMethod");

        LineNumberTarget target = new LineNumberTarget(
                LineNumberInjectionLocation.BEFORE, TestTargetService.class.getName(),
                line, 0, "staticMethod", "(I)I");
        InjectableCode code = LineInjectionTestHelper.createCode("log:static-test");
        InjectionPoint ip = new InjectionPoint(target, code);
        InjectionContext ctx = new InjectionContext(ip);

        BeforeLineInjector injector = new BeforeLineInjector();
        byte[] result = injector.inject(ctx, bytecode, new ExpressionBytecodeAssembler());

        Class<?> loaded = LineInjectionTestHelper.injectAndLoad(result, TestTargetService.class.getName());
        Method method = loaded.getMethod("staticMethod", int.class);
        Object returnValue = method.invoke(null, 5);
        assertEquals(50, returnValue);
    }
}
