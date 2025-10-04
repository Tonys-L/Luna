package fun.efto.luna.core.injection.target.type;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:34
 */
public class MethodInjectionType extends InjectionType {
    public static final InjectionType ENTER_METHOD = create("ENTER_METHOD", "方法进入前注入").register();
    public static final InjectionType EXIT_METHOD = create("EXIT_METHOD", "方法退出后注入").register();
    public static final InjectionType AROUND_METHOD = create("AROUND_METHOD", "方法环绕注入").register();

    public MethodInjectionType(String name, String description) {
        super(name, description);
    }
}
