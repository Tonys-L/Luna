package fun.efto.luna.core.plugin.builtin.line;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.plugin.InjectionRuleConverter;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/24 10:00
 */
public class LineRuleConverter implements InjectionRuleConverter {

    @Override
    public InjectionPoint convert(PersistentInjection injection) {
        InjectionLocation location = InjectionTypeRegistry.getInstance().resolve(injection.getInjectionLocation());
        InjectionTarget target = new LineNumberTarget(location, injection.getClazz(), injection.getLineNumber(), 0);
        InjectableCode code = InjectableCode.of(injection.getCode(), injection.getCodeType());
        return new InjectionPoint(injection.getId(), target, code);
    }
}
