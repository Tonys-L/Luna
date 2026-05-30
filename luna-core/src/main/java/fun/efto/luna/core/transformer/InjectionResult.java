package fun.efto.luna.core.transformer;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/02 16:00
 */
public class InjectionResult {
    private final boolean success;
    private final String message;
    private final String injectionType;
    private final String methodName;

    public InjectionResult(boolean success, String message, String injectionType, String methodName) {
        this.success = success;
        this.message = message;
        this.injectionType = injectionType;
        this.methodName = methodName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getInjectionType() {
        return injectionType;
    }

    public String getMethodName() {
        return methodName;
    }
}
