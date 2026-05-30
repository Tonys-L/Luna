package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.port.BytecodeLoader;
import fun.efto.luna.core.injection.port.BytecodePreviewer;
import fun.efto.luna.core.injection.port.InjectionVerifier;
import fun.efto.luna.core.injection.port.LocalVarValidator;
import fun.efto.luna.core.transformer.ClassTransformer;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import fun.efto.luna.core.transformer.TransformerResult;

import java.util.List;

/**
 * 注入应用服务。
 * 编排校验→预览→注入→验证流程，Controller 的唯一入口。
 * 纯生命周期操作（查询、删除）委托给 InjectionManager。
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 20:00
 */
public class InjectionService implements InjectionQuery {

    private final InjectionManager injectionManager;
    private final BytecodeLoader bytecodeLoader;
    private final BytecodePreviewer bytecodePreviewer;
    private final LocalVarValidator localVarValidator;
    private final InjectionVerifier injectionVerifier;
    private final ClassTransformer classTransformer = new DefaultClassTransformer();

    public InjectionService(InjectionManager injectionManager,
                            BytecodeLoader bytecodeLoader,
                            BytecodePreviewer bytecodePreviewer,
                            LocalVarValidator localVarValidator,
                            InjectionVerifier injectionVerifier) {
        this.injectionManager = injectionManager;
        this.bytecodeLoader = bytecodeLoader;
        this.bytecodePreviewer = bytecodePreviewer;
        this.localVarValidator = localVarValidator;
        this.injectionVerifier = injectionVerifier;
    }

    // ==================== 编排型操作 ====================

    public InjectResult inject(InjectionCommand cmd) {
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
            String id = injectionManager.addInjection(pi);
            return InjectResult.success(id);
        } catch (Throwable t) {
            return InjectResult.failure("注入失败: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getName()));
        }
    }

    public InjectTestResult injectWithTest(InjectionCommand cmd) {
        String paramError = InjectionValidator.validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) {
            return InjectTestResult.failure("validateLocalVar", paramError);
        }

        String localVarError = validateLocalVarReferences(cmd);
        if (localVarError != null) {
            return InjectTestResult.failure("validateLocalVar", localVarError);
        }

        if (isLineInjection(cmd) && (cmd.getMethod() == null || cmd.getMethod().isEmpty())) {
            return InjectTestResult.failure("validateLocalVar", "行号注入需要指定方法名(method)");
        }

        BytecodePreviewer.PreviewResult dryRunResult = preview(cmd);
        if (!dryRunResult.isTransformed()) {
            return InjectTestResult.failure("dryRun", dryRunResult.getMessage());
        }

        String injectionId;
        try {
            PersistentInjection pi = toPersistentInjection(cmd);
            injectionId = injectionManager.addInjection(pi);
        } catch (Exception e) {
            return InjectTestResult.failure("inject", e.getMessage());
        }

        InjectionVerifier.VerifyResult verifyResult = injectionVerifier.verifyOnly(cmd.getClazz(), cmd.getMethod());
        if (!verifyResult.isSuccess()) {
            return InjectTestResult.failure("verify", verifyResult.getError());
        }

        String expectedContent = cmd.getCode();
        if (expectedContent.startsWith("log:")) {
            expectedContent = expectedContent.substring(4);
        }
        boolean found = verifyResult.getOutput() != null
                && verifyResult.getOutput().contains(expectedContent);

        return new InjectTestResult(
                found,
                injectionId,
                dryRunResult.getGeneratedSize(),
                dryRunResult.getOriginalSize(),
                verifyResult.getOutput(),
                found ? null : "验证失败: 输出中未找到期望内容"
        );
    }

    public BytecodePreviewer.PreviewResult preview(InjectionCommand cmd) {
        String paramError = InjectionValidator.validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) {
            return new BytecodePreviewer.PreviewResult(false, 0, 0, paramError);
        }

        try {
            PersistentInjection pi = toPersistentInjection(cmd);
            InjectionPoint injectionPoint = InjectionPointFactory.create(pi);
            byte[] originalBytes = bytecodeLoader.loadBytecode(cmd.getClazz());
            TransformerResult result = classTransformer.transform(injectionPoint, cmd.getClazz(), originalBytes);
            return new BytecodePreviewer.PreviewResult(
                    result.isTransformed(),
                    result.getBytecode() != null ? result.getBytecode().length : 0,
                    originalBytes.length,
                    result.getMessage());
        } catch (Exception e) {
            return new BytecodePreviewer.PreviewResult(false, 0, 0, "预览失败: " + e.getMessage());
        }
    }

    public InjectionVerifier.VerifyResult verify(InjectionCommand cmd) {
        return injectionVerifier.testInjection(
                cmd.getClazz(), cmd.getMethod(), cmd.getDesc(),
                cmd.getInjectionType(), cmd.getCode());
    }

    // ==================== 委托型操作 ====================

    @Override
    public List<InjectionPoint> getActivePointsForClass(String className) {
        return injectionManager.getActivePointsForClass(className);
    }

    @Override
    public int getInjectionCount(String className) {
        return injectionManager.getInjectionCount(className);
    }

    @Override
    public List<InjectionPoint> getInjectionPoints(String className) {
        return injectionManager.getInjectionPoints(className);
    }

    public PersistentInjection getInjection(String id) {
        return injectionManager.getInjection(id);
    }

    public void removeInjection(String id) {
        injectionManager.removeInjection(id);
    }

    // ==================== 内部方法 ====================

    private String validateLocalVarReferences(InjectionCommand cmd) {
        try {
            byte[] bytecode = bytecodeLoader.loadBytecode(cmd.getClazz());
            return localVarValidator.validateLocalVarReferences(
                    cmd.getCode(), cmd.getClazz(), cmd.getMethod(),
                    cmd.getDesc(), cmd.getLineNumber(), bytecode);
        } catch (Exception e) {
            return null;
        }
    }

    private PersistentInjection toPersistentInjection(InjectionCommand cmd) {
        PersistentInjection pi = new PersistentInjection();
        pi.setClazz(cmd.getClazz());
        pi.setMethodName(cmd.getMethod());
        pi.setMethodDescriptor(cmd.getDesc());
        pi.setInjectionType(cmd.getInjectionType());
        pi.setCodeType(cmd.getCodeType());
        pi.setCode(cmd.getCode());
        pi.setLineNumber(cmd.getLineNumber() != null ? cmd.getLineNumber() : 0);
        pi.setEphemeral(true);
        return pi;
    }

    private boolean isLineInjection(InjectionCommand cmd) {
        if (cmd.getInjectionType() == null) return false;
        return cmd.getLineNumber() != null && cmd.getLineNumber() > 0;
    }

    // ==================== 结果对象 ====================

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

        public boolean isSuccess() { return success; }
        public String getInjectionId() { return injectionId; }
        public int getGeneratedSize() { return generatedSize; }
        public int getOriginalSize() { return originalSize; }
        public String getOutput() { return output; }
        public String getFailedStep() { return failedStep; }
        public String getError() { return error; }
    }
}