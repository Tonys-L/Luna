package fun.efto.luna.core.plugin.web;

import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.LunaController;
import fun.efto.luna.core.plugin.PluginInfo;
import fun.efto.luna.core.plugin.PluginManager;
import fun.efto.luna.core.plugin.PluginState;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import fun.efto.luna.core.injection.rule.template.TemplateRegistry;
import fun.efto.luna.core.infra.web.ApiResult;
import fun.efto.luna.core.infra.web.Controller;
import fun.efto.luna.core.infra.web.GetMapping;
import fun.efto.luna.core.infra.web.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : Tony.L(286269159@qq.com)
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
                buildInjectionLocations(disabledPluginIds),
                buildProbeTypes(disabledPluginIds),
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

    private List<UiManifestVO.InjectionLocationEntry> buildInjectionLocations(Set<String> disabledPluginIds) {
        Set<String> disabledLocationNames = collectDisabledLocationNames(disabledPluginIds);
        return InjectionTypeRegistry.getInstance().getAll().stream()
                .filter(t -> !disabledLocationNames.contains(t.getName()))
                .map(t -> new UiManifestVO.InjectionLocationEntry(t.getName(), t.getName(), t.getDescription()))
                .collect(Collectors.toList());
    }

    private List<UiManifestVO.ExpressionProtocolEntry> buildProbeTypes(Set<String> disabledPluginIds) {
        Set<String> disabledProbeTypes = collectDisabledProbeTypes(disabledPluginIds);
        return ProbeHandlerRegistry.getInstance().getAll().stream()
                .filter(h -> !disabledProbeTypes.contains(h.getProbeType()))
                .map(h -> new UiManifestVO.ExpressionProtocolEntry(
                        h.getProbeType(), h.getProbeType(), h.getProbeType() + " probe"))
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

    private Set<String> collectDisabledLocationNames(Set<String> disabledPluginIds) {
        Set<String> locationNames = new HashSet<>();
        for (String pluginId : disabledPluginIds) {
            pluginManager.getInjectionLocationsForPlugin(pluginId)
                    .forEach(t -> locationNames.add(t.getName()));
        }
        return locationNames;
    }

    private Set<String> collectDisabledProbeTypes(Set<String> disabledPluginIds) {
        Set<String> probeTypes = new HashSet<>();
        for (String pluginId : disabledPluginIds) {
            pluginManager.getProbeHandlersForPlugin(pluginId)
                    .forEach(h -> probeTypes.add(h.getProbeType()));
        }
        return probeTypes;
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
