package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.port.BytecodeLoader;
import fun.efto.luna.core.injection.port.LocalVarValidator;
import fun.efto.luna.core.injection.port.Retransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 INV-001（retransform 失败回滚）和 INV-011（捕获 Throwable）落实情况。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/08/02 15:00
 */
public class InjectionServiceRollbackTest {

    private DefaultInjectionRepository repository;
    private DefaultInjectionRegistry registry;
    private Retransformer failingRetransformer;
    private Retransformer throwingRetransformer;

    @BeforeEach
    void setUp() {
        repository = new DefaultInjectionRepository();
        registry = new DefaultInjectionRegistry();
        failingRetransformer = className -> {
            throw new RuntimeException("retransform failed");
        };
        throwingRetransformer = className -> {
            throw new VerifyError("bad bytecode");
        };
    }

    @Test
    void addInjectionRollsBackOnRetransformFailure() {
        InjectionService service = new InjectionService(
                repository, registry, failingRetransformer,
                (BytecodeLoader) className -> null,
                (LocalVarValidator) (code, cn, mn, md, ln, cb) -> null
        );

        PersistentInjection injection = buildInjection("test-rollback-1", "com.example.Service");
        service.addInjection(injection);

        assertNull(repository.findById("test-rollback-1"), "持久化数据应被回滚删除");
        assertFalse(registry.contains("test-rollback-1"), "注册表条目应被回滚删除");
    }

    @Test
    void addInjectionRollsBackOnVerifyError() {
        InjectionService service = new InjectionService(
                repository, registry, throwingRetransformer,
                (BytecodeLoader) className -> null,
                (LocalVarValidator) (code, cn, mn, md, ln, cb) -> null
        );

        PersistentInjection injection = buildInjection("test-rollback-2", "com.example.Service");
        service.addInjection(injection);

        assertNull(repository.findById("test-rollback-2"), "VerifyError 应被捕获并回滚");
        assertFalse(registry.contains("test-rollback-2"), "注册表条目应被回滚删除");
    }

    @Test
    void removeInjectionDoesNotPropagateRetransformFailure() {
        // 先成功添加一个注入点（无 retransformer）
        InjectionService setupService = new InjectionService(
                repository, registry, null,
                (BytecodeLoader) className -> null,
                (LocalVarValidator) (code, cn, mn, md, ln, cb) -> null
        );
        PersistentInjection injection = buildInjection("test-remove-1", "com.example.Service");
        setupService.addInjection(injection);
        assertTrue(registry.contains("test-remove-1"));

        // 使用会失败的 retransformer 进行删除
        InjectionService service = new InjectionService(
                repository, registry, failingRetransformer,
                (BytecodeLoader) className -> null,
                (LocalVarValidator) (code, cn, mn, md, ln, cb) -> null
        );

        // 不应抛出异常
        assertDoesNotThrow(() -> service.removeInjection("test-remove-1"));
        assertFalse(registry.contains("test-remove-1"));
    }

    @Test
    void suspendInjectionsByLocationDoesNotPropagateRetransformFailure() {
        InjectionService setupService = new InjectionService(
                repository, registry, null,
                (BytecodeLoader) className -> null,
                (LocalVarValidator) (code, cn, mn, md, ln, cb) -> null
        );
        PersistentInjection injection = buildInjection("test-suspend-1", "com.example.Service");
        injection.setInjectionLocation("method_enter");
        setupService.addInjection(injection);

        InjectionService service = new InjectionService(
                repository, registry, failingRetransformer,
                (BytecodeLoader) className -> null,
                (LocalVarValidator) (code, cn, mn, md, ln, cb) -> null
        );

        assertDoesNotThrow(() -> service.suspendInjectionsByLocation(
                java.util.Collections.singleton("method_enter"), "test"));
    }

    @Test
    void toggleEnabledDoesNotPropagateRetransformFailure() {
        InjectionService setupService = new InjectionService(
                repository, registry, null,
                (BytecodeLoader) className -> null,
                (LocalVarValidator) (code, cn, mn, md, ln, cb) -> null
        );
        PersistentInjection injection = buildInjection("test-toggle-1", "com.example.Service");
        setupService.addInjection(injection);

        InjectionService service = new InjectionService(
                repository, registry, failingRetransformer,
                (BytecodeLoader) className -> null,
                (LocalVarValidator) (code, cn, mn, md, ln, cb) -> null
        );

        assertDoesNotThrow(() -> service.toggleEnabled("test-toggle-1", false));
    }

    private PersistentInjection buildInjection(String id, String className) {
        PersistentInjection injection = new PersistentInjection();
        injection.setId(id);
        injection.setClazz(className);
        injection.setMethodName("testMethod");
        injection.setMethodDescriptor("()V");
        injection.setInjectionLocation("method_enter");
        injection.setProbeType("LOG");
        injection.setCodeType("EXPRESSION");
        injection.setCode("$null");
        injection.setEphemeral(true);
        return injection;
    }
}
