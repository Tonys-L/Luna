package fun.efto.luna.core.injection.target.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 注入类型基类
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public abstract class InjectionType {

    public static InjectionType of(String name, String description, String... aliases) {
        final List<String> aliasList = aliases.length > 0
            ? Arrays.asList(aliases)
            : Collections.emptyList();
        return new InjectionType() {
            @Override
            public String getName() { return name; }

            @Override
            public String getDescription() { return description; }

            @Override
            public List<String> getAliases() { return aliasList; }
        };
    }

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

    /**
     * 获取注入类型别名列表，用于别名注册和查找
     * 子类可覆盖此方法以提供别名
     * @return 别名列表，默认返回空列表
     */
    public List<String> getAliases() {
        return Collections.emptyList();
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
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
