package fun.efto.luna.core.plugin.web;

import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.rule.template.RuleTemplate;
import fun.efto.luna.core.rule.template.TemplateRegistry;
import fun.efto.luna.core.web.ApiResult;
import fun.efto.luna.core.web.Controller;
import fun.efto.luna.core.web.GetMapping;
import fun.efto.luna.core.web.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@Controller
@RequestMapping("/plugins")
public class PluginUIController implements LunaController {

    private final PluginManager pluginManager;

    public PluginUIController(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @GetMapping("/ui-manifest")
    public ApiResult getUiManifest() {
        Map<String, Object> manifest = new HashMap<>();
        Set<String> disabledPluginIds = collectDisabledPluginIds();
        manifest.put("injectionTypes", buildInjectionTypes(disabledPluginIds));
        manifest.put("expressionProtocols", buildExpressionProtocols(disabledPluginIds));
        manifest.put("templates", buildTemplates(disabledPluginIds));
        return ApiResult.ok(manifest);
    }

    private Set<String> collectDisabledPluginIds() {
        Set<String> disabled = new HashSet<>();
        for (PluginInfo info : pluginManager.listPlugins()) {
            if (info.getState() == PluginState.DISABLED) {
                disabled.add(info.getId());
            }
        }
        return disabled;
    }

    private Set<String> collectDisabledTypeNames(Set<String> disabledPluginIds) {
        if (!(pluginManager instanceof PluginManagerImpl)) {
            return Collections.emptySet();
        }
        Map<String, PluginRegistrationRecord> records = ((PluginManagerImpl) pluginManager).getRecords();
        Set<String> typeNames = new HashSet<>();
        for (String pluginId : disabledPluginIds) {
            PluginRegistrationRecord record = records.get(pluginId);
            if (record != null) {
                record.injectionTypes.forEach(t -> typeNames.add(t.getName()));
            }
        }
        return typeNames;
    }

    private Set<String> collectDisabledProtocols(Set<String> disabledPluginIds) {
        if (!(pluginManager instanceof PluginManagerImpl)) {
            return Collections.emptySet();
        }
        Map<String, PluginRegistrationRecord> records = ((PluginManagerImpl) pluginManager).getRecords();
        Set<String> protocols = new HashSet<>();
        for (String pluginId : disabledPluginIds) {
            PluginRegistrationRecord record = records.get(pluginId);
            if (record != null) {
                record.expressionHandlers.forEach(h -> protocols.add(h.getProtocol()));
            }
        }
        return protocols;
    }

    private Set<String> collectDisabledTemplateNames(Set<String> disabledPluginIds) {
        if (!(pluginManager instanceof PluginManagerImpl)) {
            return Collections.emptySet();
        }
        Map<String, PluginRegistrationRecord> records = ((PluginManagerImpl) pluginManager).getRecords();
        Set<String> templateNames = new HashSet<>();
        for (String pluginId : disabledPluginIds) {
            PluginRegistrationRecord record = records.get(pluginId);
            if (record != null) {
                record.templates.forEach(t -> templateNames.add(t.getName()));
            }
        }
        return templateNames;
    }

    private List<Map<String, String>> buildInjectionTypes(Set<String> disabledPluginIds) {
        Set<String> disabledTypeNames = collectDisabledTypeNames(disabledPluginIds);
        return InjectionTypeRegistry.getAll().stream()
                .filter(t -> !disabledTypeNames.contains(t.getName()))
                .map(t -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("name", t.getName());
                    map.put("displayName", t.getName());
                    map.put("category", t.getDescription());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, String>> buildExpressionProtocols(Set<String> disabledPluginIds) {
        Set<String> disabledProtocols = collectDisabledProtocols(disabledPluginIds);
        return ExpressionHandlerRegistry.getAll().stream()
                .filter(h -> !disabledProtocols.contains(h.getProtocol()))
                .map(h -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("protocol", h.getProtocol());
                    map.put("displayName", h.getProtocol());
                    map.put("syntax", h.getProtocol() + ":<expression>");
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildTemplates(Set<String> disabledPluginIds) {
        Set<String> disabledTemplateNames = collectDisabledTemplateNames(disabledPluginIds);
        return TemplateRegistry.getInstance().getAllTemplates().stream()
                .filter(t -> !disabledTemplateNames.contains(t.getName()))
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", t.getName());
                    map.put("displayName", t.getDisplayName());
                    map.put("description", t.getDescription());
                    map.put("category", t.getCategory());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
