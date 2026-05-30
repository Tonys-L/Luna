package fun.efto.luna.core.injection.port;

import fun.efto.luna.core.injection.InjectionPoint;

import java.util.List;

/**
 * InjectionStore（出站端口）。
 * 领域层需要"存储和查询运行时注入点"的能力，不直接依赖 InjectionPointRegistry 单例。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 18:00
 */
public interface InjectionStore {

    /**
     * 保存注入点到运行时注册表
     */
    void save(InjectionPoint point);

    /**
     * 清除指定类的所有注入点
     */
    void clear(String className);

    /**
     * 查询指定类的所有注入点
     */
    List<InjectionPoint> findByClassName(String className);

    /**
     * 统计指定类的注入数量
     */
    int countByClassName(String className);
}
