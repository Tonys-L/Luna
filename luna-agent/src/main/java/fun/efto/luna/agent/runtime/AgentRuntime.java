package fun.efto.luna.agent.runtime;

import fun.efto.luna.agent.adapter.InjectionTestHarnessAdapter;
import fun.efto.luna.agent.clazz.*;
import fun.efto.luna.agent.log.LoggerInitializer;
import fun.efto.luna.agent.web.JettyConfiguration;
import fun.efto.luna.agent.web.JettyWebServer;
import fun.efto.luna.core.analysis.decompile.DecompilerFactory;
import fun.efto.luna.core.bootstrap.BootstrapJarBuilder;
import fun.efto.luna.core.bootstrap.init.InitializerManager;
import fun.efto.luna.core.injection.DefaultInjectionRegistry;
import fun.efto.luna.core.injection.DefaultInjectionRepository;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionPointFactory;
import fun.efto.luna.core.injection.InjectionRegistry;
import fun.efto.luna.core.injection.InjectionRepository;
import fun.efto.luna.core.injection.InjectionService;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.port.BytecodeLoader;
import fun.efto.luna.core.injection.port.InjectionVerifier;
import fun.efto.luna.core.injection.port.LocalVarValidator;
import fun.efto.luna.core.injection.port.Retransformer;

import fun.efto.luna.core.infra.InstrumentationHolder;
import fun.efto.luna.core.bytecode.asm.AsmInjectionContext;
import fun.efto.luna.core.bytecode.asm.analyzer.LocalVariableScanner;
import fun.efto.luna.core.injection.InjectionValidator;
import fun.efto.luna.core.injection.port.BytecodePreviewer;
import fun.efto.luna.core.plugin.DefaultLogEmitter;
import fun.efto.luna.core.plugin.LunaPlugin;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.lifecycle.ReadyGate;

import fun.efto.luna.core.plugin.web.PluginManagerController;
import fun.efto.luna.core.plugin.web.PluginUIController;
import fun.efto.luna.core.transformer.GlobalClassFileTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/07 20:00
 */
public final class AgentRuntime {

    private static final Logger logger = LoggerFactory.getLogger(AgentRuntime.class);

    private static volatile AgentRuntime instance;

    private final AgentRuntimeContext context;

    private AgentRuntime(AgentRuntimeContext context) {
        this.context = context;
    }

    public static AgentRuntime start(String args, Instrumentation inst) {
        initLogger();

        appendBootstrapJar(inst);

        try {
            logger.info("Initializing Luna agent components...");

            // Step 1: Core initialization
            InitializerManager.getInstance().initializeAll();
            InstrumentationHolder.init(inst);

            // Step 2: ClassScanner & ClassResourceHelper
            ClassScanner classScanner = ClassScanner.getInstance(inst, createExcludeClassFilter());
            ClassResourceHelper classResourceHelper = new ClassResourceHelper(inst);

            // Step 3: Injection infrastructure
            DefaultInjectionRegistry injectionRegistry = new DefaultInjectionRegistry();
            DefaultInjectionRepository injectionRepository = new DefaultInjectionRepository();

            Retransformer retransformer = createRetransformer();

            InjectionService injectionService = assembleInjectionService(
                    classResourceHelper, inst, injectionRegistry, injectionRepository, retransformer);

            // Step 3.5: Restore persistent injections from Repository to Registry
            restorePersistentInjections(injectionRepository, injectionRegistry);

            // Step 4: Plugin system (with full dependencies)
            ReadyGate readyGate = new ReadyGate();
            BytecodeLoader bytecodeLoader = className -> classResourceHelper.loadClassBytes(className);
            PluginManagerImpl pluginManager = new PluginManagerImpl(
                    readyGate,
                    new DefaultLogEmitter(),
                    fun.efto.luna.core.probe.ProbeOutput.BUFFER,
                    retransformer,
                    null,
                    null,
                    bytecodeLoader,
                    injectionService,
                    null
            );

            // Step 5: Initialize builtin plugins
            List<LunaPlugin> builtinPlugins = BuiltinPluginProvider.builtins();
            if (!builtinPlugins.isEmpty()) {
                pluginManager.initializeAll(builtinPlugins);
                for (LunaPlugin plugin : builtinPlugins) {
                    logger.info("Initialized builtin plugin: {} ({})", plugin.getId(), plugin.getDisplayName());
                }
            } else {
                logger.warn("No builtin plugins found via ServiceLoader");
            }
            readyGate.markReady();

            // Step 6: Web server
            JettyWebServer webServer = new JettyWebServer(
                    8421, JettyConfiguration.createDevelopment(),
                    classScanner, classResourceHelper, injectionService);

            // Step 7: Register plugin management controllers
            List<fun.efto.luna.core.plugin.LunaController> pluginControllers = new ArrayList<>();
            pluginControllers.add(new PluginManagerController(pluginManager));
            pluginControllers.add(new PluginUIController(pluginManager));
            webServer.registerControllers(pluginControllers);

            webServer.start();
            logger.info("Luna agent started successfully, web server on port 8421");

            // Step 8: Register GlobalClassFileTransformer
            BytecodeLoader transformerBytecodeLoader = className -> classResourceHelper.loadClassBytes(className);
            inst.addTransformer(new GlobalClassFileTransformer(injectionRegistry, transformerBytecodeLoader), true);

            // Step 9: Initial retransform for active injections
            applyActiveInjectionsToLoadedClasses(inst, injectionService);

            // Step 10: Build context and runtime
            AgentRuntimeContext runtimeContext = new AgentRuntimeContext(
                    inst, injectionService, pluginManager, webServer, retransformer);

            AgentRuntime runtime = new AgentRuntime(runtimeContext);
            instance = runtime;

            // Step 11: Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.info("Shutting down Luna agent...");
                    runtime.stop();
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));

