package fun.efto.luna.agent.clazz;

import fun.efto.luna.core.util.ClassNameUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 15:07
 */
public final class ClassScanner {
    private static volatile ClassScanner instance;
    private final ExcludeClassFilter excludeClassFilter;
    private final Set<String> loadedClassName = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicBoolean canUpdateCache = new AtomicBoolean(true);
    private Instrumentation inst;
    private Map<String, Set<LoadedClass>> internalCache = new ConcurrentHashMap<>();

    private ClassScanner(Instrumentation inst, ExcludeClassFilter excludeClassFilter) {
        this.inst = inst;
        this.excludeClassFilter = new CompositeExcludeClassFilter(excludeClassFilter, new ExcludeLoadedClassFilter(loadedClassName));
        scan();
        inst.addTransformer(new ClassLoadMonitor(this));
    }

    public static ClassScanner getInstance(Instrumentation inst, ExcludeClassFilter excludeClassFilter) {
        if (instance == null) {
            synchronized (ClassScanner.class) {
                if (instance == null) {
                    instance = new ClassScanner(inst, excludeClassFilter);
                }
            }
        }
        return instance;
    }

    private void initCache() {
        internalCache = new ConcurrentHashMap<>();
        loadedClassName.clear();
        Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
        for (Class<?> clazz : allLoadedClasses) {
            addToCache(clazz);
        }
    }


    private void addToCache(Class<?> clazz) {
        if (Objects.nonNull(clazz)) {
            String className = clazz.getName();
            addToCache(className, clazz.getClassLoader(), clazz.getProtectionDomain());
        }
    }

    private void addToCache(String className, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        if (Objects.nonNull(className) && !className.isEmpty() && !excludeClassFilter.filter(className, classLoader, protectionDomain)) {
            String classLoaderName = determineClassLoaderName(classLoader);
            internalCache.computeIfAbsent(classLoaderName, k -> ConcurrentHashMap.newKeySet())
                    .add(new LoadedClass(className));
            loadedClassName.add(className);
        }

    }

    private String determineClassLoaderName(ClassLoader classLoader) {
        String name;
        if (classLoader == null) {
            name = "Bootstrap";
        } else {
            name = classLoader.getClass().getName();
            if (name.isEmpty()) {
                name = "unknown";
            }
        }
        return name;
    }

    public Map<String, Set<LoadedClass>> scan() {
        if (!canUpdateCache.compareAndSet(true, false)) {
            throw new IllegalStateException("Scanning is already in progress");
        }
        try {
            initCache();
        } finally {
            canUpdateCache.set(true);
        }
        return Collections.unmodifiableMap(internalCache);
    }

    public Map<String, Set<LoadedClass>> getLoadedClasses() {
        return Collections.unmodifiableMap(internalCache);
    }

    private static class ClassLoadMonitor implements ClassFileTransformer {
        private final ClassScanner classScanner;

        public ClassLoadMonitor(ClassScanner classScanner) {
            this.classScanner = classScanner;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (!classScanner.canUpdateCache.compareAndSet(true, false)) {
                return classfileBuffer;
            }
            try {
                if (className != null) {
                    String fqn = ClassNameUtils.toFqn(className);
                    classScanner.addToCache(fqn, loader, protectionDomain);
                }
            } finally {
                classScanner.canUpdateCache.set(true);
            }
            return classfileBuffer;
        }
    }

    private static class ExcludeLoadedClassFilter implements ExcludeClassFilter {

        private final Set<String> loadedClassName;

        public ExcludeLoadedClassFilter(Set<String> loadedClassName) {
            this.loadedClassName = loadedClassName;
        }

        @Override
        public boolean filter(String fqn, ClassLoader classLoader, ProtectionDomain protectionDomain) {
            return loadedClassName.contains(fqn);
        }
    }
}

