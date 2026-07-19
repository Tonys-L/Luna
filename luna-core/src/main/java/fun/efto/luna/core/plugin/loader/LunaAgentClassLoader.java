package fun.efto.luna.core.plugin.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2025/10/18 18:35
 */
public class LunaAgentClassLoader extends URLClassLoader {
    /**
     * Parent-first packages: delegated to parent ClassLoader (Bootstrap CL via Bootstrap JAR).
     * These classes are in the Bootstrap JAR and MUST be loaded by Bootstrap CL because:
     * 1. Injected bytecode references them (e.g., EvaluationContext.create(), ConditionRegistry.test(),
     *    LogProbe.onLog(), SnapshotProbe.onSnapshot(), TraceProbe.onTraceStart/End/Alert(),
     *    InvocationTraceProbe.onMethodEnter/Exit(), RingBuffer.offer())
     * 2. Agent code must share the same class instances with injected bytecode (static state like
     *    ConditionRegistry.AST_CACHE, ProbeOutput.BUFFER)
     *
     * When LunaAgentClassLoader delegates to parent, the parent chain is:
     * LunaAgentClassLoader → Platform CL → Bootstrap CL (finds classes in Bootstrap JAR)
     *
     * Classes NOT in the Bootstrap JAR but in these packages will fall through to
     * LunaAgentClassLoader.findClass() and be loaded normally — no harm done.
     *
     * The "probe." prefix covers all probe runtime classes (LogProbe, TraceProbe, SnapshotProbe,
     * InvocationTraceProbe, RingBuffer, StackFrameCapture, SnapshotSerializer, etc.).
     */
    private static final String[] PARENT_FIRST_PACKAGES = {
            "fun.efto.luna.core.probe.",
            "fun.efto.luna.core.expression.",
    };

    private static final String[] CHILD_FIRST_PACKAGES = {
            "fun.efto.luna.",
    };

    public LunaAgentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public LunaAgentClassLoader(URL[] urls) {
        super(urls);
    }

    public LunaAgentClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public void appendUrl(URL url) {
        addURL(url);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String pkg : PARENT_FIRST_PACKAGES) {
            if (name.startsWith(pkg)) {
                return super.loadClass(name, resolve);
            }
        }

        for (String pkg : CHILD_FIRST_PACKAGES) {
            if (name.startsWith(pkg)) {
                return loadClassIsolated(name);
            }
        }
        return super.loadClass(name, resolve);
    }

    private Class<?> loadClassIsolated(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                try {
                    loadedClass = findClass(name);
                } catch (ClassNotFoundException e) {
                    loadedClass = super.loadClass(name, false);
                }
            }
            return loadedClass;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (name != null && name.contains("Log4j2Plugins")) {
            return findResources(name);
        }
        return super.getResources(name);
    }

}
