package fun.efto.luna.agent.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;

/**
 * @author : Tony.L(<286269159@qq.com>)
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

    public byte[] loadClassBytes(String className) throws IOException {
        Class<?> targetClass = findLoadedClass(className);
        if (targetClass == null) {
            return loadClassBytesFromClassLoader(className);
        }
        return loadClassBytesFromLoader(className, targetClass.getClassLoader());
    }

    public byte[] loadClassBytesFromClassLoader(String className) throws IOException {
        return loadClassBytesFromLoader(className, ClassLoader.getSystemClassLoader());
    }

    public Instrumentation getInstrumentation() {
        return instrumentation;
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
}
