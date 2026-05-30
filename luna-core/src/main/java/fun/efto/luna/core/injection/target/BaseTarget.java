package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.InjectionType;

/**
 * 基础注入目标
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:41
 */
public abstract class BaseTarget implements InjectionTarget {
    private InjectionType type;
    private String targetClass;

    protected BaseTarget() {
    }

    protected BaseTarget(InjectionType type, String targetClass) {
        this.type = type;
        this.targetClass = targetClass;
    }

    @Override
    public InjectionType getType() {
        return type;
    }

    @Override
    public String getTargetClass() {
        return targetClass;
    }

    @Override
    public String getClassName() {
        return targetClass;
    }

    @Override
    public String getMethodName() {
        return "";
    }

    @Override
    public String getMethodDescriptor() {
        return "";
    }

    public void setType(InjectionType type) {
        this.type = type;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }
}
