package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.InjectableCode;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:22
 */
public class CodeCompiler {
    private static final List<CodeCompilerStrategy> STRATEGIES = new CopyOnWriteArrayList<>();

    public static void register(CodeCompilerStrategy strategy) {
        STRATEGIES.add(strategy);
    }

    public static void unregister(CodeCompilerStrategy strategy) {
        STRATEGIES.remove(strategy);
    }

    public static List<CodeCompilerStrategy> getStrategies() {
        return STRATEGIES;
    }

    public static InjectableCode compile(PersistentInjection persistent) {
        for (CodeCompilerStrategy strategy : STRATEGIES) {
            if (strategy.supports(persistent.getCodeType(), persistent.getCode())) {
                return strategy.compile(persistent);
            }
        }
        throw new IllegalArgumentException("No CodeCompilerStrategy found for codeType: " 
                + persistent.getCodeType() + ", code: " + persistent.getCode());
    }
}
