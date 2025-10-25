package fun.efto.luna.core.util;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/2 22:24
 */
public final class ClassNameUtils {
    private ClassNameUtils() {
    }

    public static String toInternalName(String name) {
        return name.replace(".", "/");
    }

    public static String toFqn(String name) {
        return name.replace("/", ".");
    }

    public static String getInternalName(Class<?> clazz) {
        return toInternalName(clazz.getName());
    }

}
