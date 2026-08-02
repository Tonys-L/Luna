package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.injection.target.MethodTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 DefaultInjectionRegistry 的正则编译失败抛出异常（M-2）。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/08/02 16:10
 */
public class DefaultInjectionRegistryTest {

    private DefaultInjectionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultInjectionRegistry();
    }

    @Test
    void registerInvalidRegexThrowsIllegalArgumentException() {
        // [unclosed* 含 * 触发正则编译路径，且 [ 未关闭为非法正则
        InjectionPoint point = buildPoint("test-regex-1", "[unclosed*");
        assertThrows(IllegalArgumentException.class, () -> registry.register(point));
    }

    @Test
    void registerValidRegexSucceeds() {
        InjectionPoint point = buildPoint("test-regex-2", "com\\.example\\..*");
        assertDoesNotThrow(() -> registry.register(point));
        assertTrue(registry.contains("test-regex-2"));
    }

    @Test
    void registerExactClassSucceeds() {
        InjectionPoint point = buildPoint("test-exact-1", "com.example.Service");
        assertDoesNotThrow(() -> registry.register(point));
        assertTrue(registry.contains("test-exact-1"));
    }

    @Test
    void registerPackagePrefixSucceeds() {
        InjectionPoint point = buildPoint("test-prefix-1", "com.example.*");
        assertDoesNotThrow(() -> registry.register(point));
        assertTrue(registry.contains("test-prefix-1"));
    }

    private InjectionPoint buildPoint(String id, String targetClass) {
        PersistentInjection source = new PersistentInjection();
        source.setId(id);
        source.setClazz(targetClass);
        source.setMethodName("method");
        source.setMethodDescriptor("()V");
        source.setInjectionLocation("method_enter");
        source.setProbeType("LOG");
        source.setCodeType("EXPRESSION");
        source.setCode("$null");
        source.setEphemeral(true);

        InjectionLocation location = InjectionLocation.of("method_enter", "Method enter", "method");
        MethodTarget target = new MethodTarget(location, targetClass, "method", "()V");
        return new InjectionPoint(id, target, null, "EXPRESSION", "LOG", source);
    }
}
