package fun.efto.luna.agent.clazz;

import fun.efto.luna.core.infra.util.ClassNameUtils;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ：Tony.L(286269159@qq.com)
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

    /**
     * 获取所有已加载类的去重包名列表（按字母排序）。
     * 用于前端"浏览包名"功能，让用户发现目标包名后再按包扫描。
     */
    public List<String> getLoadedPackages() {
        TreeSet<String> packages = new TreeSet<>();
        for (Set<LoadedClass> classes : internalCache.values()) {
            for (LoadedClass lc : classes) {
                String className = lc.getClassName();
                int lastDot = className.lastIndexOf('.');
                if (lastDot > 0) {
                    packages.add(className.substring(0, lastDot));
                }
            }
        }
        return new ArrayList<>(packages);
    }

    /**
     * 按包名前缀过滤已加载类。
     * 用户输入 "com.example" 可匹配 com.example.Foo、com.example.bar.Baz 等。
     */
    public Map<String, Set<LoadedClass>> getLoadedClassesByPackage(String packagePrefix) {
        Map<String, Set<LoadedClass>> result = new ConcurrentHashMap<>();
        String prefix = packagePrefix.endsWith(".") ? packagePrefix : packagePrefix + ".";
        for (Map.Entry<String, Set<LoadedClass>> entry : internalCache.entrySet()) {
            Set<LoadedClass> filtered = ConcurrentHashMap.newKeySet();
            for (LoadedClass lc : entry.getValue()) {
                if (lc.getClassName().startsWith(prefix)) {
                    filtered.add(lc);
                }
            }
            if (!filtered.isEmpty()) {
                result.put(entry.getKey(), filtered);
            }
        }
        return result;
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
