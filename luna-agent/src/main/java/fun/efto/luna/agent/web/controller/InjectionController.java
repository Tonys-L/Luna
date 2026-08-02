package fun.efto.luna.agent.web.controller;

import fun.efto.luna.core.infra.web.ApiResult;
import fun.efto.luna.core.infra.web.Controller;
import fun.efto.luna.core.infra.web.DeleteMapping;
import fun.efto.luna.core.infra.web.GetMapping;
import fun.efto.luna.core.infra.web.PathVariable;
import fun.efto.luna.core.infra.web.PostMapping;
import fun.efto.luna.core.infra.web.RequestBody;
import fun.efto.luna.core.infra.web.RequestMapping;
import fun.efto.luna.core.infra.web.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fun.efto.luna.agent.web.vo.InjectionListVO;
import fun.efto.luna.agent.web.vo.InjectionPointVO;
import fun.efto.luna.agent.web.vo.InjectionResultVO;
import fun.efto.luna.agent.web.vo.InjectionSuccessVO;
import fun.efto.luna.agent.web.vo.InjectionTestResultVO;
import fun.efto.luna.agent.web.vo.TestStepVO;
import fun.efto.luna.agent.web.vo.DryRunResultVO;
import fun.efto.luna.agent.web.vo.VerifyResultVO;
import fun.efto.luna.agent.web.vo.RemoveResultVO;
import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionService;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.port.BytecodePreviewer;
import fun.efto.luna.core.injection.port.InjectionVerifier;
import fun.efto.luna.core.injection.target.LineNumberTarget;
import fun.efto.luna.core.verification.VerificationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/09 10:00
 */
