package fun.efto.luna.core.injection.target;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:41
 */
public abstract class BaseTarget implements InjectionTarget {
    private InjectionLocation location;
    private String targetClass;

    protected BaseTarget() {
    }

    protected BaseTarget(InjectionLocation location, String targetClass) {
        this.location = location;
        this.targetClass = targetClass;
    }

    @Override
    public InjectionLocation getLocation() {
        return location;
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

    public void setLocation(InjectionLocation location) {
        this.location = location;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }
}
