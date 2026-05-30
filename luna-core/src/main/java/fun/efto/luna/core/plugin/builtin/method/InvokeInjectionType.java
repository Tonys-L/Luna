package fun.efto.luna.core.plugin.builtin.method;

import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.injection.target.MethodTarget;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 00:00
 */
public class InvokeInjectionType extends InjectionType {

    public static final InvokeInjectionType INVOKE = new InvokeInjectionType("invoke", "子函数调用拦截注入", "invoke", "INVOKE_METHOD");

    private final List<String> aliases;

    private InvokeInjectionType(String name, String description, String... aliases) {
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
