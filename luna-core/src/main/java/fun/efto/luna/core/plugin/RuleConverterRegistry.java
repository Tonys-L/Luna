package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.rule.InjectionRule;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public final class RuleConverterRegistry {

    private static final ConcurrentHashMap<InjectionType, InjectionRuleConverter> REGISTRY = new ConcurrentHashMap<>();

    private RuleConverterRegistry() {}

    public static void register(InjectionType type, InjectionRuleConverter converter) {
        REGISTRY.put(type, converter);
    }

    public static InjectionPoint convert(InjectionRule rule) {
        InjectionType type = InjectionTypeRegistry.resolve(rule.getInjectionType());
        InjectionRuleConverter converter = REGISTRY.get(type);
        if (converter == null) {
            throw new IllegalStateException("No rule converter found for injection type: " + type.getName());
        }
        return converter.convert(rule);
    }

    public static void unregisterAll(Map<InjectionType, InjectionRuleConverter> entries) {
        entries.keySet().forEach(REGISTRY::remove);
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
