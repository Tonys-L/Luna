package fun.efto.luna.core.injection.target;

/**
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public interface InjectionTarget {

    InjectionLocation getLocation();

    String getTargetClass();

    String getClassName();

    String getMethodName();

    String getMethodDescriptor();
}