            return runtime;

        } catch (Exception e) {
            System.err.println("[Luna] Failed to start agent: " + e.getMessage());
            e.printStackTrace();
            if (logger != null) {
                logger.error("Failed to start Luna agent", e);
            }
            throw new RuntimeException("Agent startup failed", e);
        }
    }

    public void stop() {
        try {
            JettyWebServer webServer = context.getWebServer();
            if (webServer != null) {
                logger.info("Shutting down Jetty server...");
                webServer.stop();
                logger.info("Jetty server stopped.");
            }
        } catch (Exception e) {
            logger.error("Error stopping Jetty server", e);
        }
    }

    public AgentRuntimeContext getContext() {
        return context;
    }

    public static AgentRuntime getInstance() {
        return instance;
    }

    private static void initLogger() {
        try {
            LoggerInitializer.initLoggerContext();
            logger.info("Luna logger initialized successfully");
        } catch (Exception e) {
            System.err.println("[Luna] Failed to initialize logger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void appendBootstrapJar(Instrumentation inst) {
        try {
            String agentJarPath = AgentRuntime.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            java.nio.file.Path bootstrapJar = BootstrapJarBuilder.build(agentJarPath);
            inst.appendToBootstrapClassLoaderSearch(new JarFile(bootstrapJar.toFile()));
        } catch (Exception e) {
            System.err.println("[Luna] Failed to append bootstrap jar to bootstrap classloader: " + e.getMessage());
        }
    }

    private static Retransformer createRetransformer() {
        return new Retransformer() {
            @Override
            public void retransform(String className) {
                try {
                    List<Class<?>> classes = InstrumentationHolder.findClasses(className);
                    if (!classes.isEmpty()) {
                        InstrumentationHolder.retransformClasses(classes.toArray(new Class<?>[0]));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Retransform failed for class: " + className, e);
                }
            }

            @Override
            public void retransformAll(Set<String> classNames) {
                if (classNames == null || classNames.isEmpty()) return;
                try {
                    List<Class<?>> targets = InstrumentationHolder.findModifiableClasses(classNames::contains);
                    if (!targets.isEmpty()) {
                        InstrumentationHolder.retransformClasses(targets.toArray(new Class<?>[0]));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Batch retransform failed", e);
                }
            }

            @Override
            public void retransformByPattern(String pattern) {
                if (pattern == null || pattern.isEmpty()) return;
                try {
                    String regex = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".");
                    java.util.regex.Pattern compiled = java.util.regex.Pattern.compile(regex);
                    List<Class<?>> targets = InstrumentationHolder.findModifiableClasses(
                            className -> compiled.matcher(className).matches()
                                    && !className.startsWith("java.lang.invoke."));
                    if (!targets.isEmpty()) {
                        InstrumentationHolder.retransformClasses(targets.toArray(new Class<?>[0]));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Pattern retransform failed for: " + pattern, e);
                }
            }
        };
    }

    private static InjectionService assembleInjectionService(ClassResourceHelper classResourceHelper,
                                                              Instrumentation inst,
                                                              InjectionRegistry injectionRegistry,
                                                              InjectionRepository injectionRepository,
                                                              Retransformer retransformer) {
        BytecodeLoader bytecodeLoader = className -> classResourceHelper.loadClassBytes(className);

        LocalVarValidator localVarValidator = (code, className, methodName, methodDesc, lineNumber, classBytes) -> {
            if (code == null || !code.contains("$")) return null;
            if (lineNumber == null || lineNumber < 1) return null;
            try {
                List<AsmInjectionContext.LocalVarInfo> vars =
                        LocalVariableScanner.scanVisibleLocalVariables(
                                classBytes, methodName, methodDesc, lineNumber);
                List<InjectionValidator.LocalVarInfo> domainVars = new ArrayList<>();
                for (AsmInjectionContext.LocalVarInfo v : vars) {
                    domainVars.add(new InjectionValidator.LocalVarInfo(
                            v.getName(), v.getDescriptor(), v.getSlot()));
                }
                return InjectionValidator.validateLocalVarReferences(
                        code, className, methodName, methodDesc, lineNumber, domainVars);
            } catch (Exception e) {
                return null;
            }
        };

        InjectionVerifier injectionVerifier = new InjectionTestHarnessAdapter(inst);

        BytecodePreviewer bytecodePreviewer =
                (injectionId, className, originalBytes) -> {
                    throw new UnsupportedOperationException(
                            "Use InjectionService.preview() which directly uses ClassTransformer");
                };

        return new InjectionService(injectionRepository, injectionRegistry, retransformer,
                bytecodeLoader, bytecodePreviewer, localVarValidator, injectionVerifier);
    }

    private static void restorePersistentInjections(InjectionRepository repository, InjectionRegistry registry) {
        List<PersistentInjection> all = repository.findAll();
        int restored = 0;
        for (PersistentInjection injection : all) {
            if (injection.isEphemeral()) continue;
            try {
                InjectionPoint point = InjectionPointFactory.create(injection);
                registry.register(point);
                restored++;
            } catch (Exception e) {
                logger.warn("Failed to restore injection {}: {}", injection.getId(), e.getMessage());
            }
        }
        if (restored > 0) {
            logger.info("Restored {} persistent injections to registry", restored);
        }
    }

    private static void applyActiveInjectionsToLoadedClasses(Instrumentation inst, InjectionService injectionService) {
        logger.info("Scanning already loaded classes for active injections...");
        Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
        List<Class<?>> targets = new ArrayList<>();

        for (Class<?> clazz : allLoadedClasses) {
            String className = clazz.getName();
            if (className.startsWith("java.") || className.startsWith("sun.") || className.startsWith("fun.efto.luna.")) {
                continue;
            }

            if (!injectionService.getActivePointsForClass(className).isEmpty()) {
                if (inst.isModifiableClass(clazz)) {
                    targets.add(clazz);
                }
            }
        }

        if (!targets.isEmpty()) {
            try {
                logger.info("Applying injections to {} classes via retransform...", targets.size());
                inst.retransformClasses(targets.toArray(new Class<?>[0]));
            } catch (Exception e) {
                logger.error("Initial retransform failed", e);
            }
        }
        logger.info("Initial injection application completed.");
    }

    private static CompositeExcludeClassFilter createExcludeClassFilter() {
        return new CompositeExcludeClassFilter(new ExcludeAgentClassFilter(),
                new ExcludeArrayClassFilter(),
                new ExcludeByClassLoaderNameFilter("jdk.internal.reflect.DelegatingClassLoader",
                        "sun.reflect.DelegatingClassLoader",
                        "sun.reflect.misc.MethodUtil",
                        "fun.efto.luna.core.plugin.loader.LunaAgentClassLoader"),
                new ExcludeGeneratedClassFilter());
    }
}
