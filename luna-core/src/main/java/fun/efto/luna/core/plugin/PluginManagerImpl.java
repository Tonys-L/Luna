package fun.efto.luna.core.plugin;

import fun.efto.luna.core.InstrumentationManager;
import fun.efto.luna.core.analyzer.ClassAnalyzer;
import fun.efto.luna.core.buffer.RingBuffer;
import fun.efto.luna.core.bytecode.BytecodeAssemblerRegistry;
import fun.efto.luna.core.decompile.Decompiler;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.injector.BytecodeInjectorRegistry;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleManager;
import fun.efto.luna.core.rule.RuleStatus;
import fun.efto.luna.core.rule.template.RuleTemplate;
import fun.efto.luna.core.rule.template.TemplateRegistry;
import fun.efto.luna.core.web.LunaWebServer;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
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
    private final RingBuffer<String> logBuffer;
    private final Instrumentation instrumentation;
    private final ClassAnalyzer classAnalyzer;
    private final Decompiler decompiler;
    private final LunaWebServer webServer;

    public PluginManagerImpl(ReadyGate readyGate, LogEmitter logEmitter,
                              RingBuffer<String> logBuffer, Instrumentation instrumentation,
                              ClassAnalyzer classAnalyzer, Decompiler decompiler) {
        this(readyGate, logEmitter, logBuffer, instrumentation, classAnalyzer, decompiler, null);
    }

    public PluginManagerImpl(ReadyGate readyGate, LogEmitter logEmitter,
                              RingBuffer<String> logBuffer, Instrumentation instrumentation,
                              ClassAnalyzer classAnalyzer, Decompiler decompiler,
                              LunaWebServer webServer) {
        this.readyGate = readyGate;
        this.logEmitter = logEmitter;
        this.logBuffer = logBuffer;
        this.instrumentation = instrumentation;
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
        PluginContextImpl ctx = new PluginContextImpl(record, logEmitter, logBuffer, instrumentation, classAnalyzer, decompiler);
        plugin.initialize(ctx);

        List<RuleTemplate> templates = new ArrayList<>();
        plugin.getTemplates(templates);
        TemplateRegistry registry = TemplateRegistry.getInstance();
        for (RuleTemplate t : templates) {
            registry.register(t);
        }

        List<LunaController> controllers = new ArrayList<>();
        plugin.getControllers(controllers);
        record.controllers.addAll(controllers);
        if (webServer != null && !controllers.isEmpty()) {
            webServer.registerControllers(controllers);
        }

        plugins.put(id, plugin);
        states.put(id, PluginState.ACTIVE);
        listeners.forEach(l -> {
            try { l.onLoaded(toInfo(plugin)); } catch (Exception ignored) {}
        });
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
        ServiceLoader<LunaPlugin> serviceLoader = ServiceLoader.load(LunaPlugin.class, tempLoader);

        LunaPlugin discoveredPlugin = null;
        for (LunaPlugin candidate : serviceLoader) {
            if (discoveredPlugin != null) {
                closeClassLoaderQuietly(tempLoader);
                return PluginLoadResult.failure(pluginPath, "Multiple LunaPlugin implementations found in JAR");
            }
            discoveredPlugin = candidate;
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

        try {
            states.put(id, PluginState.LOADING);
            PluginRegistrationRecord record = new PluginRegistrationRecord(id);
            records.put(id, record);
            PluginContextImpl ctx = new PluginContextImpl(record, logEmitter, logBuffer, instrumentation, classAnalyzer, decompiler);
            plugin.initialize(ctx);

            List<RuleTemplate> templates = new ArrayList<>();
            plugin.getTemplates(templates);
            TemplateRegistry templateRegistry = TemplateRegistry.getInstance();
            for (RuleTemplate t : templates) {
                templateRegistry.register(t);
            }

            List<LunaController> controllers = new ArrayList<>();
            plugin.getControllers(controllers);
            record.controllers.addAll(controllers);
            if (webServer != null && !controllers.isEmpty()) {
                webServer.registerControllers(controllers);
            }

            plugins.put(id, plugin);
            states.put(id, PluginState.ACTIVE);

            Set<String> restoredTypeNames = new HashSet<>();
            for (InjectionType type : record.injectionTypes) {
                restoredTypeNames.add(type.getName());
            }
            resumeSuspendedRules(id, restoredTypeNames);
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

        if (plugin.isBuiltin()) {
            return PluginUnloadResult.failure(pluginId, "Cannot unload builtin plugin: " + pluginId);
        }

        for (LunaPlugin other : plugins.values()) {
            if (!other.getId().equals(pluginId) && other.getDependencies().contains(pluginId)) {
                return PluginUnloadResult.failure(pluginId, "Plugin is required by: " + other.getId());
            }
        }

        PluginRegistrationRecord record = records.get(pluginId);
        List<String> suspendedRuleIds = suspendOrphanedRules(record);

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

        cleanupRegistry(record);

        Set<String> affectedClasses = AffectedClassTracker.getAffectedClasses(record);
        retransformAffectedClasses(affectedClasses);

        for (InjectionType type : record.injectionTypes) {
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

        if (oldPlugin.isBuiltin()) {
            return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), null, "Cannot update builtin plugin: " + pluginId);
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
        } catch (Exception e) {
            closeClassLoaderQuietly(tempCl);
            return PluginUpdateResult.failedNoRollback(pluginId, oldPlugin.getVersion(), null, "Failed to load new plugin: " + e.getMessage());
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
        List<String> suspendedRuleIds = suspendOrphanedRules(oldRecord);

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

    private List<String> suspendOrphanedRules(PluginRegistrationRecord record) {
        List<String> suspendedIds = new ArrayList<>();
        Set<String> typeNames = record.injectionTypes.stream()
            .map(t -> t.getName())
            .collect(Collectors.toSet());

        RuleManager ruleManager = RuleManager.getInstance();
        for (InjectionRule rule : ruleManager.getRules()) {
            if (rule.getStatus() == RuleStatus.ACTIVE && typeNames.contains(rule.getInjectionType())) {
                rule.setStatus(RuleStatus.SUSPENDED);
                rule.setSuspendReason("Plugin " + record.getPluginId() + " unloaded");
                suspendedIds.add(String.valueOf(rule.getId()));
            }
        }
        return suspendedIds;
    }

    private void resumeSuspendedRules(String pluginId, Set<String> restoredTypeNames) {
        RuleManager ruleManager = RuleManager.getInstance();
        for (InjectionRule rule : ruleManager.getSuspendedRules()) {
            if (restoredTypeNames.contains(rule.getInjectionType())) {
                rule.setStatus(RuleStatus.ACTIVE);
                rule.setSuspendReason(null);
            }
        }
    }

    private void cleanupRegistry(PluginRegistrationRecord record) {
        InjectionTypeRegistry.unregisterAll(record.injectionTypes);
        BytecodeInjectorRegistry.getInstance().getRegistry().keySet().removeAll(record.injectors.keySet());
        BytecodeAssemblerRegistry.getInstance().getRegistry().keySet().removeAll(record.assemblers.keySet());
        ExpressionHandlerRegistry.unregisterAll(record.expressionHandlers);
        RuleConverterRegistry.unregisterAll(record.ruleConverters);
        TemplateRegistry.getInstance().getAllTemplates().stream()
            .filter(t -> record.templates.contains(t))
            .forEach(t -> TemplateRegistry.getInstance().unregister(t.getName()));
        if (webServer != null && !record.controllers.isEmpty()) {
            webServer.unregisterControllers(record.controllers);
        }
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
            plugin.getAuthor(), plugin.getCategory(), plugin.isBuiltin(),
            states.getOrDefault(plugin.getId(), PluginState.UNLOADED),
            plugin.getDependencies());
    }

    @Override
    public UnloadCheckResult checkUnloadable(String pluginId) {
        LunaPlugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            return UnloadCheckResult.denied("Plugin not found: " + pluginId, Collections.emptyList());
        }
        if (plugin.isBuiltin()) {
            return UnloadCheckResult.denied("Cannot unload builtin plugin: " + pluginId, Collections.emptyList());
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
        if (plugin.isBuiltin()) {
            throw new IllegalStateException("Cannot disable builtin plugin: " + pluginId);
        }
        PluginState current = states.get(pluginId);
        if (current != PluginState.ACTIVE) {
            throw new IllegalStateException("Plugin is not ACTIVE (current: " + current + "), cannot disable: " + pluginId);
        }

        PluginRegistrationRecord record = records.get(pluginId);
        suspendOrphanedRules(record);

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
        Set<String> restoredTypeNames = record.injectionTypes.stream()
            .map(InjectionType::getName)
            .collect(Collectors.toSet());
        resumeSuspendedRules(pluginId, restoredTypeNames);

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

    private void retransformAffectedClasses(Set<String> classNames) {
        if (classNames == null || classNames.isEmpty() || instrumentation == null) return;
        try {
            InstrumentationManager instManager = InstrumentationManager.getInstance();
            List<Class<?>> targets = new ArrayList<>();
            for (Class<?> clazz : instManager.getAllLoadedClasses()) {
                if (classNames.contains(clazz.getName()) && instManager.getInstrumentation().isModifiableClass(clazz)) {
                    targets.add(clazz);
                }
            }
            if (!targets.isEmpty()) {
                instManager.retransformClasses(targets.toArray(new Class<?>[0]));
            }
        } catch (Exception e) {
            // Agent 不得影响业务执行
        }
    }
}
