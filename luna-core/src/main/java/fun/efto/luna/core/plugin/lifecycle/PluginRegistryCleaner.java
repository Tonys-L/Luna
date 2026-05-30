package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.bytecode.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.injection.CodeCompiler;
import fun.efto.luna.core.injection.CodeCompilerStrategy;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import fun.efto.luna.core.rule.template.TemplateRegistry;
import fun.efto.luna.core.web.WebServer;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/17 12:00
 */
public class PluginRegistryCleaner {
    public static void cleanup(PluginRegistrationRecord record, WebServer webServer) {
        InjectionTypeRegistry.getInstance().unregisterAll(record.getInjectionTypes());
        BytecodeInjectorRegistry.getInstance().getRegistry().keySet().removeAll(record.getInjectors().keySet());
        BytecodeAssemblerRegistry.getInstance().getRegistry().keySet().removeAll(record.getAssemblers().keySet());
        ExpressionHandlerRegistry.getInstance().unregisterAll(record.getExpressionHandlers());
        RuleConverterRegistry.getInstance().unregisterAll(record.getRuleConverters());
        for (CodeCompilerStrategy strategy : record.getCodeCompilerStrategies()) {
            CodeCompiler.unregister(strategy);
        }
        TemplateRegistry.getInstance().getAllTemplates().stream()
            .filter(t -> record.getTemplates().contains(t))
            .forEach(t -> TemplateRegistry.getInstance().unregister(t.getName()));
        if (webServer != null && !record.getControllers().isEmpty()) {
            webServer.unregisterControllers(record.getControllers());
        }
    }
}
