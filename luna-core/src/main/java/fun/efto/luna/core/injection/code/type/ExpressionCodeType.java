package fun.efto.luna.core.injection.code.type;

import fun.efto.luna.core.injection.code.ExpressBaseInjectableCode;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 15:38
 */
public class ExpressionCodeType extends CodeType {
    public static final String EXPRESSION = "EXPRESSION";
    public static final String EXPRESSION_DESCRIPTION = "基于表达式";
    public ExpressionCodeType(String name, String description) {
        super(name, description, ExpressBaseInjectableCode.class);
    }
}
