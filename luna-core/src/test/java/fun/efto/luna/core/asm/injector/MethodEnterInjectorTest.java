/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/03/29 02:30
 */
package fun.efto.luna.core.asm.injector;

import fun.efto.luna.core.InjectionContext;
import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MethodEnterInjectorTest {

    private MethodEnterInjector injector;

    @BeforeEach
    public void setUp() {
        injector = new MethodEnterInjector();
    }

    @Test
    public void testInject() {
        byte[] originalBytecode = new byte[0];

        MethodInjectionType injectionType = MethodInjectionType.ENTER;
        InjectionTarget target = new MethodTarget(injectionType, "TestClass", "testMethod", "()V");

        InjectableCode code = new InjectableCode() {
            @Override
            public String getCode() {
                return "System.out.println(\"Hello\");";
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.JAVA;
            }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, code) {
            @Override
            public fun.efto.luna.core.injection.target.type.InjectionType getInjectionType() {
                return MethodInjectionType.ENTER;
            }
        };

        InjectionContext context = new InjectionContext(injectionPoint);
        BytecodeAssembler assembler = null;

        byte[] result = injector.inject(context, originalBytecode, assembler);

        assertNotNull(result, "注入结果应该不为 null");
        assertSame(originalBytecode, result, "暂时应该返回原字节码");
    }
}
