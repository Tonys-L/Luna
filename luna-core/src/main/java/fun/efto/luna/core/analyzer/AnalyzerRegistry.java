package fun.efto.luna.core.analyzer;

import fun.efto.luna.core.Registry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 15:59
 */
public final class AnalyzerRegistry implements Registry<AnalyzerType, ClassAnalyzer> {
    private static final AnalyzerRegistry INSTANCE = new AnalyzerRegistry();
    private static final Map<AnalyzerType, ClassAnalyzer> ANALYZER_REGISTRY = new ConcurrentHashMap<>();

    private AnalyzerRegistry() {
    }

    public static AnalyzerRegistry getInstance() {
        return INSTANCE;
    }

    public Optional<ClassAnalyzer> getAnalyzer(String typeName) {
        AnalyzerType type = AnalyzerType.valueOf(typeName);
        if (type != null) {
            return get(type);
        }
        return Optional.empty();
    }

    @Override
    public Map<AnalyzerType, ClassAnalyzer> getRegistry() {
        return ANALYZER_REGISTRY;
    }
}
