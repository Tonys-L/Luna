package fun.efto.luna.core.analyzer;

import fun.efto.luna.core.type.RegisterableType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 15:59
 */
public class AnalyzerType extends RegisterableType<AnalyzerType> {
    private static final Map<String, RegisterableType<AnalyzerType>> REGISTRY = new ConcurrentHashMap<>();

    public AnalyzerType(String name, String description) {
        super(name, description);
    }

    public static AnalyzerType valueOf(String name) {
        return (AnalyzerType) REGISTRY.get(name);
    }

    @Override
    public Map<String, RegisterableType<AnalyzerType>> getRegistry() {
        return REGISTRY;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
