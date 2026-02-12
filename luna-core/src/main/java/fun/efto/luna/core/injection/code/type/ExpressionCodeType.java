package fun.efto.luna.core.injection.code.type;

import fun.efto.luna.core.injection.code.ExpressBaseInjectableCode;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 15:38
 */
public class ExpressionCodeType extends CodeType {
    public static final ExpressionCodeType EXPRESSION = new ExpressionCodeType("EXPRESSION", "基于表达式").register();

    public ExpressionCodeType(String name, String description) {
        super(name, description, ExpressBaseInjectableCode.class);
    }

    @Override
    public ExpressionCodeType register() {
        super.register();
        return this;
    }
}
