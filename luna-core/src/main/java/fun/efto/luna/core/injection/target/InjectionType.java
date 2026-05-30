package fun.efto.luna.core.injection.target;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 注入类型基类
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/03/29 02:30
 */
public abstract class InjectionType {

    private final String name;
    private final String description;

    protected InjectionType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected InjectionType() {
        this.name = null;
        this.description = null;
    }

    /**
     * 便捷工厂方法，用于创建简单的 InjectionType 实例。
     * 注意：通过此方法创建的类型不支持 createTarget()，
     * 如需创建可注入的类型，请继承 InjectionType 并覆写 createTarget()。
     */
    public static InjectionType of(String name, String description, String... aliases) {
        final List<String> aliasList = aliases.length > 0
            ? Arrays.asList(aliases)
            : Collections.emptyList();
        return new InjectionType(name, description) {
            @Override
            public List<String> getAliases() { return aliasList; }
        };
    }

    /**
     * 获取注入类型名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取注入类型描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取注入类型别名列表，用于别名注册和查找
     */
    public List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * 创建注入目标
     * 子类必须覆写此方法以提供具体的 Target 创建逻辑
     * @throws UnsupportedOperationException 如果子类未覆写此方法
     */
    public InjectionTarget createTarget(String className, String methodName, String methodDescriptor, Integer lineNumber) {
        throw new UnsupportedOperationException(
            "InjectionType '" + getName() + "' must override createTarget() to specify how to create its InjectionTarget");
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InjectionType that = (InjectionType) o;
        return getName() != null && getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}
