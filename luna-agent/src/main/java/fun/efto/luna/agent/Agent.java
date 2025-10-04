package fun.efto.luna.agent;

import fun.efto.luna.agent.clazz.*;
import fun.efto.luna.agent.web.JettyConfiguration;
import fun.efto.luna.agent.web.JettyWebServer;
import fun.efto.luna.core.InjectionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 18:52
 */
public class Agent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

    private Agent() {
    }

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        LOGGER.info("agentmain");

        InjectionExecutor injectionExecutor = InjectionExecutor.init(inst);
        ClassScanner classScanner = ClassScanner.getInstance(inst, new CompositeExcludeClassFilter(new ExcludeAgentClassFilter(), new ExcludeArrayClassFilter(),
                new ExcludeByClassLoaderNameFilter("jdk.internal.reflect.DelegatingClassLoader", "sun.reflect.DelegatingClassLoader", "sun.reflect.misc.MethodUtil")));
        JettyWebServer jettyWebServer = new JettyWebServer(8421, JettyConfiguration.createDevelopment(), injectionExecutor, classScanner);

        try {
            jettyWebServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                LOGGER.info("Shutting down Jetty server...");
                jettyWebServer.stop();  // 假设 JettyWebServer 提供了 stop() 方法
                LOGGER.info("Jetty server stopped.");
            } catch (Exception e) {
                LOGGER.error("Error stopping Jetty server", e);
            }
        }));
    }
}

