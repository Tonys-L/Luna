package fun.efto.luna.core.injection.target.type;

/**
 * 方法级注入类型
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/03/29 02:30
 */
public class MethodInjectionType extends InjectionType {

    public static final MethodInjectionType ENTER = new MethodInjectionType("method_enter", "方法进入注入");
    public static final MethodInjectionType EXIT = new MethodInjectionType("method_exit", "方法退出注入");
    public static final MethodInjectionType AROUND = new MethodInjectionType("method_around", "方法环绕注入");

    private final String name;
    private final String description;

    private MethodInjectionType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
