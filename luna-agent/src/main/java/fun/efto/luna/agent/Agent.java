package fun.efto.luna.agent;

import fun.efto.luna.agent.adapter.InjectionTestHarnessAdapter;
import fun.efto.luna.agent.clazz.*;
import fun.efto.luna.agent.log.LoggerInitializer;
import fun.efto.luna.agent.web.JettyConfiguration;
import fun.efto.luna.agent.web.JettyWebServer;
import fun.efto.luna.core.instrument.InstrumentationHolder;
import fun.efto.luna.core.init.InitializerManager;
import fun.efto.luna.core.injection.InjectionManager;
import fun.efto.luna.core.injection.InjectionPointRegistry;
import fun.efto.luna.core.injection.InjectionService;
import fun.efto.luna.core.injection.port.BytecodeLoader;
import fun.efto.luna.core.injection.port.InjectionStore;
import fun.efto.luna.core.injection.port.InjectionVerifier;
import fun.efto.luna.core.injection.port.LocalVarValidator;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.asm.AsmInjectionContext;
import fun.efto.luna.core.asm.LocalVariableScanner;
import fun.efto.luna.core.injection.InjectionValidator;
import fun.efto.luna.core.injection.port.BytecodePreviewer;
import fun.efto.luna.core.plugin.loader.LunaAgentClassLoader;
import fun.efto.luna.core.plugin.DefaultLogEmitter;
import fun.efto.luna.core.plugin.LunaPlugin;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.lifecycle.ReadyGate;
import fun.efto.luna.core.rule.RuleManager;
import fun.efto.luna.core.rule.template.TemplateRegistry;
import fun.efto.luna.core.rule.template.TemplateService;
import fun.efto.luna.core.transformer.RuleClassFileTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 18:52
 */
@SuppressWarnings("java:S106")
public class Agent {
    private static Logger logger;
    private static volatile ClassLoader lunaAgentClassLoader;

    private Agent() {
    }

    public static void premain(String args, Instrumentation inst) {
        executeWithAgentClassLoader(args, inst);
        System.out.println("[Luna] agent premain");
    }

    public static void agentmain(String args, Instrumentation inst) {
        executeWithAgentClassLoader(args, inst);
        System.out.println("[Luna] agent agentmain");
    }

