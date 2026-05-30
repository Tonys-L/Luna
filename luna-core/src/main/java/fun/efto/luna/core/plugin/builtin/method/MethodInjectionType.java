package fun.efto.luna.core.plugin.builtin.method;

import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.injection.target.MethodTarget;

import java.util.Arrays;
import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class MethodInjectionType extends InjectionType {

    public static final MethodInjectionType ENTER = new MethodInjectionType("method_enter", "方法进入注入", "enter", "method_enter", "ENTER_METHOD");
    public static final MethodInjectionType EXIT  = new MethodInjectionType("method_exit", "方法退出注入", "exit", "method_exit", "EXIT_METHOD");
    public static final MethodInjectionType AROUND = new MethodInjectionType("method_around", "方法环绕注入", "around", "method_around", "AROUND_METHOD");

    private final List<String> aliases;

    private MethodInjectionType(String name, String description, String... aliases) {
        super(name, description);
        this.aliases = Arrays.asList(aliases);
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public InjectionTarget createTarget(String className, String methodName, String methodDescriptor, Integer lineNumber) {
        return new MethodTarget(this, className, methodName, methodDescriptor);
    }
}
