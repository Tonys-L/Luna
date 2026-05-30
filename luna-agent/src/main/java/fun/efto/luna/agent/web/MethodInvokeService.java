package fun.efto.luna.agent.web;

import fun.efto.luna.agent.clazz.ClassResourceHelper;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.clazz.LoadedClass;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 方法调用服务，封装反射调用逻辑。
 * 从 TestController 提取，使 Controller 只做请求协调。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 14:00
 */
public class MethodInvokeService {

    private final ClassResourceHelper classResourceHelper;
    private final Object outputLock = new Object();

    public MethodInvokeService(ClassResourceHelper classResourceHelper) {
        this.classResourceHelper = classResourceHelper;
    }

    /**
     * 调用指定类的指定方法
     *
     * @return 调用结果
     */
    public InvokeResult invoke(String className, String methodName) {
        synchronized (outputLock) {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream captureStream = new PrintStream(baos, true);

            try {
                Class<?> targetClass = classResourceHelper.findLoadedClass(className);
                if (targetClass == null) {
                    return InvokeResult.failure("类未加载: " + className, null);
                }

                Method targetMethod = null;
                for (Method m : targetClass.getDeclaredMethods()) {
                    if (m.getName().equals(methodName)) {
                        targetMethod = m;
                        break;
                    }
                }

                if (targetMethod == null) {
                    return InvokeResult.failure("方法不存在: " + methodName, null);
                }

                targetMethod.setAccessible(true);

                Object instance = null;
                if (!Modifier.isStatic(targetMethod.getModifiers())) {
                    try {
                        instance = targetClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        instance = targetClass.getDeclaredConstructor(String.class, int.class, String.class)
                                .newInstance("test", 1, "test@test.com");
                    }
                }

                System.setOut(captureStream);
                Object result;
                try {
                    result = targetMethod.invoke(instance);
                } finally {
                    System.setOut(originalOut);
                }

                return InvokeResult.success(
                        result != null ? result.toString() : "null",
                        baos.toString());
            } catch (Exception e) {
                System.setOut(originalOut);
                String error = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                return InvokeResult.failure(error, baos.toString());
            }
        }
    }

    public static class InvokeResult {
        private final boolean success;
        private final String result;
        private final String output;
        private final String error;

        private InvokeResult(boolean success, String result, String output, String error) {
            this.success = success;
            this.result = result;
            this.output = output;
            this.error = error;
        }

        public static InvokeResult success(String result, String output) {
            return new InvokeResult(true, result, output, null);
        }

        public static InvokeResult failure(String error, String output) {
            return new InvokeResult(false, null, output, error);
        }

        public boolean isSuccess() { return success; }
        public String getResult() { return result; }
        public String getOutput() { return output; }
        public String getError() { return error; }
    }
}
