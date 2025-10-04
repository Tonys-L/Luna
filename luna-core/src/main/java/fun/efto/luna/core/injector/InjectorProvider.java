package fun.efto.luna.core.injector;

import fun.efto.luna.core.injection.target.type.InjectionType;

import java.lang.instrument.Instrumentation;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 2:58
 */
public interface InjectorProvider {

    boolean supports(InjectionType type);

    BytecodeInjector createInjector(Instrumentation inst);

    String getName();
}
