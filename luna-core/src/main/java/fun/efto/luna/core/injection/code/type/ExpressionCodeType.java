package fun.efto.luna.core.injection.code.type;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 15:38
 */
public class ExpressionCodeType extends CodeType {
    public static final CodeType EXPRESSION = create("EXPRESSION", "基于表达式").register();

    public ExpressionCodeType(String name, String description) {
        super(name, description);
    }
}
