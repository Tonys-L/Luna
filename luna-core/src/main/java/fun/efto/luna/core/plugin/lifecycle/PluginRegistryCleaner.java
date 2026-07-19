package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.CodeEngineRegistry;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import fun.efto.luna.core.infra.web.WebServer;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 12:00
 */
public class PluginRegistryCleaner {
    public static void cleanup(PluginRegistrationRecord record, WebServer webServer) {
        InjectionTypeRegistry.getInstance().unregisterAll(record.getInjectionLocations());
        BytecodeInjectorRegistry.getInstance().getRegistry().keySet().removeAll(record.getInjectors().keySet());
        ProbeHandlerRegistry.getInstance().unregisterAll(record.getProbeHandlers());
        CodeEngineRegistry.getInstance().unregisterAll(record.getCodeEngines());
        if (webServer != null && !record.getControllers().isEmpty()) {
            webServer.unregisterControllers(record.getControllers());
        }
    }
}
