/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 00:00
 */
package fun.efto.luna.core.plugin.builtin.method;

import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.injection.target.MethodTarget;

import java.util.Arrays;
import java.util.List;

public class ExceptionExitInjectionType extends InjectionType {

    public static final ExceptionExitInjectionType EXCEPTION_EXIT = new ExceptionExitInjectionType("exception_exit", "方法异常退出注入", "exception_exit", "EXCEPTION_EXIT_METHOD");

    private final List<String> aliases;

    private ExceptionExitInjectionType(String name, String description, String... aliases) {
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
