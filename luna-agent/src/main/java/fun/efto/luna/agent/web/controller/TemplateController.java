package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.web.vo.TemplateApplyResultVO;
import fun.efto.luna.core.web.ApiResult;
import fun.efto.luna.core.web.Controller;
import fun.efto.luna.core.web.GetMapping;
import fun.efto.luna.core.web.PathVariable;
import fun.efto.luna.core.web.PostMapping;
import fun.efto.luna.core.web.RequestBody;
import fun.efto.luna.core.web.RequestMapping;
import fun.efto.luna.core.rule.template.RuleTemplate;
import fun.efto.luna.core.rule.template.TemplateService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 12:00
 */
@Controller
@RequestMapping("/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ApiResult list() {
        List<RuleTemplate> templates = templateService.listTemplates();
        return ApiResult.ok(templates);
    }

    @GetMapping("/categories")
    public ApiResult categories() {
        List<RuleTemplate> allTemplates = templateService.listTemplates();
        Map<String, List<RuleTemplate>> grouped = new HashMap<>();
        for (RuleTemplate t : allTemplates) {
            String category = t.getCategory() != null ? t.getCategory() : "other";
            List<RuleTemplate> list = grouped.computeIfAbsent(category, k -> new java.util.ArrayList<>());
            list.add(t);
        }
        return ApiResult.ok(grouped);
    }

    @GetMapping("/{name}")
    public ApiResult get(@PathVariable("name") String name) {
        RuleTemplate template = templateService.getTemplate(name);
        if (template != null) {
            return ApiResult.ok(template);
        }
        return ApiResult.fail("模板不存在: " + name, 404);
    }

    @PostMapping("/apply")
    public ApiResult apply(@RequestBody ApplyTemplateRequest request) {
        TemplateService.ApplyResult result = templateService.applyTemplate(
                request.getTemplateName(),
                request.getTargetClass(),
                request.getTargetMethod(),
                request.getMethodDesc(),
                request.getParameters());

        if (!result.isSuccess()) {
            return ApiResult.fail(result.getErrorMessage(), 404);
        }

        return ApiResult.ok(new TemplateApplyResultVO(
                result.getTemplateName(), result.getTargetClass(), result.getTargetMethod(),
                result.getCreatedIds(), result.getCreatedCount()));
    }

    public static class ApplyTemplateRequest {
        private String templateName;
        private String targetClass;
        private String targetMethod;
        private String methodDesc;
        private Map<String, String> parameters;

        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getTargetClass() { return targetClass; }
        public void setTargetClass(String targetClass) { this.targetClass = targetClass; }
        public String getTargetMethod() { return targetMethod; }
        public void setTargetMethod(String targetMethod) { this.targetMethod = targetMethod; }
        public String getMethodDesc() { return methodDesc; }
        public void setMethodDesc(String methodDesc) { this.methodDesc = methodDesc; }
        public Map<String, String> getParameters() { return parameters; }
        public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
    }
}
