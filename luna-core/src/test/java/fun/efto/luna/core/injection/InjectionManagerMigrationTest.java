package fun.efto.luna.core.injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 16:00
 */
@DisplayName("InjectionManager 迁移验证测试")
public class InjectionManagerMigrationTest {

    @Test
    @DisplayName("InjectionManager 类不应再存在")
    void testInjectionManagerClassNotExist() {
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("fun.efto.luna.core.injection.InjectionManager");
        }, "InjectionManager should be deleted after migration");
    }

    @Test
    @DisplayName("InjectionService 应提供 suspendInjectionsByLocation 方法")
    void testInjectionServiceHasSuspendMethod() throws NoSuchMethodException {
        InjectionService.class.getMethod("suspendInjectionsByLocation", java.util.Set.class, String.class);
    }

    @Test
    @DisplayName("InjectionService 应提供 resumeInjectionsByLocation 方法")
    void testInjectionServiceHasResumeMethod() throws NoSuchMethodException {
        InjectionService.class.getMethod("resumeInjectionsByLocation", java.util.Set.class);
    }
}
