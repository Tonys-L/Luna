package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.type.InjectionType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:41
 */
public abstract class BaseTarget implements InjectionTarget{
    private final InjectionType type;
    private final String targetClass;

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
}
