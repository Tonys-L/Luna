package fun.efto.luna.core.injection.target.type;

import fun.efto.luna.core.injection.target.LineNumberTarget;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:34
 */
public class LineNumberInjectionType extends InjectionType {
    public static final String BEFORE_LINE = "BEFORE_LINE";
    public static final String BEFORE_LINE_DESCRIPTION = "在行前注入";

    public static final String AFTER_LINE = "AFTER_LINE";
    public static final String AFTER_LINE_DESCRIPTION = "在行后注入";

    public LineNumberInjectionType(String name, String description) {
        super(name, description, LineNumberTarget.class);
    }
}
