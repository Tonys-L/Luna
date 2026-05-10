package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.type.InjectionType;

/**
 * 注入目标接口
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public interface InjectionTarget {

    /**
     * 获取注入类型
     * @return 注入类型
     */
    InjectionType getType();

    /**
     * 获取目标类
     * @return 目标类
     */
    String getTargetClass();

    /**
     * 获取类名
     * @return 类名
     */
    String getClassName();

    /**
     * 获取方法名
     * @return 方法名
     */
    String getMethodName();

    /**
     * 获取方法描述符
     * @return 方法描述符
     */
    String getMethodDescriptor();
}
