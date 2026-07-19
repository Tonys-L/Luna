package fun.efto.luna.core.bytecode.bytekit.adapter;

import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.ClassWriter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 22:00
 */
public class ByteKitClassLoaderAwareClassWriter extends ClassWriter {

    private static final ConcurrentHashMap<String, String> SUPER_CLASS_CACHE = new ConcurrentHashMap<>();

    private final ClassLoader targetClassLoader;

    public ByteKitClassLoaderAwareClassWriter(int flags, ClassLoader targetClassLoader) {
        super(flags);
        this.targetClassLoader = targetClassLoader != null ? targetClassLoader : ClassLoader.getSystemClassLoader();
    }

    public ByteKitClassLoaderAwareClassWriter(ClassReader classReader, int flags, ClassLoader targetClassLoader) {
        super(classReader, flags);
        this.targetClassLoader = targetClassLoader != null ? targetClassLoader : ClassLoader.getSystemClassLoader();
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        String cacheKey = type1 + "|" + type2;
        String cached = SUPER_CLASS_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String result = computeCommonSuperClass(type1, type2);
        SUPER_CLASS_CACHE.put(cacheKey, result);
        return result;
    }

    private String computeCommonSuperClass(String type1, String type2) {
        try {
            Class<?> class1 = Class.forName(type1.replace('/', '.'), false, targetClassLoader);
            Class<?> class2 = Class.forName(type2.replace('/', '.'), false, targetClassLoader);
            if (class1.isAssignableFrom(class2)) {
                return type1;
            }
            if (class2.isAssignableFrom(class1)) {
                return type2;
            }
            if (!class1.isInterface() && !class2.isInterface()) {
                Class<?> superClass = class1;
                while (superClass != null && superClass != Object.class) {
                    superClass = superClass.getSuperclass();
                    if (superClass != null && superClass.isAssignableFrom(class2)) {
                        return superClass.getName().replace('.', '/');
                    }
                }
            }
            return "java/lang/Object";
        } catch (ClassNotFoundException e) {
            return "java/lang/Object";
        }
    }
}
