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
import fun.efto.luna.agent.web.vo.InjectionListVO;
import fun.efto.luna.agent.web.vo.InjectionPointVO;
import fun.efto.luna.agent.web.vo.InjectionResultVO;
import fun.efto.luna.agent.web.vo.InjectionSuccessVO;
import fun.efto.luna.agent.web.vo.InjectionTestResultVO;
import fun.efto.luna.agent.web.vo.TestStepVO;
import fun.efto.luna.agent.web.vo.DryRunResultVO;
import fun.efto.luna.agent.web.vo.VerifyResultVO;
import fun.efto.luna.agent.web.vo.RemoveResultVO;
import fun.efto.luna.core.injection.InjectionCommand;
import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.InjectionService;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.port.BytecodePreviewer;
import fun.efto.luna.core.injection.port.InjectionVerifier;
import fun.efto.luna.core.injection.target.LineNumberTarget;

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

    private final InjectionService injectionService;

    public InjectionController(InjectionService injectionService) {
        this.injectionService = injectionService;
    }

    @GetMapping("/list")
    public ApiResult injectList(@RequestParam("class") String className) {
        if (className == null || className.isEmpty()) {
            return ApiResult.fail("Missing class parameter");
        }

        List<InjectionPoint> points = injectionService.getInjectionPoints(className);
        List<InjectionPointVO> voList = new ArrayList<>();
        for (InjectionPoint point : points) {
            InjectionPointVO vo = new InjectionPointVO();
            vo.setId(point.getId());
            vo.setType(point.getInjectionType().toString());
            vo.setMethod(point.getTarget().getMethodName());
            vo.setCode(point.getCode().getCode());
            vo.setCodeType(point.getCodeType().toString());
            if (point.getTarget() instanceof LineNumberTarget) {
                vo.setLineNumber(((LineNumberTarget) point.getTarget()).getLineNumber());
            }
            voList.add(vo);
        }
        return ApiResult.ok(new InjectionListVO(voList));
    }

    @PostMapping
    public ApiResult inject(@RequestBody InjectionCommand cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        InjectionService.InjectResult result = injectionService.inject(cmd);
        if (result.isSuccess()) {
            InjectionResultVO resultVO = new InjectionResultVO(true, cmd.getInjectionType(),
                    cmd.getMethod(), "Retransform completed", result.getInjectionId());
            List<InjectionResultVO> results = new ArrayList<>();
            results.add(resultVO);
            return ApiResult.ok(new InjectionSuccessVO(true, results, result.getInjectionId()));
        } else {
            return ApiResult.fail(result.getError(), 500);
        }
    }

    @PostMapping("/test")
    public ApiResult injectTest(@RequestBody InjectionCommand cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        InjectionService.InjectTestResult result = injectionService.injectWithTest(cmd);

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

        if (!result.isSuccess()) {
            String expectedContent = cmd.getCode();
            if (expectedContent.startsWith("log:")) {
                expectedContent = expectedContent.substring(4);
            }
            steps.put("validate", TestStepVO.validateResult(false, expectedContent, result.getOutput()));
            return failWith("One or more steps failed",
                    new InjectionTestResultVO(false, steps, "验证失败: 输出中未找到期望内容"));
        }

        String expectedContent = cmd.getCode();
        if (expectedContent.startsWith("log:")) {
            expectedContent = expectedContent.substring(4);
        }
        steps.put("validate", TestStepVO.validateResult(true, expectedContent, result.getOutput()));
        return ApiResult.ok(new InjectionTestResultVO(true, steps, null));
    }

    @PostMapping("/dry-run")
    public ApiResult dryRun(@RequestBody InjectionCommand cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        BytecodePreviewer.PreviewResult result = injectionService.preview(cmd);
        if (result.isTransformed()) {
            return ApiResult.ok(new DryRunResultVO(
                    result.getGeneratedSize(), result.getOriginalSize(), cmd.getMethod(), result.getMessage()));
        } else {
            return ApiResult.fail(result.getMessage());
        }
    }

    @PostMapping("/verify")
    public ApiResult verify(@RequestBody InjectionCommand cmd) {
        if (cmd == null || !cmd.isValid()) {
            return ApiResult.fail("缺少必要参数");
        }

        InjectionVerifier.VerifyResult result = injectionService.verify(cmd);
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
}
