package fun.efto.luna.core.plugin.registry;

import fun.efto.luna.core.infra.type.Registry;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.InjectionRuleConverter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/24 10:00
 */
public final class RuleConverterRegistry implements Registry<InjectionLocation, InjectionRuleConverter> {

    private static final Map<InjectionLocation, InjectionRuleConverter> REGISTRY = new ConcurrentHashMap<>();
    private static final RuleConverterRegistry INSTANCE = new RuleConverterRegistry();

    private RuleConverterRegistry() {}

    public static RuleConverterRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<InjectionLocation, InjectionRuleConverter> getRegistry() {
        return REGISTRY;
    }

    @Override
    public InjectionRuleConverter register(InjectionLocation location, InjectionRuleConverter converter) {
        REGISTRY.put(location, converter);
        return converter;
    }

    @Override
    public Optional<InjectionRuleConverter> get(InjectionLocation location) {
        return Optional.ofNullable(REGISTRY.get(location));
    }

    public InjectionPoint convert(PersistentInjection injection) {
        InjectionLocation location = InjectionTypeRegistry.getInstance().resolve(injection.getInjectionLocation());
        InjectionRuleConverter converter = REGISTRY.get(location);
        if (converter == null) {
            throw new IllegalStateException("No InjectionRuleConverter registered for location: " + location.getName());
        }
        return converter.convert(injection);
    }

    public void unregisterAll(Map<InjectionLocation, InjectionRuleConverter> entries) {
        entries.keySet().forEach(REGISTRY::remove);
    }

    public void clear() {
        REGISTRY.clear();
    }
}
