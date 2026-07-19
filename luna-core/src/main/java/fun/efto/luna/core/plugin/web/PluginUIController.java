package fun.efto.luna.core.plugin.web;

import fun.efto.luna.core.plugin.InjectionLocationUIDescriptor;
import fun.efto.luna.core.plugin.LunaController;
import fun.efto.luna.core.plugin.PluginInfo;
import fun.efto.luna.core.plugin.PluginManager;
import fun.efto.luna.core.plugin.PluginState;
import fun.efto.luna.core.plugin.ProbeHandler;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
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
                buildProbeTypeEntries(disabledPluginIds)));
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
        InjectionTypeRegistry registry = InjectionTypeRegistry.getInstance();
        return registry.getAll().stream()
                .filter(t -> !disabledLocationNames.contains(t.getName()))
                .distinct()
                .map(t -> {
                    InjectionLocationUIDescriptor ui = registry.getUIDescriptor(t.getName());
                    String categoryLabel = ui != null ? ui.getCategoryLabel() : t.getCategory();
                    String color = ui != null ? ui.getColor() : "#6b7280";
                    return new UiManifestVO.InjectionLocationEntry(
                            t.getName(), t.getDescription(), t.getCategory(), categoryLabel, color);
                })
                .collect(Collectors.toList());
    }

    private List<UiManifestVO.ProbeTypeEntry> buildProbeTypeEntries(Set<String> disabledPluginIds) {
        Set<String> disabledProbeTypes = collectDisabledProbeTypes(disabledPluginIds);
        return ProbeHandlerRegistry.getInstance().getAll().stream()
                .filter(h -> !disabledProbeTypes.contains(h.getProbeType()))
                .map(h -> new UiManifestVO.ProbeTypeEntry(
                        h.getProbeType(),
                        h.getDisplayName(),
                        h.getSyntax(),
                        h.getIcon(),
                        h.getCategory(),
                        h.usesCode(),
                        h.getCodeType(),
                        h.supportedInjectionLocations(),
                        h.getQuickActionBehavior().name(),
                        h.getGlyphColor(),
                        h.getConfigSchema()))
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
}
