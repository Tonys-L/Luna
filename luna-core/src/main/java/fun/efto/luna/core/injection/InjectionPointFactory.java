package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:40
 */
public class InjectionPointFactory {

    public static InjectionPoint create(PersistentInjection persistent) {
        InjectionLocation location = InjectionTypeRegistry.getInstance().resolve(persistent.getInjectionLocation());
        if (location == null) {
            throw new IllegalArgumentException("Unknown injection location: " + persistent.getInjectionLocation());
        }
        if (persistent.getClazz() == null || persistent.getClazz().isEmpty()) {
            throw new IllegalArgumentException("Missing className for injection: " + persistent.getId());
        }

        InjectionTarget target = location.createTarget(
            persistent.getClazz(),
            persistent.getMethodName(),
            persistent.getMethodDescriptor() != null ? persistent.getMethodDescriptor() : "",
            persistent.getLineNumber()
        );

        InjectableCode code = CodeCompiler.compile(persistent);
        return new InjectionPoint(persistent.getId(), target, code);
    }
}
