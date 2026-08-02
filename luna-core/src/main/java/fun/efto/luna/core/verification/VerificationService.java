package fun.efto.luna.core.verification;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.InjectionService;
import fun.efto.luna.core.injection.InjectionValidator;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.InjectionPointFactory;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.port.BytecodeLoader;
import fun.efto.luna.core.injection.port.BytecodePreviewer;
import fun.efto.luna.core.injection.port.InjectionVerifier;
import fun.efto.luna.core.transformer.ClassTransformer;
import fun.efto.luna.core.transformer.DefaultClassTransformer;
import fun.efto.luna.core.transformer.TransformerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 验证预览服务：字节码预览、注入验证、注入测试（dryRun → inject → verify → validate）。
 *
 * <p>从 {@link InjectionService} 提炼，使验证职责内聚为独立 Module。
 * 依赖方向：{@code VerificationService} → {@link InjectionService}（单向）。</p>
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/07/19 16:00
 */
public class VerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationService.class);

    private final InjectionService injectionService;
    private final BytecodeLoader bytecodeLoader;
    private final InjectionVerifier injectionVerifier;
    private final ClassTransformer classTransformer = new DefaultClassTransformer();

    public VerificationService(InjectionService injectionService,
                               BytecodeLoader bytecodeLoader,
                               InjectionVerifier injectionVerifier) {
        this.injectionService = injectionService;
        this.bytecodeLoader = bytecodeLoader;
        this.injectionVerifier = injectionVerifier;
    }

    /**
     * 注入测试完整流程：校验 → 预览 → 注入 → 验证 → 内容比对。
     */
    public InjectionService.InjectTestResult injectWithTest(InjectRequest cmd) {
        String probeValidationError = injectionService.validateProbeHandler(cmd);
        if (probeValidationError != null) {
            return InjectionService.InjectTestResult.failure("probeValidation", probeValidationError);
        }

        String paramError = InjectionValidator.validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) {
            return InjectionService.InjectTestResult.failure("validateLocalVar", paramError);
        }

        String localVarError = injectionService.validateLocalVarReferences(cmd);
        if (localVarError != null) {
            return InjectionService.InjectTestResult.failure("validateLocalVar", localVarError);
        }

        if (injectionService.isLineInjection(cmd) && (cmd.getMethod() == null || cmd.getMethod().isEmpty())) {
            return InjectionService.InjectTestResult.failure("validateLocalVar", "行号注入需要指定方法名(method)");
        }

        BytecodePreviewer.PreviewResult dryRunResult = preview(cmd);
        if (!dryRunResult.isTransformed()) {
            return InjectionService.InjectTestResult.failure("dryRun", dryRunResult.getMessage());
        }

        String injectionId;
        try {
            PersistentInjection pi = injectionService.toPersistentInjection(cmd);
            injectionId = injectionService.addInjection(pi);
        } catch (Exception e) {
            LOGGER.error("Inject with test failed", e);
            return InjectionService.InjectTestResult.failure("inject", e.getMessage());
        }

        InjectionVerifier.VerifyResult verifyResult = injectionVerifier.verifyOnly(cmd.getClazz(), cmd.getMethod());
        if (!verifyResult.isSuccess()) {
            return InjectionService.InjectTestResult.failure("verify", verifyResult.getError());
        }

        String expectedContent = cmd.getCode();
        boolean found = verifyResult.getOutput() != null
                && verifyResult.getOutput().contains(expectedContent);

        return InjectionService.InjectTestResult.of(
                found,
                injectionId,
                dryRunResult.getGeneratedSize(),
                dryRunResult.getOriginalSize(),
                verifyResult.getOutput(),
                found ? null : "验证失败: 输出中未找到期望内容"
        );
    }

    /**
     * 字节码预览：仅模拟变换，不实际注入。
     */
    public BytecodePreviewer.PreviewResult preview(InjectRequest cmd) {
        String paramError = InjectionValidator.validateParamReferences(cmd.getCode(), cmd.getDesc());
        if (paramError != null) {
            return new BytecodePreviewer.PreviewResult(false, 0, 0, paramError);
        }

        try {
            PersistentInjection pi = injectionService.toPersistentInjection(cmd);
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

    /**
     * 注入验证：对已注入方法执行反射调用，验证探针输出。
     */
    public InjectionVerifier.VerifyResult verify(InjectRequest cmd) {
        return injectionVerifier.testInjection(
                cmd.getClazz(), cmd.getMethod(), cmd.getDesc(),
                cmd.getInjectionLocation(), cmd.getCode());
    }
}
