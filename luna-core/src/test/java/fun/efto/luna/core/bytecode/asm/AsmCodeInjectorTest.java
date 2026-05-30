package fun.efto.luna.core.bytecode.asm;

import fun.efto.luna.core.TestSetup;
import fun.efto.luna.core.bytecode.asm.assembler.ExpressionBytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.injection.InjectionContext;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionType;
import fun.efto.luna.core.testing.LineInjectionTestHelper;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.CodeInjector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/31 21:00
 */
@DisplayName("AsmCodeInjector 测试")
class AsmCodeInjectorTest {

    private static byte[] bytecode;
    private static int firstLine;

    @BeforeAll
    static void setUp() {
        TestSetup.init();
        bytecode = LineInjectionTestHelper.getClassBytecode(
                LineInjectionTestHelper.TestTargetService.class);
        firstLine = LineInjectionTestHelper.findFirstMethodLine(bytecode, "processWithLoop");
    }

    @Nested
    @DisplayName("接口实现")
    class InterfaceTests {

        @Test
        @DisplayName("AsmCodeInjector 实现 CodeInjector 接口")
        void testImplementsInterface() {
            BytecodeInjector bytecodeInjector = (ctx, bytes, assembler) -> bytes;
            CodeInjector injector = new AsmCodeInjector(bytecodeInjector, new ExpressionBytecodeAssembler());
            assertNotNull(injector);
            assertTrue(injector instanceof CodeInjector);
        }
    }

    @Nested
    @DisplayName("inject 委托")
    class InjectDelegationTests {

        @Test
        @DisplayName("inject 委托给 BytecodeInjector 并返回结果")
        void testInjectDelegates() {
            byte[] injectedBytecode = new byte[]{1, 2, 3};
            BytecodeInjector bytecodeInjector = (ctx, bytes, assembler) -> injectedBytecode;

            CodeInjector injector = new AsmCodeInjector(bytecodeInjector, new ExpressionBytecodeAssembler());

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE,
                    LineInjectionTestHelper.TestTargetService.class.getName(),
                    firstLine, 0, "processWithLoop", "(I)V");
            InjectionPoint point = new InjectionPoint(target, createCode("log:test"));

            byte[] result = injector.inject(point, bytecode);

            assertArrayEquals(injectedBytecode, result,
                    "inject 应委托给 BytecodeInjector 并返回其结果");
        }

        @Test
        @DisplayName("inject 使用真实 BytecodeInjector 产生有效字节码")
        void testInjectWithRealInjector() {
            BytecodeInjector realInjector = BytecodeInjectorRegistry.getInstance()
                    .get(LineNumberInjectionType.BEFORE).get();

            CodeInjector injector = new AsmCodeInjector(realInjector, new ExpressionBytecodeAssembler());

            LineNumberTarget target = new LineNumberTarget(
                    LineNumberInjectionType.BEFORE,
                    LineInjectionTestHelper.TestTargetService.class.getName(),
                    firstLine, 0, "processWithLoop", "(I)V");
            InjectionPoint point = new InjectionPoint(target, createCode("log:test"));

            byte[] result = injector.inject(point, bytecode);

            assertNotNull(result);
            assertTrue(result.length > 0, "注入后的字节码不应为空");
            assertNotEquals(bytecode.length, 0, "原始字节码长度不为零");
        }
    }

    private static InjectableCode createCode(String codeStr) {
        return new InjectableCode() {
            @Override
            public String getCode() {
                return codeStr;
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };
    }
}
