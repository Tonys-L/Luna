package fun.efto.luna.agent.clazz;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * 类资源访问辅助，封装 Instrumentation + 类字节码加载。
 * 不属于 Web 层，属于类扫描/加载基础设施。
 *
 * <p>字节码获取策略（参考 arthas 的 JadDecompiler）：
 * <ol>
 *   <li>优先通过 {@code ClassLoader.getResourceAsStream} 获取（轻量，无需 retransform）。</li>
 *   <li>失败时通过 {@code retransformClasses} 获取（JVM 内存直取，最可靠）。</li>
 * </ol>
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/09 10:00
 */
public class ClassResourceHelper {

    private final Instrumentation instrumentation;

    public ClassResourceHelper(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public Class<?> findLoadedClass(String className) {
        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        for (Class<?> clazz : allLoadedClasses) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        return null;
    }

    /**
     * 加载指定类的字节码。
     *
     * <p>策略 1：通过 {@code ClassLoader.getResourceAsStream} 获取（轻量）。
     * 策略 2：失败时通过 {@code retransformClasses} 获取（参考 arthas，JVM 内存直取，最可靠）。
     *
     * @param className 全限定类名
     * @return 类字节码
     * @throws IOException 获取失败
     */
    public byte[] loadClassBytes(String className) throws IOException {
        Class<?> targetClass = findLoadedClass(className);
        ClassLoader loader = targetClass != null
                ? targetClass.getClassLoader()
                : ClassLoader.getSystemClassLoader();

        // 策略 1：getResourceAsStream（轻量，无需 retransform）
        try {
            return loadClassBytesFromLoader(className, loader);
        } catch (IOException e) {
            // getResourceAsStream 失败（如 fat jar 内部类、动态代理类等），
            // 降级到策略 2：retransformClasses
            if (targetClass != null) {
                return loadClassBytesViaRetransform(targetClass);
            }
            throw e;
        }
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public int getLoadedClassCount() {
        return instrumentation.getAllLoadedClasses().length;
    }

    private byte[] loadClassBytesFromLoader(String className, ClassLoader loader) throws IOException {
        String path = className.replace('.', '/') + ".class";
        InputStream is = loader.getResourceAsStream(path);
        if (is == null) {
            throw new IOException("Cannot find class file for: " + className);
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }

    /**
     * 参考 arthas 的 ClassDumpTransformer：通过 retransformClasses 回调收集目标类的字节码。
     *
     * <p>JVM 会同步回调 {@link ClassFileTransformer#transform}，传入类加载时的原始字节码。
     * 此方式不依赖文件系统、不依赖 ClassLoader 实现，只要类已加载就能拿到字节码。
     *
     * <p>transform 返回 null（不修改字节码），对目标类无副作用。
     *
     * @param targetClass 目标类
     * @return 类字节码
     * @throws IOException retransform 失败
     */
    private byte[] loadClassBytesViaRetransform(Class<?> targetClass) throws IOException {
        ClassDumpTransformer transformer = new ClassDumpTransformer(targetClass);
        try {
            instrumentation.addTransformer(transformer, true);
            instrumentation.retransformClasses(targetClass);
        } catch (Throwable e) {
            throw new IOException("Retransform failed for " + targetClass.getName() + ": " + e.getMessage(), e);
        } finally {
            instrumentation.removeTransformer(transformer);
        }

        byte[] bytes = transformer.getDumpedBytes();
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Retransform did not return bytecode for: " + targetClass.getName());
        }
        return bytes;
    }

    /**
     * 专用于获取字节码的 Transformer，参考 arthas 的 ClassDumpTransformer。
     * transform 返回 null（不修改字节码），仅收集 classfileBuffer。
     */
    private static class ClassDumpTransformer implements ClassFileTransformer {
        private final Class<?> targetClass;
        private volatile byte[] dumpedBytes;

        ClassDumpTransformer(Class<?> targetClass) {
            this.targetClass = targetClass;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) {
            if (targetClass.equals(classBeingRedefined)) {
                dumpedBytes = classfileBuffer;
            }
            return null;
        }

        byte[] getDumpedBytes() {
            return dumpedBytes;
        }
    }
}
