package fun.efto.luna.core.injection.target.type;

import fun.efto.luna.core.injection.target.MethodTarget;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:34
 */
public class MethodInjectionType extends InjectionType {
    public static final String ENTER_METHOD = "ENTER_METHOD";
    public static final String ENTER_METHOD_DESCRIPTION = "方法进入前注入";
    public static final String EXIT_METHOD = "EXIT_METHOD";
    public static final String EXIT_METHOD_DESCRIPTION = "方法退出后注入";
    public static final String AROUND_METHOD = "AROUND_METHOD";
    public static final String AROUND_METHOD_DESCRIPTION = "方法环绕注入";

    public MethodInjectionType(String name, String description) {
        super(name, description, MethodTarget.class);
    }
}
