package fun.efto.luna.core.plugin.web;

import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.plugin.LunaController;
import fun.efto.luna.core.plugin.PluginInfo;
import fun.efto.luna.core.plugin.PluginManager;
import fun.efto.luna.core.plugin.PluginState;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.rule.template.TemplateRegistry;
import fun.efto.luna.core.web.ApiResult;
import fun.efto.luna.core.web.Controller;
import fun.efto.luna.core.web.GetMapping;
import fun.efto.luna.core.web.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ：Tony.L(286269159@qq.com)
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
        Set<String> disabledPluginIds = collectDisabledPluginIds();
        return ApiResult.ok(new UiManifestVO(
                buildInjectionTypes(disabledPluginIds),
                buildExpressionProtocols(disabledPluginIds),
                buildTemplates(disabledPluginIds)));
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

    private List<UiManifestVO.InjectionTypeEntry> buildInjectionTypes(Set<String> disabledPluginIds) {
        Set<String> disabledTypeNames = collectDisabledTypeNames(disabledPluginIds);
        return InjectionTypeRegistry.getInstance().getAll().stream()
                .filter(t -> !disabledTypeNames.contains(t.getName()))
                .map(t -> new UiManifestVO.InjectionTypeEntry(t.getName(), t.getName(), t.getDescription()))
                .collect(Collectors.toList());
    }

    private List<UiManifestVO.ExpressionProtocolEntry> buildExpressionProtocols(Set<String> disabledPluginIds) {
        Set<String> disabledProtocols = collectDisabledProtocols(disabledPluginIds);
        return ExpressionHandlerRegistry.getInstance().getAll().stream()
                .filter(h -> !disabledProtocols.contains(h.getProtocol()))
                .map(h -> new UiManifestVO.ExpressionProtocolEntry(
                        h.getProtocol(), h.getProtocol(), h.getProtocol() + ":<expression>"))
                .collect(Collectors.toList());
    }

    private List<UiManifestVO.TemplateEntry> buildTemplates(Set<String> disabledPluginIds) {
        Set<String> disabledTemplateNames = collectDisabledTemplateNames(disabledPluginIds);
        return TemplateRegistry.getInstance().getAllTemplates().stream()
                .filter(t -> !disabledTemplateNames.contains(t.getName()))
                .map(t -> new UiManifestVO.TemplateEntry(
                        t.getName(), t.getDisplayName(), t.getDescription(), t.getCategory()))
                .collect(Collectors.toList());
    }

    private Set<String> collectDisabledTypeNames(Set<String> disabledPluginIds) {
        Set<String> typeNames = new HashSet<>();
        for (String pluginId : disabledPluginIds) {
            pluginManager.getInjectionTypesForPlugin(pluginId)
                    .forEach(t -> typeNames.add(t.getName()));
        }
        return typeNames;
    }

    private Set<String> collectDisabledProtocols(Set<String> disabledPluginIds) {
        Set<String> protocols = new HashSet<>();
        for (String pluginId : disabledPluginIds) {
            pluginManager.getExpressionHandlersForPlugin(pluginId)
                    .forEach(h -> protocols.add(h.getProtocol()));
        }
        return protocols;
    }

    private Set<String> collectDisabledTemplateNames(Set<String> disabledPluginIds) {
        Set<String> templateNames = new HashSet<>();
        for (String pluginId : disabledPluginIds) {
            pluginManager.getTemplatesForPlugin(pluginId)
                    .forEach(t -> templateNames.add(t.getName()));
        }
        return templateNames;
    }
}
