package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.type.InjectionType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 2:51
 */
public interface InjectionTarget {
    InjectionType getType();

    String getTargetClass();
}
