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

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2025/10/2 15:07
 */
public final class ClassScanner {
    private static volatile ClassScanner instance;
    private final ExcludeClassFilter excludeClassFilter;
    private final Set<String> loadedClassName = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile Map<String, Set<LoadedClass>> internalCache = new ConcurrentHashMap<>();
    private final Instrumentation inst;

    private ClassScanner(Instrumentation inst, ExcludeClassFilter excludeClassFilter) {
        this.inst = inst;
        this.excludeClassFilter = excludeClassFilter;
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
        Map<String, Set<LoadedClass>> newCache = new ConcurrentHashMap<>();
        Set<String> newLoadedNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

        Class<?>[] allLoadedClasses = inst.getAllLoadedClasses();
        for (Class<?> clazz : allLoadedClasses) {
            if (Objects.nonNull(clazz)) {
                String className = clazz.getName();
                addToCache(className, clazz.getClassLoader(), clazz.getProtectionDomain(), newCache, newLoadedNames);
            }
        }

        this.internalCache = newCache;
        this.loadedClassName.clear();
        this.loadedClassName.addAll(newLoadedNames);
    }

    private void addToCache(String className, ClassLoader classLoader, ProtectionDomain protectionDomain,
                            Map<String, Set<LoadedClass>> cache, Set<String> nameSet) {
        if (Objects.isNull(className) || className.isEmpty()) {
            return;
        }
        if (excludeClassFilter.filter(className, classLoader, protectionDomain)) {
            return;
        }
        if (!nameSet.add(className)) {
            return;
        }
        String classLoaderName = determineClassLoaderName(classLoader);
        cache.computeIfAbsent(classLoaderName, k -> ConcurrentHashMap.newKeySet())
                .add(new LoadedClass(className));
    }

    void onClassLoaded(String className, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        if (Objects.isNull(className) || className.isEmpty()) {
            return;
        }
        if (excludeClassFilter.filter(className, classLoader, protectionDomain)) {
            return;
        }
        if (!loadedClassName.add(className)) {
            return;
        }
        String classLoaderName = determineClassLoaderName(classLoader);
        internalCache.computeIfAbsent(classLoaderName, k -> ConcurrentHashMap.newKeySet())
                .add(new LoadedClass(className));
    }

    private String determineClassLoaderName(ClassLoader classLoader) {
        if (classLoader == null) {
            return "Bootstrap";
        }
        String name = classLoader.getClass().getName();
        return name.isEmpty() ? "unknown" : name;
    }

    public Map<String, Set<LoadedClass>> scan() {
        initCache();
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
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (className != null) {
                String fqn = ClassNameUtils.toFqn(className);
                classScanner.onClassLoaded(fqn, loader, protectionDomain);
            }
            return classfileBuffer;
        }
    }
}
