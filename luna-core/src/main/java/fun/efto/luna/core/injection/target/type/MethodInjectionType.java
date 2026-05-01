package fun.efto.luna.core.injection.target.type;

/**
 * 方法级注入类型
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class MethodInjectionType extends InjectionType {

    public static final MethodInjectionType ENTER = new MethodInjectionType();
    public static final MethodInjectionType EXIT = new MethodInjectionType();
    public static final MethodInjectionType AROUND = new MethodInjectionType();

    @Override
    public String getName() {
        return "method";
    }

    @Override
    public String getDescription() {
        return "方法级注入";
    }
}
