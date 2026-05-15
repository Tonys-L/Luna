package fun.efto.luna.core.init;

import fun.efto.luna.core.analyzer.AnalyzerRegistry;
import fun.efto.luna.core.analyzer.AnalyzerType;
import fun.efto.luna.core.asm.analyzer.AsmClassAnalyzer;
import fun.efto.luna.core.plugin.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.LunaPlugin;
import fun.efto.luna.core.plugin.RuleConverterRegistry;
import fun.efto.luna.core.plugin.builtin.LineInjectionPlugin;
import fun.efto.luna.core.plugin.builtin.MethodInjectionPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2025/10/4 17:36
 */
public class DefaultInitializer implements Initializer {
    @Override
    public void initialize() {
        AnalyzerRegistry analyzerRegistry = AnalyzerRegistry.getInstance();
        analyzerRegistry.register(new AnalyzerType("ASM", "ASM字节码分析器").register(), new AsmClassAnalyzer());

        List<LunaPlugin> discoveredPlugins = new ArrayList<>();
        ServiceLoader.load(LunaPlugin.class).forEach(discoveredPlugins::add);
        for (LunaPlugin plugin : discoveredPlugins) {
            try {
                plugin.onRegister(new DefaultPluginContext());
            } catch (Exception e) {
                System.err.println("[Luna] Failed to register plugin: " + plugin.getClass().getName() + " - " + e.getMessage());
            }
        }
    }

    private static class DefaultPluginContext implements fun.efto.luna.core.plugin.PluginContext {
        @Override
        public fun.efto.luna.core.plugin.PluginContext registerInjectionType(fun.efto.luna.core.injection.target.type.InjectionType type) {
            InjectionTypeRegistry.register(type);
            return this;
        }

        @Override
        public PluginContext registerRuleConverter(String injectionTypeName, fun.efto.luna.core.plugin.InjectionRuleConverter converter) {
            RuleConverterRegistry.register(injectionTypeName, converter);
            return this;
        }

        @Override
        public PluginContext registerExpressionHandler(String protocol, fun.efto.luna.core.plugin.ExpressionHandler handler) {
            fun.efto.luna.core.plugin.ExpressionHandlerRegistry.register(protocol, handler);
            return this;
        }
    }
}
