/**
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InjectionPointTest {

    private InjectableCode injectableCode;
    private InjectionTarget injectionTarget;
    private InjectionType methodInjectionType;
    private InjectionType lineNumberInjectionType;

    @BeforeEach
    public void setUp() {
        // 创建测试对象
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

        methodInjectionType = new MethodInjectionType();
        injectionTarget = new MethodTarget((MethodInjectionType) methodInjectionType, "com.example.TestClass", "testMethod", "()V");
        lineNumberInjectionType = new LineNumberInjectionType(10);
    }

    @Test
    public void testInjectableCode() {
        // 测试 InjectableCode 接口
        assertNotNull(injectableCode, "InjectableCode 实例应该不为空");
        assertEquals("System.out.println(\"Hello, Luna!\");", injectableCode.getCode(), "代码内容应该正确");
        assertEquals(CodeType.JAVA, injectableCode.getCodeType(), "代码类型应该正确");
    }

    @Test
    public void testInjectionTarget() {
        // 测试 InjectionTarget 类
        assertNotNull(injectionTarget, "InjectionTarget 实例应该不为空");
        assertEquals("com.example.TestClass", injectionTarget.getClassName(), "类名应该正确");
        assertEquals("testMethod", injectionTarget.getMethodName(), "方法名应该正确");
        assertEquals("()V", injectionTarget.getMethodDescriptor(), "方法描述符应该正确");
    }

    @Test
    public void testMethodInjectionType() {
        // 测试 MethodInjectionType 类
        assertNotNull(methodInjectionType, "MethodInjectionType 实例应该不为空");
        assertEquals("method", methodInjectionType.getName(), "注入类型名称应该正确");
    }

    @Test
    public void testLineNumberInjectionType() {
        // 测试 LineNumberInjectionType 类
        assertNotNull(lineNumberInjectionType, "LineNumberInjectionType 实例应该不为空");
        assertEquals("lineNumber", lineNumberInjectionType.getName(), "注入类型名称应该正确");
        
        // 验证行号
        LineNumberInjectionType lineType = (LineNumberInjectionType) lineNumberInjectionType;
        assertEquals(10, lineType.getLineNumber(), "行号应该正确");
    }
}
