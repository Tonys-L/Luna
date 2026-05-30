package fun.efto.luna.core.injection;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:47
 */
public interface InjectionLifecycle {
    String addInjection(PersistentInjection injection);
    void removeInjection(String id);
    void updateInjection(String id, PersistentInjection injection);
    void toggleEnabled(String id, boolean enabled);
}
