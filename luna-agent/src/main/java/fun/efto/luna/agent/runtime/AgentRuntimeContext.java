package fun.efto.luna.agent.runtime;

import fun.efto.luna.agent.web.JettyWebServer;
import fun.efto.luna.core.injection.InjectionService;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.plugin.PluginManager;

import java.lang.instrument.Instrumentation;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/07 20:00
 */
public final class AgentRuntimeContext {

    private final Instrumentation instrumentation;
    private final InjectionService injectionService;
    private final PluginManager pluginManager;
    private final JettyWebServer webServer;
    private final Retransformer retransformer;

    public AgentRuntimeContext(Instrumentation instrumentation,
                               InjectionService injectionService,
                               PluginManager pluginManager,
                               JettyWebServer webServer,
                               Retransformer retransformer) {
        this.instrumentation = instrumentation;
        this.injectionService = injectionService;
        this.pluginManager = pluginManager;
        this.webServer = webServer;
        this.retransformer = retransformer;
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public InjectionService getInjectionService() {
        return injectionService;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public JettyWebServer getWebServer() {
        return webServer;
    }

    public Retransformer getRetransformer() {
        return retransformer;
    }
}
