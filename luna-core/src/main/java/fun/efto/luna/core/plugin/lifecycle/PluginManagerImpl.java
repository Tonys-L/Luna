package fun.efto.luna.core.plugin.lifecycle;

import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.common.RingBuffer;
import fun.efto.luna.core.bytecode.asm.assembler.BytecodeAssemblerRegistry;
import fun.efto.luna.core.decompile.Decompiler;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.config.ConfigManager;
import fun.efto.luna.core.plugin.*;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.plugin.registry.ExpressionHandlerRegistry;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import fun.efto.luna.core.plugin.loader.PluginClassLoader;
import fun.efto.luna.core.plugin.loader.PluginDependencyResolver;
import fun.efto.luna.core.plugin.loader.LunaAgentClassLoader;
import fun.efto.luna.core.rule.template.RuleTemplate;
import fun.efto.luna.core.web.WebServer;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class PluginManagerImpl implements PluginManager {
    private final Map<String, LunaPlugin> plugins = new ConcurrentHashMap<>();
    private final Map<String, PluginRegistrationRecord> records = new ConcurrentHashMap<>();
    private final Map<String, PluginState> states = new ConcurrentHashMap<>();
    private final Map<String, PluginClassLoader> classLoaders = new ConcurrentHashMap<>();
    private final Map<String, String> pluginPaths = new ConcurrentHashMap<>();
    private final List<PluginLifecycleListener> listeners = new CopyOnWriteArrayList<>();
    private final StampedLock transformLock = new StampedLock();
    private final ReadyGate readyGate;
    private final LogEmitter logEmitter;
    private final RingBuffer<ProbeMessage> logBuffer;
    private final Retransformer retransformer;
    private final ClassAnalyzer classAnalyzer;
    private final Decompiler decompiler;
    private final WebServer webServer;

    public PluginManagerImpl(ReadyGate readyGate, LogEmitter logEmitter,
                              RingBuffer<ProbeMessage> logBuffer, Retransformer retransformer,
                              ClassAnalyzer classAnalyzer, Decompiler decompiler) {
        this(readyGate, logEmitter, logBuffer, retransformer, classAnalyzer, decompiler, null);
    }

    public PluginManagerImpl(ReadyGate readyGate, LogEmitter logEmitter,
                              RingBuffer<ProbeMessage> logBuffer, Retransformer retransformer,
                              ClassAnalyzer classAnalyzer, Decompiler decompiler,
                              WebServer webServer) {
        this.readyGate = readyGate;
        this.logEmitter = logEmitter;
        this.logBuffer = logBuffer;
        this.retransformer = retransformer;
        this.classAnalyzer = classAnalyzer;
        this.decompiler = decompiler;
        this.webServer = webServer;
    }

    public void initializeAll(List<LunaPlugin> discoveredPlugins) {
        List<LunaPlugin> sorted = PluginDependencyResolver.resolve(discoveredPlugins);
        for (LunaPlugin plugin : sorted) {
            initializePlugin(plugin);
        }
    }

    private void initializePlugin(LunaPlugin plugin) {
        String id = plugin.getId();
        states.put(id, PluginState.LOADING);
        PluginRegistrationRecord record = new PluginRegistrationRecord(id);
        records.put(id, record);

        initPlugin(plugin, record);

        listeners.forEach(l -> {
            try { l.onLoaded(toInfo(plugin)); } catch (Exception ignored) {}
        });
    }

    private void initPlugin(LunaPlugin plugin, PluginRegistrationRecord record) {
        PluginContextImpl ctx = new PluginContextImpl(record, logEmitter, logBuffer, retransformer, classAnalyzer, decompiler);
        plugin.initialize(ctx);

        List<LunaController> controllers = new ArrayList<>();
        plugin.getControllers(controllers);
        controllers.forEach(record::addController);
        if (webServer != null && !controllers.isEmpty()) {
            webServer.registerControllers(controllers);
        }

        plugins.put(plugin.getId(), plugin);
        states.put(plugin.getId(), PluginState.ACTIVE);
    }

    @Override
    public PluginLoadResult load(String pluginPath) {
        File jarFile = new File(pluginPath);
        if (!jarFile.exists()) {
            return PluginLoadResult.failure(pluginPath, "Plugin JAR not found: " + pluginPath);
        }

        URL jarUrl;
        try {
            jarUrl = jarFile.toURI().toURL();
        } catch (MalformedURLException e) {
            return PluginLoadResult.failure(pluginPath, "Invalid JAR path: " + pluginPath);
        }

        PluginClassLoader tempLoader = new PluginClassLoader("temp", new URL[]{jarUrl}, getClass().getClassLoader());

        LunaPlugin discoveredPlugin = null;
        try {
            ServiceLoader<LunaPlugin> serviceLoader = ServiceLoader.load(LunaPlugin.class, tempLoader);
            for (LunaPlugin candidate : serviceLoader) {
                if (discoveredPlugin != null) {
                    closeClassLoaderQuietly(tempLoader);
                    return PluginLoadResult.failure(pluginPath, "Multiple LunaPlugin implementations found in JAR");
                }
                discoveredPlugin = candidate;
            }
        } catch (Throwable t) {
            closeClassLoaderQuietly(tempLoader);
            return PluginLoadResult.failure(pluginPath, "Failed to load plugin via ServiceLoader: " + t.getMessage());
        }

        if (discoveredPlugin == null) {
            closeClassLoaderQuietly(tempLoader);
            return PluginLoadResult.failure(pluginPath, "No LunaPlugin implementation found in JAR");
        }

        final LunaPlugin plugin = discoveredPlugin;
        String id = plugin.getId();
        if (plugins.containsKey(id)) {
            closeClassLoaderQuietly(tempLoader);
            return PluginLoadResult.failure(id, "Plugin ID conflict: " + id + " is already loaded");
        }

        List<String> missingDeps = new ArrayList<>();
        for (String dep : plugin.getDependencies()) {
            if (!plugins.containsKey(dep)) {
                missingDeps.add(dep);
            }
        }
        if (!missingDeps.isEmpty()) {
            closeClassLoaderQuietly(tempLoader);
            return PluginLoadResult.failure(id, "Missing dependencies: " + missingDeps);
        }

        PluginClassLoader pluginCl = new PluginClassLoader(id, new URL[]{jarUrl}, getClass().getClassLoader());

        long stamp = transformLock.writeLock();
        try {
            PluginLoadResult result = doLoadCore(plugin, pluginCl, pluginPath);
            if (!result.isSuccess()) {
                closeClassLoaderQuietly(pluginCl);
            }
            return result;
        } finally {
            transformLock.unlockWrite(stamp);
        }
    }

    private PluginLoadResult doLoadCore(LunaPlugin plugin, PluginClassLoader pluginCl, String pluginPath) {
        String id = plugin.getId();
        if (plugins.containsKey(id)) {
            return PluginLoadResult.failure(id, "Plugin ID conflict: " + id + " is already loaded");
        }

        classLoaders.put(id, pluginCl);
        pluginPaths.put(id, pluginPath);

        PluginRegistrationRecord record;
        try {
            states.put(id, PluginState.LOADING);
            record = new PluginRegistrationRecord(id);
            records.put(id, record);
            initPlugin(plugin, record);

            Set<String> restoredTypeNames = new HashSet<>();
            for (InjectionType type : record.getInjectionTypes()) {
                restoredTypeNames.add(type.getName());
            }
            RuleSuspensionManager.resumeSuspendedRules(id, restoredTypeNames);
        } catch (Exception e) {
            states.put(id, PluginState.UNLOADED);
            records.remove(id);
            plugins.remove(id);
            classLoaders.remove(id);
            pluginPaths.remove(id);
            closeClassLoaderQuietly(pluginCl);
            return PluginLoadResult.failure(id, "Plugin initialization failed: " + e.getMessage());
        }

        listeners.forEach(l -> {
            try { l.onLoaded(toInfo(plugin)); } catch (Exception ignored) {}
        });
        return PluginLoadResult.success(id, plugin.getVersion());
    }

    @Override
    public PluginUnloadResult unload(String pluginId) {
        LunaPlugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return PluginUnloadResult.failure(pluginId, "Plugin not found: " + pluginId);
        }

        for (LunaPlugin other : plugins.values()) {
            if (!other.getId().equals(pluginId) && other.getDependencies().contains(pluginId)) {
                return PluginUnloadResult.failure(pluginId, "Plugin is required by: " + other.getId());
            }
        }

        PluginRegistrationRecord record = records.get(pluginId);
        List<String> suspendedRuleIds = RuleSuspensionManager.suspendOrphanedRules(record);

        PluginClassLoader cl;
        long stamp = transformLock.writeLock();
        try {
            cl = doUnloadCore(pluginId, plugin, record);
        } finally {
            transformLock.unlockWrite(stamp);
        }
        closeClassLoaderQuietly(cl);

        listeners.forEach(l -> {
            try { l.onUnloaded(toInfo(plugin)); } catch (Exception ignored) {}
        });
        return PluginUnloadResult.success(pluginId, suspendedRuleIds);
    }

    private PluginClassLoader doUnloadCore(String pluginId, LunaPlugin plugin, PluginRegistrationRecord record) {
        states.put(pluginId, PluginState.UNLOADING);

        try {
            plugin.destroy();
        } catch (Exception ignored) {}

        PluginRegistryCleaner.cleanup(record, webServer);

        Set<String> affectedClasses = AffectedClassTracker.getAffectedClasses(record);
        retransformAffectedClasses(affectedClasses);

        for (InjectionType type : record.getInjectionTypes()) {
            AffectedClassTracker.remove(type.getName(), affectedClasses.toString());
        }

        plugins.remove(pluginId);
        records.remove(pluginId);
        states.put(pluginId, PluginState.UNLOADED);

        PluginClassLoader cl = classLoaders.remove(pluginId);
        pluginPaths.remove(pluginId);
        return cl;
    }

    @Override
    public PluginUpdateResult update(String pluginId) {
        LunaPlugin oldPlugin = plugins.get(pluginId);
        if (oldPlugin == null) {
            return PluginUpdateResult.failedNoRollback(pluginId, null, null, "Plugin not found: " + pluginId);
        }

        String storedPath = pluginPaths.get(pluginId);
        if (storedPath == null) {
            return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), null, "Plugin path not found for: " + pluginId);
        }

        File jarFile = new File(storedPath);
        if (!jarFile.exists()) {
            return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), null, "Plugin JAR not found: " + storedPath);
        }

        URL jarUrl;
        try {
            jarUrl = jarFile.toURI().toURL();
        } catch (MalformedURLException e) {
            return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), null, "Invalid JAR path: " + storedPath);
        }

        PluginClassLoader tempCl = new PluginClassLoader("temp-update", new URL[]{jarUrl}, getClass().getClassLoader());
        LunaPlugin discoveredNewPlugin = null;
        try {
            ServiceLoader<LunaPlugin> serviceLoader = ServiceLoader.load(LunaPlugin.class, tempCl);
            for (LunaPlugin candidate : serviceLoader) {
                if (discoveredNewPlugin != null) {
                    closeClassLoaderQuietly(tempCl);
                    return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), null, "Multiple LunaPlugin implementations found in JAR");
                }
                discoveredNewPlugin = candidate;
            }
        } catch (Throwable t) {
            closeClassLoaderQuietly(tempCl);
            return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), null, "Failed to load new plugin: " + t.getMessage());
        }

        if (discoveredNewPlugin == null) {
            closeClassLoaderQuietly(tempCl);
            return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), null, "No LunaPlugin implementation found in JAR");
        }

        final LunaPlugin newPlugin = discoveredNewPlugin;
        if (!newPlugin.getId().equals(pluginId)) {
            closeClassLoaderQuietly(tempCl);
            return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), newPlugin.getVersion(), "Plugin ID mismatch: expected " + pluginId + " but got " + newPlugin.getId());
        }

        for (String dep : newPlugin.getDependencies()) {
            if (!plugins.containsKey(dep) || dep.equals(pluginId)) {
                if (dep.equals(pluginId)) continue;
                closeClassLoaderQuietly(tempCl);
                return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), newPlugin.getVersion(), "Missing dependency: " + dep);
            }
        }

        String oldVersion = oldPlugin.getVersion();
        String newVersion = newPlugin.getVersion();

        PluginRegistrationRecord oldRecord = records.get(pluginId);
        List<String> suspendedRuleIds = RuleSuspensionManager.suspendOrphanedRules(oldRecord);

        PluginClassLoader newPluginCl = new PluginClassLoader(pluginId, new URL[]{jarUrl}, getClass().getClassLoader());

        PluginClassLoader oldCl = null;
        PluginLoadResult loadResult;
        long stamp = transformLock.writeLock();
        try {
            oldCl = doUnloadCore(pluginId, oldPlugin, oldRecord);

            loadResult = doLoadCore(newPlugin, newPluginCl, storedPath);
            if (!loadResult.isSuccess()) {
                closeClassLoaderQuietly(newPluginCl);
            }
        } finally {
            transformLock.unlockWrite(stamp);
        }
        closeClassLoaderQuietly(oldCl);

        if (loadResult.isSuccess()) {
            listeners.forEach(l -> {
                try { l.onUpdated(toInfo(newPlugin), oldVersion, newVersion); } catch (Exception ignored) {}
            });
            return PluginUpdateResult.success(pluginId, oldVersion, newVersion);
        }

        PluginLoadResult rollbackResult = load(storedPath);
        if (rollbackResult.isSuccess()) {
            return PluginUpdateResult.failedWithRollback(pluginId, oldVersion, newVersion, "Load new version failed: " + loadResult.getErrorMessage());
        }

        return PluginUpdateResult.failedNoRollback(pluginId, oldVersion, newVersion, "Load new version failed and rollback also failed: " + loadResult.getErrorMessage());
    }



    private void closeClassLoaderQuietly(PluginClassLoader cl) {
        if (cl != null) {
            try {
                cl.close();
            } catch (IOException ignored) {}
        }
    }

    private PluginInfo toInfo(LunaPlugin plugin) {
        return new PluginInfo(plugin.getId(), plugin.getDisplayName(), plugin.getVersion(),
            plugin.getAuthor(), plugin.getCategory(),
            states.getOrDefault(plugin.getId(), PluginState.UNLOADED),
            plugin.getDependencies());
    }

    @Override
    public UnloadCheckResult checkUnloadable(String pluginId) {
        LunaPlugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return UnloadCheckResult.denied("Plugin not found: " + pluginId, Collections.emptyList());
        }
        List<String> dependents = new ArrayList<>();
        for (LunaPlugin other : plugins.values()) {
            if (!other.getId().equals(pluginId) && other.getDependencies().contains(pluginId)) {
                dependents.add(other.getId());
            }
        }
        if (!dependents.isEmpty()) {
            return UnloadCheckResult.denied("Plugin is required by other plugins", dependents);
        }
        return UnloadCheckResult.allowed();
    }

    @Override
    public void disable(String pluginId) {
        LunaPlugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }
        PluginState current = states.get(pluginId);
        if (current != PluginState.ACTIVE) {
            throw new IllegalStateException("Plugin is not ACTIVE (current: " + current + "), cannot disable: " + pluginId);
        }

        PluginRegistrationRecord record = records.get(pluginId);
        RuleSuspensionManager.suspendOrphanedRules(record);

        states.put(pluginId, PluginState.DISABLED);

        listeners.forEach(l -> {
            try { l.onDisabled(toInfo(plugin)); } catch (Exception ignored) {}
        });
    }

    @Override
    public void enable(String pluginId) {
        LunaPlugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found: " + pluginId);
        }
        PluginState current = states.get(pluginId);
        if (current != PluginState.DISABLED) {
            throw new IllegalStateException("Plugin is not DISABLED (current: " + current + "), cannot enable: " + pluginId);
        }

        PluginRegistrationRecord record = records.get(pluginId);
        Set<String> restoredTypeNames = record.getInjectionTypes().stream()
            .map(InjectionType::getName)
            .collect(Collectors.toSet());
        RuleSuspensionManager.resumeSuspendedRules(pluginId, restoredTypeNames);

        states.put(pluginId, PluginState.ACTIVE);

        listeners.forEach(l -> {
            try { l.onEnabled(toInfo(plugin)); } catch (Exception ignored) {}
        });
    }
    @Override
    public List<PluginInfo> listPlugins() {
        return plugins.values().stream().map(this::toInfo).collect(Collectors.toList());
    }
    @Override
    public LunaPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }
    @Override public PluginState getState(String pluginId) { return states.getOrDefault(pluginId, PluginState.UNLOADED); }
    @Override public void addListener(PluginLifecycleListener listener) { listeners.add(listener); }
    @Override public void removeListener(PluginLifecycleListener listener) { listeners.remove(listener); }
    @Override public StampedLock getTransformLock() { return transformLock; }
    public Map<String, PluginRegistrationRecord> getRecords() { return records; }

    @Override
    public Set<InjectionType> getInjectionTypesForPlugin(String pluginId) {
        PluginRegistrationRecord record = records.get(pluginId);
        return record != null ? new HashSet<>(record.getInjectionTypes()) : Collections.emptySet();
    }

    @Override
    public Set<ExpressionHandler> getExpressionHandlersForPlugin(String pluginId) {
        PluginRegistrationRecord record = records.get(pluginId);
        return record != null ? new HashSet<>(record.getExpressionHandlers()) : Collections.emptySet();
    }

    @Override
    public Set<RuleTemplate> getTemplatesForPlugin(String pluginId) {
        PluginRegistrationRecord record = records.get(pluginId);
        return record != null ? new HashSet<>(record.getTemplates()) : Collections.emptySet();
    }

    private void retransformAffectedClasses(Set<String> classNames) {
        retransformer.retransformAll(classNames);
    }

    @Override
    public Map<String, String> getPluginConfig(String pluginId) {
        return ConfigManager.getPluginConfig(pluginId);
    }

    @Override
    public void savePluginConfig(String pluginId, Map<String, String> config) {
        ConfigManager.savePluginConfig(pluginId, config);
    }
}
