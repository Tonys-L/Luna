package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.InjectionType;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/25 23:00
 */
public class ConstructorTarget extends BaseTarget {

    private final String methodDescriptor;

    public ConstructorTarget(InjectionType type, String className, String methodDescriptor) {
        super(type, className);
        this.methodDescriptor = methodDescriptor;
    }

    @Override
    public String getMethodName() { return "<init>"; }
    @Override
    public String getMethodDescriptor() { return methodDescriptor; }

    public int getLineNumber() { return -1; }
}
