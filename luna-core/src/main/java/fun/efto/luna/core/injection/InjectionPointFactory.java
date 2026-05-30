/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:40
 */
package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;

public class InjectionPointFactory {

    public static InjectionPoint create(PersistentInjection persistent) {
        InjectionType type = InjectionTypeRegistry.getInstance().resolve(persistent.getInjectionType());
        if (type == null) {
            throw new IllegalArgumentException("Unknown injection type: " + persistent.getInjectionType());
        }
        if (persistent.getClazz() == null || persistent.getClazz().isEmpty()) {
            throw new IllegalArgumentException("Missing className for injection: " + persistent.getId());
        }

        InjectionTarget target = type.createTarget(
            persistent.getClazz(),
            persistent.getMethodName(),
            persistent.getMethodDescriptor() != null ? persistent.getMethodDescriptor() : "",
            persistent.getLineNumber()
        );

        InjectableCode code = CodeCompiler.compile(persistent);
        return new InjectionPoint(persistent.getId(), target, code);
    }
}
