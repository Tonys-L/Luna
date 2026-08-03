package fun.efto.luna.core.plugin.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/02 23:30
 */
public class LunaAgentClassLoaderLog4j2Test {

    @Test
    void testLog4j2LookupClassLoadingConsistency() throws Exception {
        String agentJarPath = System.getProperty("luna.agent.jar");
        if (agentJarPath == null) {
            File jarFile = new File("../luna-agent/target/luna-agent-1.0-SNAPSHOT.jar");
            if (!jarFile.exists()) {
                System.out.println("SKIP: luna-agent JAR not found");
                return;
            }
            agentJarPath = jarFile.getAbsolutePath();
        }

        URL jarUrl = new File(agentJarPath).toURI().toURL();
        LunaAgentClassLoader cl = new LunaAgentClassLoader(
                new URL[]{jarUrl},
                ClassLoader.getSystemClassLoader().getParent()
        );

        Class<?> strLookupClass = cl.loadClass("fun.efto.luna.shadow.log4j2.core.lookup.StrLookup");
        ClassLoader strLookupCL = strLookupClass.getClassLoader();
        System.out.println("[DEBUG-a4f2] StrLookup loaded by: " + strLookupCL);

        Class<?> eventLookupClass = cl.loadClass("fun.efto.luna.shadow.log4j2.core.lookup.EventLookup");
        ClassLoader eventLookupCL = eventLookupClass.getClassLoader();
        System.out.println("[DEBUG-a4f2] EventLookup loaded by: " + eventLookupCL);

        Class<?> interpolatorClass = cl.loadClass("fun.efto.luna.shadow.log4j2.core.lookup.Interpolator");
        ClassLoader interpolatorCL = interpolatorClass.getClassLoader();
        System.out.println("[DEBUG-a4f2] Interpolator loaded by: " + interpolatorCL);

        boolean sameCL = strLookupCL.equals(eventLookupCL);
        System.out.println("[DEBUG-a4f2] Same ClassLoader for StrLookup and EventLookup: " + sameCL);

        if (!sameCL) {
            System.out.println("[DEBUG-a4f2] BUG: Different ClassLoaders will cause ClassCastException!");
            System.out.println("[DEBUG-a4f2] StrLookup CL hash: " + System.identityHashCode(strLookupCL));
            System.out.println("[DEBUG-a4f2] EventLookup CL hash: " + System.identityHashCode(eventLookupCL));
        }

        assertTrue(sameCL, "StrLookup and EventLookup must be loaded by the same ClassLoader");
    }

    @Test
    void testLog4j2InitializationNoClassCastException() throws Exception {
        String agentJarPath = System.getProperty("luna.agent.jar");
        if (agentJarPath == null) {
            File jarFile = new File("../luna-agent/target/luna-agent-1.0-SNAPSHOT.jar");
            if (!jarFile.exists()) {
                System.out.println("SKIP: luna-agent JAR not found");
                return;
            }
            agentJarPath = jarFile.getAbsolutePath();
        }

        URL jarUrl = new File(agentJarPath).toURI().toURL();
        LunaAgentClassLoader cl = new LunaAgentClassLoader(
                new URL[]{jarUrl},
                ClassLoader.getSystemClassLoader().getParent()
        );

        Thread.currentThread().setContextClassLoader(cl);

        Class<?> configuratorClass = cl.loadClass("fun.efto.luna.shadow.log4j2.core.config.Configurator");
        Method initialize = configuratorClass.getMethod("initialize", String.class, String.class);
        try {
            Object result = initialize.invoke(null, "luna-test", (String) null);
            System.out.println("[DEBUG-a4f2] Log4j2 initialized successfully: " + result);
        } catch (Exception e) {
            System.out.println("[DEBUG-a4f2] Log4j2 initialization failed: " + e.getCause());
            if (e.getCause() != null && e.getCause().getCause() != null) {
                System.out.println("[DEBUG-a4f2] Root cause: " + e.getCause().getCause().getClass().getName());
                if (e.getCause().getCause() instanceof ClassCastException) {
                    fail("ClassCastException during Log4j2 initialization: " + e.getCause().getCause().getMessage());
                }
            }
        }
    }
}
