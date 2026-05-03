package fun.efto.luna.core.testing;

import fun.efto.luna.core.InstrumentationManager;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.injection.target.type.MethodInjectionType;
import fun.efto.luna.core.transformer.ClassFileTransformerAdapter;
import fun.efto.luna.core.transformer.ClassTransformer;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import fun.efto.luna.core.transformer.TransformerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/02 10:00
 */
public class InjectionTestHarness {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionTestHarness.class);

    private final Instrumentation instrumentation;
    private final ReentrantLock lock = new ReentrantLock();

    public InjectionTestHarness(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public TestResult testInjection(String className, String methodName, String descriptor,
                                    String injectionTypeStr, String code) {
        lock.lock();
        try {
            return doTestInjection(className, methodName, descriptor, injectionTypeStr, code);
        } finally {
            lock.unlock();
        }
    }

    public TestResult verifyOnly(String className, String methodName) {
        lock.lock();
        try {
            return doVerifyOnly(className, methodName);
        } finally {
            lock.unlock();
        }
    }

    private TestResult doVerifyOnly(String className, String methodName) {
        Class<?> targetClass = findLoadedClass(className);
        if (targetClass == null) {
            return TestResult.fail("Class not loaded: " + className);
        }

        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(baos, true);

        try {
            Method targetMethod = findMethod(targetClass, methodName);
            if (targetMethod == null) {
                return TestResult.fail("Method not found: " + methodName);
            }

            targetMethod.setAccessible(true);
            Object instance = null;
            if (!Modifier.isStatic(targetMethod.getModifiers())) {
                try {
                    instance = targetClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    return TestResult.fail("Cannot create instance: " + e.getMessage());
                }
            }

            Object[] args = buildDefaultArgs(targetMethod);

            System.setOut(captureStream);
            try {
                targetMethod.invoke(instance, args);
            } catch (Exception e) {
                // method threw exception, but injected code may have already printed output
            } finally {
                System.setOut(originalOut);
            }

            String output = baos.toString();
            return TestResult.success(output, null);
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return TestResult.fail(errorMsg);
        }
    }

    private TestResult doTestInjection(String className, String methodName, String descriptor,
                                       String injectionTypeStr, String code) {
        MethodInjectionType injectionType = resolveInjectionType(injectionTypeStr);
        MethodTarget target = new MethodTarget(injectionType, className, methodName, descriptor);

        InjectableCode injectableCode = new InjectableCode() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, injectableCode);
        ClassTransformer classTransformer = new DefaultClassTransformer();
        ClassFileTransformerAdapter adapter = new ClassFileTransformerAdapter(injectionPoint, classTransformer);

        InstrumentationManager instManager = InstrumentationManager.getInstance();

        Class<?> targetClass = findLoadedClass(className);
        if (targetClass == null) {
            return TestResult.fail("Class not loaded: " + className);
        }

        instManager.addTransformer(adapter, true);
        byte[] generatedBytecode = null;

        try {
            instManager.retransformClasses(targetClass);
        } catch (Exception e) {
            LOGGER.error("Retransform failed for class: {}", className, e);
            return TestResult.fail("Retransform failed: " + e.getMessage());
        } finally {
            instManager.removeTransformer(adapter);
        }

        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(baos, true);

        try {
            Method targetMethod = findMethod(targetClass, methodName);
            if (targetMethod == null) {
                return TestResult.fail("Method not found: " + methodName);
            }

            targetMethod.setAccessible(true);
            Object instance = null;
            if (!Modifier.isStatic(targetMethod.getModifiers())) {
                try {
                    instance = targetClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    return TestResult.fail("Cannot create instance: " + e.getMessage());
                }
            }

            Object[] args = buildDefaultArgs(targetMethod);

            System.setOut(captureStream);
            try {
                targetMethod.invoke(instance, args);
            } catch (Exception e) {
                // method threw exception, but injected code may have already printed output
            } finally {
                System.setOut(originalOut);
            }

            String output = baos.toString();
            return TestResult.success(output, generatedBytecode);
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return TestResult.fail(errorMsg, generatedBytecode);
        }
    }

    public TestResult dryRun(String className, String methodName, String descriptor,
                             String injectionTypeStr, String code) {
        MethodInjectionType injectionType = resolveInjectionType(injectionTypeStr);
        MethodTarget target = new MethodTarget(injectionType, className, methodName, descriptor);

        InjectableCode injectableCode = new InjectableCode() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public CodeType getCodeType() {
                return CodeType.EXPRESSION;
            }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, injectableCode);

        byte[] classBytes;
        try {
            classBytes = loadClassBytes(className);
        } catch (IOException e) {
            return TestResult.fail("Cannot read class bytes: " + e.getMessage());
        }

        ClassTransformer classTransformer = new DefaultClassTransformer();
        TransformerResult result = classTransformer.transform(injectionPoint, className, classBytes);

        if (result.isTransformed()) {
            return TestResult.success(result.getMessage(), result.getBytecode(), classBytes.length);
        } else {
            return TestResult.fail(result.getMessage(), result.getBytecode());
        }
    }

    private Class<?> findLoadedClass(String className) {
        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        for (Class<?> clazz : allLoadedClasses) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        return null;
    }

    private Method findMethod(Class<?> clazz, String methodName) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) {
                return m;
            }
        }
        return null;
    }

    private Object[] buildDefaultArgs(Method method) {
        java.lang.Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = getDefaultValue(paramTypes[i]);
        }
        return args;
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == String.class) return "";
        return null;
    }

    private MethodInjectionType resolveInjectionType(String injectionTypeStr) {
        if (injectionTypeStr == null || injectionTypeStr.isEmpty()) {
            return MethodInjectionType.ENTER;
        }
        switch (injectionTypeStr.toUpperCase()) {
            case "METHOD_ENTER":
            case "ENTER":
                return MethodInjectionType.ENTER;
            case "METHOD_EXIT":
            case "EXIT":
                return MethodInjectionType.EXIT;
            case "METHOD_AROUND":
            case "AROUND":
                return MethodInjectionType.AROUND;
            default:
                return MethodInjectionType.ENTER;
        }
    }

    private byte[] loadClassBytes(String className) throws IOException {
        String path = className.replace('.', '/') + ".class";
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IOException("Cannot find class file for: " + className);
        }
        try (InputStream is = inputStream;
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
}
