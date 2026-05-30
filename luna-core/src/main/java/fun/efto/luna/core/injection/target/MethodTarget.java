package fun.efto.luna.core.injection.target;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:29
 */
public class MethodTarget extends BaseTarget {
    private String methodName;
    private String methodDescriptor;

    public MethodTarget() {
    }

    public MethodTarget(InjectionType injectionType, String targetClass, String methodName, String methodDescriptor) {
        super(injectionType, targetClass);
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setMethodDescriptor(String methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }
}
