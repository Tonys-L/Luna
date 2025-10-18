package fun.efto.luna.core.init;

import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.code.type.ExpressionCodeType;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 17:36
 */
public class DefaultInitializer implements Initializer {
    @Override
    public void initialize() {
        InjectionType.createByType(MethodInjectionType.ENTER_METHOD, MethodInjectionType.ENTER_METHOD_DESCRIPTION, MethodInjectionType.class).register();
        InjectionType.createByType(MethodInjectionType.EXIT_METHOD, MethodInjectionType.EXIT_METHOD_DESCRIPTION, MethodInjectionType.class).register();
        InjectionType.createByType(MethodInjectionType.AROUND_METHOD, MethodInjectionType.AROUND_METHOD_DESCRIPTION, MethodInjectionType.class).register();
        InjectionType.createByType(LineNumberInjectionType.BEFORE_LINE, LineNumberInjectionType.BEFORE_LINE_DESCRIPTION, LineNumberInjectionType.class).register();
        InjectionType.createByType(LineNumberInjectionType.AFTER_LINE, LineNumberInjectionType.AFTER_LINE_DESCRIPTION, LineNumberInjectionType.class).register();
        CodeType.createByType(ExpressionCodeType.EXPRESSION, ExpressionCodeType.EXPRESSION_DESCRIPTION, ExpressionCodeType.class).register();
    }
}
