package fun.efto.luna.agent.adapter;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.CodeType;
import fun.efto.luna.core.injection.port.InjectionVerifier;
import fun.efto.luna.core.injection.target.MethodTarget;
import fun.efto.luna.core.infra.InstrumentationHolder;
import fun.efto.luna.core.plugin.builtin.method.MethodInjectionLocation;
import fun.efto.luna.core.transformer.ClassFileTransformerAdapter;
import fun.efto.luna.core.transformer.ClassTransformer;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 21:00
 */
public class InjectionTestHarnessAdapter implements InjectionVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionTestHarnessAdapter.class);

    private final Instrumentation instrumentation;
    private final ReentrantLock lock = new ReentrantLock();

    public InjectionTestHarnessAdapter(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public VerifyResult testInjection(String className, String methodName, String descriptor,
                                       String injectionLocationStr, String code) {
        lock.lock();
        try {
            return doTestInjection(className, methodName, descriptor, injectionLocationStr, code);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public VerifyResult verifyOnly(String className, String methodName) {
        lock.lock();
        try {
            return doVerifyOnly(className, methodName);
        } finally {
            lock.unlock();
        }
    }

    private VerifyResult doVerifyOnly(String className, String methodName) {
        Class<?> targetClass = findLoadedClass(className);
        if (targetClass == null) {
            return VerifyResult.failure("Class not loaded: " + className);
        }

        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(baos, true);

        try {
            Method targetMethod = findMethod(targetClass, methodName);
            if (targetMethod == null) {
                return VerifyResult.failure("Method not found: " + methodName);
            }

            targetMethod.setAccessible(true);
            Object instance = null;
            if (!Modifier.isStatic(targetMethod.getModifiers())) {
                try {
                    instance = targetClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    return VerifyResult.failure("Cannot create instance: " + e.getMessage());
                }
            }

            Object[] args = buildDefaultArgs(targetMethod);

            System.setOut(captureStream);
            try {
                targetMethod.invoke(instance, args);
            } catch (Exception e) {
            } finally {
                System.setOut(originalOut);
            }

            String output = baos.toString();
            return VerifyResult.success(output);
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return VerifyResult.failure(errorMsg);
        }
    }

    private VerifyResult doTestInjection(String className, String methodName, String descriptor,
                                          String injectionLocationStr, String code) {
        MethodInjectionLocation injectionLocation = resolveInjectionLocation(injectionLocationStr);
        MethodTarget target = new MethodTarget(injectionLocation, className, methodName, descriptor);

        InjectableCode injectableCode = new InjectableCode() {
            @Override
            public String getCode() { return code; }

            @Override
            public CodeType getCodeType() { return CodeType.EXPRESSION; }
        };

        InjectionPoint injectionPoint = new InjectionPoint(target, injectableCode);
        ClassTransformer classTransformer = new DefaultClassTransformer();
        ClassFileTransformerAdapter adapter = new ClassFileTransformerAdapter(injectionPoint, classTransformer);

        InstrumentationHolder.addTransformer(adapter, true);

        Class<?> targetClass = findLoadedClass(className);
        if (targetClass == null) {
            InstrumentationHolder.removeTransformer(adapter);
            return VerifyResult.failure("Class not loaded: " + className);
        }

        try {
            InstrumentationHolder.retransformClasses(targetClass);
        } catch (Exception e) {
            LOGGER.error("Retransform failed for class: {}", className, e);
            return VerifyResult.failure("Retransform failed: " + e.getMessage());
        } finally {
            InstrumentationHolder.removeTransformer(adapter);
        }

        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream captureStream = new PrintStream(baos, true);

        try {
            Method targetMethod = findMethod(targetClass, methodName);
            if (targetMethod == null) {
                return VerifyResult.failure("Method not found: " + methodName);
            }

            targetMethod.setAccessible(true);
            Object instance = null;
            if (!Modifier.isStatic(targetMethod.getModifiers())) {
                try {
                    instance = targetClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    return VerifyResult.failure("Cannot create instance: " + e.getMessage());
                }
            }

            Object[] args = buildDefaultArgs(targetMethod);

            System.setOut(captureStream);
            try {
                targetMethod.invoke(instance, args);
            } catch (Exception e) {
            } finally {
                System.setOut(originalOut);
            }

            String output = baos.toString();
            return VerifyResult.success(output);
        } catch (Exception e) {
            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return VerifyResult.failure(errorMsg);
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

    private MethodInjectionLocation resolveInjectionLocation(String injectionLocationStr) {
        if (injectionLocationStr == null || injectionLocationStr.isEmpty()) {
            return MethodInjectionLocation.ENTER;
        }
        switch (injectionLocationStr.toUpperCase()) {
            case "METHOD_ENTER":
            case "ENTER":
                return MethodInjectionLocation.ENTER;
            case "METHOD_EXIT":
            case "EXIT":
                return MethodInjectionLocation.EXIT;
            case "METHOD_AROUND":
            case "AROUND":
                return MethodInjectionLocation.AROUND;
            default:
                return MethodInjectionLocation.ENTER;
        }
    }
}
