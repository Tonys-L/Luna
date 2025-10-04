package fun.efto.luna.core.injection.target.type;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:34
 */
public class LineNumberInjectionType extends InjectionType {
    public static final InjectionType BEFORE_LINE = create("BEFORE_LINE", "在指定行号之前注入").register();
    public static final InjectionType AFTER_LINE = create("AFTER_LINE", "在指定行号之后注入").register();

    public LineNumberInjectionType(String name, String description) {
        super(name, description);
    }
}
