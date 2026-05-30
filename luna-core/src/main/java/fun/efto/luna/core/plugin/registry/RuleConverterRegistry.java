package fun.efto.luna.core.plugin.registry;

import fun.efto.luna.core.type.Registry;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.plugin.InjectionRuleConverter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/24 10:00
 */
public final class RuleConverterRegistry implements Registry<InjectionType, InjectionRuleConverter> {

    private static final Map<InjectionType, InjectionRuleConverter> REGISTRY = new ConcurrentHashMap<>();
    private static final RuleConverterRegistry INSTANCE = new RuleConverterRegistry();

    private RuleConverterRegistry() {}

    public static RuleConverterRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<InjectionType, InjectionRuleConverter> getRegistry() {
        return REGISTRY;
    }

    @Override
    public InjectionRuleConverter register(InjectionType type, InjectionRuleConverter converter) {
        REGISTRY.put(type, converter);
        return converter;
    }

    @Override
    public Optional<InjectionRuleConverter> get(InjectionType type) {
        return Optional.ofNullable(REGISTRY.get(type));
    }

    public InjectionPoint convert(PersistentInjection injection) {
        InjectionType type = InjectionTypeRegistry.getInstance().resolve(injection.getInjectionType());
        InjectionRuleConverter converter = REGISTRY.get(type);
        if (converter == null) {
            throw new IllegalStateException("No InjectionRuleConverter registered for type: " + type.getName());
        }
        return converter.convert(injection);
    }

    public void unregisterAll(Map<InjectionType, InjectionRuleConverter> entries) {
        entries.keySet().forEach(REGISTRY::remove);
    }

    public void clear() {
        REGISTRY.clear();
    }
}
