package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.type.MethodInjectionType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:29
 */
public class MethodTarget extends BaseTarget {
    private String methodName;
    private String methodDescriptor;

    public MethodTarget() {
    }

    public MethodTarget(MethodInjectionType type, String targetClass, String methodName, String methodDescriptor) {
        super(type, targetClass);
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