@Controller
@RequestMapping("/injections")
public class InjectionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectionController.class);

    private final InjectionService injectionService;
    private final VerificationService verificationService;

    public InjectionController(InjectionService injectionService, VerificationService verificationService) {
        this.injectionService = injectionService;
        this.verificationService = verificationService;
    }

    @GetMapping("/list")
    public ApiResult injectList(@RequestParam("class") String className) {
        if (className == null || className.isEmpty()) {
            return ApiResult.fail("Missing class parameter");
        }

        List<InjectionPoint> points = injectionService.getInjectionPoints(className);
        List<InjectionPointVO> voList = new ArrayList<>();
        for (InjectionPoint point : points) {
            voList.add(toVO(point));
        }
        return ApiResult.ok(new InjectionListVO(voList));
    }

    @GetMapping("/persistent")
    public ApiResult persistentList() {
        List<PersistentInjection> all = injectionService.getAllInjections();
        List<InjectionPointVO> voList = new ArrayList<>();
        for (PersistentInjection pi : all) {
            if (pi.isEphemeral()) continue;
            // 只展示当前实际生效的注入（在 registry 中能找到的）
            if (!injectionService.isActive(pi.getId())) continue;
            InjectionPointVO vo = new InjectionPointVO();
            vo.setId(pi.getId());
            vo.setClazz(pi.getClazz());
            vo.setProbeType(pi.getProbeType());
            vo.setInjectionLocation(pi.getInjectionLocation());
            vo.setMethod(pi.getMethodName());
            vo.setCode(pi.getCode());
            vo.setCodeType(pi.getCodeType());
            vo.setLineNumber(pi.getLineNumber());
            vo.setEphemeral(false);
            vo.setGroupId(pi.getGroupId());
            voList.add(vo);
        }
        return ApiResult.ok(new InjectionListVO(voList));
    }

    @PostMapping
    public ApiResult inject(@RequestBody InjectRequest cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        LOGGER.info("[DIAG-INJECT] code='{}', codeType='{}', probeType='{}', location='{}', method='{}', desc='{}'",
                cmd.getCode(), cmd.getCodeType(), cmd.getProbeType(), cmd.getInjectionLocation(), cmd.getMethod(), cmd.getDesc());

        InjectionService.InjectResult result = injectionService.inject(cmd);
        if (result.isSuccess()) {
            InjectionResultVO resultVO = new InjectionResultVO(true, cmd.getInjectionLocation(),
                    cmd.getMethod(), "Retransform completed", result.getInjectionId());
            List<InjectionResultVO> results = new ArrayList<>();
            results.add(resultVO);
            return ApiResult.ok(new InjectionSuccessVO(true, results, result.getInjectionId()));
        } else {
            return ApiResult.fail(result.getError(), 500);
        }
    }

    @PostMapping("/test")
    public ApiResult injectTest(@RequestBody InjectRequest cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        InjectionService.InjectTestResult result = verificationService.injectWithTest(cmd);

        Map<String, TestStepVO> steps = new LinkedHashMap<>();

        if ("validateLocalVar".equals(result.getFailedStep())) {
            steps.put("dryRun", TestStepVO.skipped("validateLocalVar failed"));
            steps.put("inject", TestStepVO.skipped("dry-run failed"));
            steps.put("verify", TestStepVO.skipped("inject failed"));
            steps.put("validate", TestStepVO.skipped("inject failed"));
            return failWith(result.getError(), new InjectionTestResultVO(false, steps, result.getError()));
        }

        if ("dryRun".equals(result.getFailedStep())) {
            steps.put("dryRun", TestStepVO.failure(result.getError()));
            steps.put("inject", TestStepVO.skipped("dry-run failed"));
            steps.put("verify", TestStepVO.skipped("inject failed"));
            steps.put("validate", TestStepVO.skipped("inject failed"));
            return failWith("One or more steps failed", new InjectionTestResultVO(false, steps, result.getError()));
        }

        steps.put("dryRun", TestStepVO.successWithBytecode(
                result.getGeneratedSize(), result.getOriginalSize(), null));

        if ("inject".equals(result.getFailedStep())) {
            steps.put("inject", TestStepVO.failure(result.getError()));
            steps.put("verify", TestStepVO.skipped("inject failed"));
            steps.put("validate", TestStepVO.skipped("inject failed"));
            return failWith("One or more steps failed", new InjectionTestResultVO(false, steps, result.getError()));
        }

        steps.put("inject", TestStepVO.success("Retransform completed"));

        if ("verify".equals(result.getFailedStep())) {
            steps.put("verify", TestStepVO.verifyFailure(result.getError()));
            steps.put("validate", TestStepVO.skipped("verify failed"));
            return failWith("One or more steps failed", new InjectionTestResultVO(false, steps, result.getError()));
        }

        steps.put("verify", TestStepVO.verifySuccess(result.getOutput()));

        String expectedContent = cmd.getCode();
        if (!result.isSuccess()) {
            steps.put("validate", TestStepVO.validateResult(false, expectedContent, result.getOutput()));
            return failWith("One or more steps failed",
                    new InjectionTestResultVO(false, steps, "验证失败: 输出中未找到期望内容"));
        }

        steps.put("validate", TestStepVO.validateResult(true, expectedContent, result.getOutput()));
        return ApiResult.ok(new InjectionTestResultVO(true, steps, null));
    }

    @PostMapping("/dry-run")
    public ApiResult dryRun(@RequestBody InjectRequest cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        BytecodePreviewer.PreviewResult result = verificationService.preview(cmd);
        if (result.isTransformed()) {
            return ApiResult.ok(new DryRunResultVO(
                    result.getGeneratedSize(), result.getOriginalSize(), cmd.getMethod(), result.getMessage()));
        } else {
            return ApiResult.fail(result.getMessage());
        }
    }

    @PostMapping("/verify")
    public ApiResult verify(@RequestBody InjectRequest cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        InjectionVerifier.VerifyResult result = verificationService.verify(cmd);
        if (result.isSuccess()) {
            return ApiResult.ok(VerifyResultVO.success(result.getOutput()));
        } else {
            return failWith(result.getError(), VerifyResultVO.failure(result.getError()));
        }
    }

    @DeleteMapping("/{id}")
    public ApiResult remove(@PathVariable("id") String id) {
        if (id == null || id.isEmpty()) {
            return ApiResult.fail("Missing id parameter");
        }

        PersistentInjection pi = injectionService.getInjection(id);
        if (pi == null) {
            return ApiResult.fail("Injection point not found: " + id, 404);
        }

        String className = pi.getClazz();
        injectionService.removeInjection(id);

        return ApiResult.ok(new RemoveResultVO(true, id, className));
    }

    private ApiResult failWith(String error, Object data) {
        return ApiResult.fail(error, data, 400);
    }

    private InjectionPointVO toVO(InjectionPoint point) {
        InjectionPointVO vo = new InjectionPointVO();
        vo.setId(point.getId());
        vo.setClazz(point.getTarget().getClassName());
        vo.setProbeType(point.getProbeType());
        vo.setInjectionLocation(point.getInjectionLocation().toString());
        vo.setMethod(point.getTarget().getMethodName());
        vo.setCode(point.getCode() != null ? point.getCode().getContent() : null);
        vo.setCodeType(point.getCodeType() != null ? point.getCodeType().toString() : null);
        vo.setTargetType(point.getTarget().getClass().getSimpleName());
        if (point.toPersistentInjection() != null) {
            vo.setEphemeral(point.toPersistentInjection().isEphemeral());
            vo.setGroupId(point.toPersistentInjection().getGroupId());
        }
        if (point.getTarget() instanceof LineNumberTarget) {
            vo.setLineNumber(((LineNumberTarget) point.getTarget()).getLineNumber());
        }
        return vo;
    }
}
