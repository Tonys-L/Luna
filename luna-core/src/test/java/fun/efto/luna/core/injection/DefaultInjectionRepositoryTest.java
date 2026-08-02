package fun.efto.luna.core.injection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 DefaultInjectionRepository 的 null 校验（M-1）。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/08/02 16:00
 */
public class DefaultInjectionRepositoryTest {

    private DefaultInjectionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new DefaultInjectionRepository();
    }

    @Test
    void saveNullInjectionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    void saveInjectionWithNullIdThrowsException() {
        PersistentInjection injection = new PersistentInjection();
        assertThrows(IllegalArgumentException.class, () -> repository.save(injection));
    }

    @Test
    void deleteNullIdDoesNotThrow() {
        assertDoesNotThrow(() -> repository.delete(null));
    }

    @Test
    void findByIdNullIdReturnsNull() {
        assertNull(repository.findById(null));
    }

    @Test
    void findByGroupIdNullReturnsEmptyList() {
        List<PersistentInjection> result = repository.findByGroupId(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void saveAndFindByIdWorksCorrectly() {
        PersistentInjection injection = buildInjection("test-repo-1", "group-A");
        repository.save(injection);

        PersistentInjection found = repository.findById("test-repo-1");
        assertNotNull(found);
        assertEquals("group-A", found.getGroupId());
    }

    @Test
    void findByGroupIdFindsMatchingInjections() {
        repository.save(buildInjection("test-1", "group-A"));
        repository.save(buildInjection("test-2", "group-A"));
        repository.save(buildInjection("test-3", "group-B"));

        List<PersistentInjection> result = repository.findByGroupId("group-A");
        assertEquals(2, result.size());
    }

    private PersistentInjection buildInjection(String id, String groupId) {
        PersistentInjection injection = new PersistentInjection();
        injection.setId(id);
        injection.setClazz("com.example.Service");
        injection.setMethodName("method");
        injection.setMethodDescriptor("()V");
        injection.setInjectionLocation("method_enter");
        injection.setProbeType("LOG");
        injection.setCodeType("EXPRESSION");
        injection.setCode("$null");
        injection.setEphemeral(true);
        injection.setGroupId(groupId);
        return injection;
    }
}
