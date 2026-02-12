package fun.efto.luna.core.injection.target.type;

import fun.efto.luna.core.injection.target.MethodTarget;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:34
 */
public class MethodInjectionType extends InjectionType {
    public static final MethodInjectionType ENTER = new MethodInjectionType("ENTER_METHOD", "方法进入前注入").register();
    public static final MethodInjectionType EXIT = new MethodInjectionType("EXIT_METHOD", "方法退出后注入").register();
    public static final MethodInjectionType AROUND = new MethodInjectionType("AROUND_METHOD", "方法环绕注入").register();

    public MethodInjectionType(String name, String description) {
        super(name, description, MethodTarget.class);
    }

    @Override
    public MethodInjectionType register() {
        super.register();
        return this;
    }
}
