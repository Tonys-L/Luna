package fun.efto.luna.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 访问标识格式化工具类
 * 将Java字节码中的访问标识转换为可读的字符串格式
 *
 * @author Tony.L(286269159 @ qq.com)
 * @since 2025/10/23
 */
public final class AccessFlagsConverter {

    public static final String PUBLIC = "public";
    public static final String PRIVATE = "private";
    public static final String PROTECTED = "protected";
    public static final String STATIC = "static";
    public static final String FINAL = "final";
    public static final String BRIDGE = "bridge";
    public static final String VOLATILE = "volatile";
    public static final String VARARGS = "varargs";
    public static final String TRANSIENT = "transient";
    public static final String NATIVE = "native";
    public static final String ABSTRACT = "abstract";
    public static final String STRICTFP = "strictfp";
    public static final String SYNTHETIC = "synthetic";
    public static final String METHOD = "method";
    public static final String CLASS = "class";
    public static final String SYNCHRONIZED = "synchronized";
    public static final String SUPER = "super";
    public static final String INTERFACE = "interface";
    public static final String ANNOTATION = "annotation";
    public static final String ENUM = "enum";
    public static final String MODULE = "module";
    public static final String PACKAGE_PRIVATE = "package-private";
    public static final String FIELD = "field";

    private AccessFlagsConverter() {
    }

    /**
     * 将Java访问标识转换为可读的字符串格式
     *
     * @param accessFlags Java访问标识
     * @param type        类型 ("class", "field", "method")
     * @return 可读的访问标识字符串
     */
    public static String convertAccessFlags(int accessFlags, String type) {
        List<String> flags = new ArrayList<>();
        addCommonAccessFlags(flags, accessFlags, type);
        addTypeSpecificFlags(flags, accessFlags, type);
        return flags.isEmpty() ? PACKAGE_PRIVATE : String.join(" ", flags);
    }

    /**
     * 将类访问标识转换为简化的可读格式
     *
     * @param accessFlags 类访问标识
     * @return 简化的访问标识字符串
     */
    public static String convertClassAccessFlags(int accessFlags) {
        return convertAccessFlags(accessFlags, CLASS);
    }

    /**
     * 将字段访问标识转换为简化的可读格式
     *
     * @param accessFlags 字段访问标识
     * @return 简化的访问标识字符串
     */
    public static String convertFieldAccessFlags(int accessFlags) {
        return convertAccessFlags(accessFlags, FIELD);
    }

    /**
     * 将方法访问标识转换为简化的可读格式
     *
     * @param accessFlags 方法访问标识
     * @return 简化的访问标识字符串
     */
    public static String convertMethodAccessFlags(int accessFlags) {
        return convertAccessFlags(accessFlags, METHOD);
    }

    private static void addCommonAccessFlags(List<String> flags, int accessFlags, String type) {
        if (isPublic(accessFlags)) {
            flags.add(PUBLIC);
        }
        if (isPrivate(accessFlags)) {
            flags.add(PRIVATE);
        }
        if (isProtected(accessFlags)) {
            flags.add(PROTECTED);
        }
        if (isStatic(accessFlags)) {
            flags.add(STATIC);
        }
        if (isFinal(accessFlags)) {
            flags.add(FINAL);
        }
        if (isBridgeOrVolatile(accessFlags, type)) {
            flags.add(BRIDGE.equals(getBridgeOrVolatileFlag(type)) ? BRIDGE : VOLATILE);
        }
        if (isVarArgsOrTransient(accessFlags, type)) {
            flags.add(VARARGS.equals(getVarArgsOrTransientFlag(type)) ? VARARGS : TRANSIENT);
        }
        if (isNative(accessFlags)) {
            flags.add(NATIVE);
        }
        if (isAbstract(accessFlags)) {
            flags.add(ABSTRACT);
        }
        if (isStrictfp(accessFlags)) {
            flags.add(STRICTFP);
        }
        if (isSynthetic(accessFlags)) {
            flags.add(SYNTHETIC);
        }
    }

    private static void addTypeSpecificFlags(List<String> flags, int accessFlags, String type) {
        if (CLASS.equals(type)) {
            addClassSpecificFlags(flags, accessFlags);
        } else if (METHOD.equals(type) && (isSynchronized(accessFlags))) {
            flags.add(SYNCHRONIZED);
        }
    }

    private static void addClassSpecificFlags(List<String> flags, int accessFlags) {
        if (isSuper(accessFlags)) {
            flags.add(SUPER);
        }
        if (isInterface(accessFlags)) {
            flags.add(INTERFACE);
        }
        if (isAnnotation(accessFlags)) {
            flags.add(ANNOTATION);
        }
        if (isEnum(accessFlags)) {
            flags.add(ENUM);
        }
        if (isModule(accessFlags)) {
            flags.add(MODULE);
        }
    }


    private static String getBridgeOrVolatileFlag(String type) {
        return METHOD.equals(type) ? BRIDGE : VOLATILE;
    }

    private static String getVarArgsOrTransientFlag(String type) {
        return METHOD.equals(type) ? VARARGS : TRANSIENT;
    }

    private static boolean isPublic(int accessFlags) {
        return (accessFlags & 0x0001) != 0;
    }

    private static boolean isPrivate(int accessFlags) {
        return (accessFlags & 0x0002) != 0;
    }

    private static boolean isProtected(int accessFlags) {
        return (accessFlags & 0x0004) != 0;
    }

    private static boolean isStatic(int accessFlags) {
        return (accessFlags & 0x0008) != 0;
    }

    private static boolean isFinal(int accessFlags) {
        return (accessFlags & 0x0010) != 0;
    }

    private static boolean isSuper(int accessFlags) {
        return (accessFlags & 0x0020) != 0;
    }

    private static boolean isSynchronized(int accessFlags) {
        return (accessFlags & 0x0020) != 0;
    }

    private static boolean isBridgeOrVolatile(int accessFlags, String type) {
        return (accessFlags & 0x0040) != 0;
    }

    private static boolean isVarArgsOrTransient(int accessFlags, String type) {
        return (accessFlags & 0x0080) != 0;
    }

    private static boolean isNative(int accessFlags) {
        return (accessFlags & 0x0100) != 0;
    }

    private static boolean isInterface(int accessFlags) {
        return (accessFlags & 0x0200) != 0;
    }

    private static boolean isAbstract(int accessFlags) {
        return (accessFlags & 0x0400) != 0;
    }

    private static boolean isStrictfp(int accessFlags) {
        return (accessFlags & 0x0800) != 0;
    }

    private static boolean isSynthetic(int accessFlags) {
        return (accessFlags & 0x1000) != 0;
    }

    private static boolean isAnnotation(int accessFlags) {
        return (accessFlags & 0x2000) != 0;
    }

    private static boolean isEnum(int accessFlags) {
        return (accessFlags & 0x4000) != 0;
    }

    private static boolean isModule(int accessFlags) {
        return (accessFlags & 0x8000) != 0;
    }
}