package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.type.MethodInjectionType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:29
 */
public class MethodTarget extends BaseTarget {
    private final String methodName;
    private final String methodDescriptor;

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
}
