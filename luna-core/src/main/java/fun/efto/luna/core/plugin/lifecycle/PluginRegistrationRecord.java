package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.injection.CodeEngine;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.injection.rule.template.RuleTemplate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public final class PluginRegistrationRecord {
    private final String pluginId;
    private final List<InjectionLocation> injectionLocations = new CopyOnWriteArrayList<>();
    private final Map<InjectionLocation, BytecodeInjector> injectors = new LinkedHashMap<>();
    private final Map<InjectionLocation, InjectionRuleConverter> ruleConverters = new LinkedHashMap<>();
    private final List<RuleTemplate> templates = new ArrayList<>();
    private final List<LunaController> controllers = new ArrayList<>();
    private final List<CodeEngine> codeEngines = new ArrayList<>();
    private final List<ProbeHandler> probeHandlers = new ArrayList<>();
    private final List<String> bootstrapClasses = new ArrayList<>();

    public PluginRegistrationRecord(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginId() { return pluginId; }

    public List<InjectionLocation> getInjectionLocations() { return Collections.unmodifiableList(injectionLocations); }
    public Map<InjectionLocation, BytecodeInjector> getInjectors() { return Collections.unmodifiableMap(injectors); }
    public List<ProbeHandler> getProbeHandlers() { return Collections.unmodifiableList(probeHandlers); }
    public Map<InjectionLocation, InjectionRuleConverter> getRuleConverters() { return Collections.unmodifiableMap(ruleConverters); }
    public List<RuleTemplate> getTemplates() { return Collections.unmodifiableList(templates); }
    public List<LunaController> getControllers() { return Collections.unmodifiableList(controllers); }
    public List<CodeEngine> getCodeEngines() { return Collections.unmodifiableList(codeEngines); }
    public List<String> getBootstrapClasses() { return Collections.unmodifiableList(bootstrapClasses); }

    void addInjectionLocation(InjectionLocation location) { injectionLocations.add(location); }
    void addInjector(InjectionLocation location, BytecodeInjector injector) { injectors.put(location, injector); }
    void addRuleConverter(InjectionLocation location, InjectionRuleConverter converter) { ruleConverters.put(location, converter); }
    void addTemplate(RuleTemplate template) { templates.add(template); }
    void addController(LunaController controller) { controllers.add(controller); }
    void addCodeEngine(CodeEngine engine) { codeEngines.add(engine); }
    void addProbeHandler(ProbeHandler handler) { probeHandlers.add(handler); }
    void addBootstrapClass(String internalName) { bootstrapClasses.add(internalName); }
}
