package fun.efto.luna.core;

import java.lang.instrument.Instrumentation;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 23:11
 */
class InstrumentationManager {
    private static volatile InstrumentationManager instance;
    private final Instrumentation inst;

    private InstrumentationManager(Instrumentation instrumentation) {
        this.inst = instrumentation;
    }

    public static InstrumentationManager init(Instrumentation instrumentation) {
        if (instance == null) {
            synchronized (InstrumentationManager.class) {
                if (instance == null) {
                    instance = new InstrumentationManager(instrumentation);
                }
            }
        }
        return instance;
    }

    public static InstrumentationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("未初始化");
        }
        return instance;
    }

    public Instrumentation getInstrumentation() {
        return inst;
    }

    public Class<?>[] getAllLoadedClasses() {
        return inst.getAllLoadedClasses();
    }


    public void retransformClasses(Class<?>... classes) throws java.lang.instrument.UnmodifiableClassException {
        inst.retransformClasses(classes);
    }


    public void addTransformer(java.lang.instrument.ClassFileTransformer transformer, boolean canRetransform) {
        inst.addTransformer(transformer, canRetransform);
    }

    public void removeTransformer(java.lang.instrument.ClassFileTransformer transformer) {
        inst.removeTransformer(transformer);
    }
}
