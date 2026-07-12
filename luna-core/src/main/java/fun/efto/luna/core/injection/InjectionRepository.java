package fun.efto.luna.core.injection;

import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/06/01 15:00
 */
public interface InjectionRepository {
    void save(PersistentInjection injection);
    void delete(String id);
    PersistentInjection findById(String id);
    List<PersistentInjection> findAll();
    List<PersistentInjection> findByGroupId(String groupId);
}
