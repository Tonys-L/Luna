package fun.efto.luna.core.plugin.builtin.method;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.plugin.InjectionRuleConverter;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/24 10:00
 */
public class MethodRuleConverter implements InjectionRuleConverter {

    @Override
    public InjectionPoint convert(PersistentInjection injection) {
        InjectionLocation location = InjectionTypeRegistry.getInstance().resolve(injection.getInjectionLocation());
        InjectionTarget target = new MethodTarget(location, injection.getClazz(), injection.getMethodName(), injection.getMethodDescriptor());
        String condition = injection.getExpression() != null && !injection.getExpression().trim().isEmpty()
                ? "${" + injection.getExpression().trim() + "}" : null;
        CompiledCode code = new CompiledCode(condition, injection.getCode());
        return new InjectionPoint(injection.getId(), target, code, injection.getCodeType(), injection.getProbeType(), injection);
    }
}
