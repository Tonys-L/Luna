package fun.efto.luna.core.injection.target.type;

/**
 * 注入类型基类
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public abstract class InjectionType {

    /**
     * 获取注入类型名称
     * @return 注入类型名称
     */
    public abstract String getName();

    /**
     * 获取注入类型描述
     * @return 注入类型描述
     */
    public abstract String getDescription();

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InjectionType that = (InjectionType) o;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
