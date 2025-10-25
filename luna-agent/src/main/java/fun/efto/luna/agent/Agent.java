package fun.efto.luna.agent;

import fun.efto.luna.agent.classloader.LunaAgentClassLoader;
import fun.efto.luna.agent.clazz.*;
import fun.efto.luna.agent.log.LoggerInitializer;
import fun.efto.luna.agent.web.JettyConfiguration;
import fun.efto.luna.agent.web.JettyWebServer;
import fun.efto.luna.core.InjectionExecutor;
import fun.efto.luna.core.init.InitializerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 18:52
 */
@SuppressWarnings("java:S106")
public class Agent {
    // 延迟初始化Logger，在日志系统配置完成后再获取Logger实例
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
            // 使用自定义类加载器加载指定类
            Class<?> agentMainClass = lunaAgentClassLoader.loadClass("fun.efto.luna.agent.Agent");
            // 获取对应的方法
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
            InitializerManager.getInstance().initializeAll();
            InjectionExecutor injectionExecutor = InjectionExecutor.init(inst);
            ClassScanner classScanner = ClassScanner.getInstance(inst, createExcludeClassFilter());
            JettyWebServer jettyWebServer = new JettyWebServer(8421, JettyConfiguration.createDevelopment(), injectionExecutor, classScanner);

            jettyWebServer.start();

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
            logger.error("Failed to start Luna agent", e);
            System.err.println("[Luna] Failed to start agent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static CompositeExcludeClassFilter createExcludeClassFilter() {
        return new CompositeExcludeClassFilter(new ExcludeAgentClassFilter(),
                new ExcludeArrayClassFilter(),
                new ExcludeByClassLoaderNameFilter("jdk.internal.reflect.DelegatingClassLoader",
                        "sun.reflect.DelegatingClassLoader",
                        "sun.reflect.misc.MethodUtil",
                        "fun.efto.luna.agent.classloader.LunaAgentClassLoader"),
                new ExcludeGeneratedClassFilter());
    }

    /**
     * 初始化独立的日志上下文，避免影响宿主应用的日志系统
     */
    private static void initLogger() {
        try {
            // 首先初始化独立的日志上下文
            LoggerInitializer.initLoggerContext();
            // 然后获取Logger实例，确保使用的是我们配置的日志系统
            logger = LoggerFactory.getLogger(Agent.class);
            logger.info("Luna logger initialized successfully");
        } catch (Exception e) {
            System.err.println("[Luna] 初始化 logger 失败: " + e.getMessage());
            e.printStackTrace();
            // 即使初始化失败，也确保有一个Logger实例可用
            logger = LoggerFactory.getLogger(Agent.class);
        }
    }

    private static void initializeAgentEnvironment() {
        try {
            // 获取 agent jar 文件路径
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