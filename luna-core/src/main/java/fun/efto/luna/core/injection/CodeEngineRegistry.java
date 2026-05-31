package fun.efto.luna.core.injection;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 10:00
 */
public final class CodeEngineRegistry {

    private static final Map<String, CodeEngine> REGISTRY = new ConcurrentHashMap<>();
    private static final CodeEngineRegistry INSTANCE = new CodeEngineRegistry();

    private CodeEngineRegistry() {}

    public static CodeEngineRegistry getInstance() {
        return INSTANCE;
    }

    public CodeEngine register(CodeEngine engine) {
        REGISTRY.put(engine.getCodeType().toUpperCase(), engine);
        return engine;
    }

    public Optional<CodeEngine> get(String codeType) {
        if (codeType == null) return Optional.empty();
        return Optional.ofNullable(REGISTRY.get(codeType.toUpperCase()));
    }

    public void unregister(CodeEngine engine) {
        REGISTRY.remove(engine.getCodeType().toUpperCase());
    }

    public void unregisterAll(Collection<CodeEngine> engines) {
        engines.forEach(e -> REGISTRY.remove(e.getCodeType().toUpperCase()));
    }

    public Collection<CodeEngine> getAll() {
        return REGISTRY.values();
    }

    public void clear() {
        REGISTRY.clear();
    }
}
