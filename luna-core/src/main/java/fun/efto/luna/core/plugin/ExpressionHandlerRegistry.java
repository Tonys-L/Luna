package fun.efto.luna.core.plugin;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public final class ExpressionHandlerRegistry {

    private static final ConcurrentHashMap<String, ExpressionHandler> HANDLERS = new ConcurrentHashMap<>();

    private ExpressionHandlerRegistry() {}

    public static void register(ExpressionHandler handler) {
        HANDLERS.put(handler.getProtocol(), handler);
    }

    public static ExpressionHandler get(String protocol) {
        return HANDLERS.get(protocol);
    }

    public static boolean hasProtocol(String content) {
        if (content == null || content.isEmpty()) return false;
        int colon = content.indexOf(':');
        if (colon <= 0) return false;
        return HANDLERS.containsKey(content.substring(0, colon));
    }

    public static void unregisterAll(Collection<ExpressionHandler> handlers) {
        handlers.forEach(h -> HANDLERS.remove(h.getProtocol()));
    }

    public static Collection<ExpressionHandler> getAll() {
        return HANDLERS.values();
    }

    public static void clear() {
        HANDLERS.clear();
    }
}
