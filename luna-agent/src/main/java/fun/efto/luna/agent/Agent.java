package fun.efto.luna.agent;

import fun.efto.luna.agent.runtime.AgentRuntime;
import fun.efto.luna.core.plugin.lifecycle.PluginManagerImpl;
import fun.efto.luna.core.plugin.loader.LunaAgentClassLoader;
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
        AgentRuntime.start(args, inst);
    }

    public static PluginManagerImpl getPluginManager() {
        AgentRuntime runtime = AgentRuntime.getInstance();
        if (runtime != null && runtime.getContext() != null) {
            return (PluginManagerImpl) runtime.getContext().getPluginManager();
        }
        return null;
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
        logger = LoggerFactory.getLogger(Agent.class);
        logger.info("Luna agent main");
    }
}
