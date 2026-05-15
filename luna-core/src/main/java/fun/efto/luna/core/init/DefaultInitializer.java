package fun.efto.luna.core.init;

import fun.efto.luna.core.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.analyzer.AnalyzerType;
import fun.efto.luna.core.asm.analyzer.AsmClassAnalyzer;
import fun.efto.luna.core.plugin.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 17:36
 */
public class DefaultInitializer implements Initializer {
    @Override
    public void initialize() {
        AnalyzerRegistry analyzerRegistry = AnalyzerRegistry.getInstance();
        analyzerRegistry.register(new AnalyzerType("ASM", "ASM字节码分析器").register(), new AsmClassAnalyzer());

        PluginManagerImpl pluginManager = new PluginManagerImpl(
            new ReadyGate(),
            new DefaultLogEmitter(),
            null,
            null,
            null,
            null
        );

        List<LunaPlugin> discoveredPlugins = new ArrayList<>();
        ServiceLoader.load(LunaPlugin.class).forEach(discoveredPlugins::add);
        pluginManager.initializeAll(discoveredPlugins);
    }
}
