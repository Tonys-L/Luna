package fun.efto.luna.core.plugin;

import fun.efto.luna.core.bytecode.BytecodeAssembler;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injector.BytecodeInjector;
import fun.efto.luna.core.rule.template.RuleTemplate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public final class PluginRegistrationRecord {
    private final String pluginId;
    public final List<InjectionType> injectionTypes = new CopyOnWriteArrayList<>();
    public final Map<InjectionType, BytecodeInjector> injectors = new LinkedHashMap<>();
    public final Map<CodeType, BytecodeAssembler> assemblers = new LinkedHashMap<>();
    public final List<ExpressionHandler> expressionHandlers = new ArrayList<>();
    public final Map<InjectionType, InjectionRuleConverter> ruleConverters = new LinkedHashMap<>();
    public final List<RuleTemplate> templates = new ArrayList<>();
    public final List<LunaController> controllers = new ArrayList<>();

    public PluginRegistrationRecord(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginId() { return pluginId; }
}
