package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionLocation;
import fun.efto.luna.core.plugin.builtin.line.LineNumberInjectionLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since : 2026/03/29 02:30
 */
public class InjectionPointTest {

    private InjectableCode injectableCode;
    private InjectionTarget injectionTarget;
    private InjectionLocation methodInjectionLocation;
    private InjectionLocation lineNumberInjectionLocation;

    @BeforeEach
    public void setUp() {
        injectableCode = new InjectableCode() {
            @Override
            public String getCode() {
                return "System.out.println(\"Hello, Luna!\");";
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.JAVA;
            }
        };

        methodInjectionLocation = MethodInjectionLocation.ENTER;
        injectionTarget = new MethodTarget((MethodInjectionLocation) methodInjectionLocation, "com.example.TestClass", "testMethod", "()V");
        lineNumberInjectionLocation = LineNumberInjectionLocation.BEFORE.withLineNumber(10);
    }

    @Test
    public void testInjectableCode() {
        assertNotNull(injectableCode, "InjectableCode 实例应该不为空");
        assertEquals("System.out.println(\"Hello, Luna!\");", injectableCode.getCode(), "代码内容应该正确");
        assertEquals(CodeType.JAVA, injectableCode.getCodeType(), "代码类型应该正确");
    }

    @Test
    public void testInjectionTarget() {
        assertNotNull(injectionTarget, "InjectionTarget 实例应该不为空");
        assertEquals("com.example.TestClass", injectionTarget.getClassName(), "类名应该正确");
        assertEquals("testMethod", injectionTarget.getMethodName(), "方法名应该正确");
        assertEquals("()V", injectionTarget.getMethodDescriptor(), "方法描述符应该正确");
    }

    @Test
    public void testMethodInjectionLocationUniqueNames() {
        assertNotEquals(MethodInjectionLocation.ENTER.getName(), MethodInjectionLocation.EXIT.getName(),
                "ENTER 和 EXIT 的名称应该不同");
        assertNotEquals(MethodInjectionLocation.ENTER.getName(), MethodInjectionLocation.AROUND.getName(),
                "ENTER 和 AROUND 的名称应该不同");
        assertNotEquals(MethodInjectionLocation.EXIT.getName(), MethodInjectionLocation.AROUND.getName(),
                "EXIT 和 AROUND 的名称应该不同");
        assertEquals("method_enter", MethodInjectionLocation.ENTER.getName());
        assertEquals("method_exit", MethodInjectionLocation.EXIT.getName());
        assertEquals("method_around", MethodInjectionLocation.AROUND.getName());
    }

    @Test
    public void testLineNumberInjectionLocationUniqueNames() {
        assertNotEquals(LineNumberInjectionLocation.BEFORE.getName(), LineNumberInjectionLocation.AFTER.getName(),
                "BEFORE 和 AFTER 的名称应该不同");
        assertEquals("line_before", LineNumberInjectionLocation.BEFORE.getName());
        assertEquals("line_after", LineNumberInjectionLocation.AFTER.getName());
    }

    @Test
    public void testLineNumberInjectionLocationWithLineNumber() {
        LineNumberInjectionLocation lineType = LineNumberInjectionLocation.BEFORE.withLineNumber(10);
        assertEquals("line_before", lineType.getName());
        assertEquals(10, lineType.getLineNumber());
    }

    @Test
    public void testInjectionLocationNotEqual() {
        assertNotEquals(MethodInjectionLocation.ENTER, MethodInjectionLocation.EXIT,
                "不同的 InjectionLocation 不应该相等");
        assertNotEquals(MethodInjectionLocation.ENTER, LineNumberInjectionLocation.BEFORE,
                "不同类型的 InjectionLocation 不应该相等");
    }
}
