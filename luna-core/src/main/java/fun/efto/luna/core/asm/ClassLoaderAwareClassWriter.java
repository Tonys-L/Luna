package fun.efto.luna.core.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/02 21:00
 */
public class ClassLoaderAwareClassWriter extends ClassWriter {

    private final ClassLoader targetClassLoader;

    public ClassLoaderAwareClassWriter(ClassReader cr, int flags, ClassLoader targetClassLoader) {
        super(cr, flags);
        this.targetClassLoader = targetClassLoader != null ? targetClassLoader : ClassLoader.getSystemClassLoader();
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
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
