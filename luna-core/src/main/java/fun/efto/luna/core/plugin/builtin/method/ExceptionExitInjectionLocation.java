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
public class ExceptionExitInjectionLocation extends InjectionLocation {

    public static final ExceptionExitInjectionLocation EXCEPTION_EXIT = new ExceptionExitInjectionLocation("exception_exit", "方法异常退出注入", "method", "exception_exit", "EXCEPTION_EXIT_METHOD");

    private final List<String> aliases;

    private ExceptionExitInjectionLocation(String name, String description, String category, String... aliases) {
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
