package fun.efto.luna.core.common;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/15 11:00
 */
public final class InstrumentationHolder {

    private static volatile java.lang.instrument.Instrumentation instrumentation;

    private InstrumentationHolder() {
    }

    public static void init(java.lang.instrument.Instrumentation inst) {
        if (instrumentation == null) {
            synchronized (InstrumentationHolder.class) {
                if (instrumentation == null) {
                    instrumentation = inst;
                }
            }
        }
    }

    public static java.lang.instrument.Instrumentation getInstrumentation() {
        java.lang.instrument.Instrumentation result = instrumentation;
        if (result == null) {
            throw new IllegalStateException("Instrumentation not initialized");
        }
        return result;
    }

    public static Class<?>[] getAllLoadedClasses() {
        return getInstrumentation().getAllLoadedClasses();
    }

    public static List<Class<?>> findModifiableClasses(Predicate<String> classNameFilter) {
        java.lang.instrument.Instrumentation inst = getInstrumentation();
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (classNameFilter.test(clazz.getName()) && inst.isModifiableClass(clazz)) {
                result.add(clazz);
            }
        }
        return result;
    }

    public static List<Class<?>> findClasses(String className) {
        java.lang.instrument.Instrumentation inst = getInstrumentation();
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz.getName().equals(className) && inst.isModifiableClass(clazz)) {
                result.add(clazz);
            }
        }
        return result;
    }

    public static void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        getInstrumentation().retransformClasses(classes);
    }

    public static void addTransformer(java.lang.instrument.ClassFileTransformer transformer, boolean canRetransform) {
        getInstrumentation().addTransformer(transformer, canRetransform);
    }

    public static void removeTransformer(java.lang.instrument.ClassFileTransformer transformer) {
        getInstrumentation().removeTransformer(transformer);
    }

    public static boolean isModifiableClass(Class<?> clazz) {
        return getInstrumentation().isModifiableClass(clazz);
    }
}
