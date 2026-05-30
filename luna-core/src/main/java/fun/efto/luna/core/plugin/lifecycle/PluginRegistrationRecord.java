package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.bytecode.asm.injector.BytecodeInjector;
import fun.efto.luna.core.injection.CodeCompilerStrategy;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.injection.code.CodeType;
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
    private final List<InjectionType> injectionTypes = new CopyOnWriteArrayList<>();
    private final Map<InjectionType, BytecodeInjector> injectors = new LinkedHashMap<>();
    private final Map<CodeType, BytecodeAssembler> assemblers = new LinkedHashMap<>();
    private final List<ExpressionHandler> expressionHandlers = new ArrayList<>();
    private final Map<InjectionType, InjectionRuleConverter> ruleConverters = new LinkedHashMap<>();
    private final List<RuleTemplate> templates = new ArrayList<>();
    private final List<LunaController> controllers = new ArrayList<>();
    private final List<CodeCompilerStrategy> codeCompilerStrategies = new ArrayList<>();
    private final List<String> bootstrapClasses = new ArrayList<>();

    public PluginRegistrationRecord(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginId() { return pluginId; }

    public List<InjectionType> getInjectionTypes() { return Collections.unmodifiableList(injectionTypes); }
    public Map<InjectionType, BytecodeInjector> getInjectors() { return Collections.unmodifiableMap(injectors); }
    public Map<CodeType, BytecodeAssembler> getAssemblers() { return Collections.unmodifiableMap(assemblers); }
    public List<ExpressionHandler> getExpressionHandlers() { return Collections.unmodifiableList(expressionHandlers); }
    public Map<InjectionType, InjectionRuleConverter> getRuleConverters() { return Collections.unmodifiableMap(ruleConverters); }
    public List<RuleTemplate> getTemplates() { return Collections.unmodifiableList(templates); }
    public List<LunaController> getControllers() { return Collections.unmodifiableList(controllers); }
    public List<CodeCompilerStrategy> getCodeCompilerStrategies() { return Collections.unmodifiableList(codeCompilerStrategies); }
    public List<String> getBootstrapClasses() { return Collections.unmodifiableList(bootstrapClasses); }

    void addInjectionType(InjectionType type) { injectionTypes.add(type); }
    void addInjector(InjectionType type, BytecodeInjector injector) { injectors.put(type, injector); }
    void addAssembler(CodeType type, BytecodeAssembler assembler) { assemblers.put(type, assembler); }
    void addExpressionHandler(ExpressionHandler handler) { expressionHandlers.add(handler); }
    void addRuleConverter(InjectionType type, InjectionRuleConverter converter) { ruleConverters.put(type, converter); }
    void addTemplate(RuleTemplate template) { templates.add(template); }
    void addController(LunaController controller) { controllers.add(controller); }
    void addCodeCompilerStrategy(CodeCompilerStrategy strategy) { codeCompilerStrategies.add(strategy); }
    void addBootstrapClass(String internalName) { bootstrapClasses.add(internalName); }
}
