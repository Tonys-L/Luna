package fun.efto.luna.core.plugin.registry;

import fun.efto.luna.core.common.type.Registry;
import fun.efto.luna.core.plugin.ExpressionHandler;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public final class ExpressionHandlerRegistry implements Registry<String, ExpressionHandler> {

    private static final Map<String, ExpressionHandler> REGISTRY = new ConcurrentHashMap<>();
    private static final ExpressionHandlerRegistry INSTANCE = new ExpressionHandlerRegistry();

    private ExpressionHandlerRegistry() {}

    public static ExpressionHandlerRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public Map<String, ExpressionHandler> getRegistry() {
        return REGISTRY;
    }

    @Override
    public ExpressionHandler register(String protocol, ExpressionHandler handler) {
        REGISTRY.put(protocol, handler);
        return handler;
    }

    public ExpressionHandler register(ExpressionHandler handler) {
        REGISTRY.put(handler.getProtocol(), handler);
        return handler;
    }

    @Override
    public Optional<ExpressionHandler> get(String protocol) {
        return Optional.ofNullable(REGISTRY.get(protocol));
    }

    public boolean hasProtocol(String content) {
        if (content == null || content.isEmpty()) return false;
        int colon = content.indexOf(':');
        if (colon <= 0) return false;
        return REGISTRY.containsKey(content.substring(0, colon));
    }

    public void unregisterAll(Collection<ExpressionHandler> handlers) {
        handlers.forEach(h -> REGISTRY.remove(h.getProtocol()));
    }

    public Collection<ExpressionHandler> getAll() {
        return REGISTRY.values();
    }

    public void clear() {
        REGISTRY.clear();
    }
}
