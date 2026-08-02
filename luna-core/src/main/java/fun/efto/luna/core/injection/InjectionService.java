package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.port.BytecodeLoader;
import fun.efto.luna.core.injection.port.LocalVarValidator;
import fun.efto.luna.core.injection.port.Retransformer;
import fun.efto.luna.core.plugin.PluginContext;
import fun.efto.luna.core.plugin.ProbeHandler;
import fun.efto.luna.core.plugin.ValidationResult;
import fun.efto.luna.core.plugin.registry.ProbeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 20:00
 */
public class InjectionService implements InjectionQuery, InjectionLifecycle {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionService.class);

    private final InjectionRepository injectionRepository;
    private final InjectionRegistry injectionRegistry;
    private final Retransformer retransformer;
    private final BytecodeLoader bytecodeLoader;
    private final LocalVarValidator localVarValidator;

    public InjectionService(InjectionRepository injectionRepository,
                            InjectionRegistry injectionRegistry,
                            Retransformer retransformer,
                            BytecodeLoader bytecodeLoader,
                            LocalVarValidator localVarValidator) {
        this.injectionRepository = injectionRepository;
        this.injectionRegistry = injectionRegistry;
        this.retransformer = retransformer;
        this.bytecodeLoader = bytecodeLoader;
        this.localVarValidator = localVarValidator;
    }

    public InjectResult inject(InjectRequest cmd) {
        String probeValidationError = validateProbeHandler(cmd);
        if (probeValidationError != null) {
            return InjectResult.failure(probeValidationError);
        }

        String paramError = InjectionValidator.validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) {
            return InjectResult.failure(paramError);
        }

        String localVarError = validateLocalVarReferences(cmd);
        if (localVarError != null) {
            return InjectResult.failure(localVarError);
        }

        if (isLineInjection(cmd) && (cmd.getMethod() == null || cmd.getMethod().isEmpty())) {
            return InjectResult.failure("行号注入需要指定方法名(method)");
        }

        try {
            PersistentInjection pi = toPersistentInjection(cmd);
            String id = addInjection(pi);
            return InjectResult.success(id);
        } catch (Throwable t) {
            LOGGER.error("Inject failed", t);
            return InjectResult.failure("注入失败: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getName()));
        }
    }

    @Override
    public List<InjectionPoint> getActivePointsForClass(String className) {
        return injectionRegistry.getActivePointsForClass(className);
    }

    @Override
    public int getInjectionCount(String className) {
        return injectionRegistry.getInjectionCount(className);
    }

    @Override
    public List<InjectionPoint> getInjectionPoints(String className) {
        return injectionRegistry.getInjectionPoints(className);
    }

    @Override
    public boolean contains(String pointId) {
        return injectionRegistry.contains(pointId);
    }

    public PersistentInjection getInjection(String id) {
        return injectionRepository.findById(id);
    }

    public List<PersistentInjection> getAllInjections() {
        return injectionRepository.findAll();
    }

    public boolean isActive(String injectionId) {
        return injectionRegistry.contains(injectionId);
    }

    @Override
    public String addInjection(PersistentInjection injection) {
        if (injection.getId() == null) {
            injection.setId(UUID.randomUUID().toString());
        }
        injectionRepository.save(injection);

        try {
            InjectionPoint point = InjectionPointFactory.create(injection);
            injectionRegistry.register(point);

            // Fire onInject hook after successful save + register, before retransform
            fireOnInject(injection);

            triggerRetransform(injection.getClazz());
        } catch (Throwable t) {
            // INV-001: retransform 失败时回滚持久化数据和注册表条目，避免数据与运行时状态不一致
            LOGGER.error("Failed to register injection point: {}, rolling back", injection.getId(), t);
            try {
                injectionRegistry.unregister(injection.getId());
                injectionRepository.delete(injection.getId());
            } catch (Exception rollbackEx) {
                LOGGER.error("Rollback failed for injection: {}", injection.getId(), rollbackEx);
            }
        }

        return injection.getId();
    }

    @Override
    public void removeInjection(String id) {
        PersistentInjection injection = injectionRepository.findById(id);
        if (injection != null) {
            // Route to plugin's onDelete hook for associated cleanup (e.g., paired injection deletion)
            ProbeHandlerRegistry.getInstance().get(injection.getProbeType())
                .ifPresent(handler -> handler.onDelete(injection, injectionRepository, injectionRegistry));
            // Core mechanism: delete the injection itself
            injectionRepository.delete(id);
            injectionRegistry.unregister(id);
            try {
                triggerRetransform(injection.getClazz());
            } catch (Throwable t) {
                // INV-011: 捕获 Throwable 处理 VerifyError 等 Error 类型异常
                LOGGER.error("Retransform failed during removeInjection for class: {}", injection.getClazz(), t);
            }
        }
    }

    @Override
    public void updateInjection(String id, PersistentInjection injection) {
        injection.setId(id);
        injectionRepository.save(injection);

        injectionRegistry.unregister(id);
        try {
            InjectionPoint point = InjectionPointFactory.create(injection);
            injectionRegistry.register(point);
            triggerRetransform(injection.getClazz());
        } catch (Throwable t) {
            // INV-011: 捕获 Throwable 处理 VerifyError 等 Error 类型异常
            LOGGER.error("Failed to update injection point: {}", id, t);
        }
    }

    @Override
    public void toggleEnabled(String id, boolean enabled) {
        PersistentInjection injection = injectionRepository.findById(id);
        if (injection != null) {
            injection.setEnabled(enabled);
            injectionRepository.save(injection);

            if (enabled) {
                try {
                    InjectionPoint point = InjectionPointFactory.create(injection);
                    injectionRegistry.register(point);
                    triggerRetransform(injection.getClazz());
                } catch (Throwable t) {
                    // INV-011: 捕获 Throwable 处理 VerifyError 等 Error 类型异常
                    // 回滚 enabled 标志和 registry 条目，保持持久化与运行时状态一致
                    LOGGER.error("Failed to create injection point on enable: {}, rolling back", id, t);
                    injection.setEnabled(false);
                    injectionRepository.save(injection);
                    injectionRegistry.unregister(id);
                }
            } else {
                injectionRegistry.unregister(id);
                try {
                    triggerRetransform(injection.getClazz());
                } catch (Throwable t) {
                    LOGGER.error("Retransform failed during toggleEnabled for class: {}", injection.getClazz(), t);
                }
            }
        }
    }

    public List<String> suspendInjectionsByLocation(Set<String> locationNames, String reason) {
        List<String> suspendedIds = new ArrayList<>();
        for (PersistentInjection injection : injectionRepository.findAll()) {
            if (injection.getStatus() == InjectionStatus.ACTIVE && locationNames.contains(injection.getInjectionLocation())) {
                injection.setStatus(InjectionStatus.SUSPENDED);
                injection.setSuspendReason(reason);
                injectionRepository.save(injection);
                injectionRegistry.unregister(injection.getId());
                try {
                    triggerRetransform(injection.getClazz());
                } catch (Throwable t) {
                    // INV-011: 捕获 Throwable 处理 VerifyError 等 Error 类型异常
                    LOGGER.error("Retransform failed during suspendInjectionsByLocation for class: {}", injection.getClazz(), t);
                }
                suspendedIds.add(injection.getId());
            }
        }
        return suspendedIds;
    }

    public void resumeInjectionsByLocation(Set<String> locationNames) {
        for (PersistentInjection injection : injectionRepository.findAll()) {
            if (injection.getStatus() == InjectionStatus.SUSPENDED && locationNames.contains(injection.getInjectionLocation())) {
                injection.setStatus(InjectionStatus.ACTIVE);
                injection.setSuspendReason(null);
                injectionRepository.save(injection);
                try {
                    InjectionPoint point = InjectionPointFactory.create(injection);
                    injectionRegistry.register(point);
                    triggerRetransform(injection.getClazz());
                } catch (Throwable t) {
                    // INV-011: 捕获 Throwable 处理 VerifyError 等 Error 类型异常
                    LOGGER.error("Failed to re-register injection point on resume: {}", injection.getId(), t);
                }
            }
        }
    }

    private void triggerRetransform(String className) {
        if (retransformer != null) {
            try {
                if (className.contains("*")) {
                    retransformer.retransformByPattern(className);
                } else {
                    retransformer.retransform(className);
                }
            } catch (Throwable t) {
                // INV-011: 捕获 Throwable 处理 VerifyError 等 Error 类型异常
                LOGGER.error("Retransform failed for class: {}", className, t);
                throw new RuntimeException("Retransform failed for class: " + className, t);
            }
        }
    }

    private void fireOnInject(PersistentInjection injection) {
        try {
            ProbeHandlerRegistry registry = ProbeHandlerRegistry.getInstance();
            registry.get(injection.getProbeType()).ifPresent(handler -> {
                registry.getPluginContext(injection.getProbeType()).ifPresent(ctx -> {
                    handler.onInject(injection, ctx);
                });
            });
        } catch (Exception e) {
            LOGGER.error("onInject hook failed for injection: {} (probeType={})",
                    injection.getId(), injection.getProbeType(), e);
        }
    }

    public String validateLocalVarReferences(InjectRequest cmd) {
        try {
            byte[] bytecode = bytecodeLoader.loadBytecode(cmd.getClazz());
            return localVarValidator.validateLocalVarReferences(
                    cmd.getCode(), cmd.getClazz(), cmd.getMethod(),
                    cmd.getDesc(), cmd.getLineNumber(), bytecode);
        } catch (Exception e) {
            LOGGER.warn("Failed to validate local var references for class: {}", cmd.getClazz(), e);
            return "局部变量校验失败: " + e.getMessage();
        }
    }

    public PersistentInjection toPersistentInjection(InjectRequest cmd) {
        PersistentInjection pi = new PersistentInjection();
        pi.setClazz(cmd.getClazz());
        pi.setMethodName(cmd.getMethod());
        pi.setMethodDescriptor(cmd.getDesc());
        pi.setInjectionLocation(cmd.getInjectionLocation());
        pi.setProbeType(cmd.getProbeType());
        pi.setCodeType(cmd.getCodeType());
        pi.setCode(cmd.getCode());
        pi.setLineNumber(cmd.getLineNumber() != null ? cmd.getLineNumber() : 0);
        pi.setEphemeral(cmd.isEphemeral());
        pi.setGroupId(cmd.getGroupId());
        return pi;
    }

    public boolean isLineInjection(InjectRequest cmd) {
        if (cmd.getInjectionLocation() == null) return false;
        return cmd.getLineNumber() != null && cmd.getLineNumber() > 0;
    }

    public String validateProbeHandler(InjectRequest cmd) {
        if (cmd.getProbeType() == null || cmd.getProbeType().isEmpty()) {
            return "probeType is required";
        }
        ProbeHandler handler = ProbeHandlerRegistry.getInstance().get(cmd.getProbeType()).orElse(null);
        if (handler == null) {
            return "Unknown probe type: " + cmd.getProbeType();
        }
        ValidationResult result = handler.validate(cmd);
        if (!result.isValid()) {
            return result.getErrorMessage();
        }
        return null;
    }

    public static class InjectResult {
        private final boolean success;
        private final String injectionId;
        private final String error;

        private InjectResult(boolean success, String injectionId, String error) {
            this.success = success;
            this.injectionId = injectionId;
            this.error = error;
        }

        public static InjectResult success(String injectionId) {
            return new InjectResult(true, injectionId, null);
        }

        public static InjectResult failure(String error) {
            return new InjectResult(false, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getInjectionId() { return injectionId; }
        public String getError() { return error; }
    }

    public static class InjectTestResult {
        private final boolean success;
        private final String injectionId;
        private final int generatedSize;
        private final int originalSize;
        private final String output;
        private final String failedStep;
        private final String error;

        private InjectTestResult(boolean success, String injectionId, int generatedSize,
                                 int originalSize, String output, String error) {
            this.success = success;
            this.injectionId = injectionId;
            this.generatedSize = generatedSize;
            this.originalSize = originalSize;
            this.output = output;
            this.failedStep = null;
            this.error = error;
        }

        private InjectTestResult(boolean success, String injectionId, int generatedSize,
                                 int originalSize, String output, String failedStep, String error) {
            this.success = success;
            this.injectionId = injectionId;
            this.generatedSize = generatedSize;
            this.originalSize = originalSize;
            this.output = output;
            this.failedStep = failedStep;
            this.error = error;
        }

        public static InjectTestResult failure(String step, String error) {
            return new InjectTestResult(false, null, 0, 0, null, step, error);
        }

        public static InjectTestResult of(boolean success, String injectionId, int generatedSize,
                                          int originalSize, String output, String error) {
            return new InjectTestResult(success, injectionId, generatedSize, originalSize, output, error);
        }

        public boolean isSuccess() { return success; }
        public String getInjectionId() { return injectionId; }
        public int getGeneratedSize() { return generatedSize; }
        public int getOriginalSize() { return originalSize; }
        public String getOutput() { return output; }
        public String getFailedStep() { return failedStep; }
        public String getError() { return error; }
    }
}
