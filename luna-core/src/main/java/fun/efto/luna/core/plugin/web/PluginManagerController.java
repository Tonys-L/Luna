package fun.efto.luna.core.plugin.web;

import fun.efto.luna.core.config.ConfigManager;
import fun.efto.luna.core.plugin.LunaController;
import fun.efto.luna.core.plugin.PluginInfo;
import fun.efto.luna.core.plugin.PluginLoadResult;
import fun.efto.luna.core.plugin.PluginManager;
import fun.efto.luna.core.plugin.PluginUnloadResult;
import fun.efto.luna.core.plugin.PluginUpdateResult;
import fun.efto.luna.core.web.ApiResult;
import fun.efto.luna.core.web.Controller;
import fun.efto.luna.core.web.GetMapping;
import fun.efto.luna.core.web.PathVariable;
import fun.efto.luna.core.web.PostMapping;
import fun.efto.luna.core.web.PutMapping;
import fun.efto.luna.core.web.RequestBody;
import fun.efto.luna.core.web.RequestMapping;
import fun.efto.luna.core.web.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@Controller
@RequestMapping("/plugins")
public class PluginManagerController implements LunaController {

    private final PluginManager pluginManager;

    public PluginManagerController(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @GetMapping("")
    public ApiResult listPlugins() {
        return ApiResult.ok(pluginManager.listPlugins());
    }

    @GetMapping("/{pluginId}")
    public ApiResult getPlugin(@PathVariable("pluginId") String pluginId) {
        List<PluginInfo> plugins = pluginManager.listPlugins();
        Optional<PluginInfo> found = plugins.stream()
                .filter(p -> p.getId().equals(pluginId))
                .findFirst();
        if (found.isPresent()) {
            return ApiResult.ok(found.get());
        }
        return ApiResult.fail("Plugin not found: " + pluginId, 404);
    }

    @PostMapping("/load")
    public ApiResult load(@RequestParam("pluginId") String pluginId) {
        PluginLoadResult result = pluginManager.load(pluginId);
        if (result.isSuccess()) {
            return ApiResult.ok(result);
        }
        return ApiResult.fail(result.getErrorMessage(), 400);
    }

    @PostMapping("/unload")
    public ApiResult unload(@RequestParam("pluginId") String pluginId) {
        PluginUnloadResult result = pluginManager.unload(pluginId);
        if (result.isSuccess()) {
            return ApiResult.ok(result);
        }
        return ApiResult.fail(result.getErrorMessage(), 400);
    }

    @PostMapping("/update")
    public ApiResult update(@RequestParam("pluginId") String pluginId) {
        PluginUpdateResult result = pluginManager.update(pluginId);
        if (result.isSuccess()) {
            return ApiResult.ok(result);
        }
        return ApiResult.fail(result.getErrorMessage(), 400);
    }

    @GetMapping("/{pluginId}/config")
    public ApiResult getConfig(@PathVariable("pluginId") String pluginId) {
        Map<String, String> config = ConfigManager.getPluginConfig(pluginId);
        return ApiResult.ok(config);
    }

    @PutMapping("/{pluginId}/config")
    public ApiResult saveConfig(@PathVariable("pluginId") String pluginId,
                                @RequestBody Map<String, String> config) {
        ConfigManager.savePluginConfig(pluginId, config);
        return ApiResult.ok(config);
    }
}