    private static void executeWithAgentClassLoader(String args, Instrumentation inst) {
        try {
            initializeAgentEnvironment();
            Class<?> agentMainClass = lunaAgentClassLoader.loadClass("fun.efto.luna.agent.Agent");
            Method method = agentMainClass.getDeclaredMethod("startAgent", String.class, Instrumentation.class);
            method.setAccessible(true);
            method.invoke(null, args, inst);
        } catch (Exception e) {
            System.err.println("[Luna] Failed to execute with custom classloader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("java:S1144")
    private static void startAgent(String args, Instrumentation inst) {
        initLogger();
        try {
            String agentJarPath = Agent.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            logger.info("Appending agent jar to bootstrap classloader: {}", agentJarPath);
            inst.appendToBootstrapClassLoaderSearch(new JarFile(agentJarPath));

            logger.info("Initializing Luna agent components...");
            InitializerManager.getInstance().initializeAll();
            InstrumentationHolder.init(inst);

            initializeBuiltinPlugins();

            ClassScanner classScanner = ClassScanner.getInstance(inst, createExcludeClassFilter());
            ClassResourceHelper classResourceHelper = new ClassResourceHelper(inst);

            InjectionService injectionService = assembleInjectionService(classResourceHelper, inst);

            RuleManager ruleManager = RuleManager.getInstance();
            TemplateService templateService = new TemplateService(
                    TemplateRegistry.getInstance(), ruleManager);

            JettyWebServer jettyWebServer = new JettyWebServer(
                    8421, JettyConfiguration.createDevelopment(),
                    classScanner, classResourceHelper, injectionService,
                    ruleManager, templateService);

            jettyWebServer.start();
            logger.info("Luna agent started successfully, web server on port 8421");

            inst.addTransformer(new RuleClassFileTransformer(), true);
            applyRulesToLoadedClasses(inst);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.info("Shutting down Jetty server...");
                    jettyWebServer.stop();
                    logger.info("Jetty server stopped.");
                } catch (Exception e) {
                    logger.error("Error stopping Jetty server", e);
                }
            }));
        } catch (Exception e) {
            System.err.println("[Luna] Failed to start agent: " + e.getMessage());
            e.printStackTrace();
            if (logger != null) {
                logger.error("Failed to start Luna agent", e);
            }
        }
    }

    private static InjectionService assembleInjectionService(ClassResourceHelper classResourceHelper,
                                                              Instrumentation inst) {
        InjectionManager injectionManager = InjectionManager.getInstance();

        // Retransformer 适配器
        Retransformer retransformer = new Retransformer() {
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
        };

        // InjectionStore 适配器
        InjectionStore injectionStore = InjectionPointRegistry.getInstance().asInjectionStore();

        // 注册端口到 InjectionManager
        injectionManager.setRetransformer(retransformer);
        injectionManager.setInjectionStore(injectionStore);

        // BytecodeLoader 适配器
        BytecodeLoader bytecodeLoader = className -> classResourceHelper.loadClassBytes(className);

        // LocalVarValidator 适配器
        LocalVarValidator localVarValidator = (code, className, methodName, methodDesc, lineNumber, classBytes) -> {
            if (code == null || !code.contains("$")) return null;
            if (lineNumber == null || lineNumber < 1) return null;
            try {
                List<AsmInjectionContext.LocalVarInfo> vars =
                        LocalVariableScanner.scanVisibleLocalVariables(
                                classBytes, methodName, methodDesc, lineNumber);
                List<InjectionValidator.LocalVarInfo> domainVars =
                new ArrayList<>();
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

        // InjectionVerifier 适配器
        InjectionVerifier injectionVerifier = new InjectionTestHarnessAdapter(inst);

        // BytecodePreviewer 适配器（InjectionService.preview() 直接用 ClassTransformer，此端口仅作备用）
        BytecodePreviewer bytecodePreviewer =
                (injectionId, className, originalBytes) -> {
                    throw new UnsupportedOperationException(
                            "Use InjectionService.preview() which directly uses ClassTransformer");
                };

        return new InjectionService(injectionManager, bytecodeLoader, bytecodePreviewer,
                localVarValidator, injectionVerifier);
    }

    private static void applyRulesToLoadedClasses(Instrumentation inst) {
        logger.info("Scanning already loaded classes for matching rules...");
        Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
        List<Class<?>> targets = new ArrayList<>();

        for (Class<?> clazz : allLoadedClasses) {
            String className = clazz.getName();
            if (className.startsWith("java.") || className.startsWith("sun.") || className.startsWith("fun.efto.luna.")) {
                continue;
            }

            if (!RuleManager.getInstance().findRulesForClass(className).isEmpty()) {
                if (inst.isModifiableClass(clazz)) {
                    targets.add(clazz);
                }
            }
        }

        if (!targets.isEmpty()) {
            try {
                logger.info("Applying rules to {} classes via retransform...", targets.size());
                inst.retransformClasses(targets.toArray(new Class<?>[0]));
            } catch (Exception e) {
                logger.error("Initial retransform failed", e);
            }
        }
        logger.info("Initial rule application completed.");
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

    private static void initLogger() {
        try {
            LoggerInitializer.initLoggerContext();
            logger = LoggerFactory.getLogger(Agent.class);
            logger.info("Luna logger initialized successfully");
        } catch (Exception e) {
            System.err.println("[Luna] 初始化 logger 失败: " + e.getMessage());
            e.printStackTrace();
            logger = LoggerFactory.getLogger(Agent.class);
        }
    }

    private static void initializeBuiltinPlugins() {
        List<LunaPlugin> builtinPlugins = new ArrayList<>();
        java.util.ServiceLoader<LunaPlugin> loader = java.util.ServiceLoader.load(LunaPlugin.class);
        for (LunaPlugin plugin : loader) {
            builtinPlugins.add(plugin);
        }

        if (builtinPlugins.isEmpty()) {
            logger.warn("No builtin plugins found via ServiceLoader");
            return;
        }

        PluginManagerImpl pluginManager = new PluginManagerImpl(
                new ReadyGate(),
                new DefaultLogEmitter(),
                new fun.efto.luna.core.buffer.RingBuffer<>(4096),
                null,
                null,
                null
        );

        for (LunaPlugin plugin : builtinPlugins) {
            try {
                pluginManager.initializeAll(java.util.Collections.singletonList(plugin));
                logger.info("Initialized builtin plugin: {} ({})", plugin.getId(), plugin.getDisplayName());
            } catch (Exception e) {
                logger.warn("Skipped builtin plugin {} due to unmet dependencies: {}", plugin.getId(), e.getMessage());
            }
        }
    }

    private static void initializeAgentEnvironment() {
        try {
            String agentJarPath = Agent.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();

            URL[] urls = {new File(agentJarPath).toURI().toURL()};
            lunaAgentClassLoader = new LunaAgentClassLoader(urls,
                    Agent.class.getClassLoader().getParent());
            Thread.currentThread().setContextClassLoader(lunaAgentClassLoader);
        } catch (Exception e) {
            System.err.println("Failed to create luna agent classloader: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        initLogger();
        logger.info("Luna agent main");
    }
}
