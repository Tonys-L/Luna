package fun.efto.luna.core.market;

import fun.efto.luna.core.plugin.LunaController;
import fun.efto.luna.core.plugin.loader.PluginLoader;
import fun.efto.luna.core.plugin.PluginManager;
import fun.efto.luna.core.plugin.PluginLoadResult;
import fun.efto.luna.core.plugin.PluginUnloadResult;
import fun.efto.luna.core.plugin.PluginUpdateResult;
import fun.efto.luna.core.infra.web.ApiResult;
import fun.efto.luna.core.infra.web.Controller;
import fun.efto.luna.core.infra.web.DeleteMapping;
import fun.efto.luna.core.infra.web.GetMapping;
import fun.efto.luna.core.infra.web.PathVariable;
import fun.efto.luna.core.infra.web.PostMapping;
import fun.efto.luna.core.infra.web.RequestMapping;
import fun.efto.luna.core.infra.web.RequestParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:35
 */
@Controller
@RequestMapping("/plugins/market")
public class MarketController implements LunaController {

    private static final Logger log = Logger.getLogger(MarketController.class.getName());

    private final PluginManager pluginManager;
    private volatile MarketClient marketClient;

    public MarketController(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void setMarketClient(MarketClient client) {
        this.marketClient = client;
    }

    @GetMapping("/search")
    public ApiResult search(@RequestParam("keyword") String keyword) {
        MarketClient client = getMarketClient();
        if (client == null) {
            return ApiResult.fail("Market not configured", 503);
        }
        List<PluginMetadata> results = client.search(keyword);
        return ApiResult.ok(results);
    }

    @GetMapping("/plugins/{pluginId}")
    public ApiResult getPluginDetail(@PathVariable("pluginId") String pluginId) {
        MarketClient client = getMarketClient();
        if (client == null) {
            return ApiResult.fail("Market not configured", 503);
        }
        PluginMetadata metadata = client.getMetadata(pluginId, null);
        if (metadata == null) {
            return ApiResult.fail("Plugin not found: " + pluginId, 404);
        }
        return ApiResult.ok(metadata);
    }

    @PostMapping("/install/{pluginId}")
    public ApiResult install(@PathVariable("pluginId") String pluginId) {
        MarketClient client = getMarketClient();
        if (client == null) {
            return ApiResult.fail("Market not configured", 503);
        }

        PluginMetadata meta = client.getMetadata(pluginId, null);
        if (meta == null) {
            return ApiResult.fail("Plugin not found in market: " + pluginId, 404);
        }

        try {
            Path finalJar = downloadAndVerify(client, meta, pluginId);
            PluginLoadResult result = pluginManager.load(finalJar.toString());
            if (result.isSuccess()) {
                return ApiResult.ok(result);
            }
            return ApiResult.fail(result.getErrorMessage(), 400);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Plugin install failed: " + pluginId, e);
            return ApiResult.fail("Install failed: " + e.getMessage(), 500);
        }
    }

    @GetMapping("/check-updates")
    public ApiResult checkUpdates() {
        MarketClient client = getMarketClient();
        if (client == null) {
            return ApiResult.fail("Market not configured", 503);
        }
        return ApiResult.ok(java.util.Collections.emptyList());
    }

    @DeleteMapping("/plugins/{pluginId}/uninstall")
    public ApiResult uninstall(@PathVariable("pluginId") String pluginId) {
        PluginUnloadResult result = pluginManager.unload(pluginId);
        if (!result.isSuccess()) {
            return ApiResult.fail(result.getErrorMessage(), 400);
        }

        try {
            Path pluginDir = PluginLoader.getPluginsDir().resolve(pluginId);
            if (Files.isDirectory(pluginDir)) {
                try (java.util.stream.Stream<Path> walk = Files.walk(pluginDir)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to delete plugin files for: " + pluginId, e);
        }

        return ApiResult.ok(result);
    }

    @PostMapping("/plugins/{pluginId}/update")
    public ApiResult update(@PathVariable("pluginId") String pluginId) {
        MarketClient client = getMarketClient();
        if (client == null) {
            return ApiResult.fail("Market not configured", 503);
        }

        PluginMetadata meta = client.getMetadata(pluginId, null);
        if (meta == null) {
            return ApiResult.fail("Plugin not found in market: " + pluginId, 404);
        }

        try {
            Path finalJar = downloadAndVerify(client, meta, pluginId);
            PluginUpdateResult result = pluginManager.update(pluginId);
            if (result.isSuccess()) {
                return ApiResult.ok(result);
            }
            if (result.isRolledBack()) {
                return ApiResult.fail("Update failed but rolled back: " + result.getErrorMessage(), 500);
            }
            return ApiResult.fail(result.getErrorMessage(), 500);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Plugin update failed: " + pluginId, e);
            return ApiResult.fail("Update failed: " + e.getMessage(), 500);
        }
    }

    private Path downloadAndVerify(MarketClient client, PluginMetadata meta, String pluginId) throws Exception {
        String downloadUrl = meta.getDownloadUrl();
        String expectedChecksum = meta.getChecksum();
        String version = meta.getVersion();

        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new IllegalArgumentException("No download URL for plugin: " + pluginId);
        }

        Path pluginDir = PluginLoader.getPluginsDir().resolve(pluginId);
        Path tmpJar = client.download(downloadUrl, pluginDir);

        if (expectedChecksum != null && !expectedChecksum.isEmpty()) {
            String actualChecksum = MarketClient.sha256(tmpJar);
            String expected = expectedChecksum.replace("sha256:", "");
            if (!expected.equals(actualChecksum)) {
                Files.deleteIfExists(tmpJar);
                throw new SecurityException("Checksum verification failed, file may be tampered");
            }
        }

        Path finalJar = pluginDir.resolve(pluginId + "-" + (version != null ? version : "latest") + ".jar");
        Files.move(tmpJar, finalJar, StandardCopyOption.REPLACE_EXISTING);
        return finalJar;
    }

    private MarketClient getMarketClient() {
        return marketClient;
    }
}
