package fun.efto.luna.core.injection;

import java.util.List;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:45
 */
public interface InjectionQuery {
    /**
     * O(1) 获取某个类下当前已编译激活的强类型运行时注入点列表
     * @param className 类全限定名
     * @return 已编译激活的注入点列表，无注入点时返回空列表
     */
    List<InjectionPoint> getActivePointsForClass(String className);

    /**
     * 获取指定类的注入数量
     * @param className 类全限定名
     * @return 注入数量
     */
    int getInjectionCount(String className);

    /**
     * 获取指定类的所有注入点
     * @param className 类全限定名
     * @return 注入点列表
     */
    List<InjectionPoint> getInjectionPoints(String className);
}
