package fun.efto.luna.core.injection.target.type;

import fun.efto.luna.core.injection.target.LineNumberTarget;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:34
 */
public class LineNumberInjectionType extends InjectionType {

    public static final LineNumberInjectionType BEFORE = new LineNumberInjectionType("BEFORE_LINE", "在行前注入").register();
    public static final LineNumberInjectionType AFTER = new LineNumberInjectionType("AFTER_LINE", "在行后注入").register();

    public LineNumberInjectionType(String name, String description) {
        super(name, description, LineNumberTarget.class);
    }

    @Override
    public LineNumberInjectionType register() {
        super.register();
        return this;
    }
}
