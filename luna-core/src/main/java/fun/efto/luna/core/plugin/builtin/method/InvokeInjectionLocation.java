package fun.efto.luna.core.plugin.builtin.method;

import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.injection.target.MethodTarget;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/06/01 00:00
 */
public class InvokeInjectionLocation extends InjectionLocation {

    public static final InvokeInjectionLocation INVOKE = new InvokeInjectionLocation("invoke", "子函数调用拦截注入", "method", "invoke", "INVOKE_METHOD");

    private final List<String> aliases;

    private InvokeInjectionLocation(String name, String description, String category, String... aliases) {
        super(name, description, category);
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
