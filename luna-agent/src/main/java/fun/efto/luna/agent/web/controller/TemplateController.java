package fun.efto.luna.agent.web.controller;

import fun.efto.luna.agent.web.mvc.ApiResult;
import fun.efto.luna.agent.web.mvc.Controller;
import fun.efto.luna.agent.web.mvc.GetMapping;
import fun.efto.luna.agent.web.mvc.PathVariable;
import fun.efto.luna.agent.web.mvc.PostMapping;
import fun.efto.luna.agent.web.mvc.RequestBody;
import fun.efto.luna.agent.web.mvc.RequestMapping;
import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleManager;
import fun.efto.luna.core.rule.template.RuleTemplate;
import fun.efto.luna.core.rule.template.TemplateEngine;
import fun.efto.luna.core.rule.template.TemplateRegistry;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 12:00
 */
@Controller
@RequestMapping("/templates")
public class TemplateController {

    @GetMapping
    public ApiResult list() {
        List<RuleTemplate> templates = TemplateRegistry.getInstance().getAllTemplates();
        return ApiResult.ok(templates);
    }

    @GetMapping("/categories")
    public ApiResult categories() {
        List<RuleTemplate> allTemplates = TemplateRegistry.getInstance().getAllTemplates();
        Map<String, List<RuleTemplate>> grouped = new HashMap<>();
        for (RuleTemplate t : allTemplates) {
            String category = t.getCategory() != null ? t.getCategory() : "other";
            java.util.List<RuleTemplate> list = grouped.computeIfAbsent(category, k -> new java.util.ArrayList<>());
            list.add(t);
        }
        return ApiResult.ok(grouped);
    }

    @GetMapping("/{name}")
    public ApiResult get(@PathVariable("name") String name) {
        RuleTemplate template = TemplateRegistry.getInstance().getTemplate(name);
        if (template != null) {
            return ApiResult.ok(template);
        }
        return ApiResult.fail("模板不存在: " + name, HttpServletResponse.SC_NOT_FOUND);
    }

    @PostMapping("/apply")
    public ApiResult apply(@RequestBody ApplyTemplateRequest request) {
        RuleTemplate template = TemplateRegistry.getInstance().getTemplate(request.getTemplateName());
        if (template == null) {
            return ApiResult.fail("模板不存在: " + request.getTemplateName(), HttpServletResponse.SC_NOT_FOUND);
        }

        List<InjectionRule> rules = TemplateEngine.apply(
                template,
                request.getTargetClass(),
                request.getTargetMethod(),
                request.getMethodDesc(),
                request.getParameters()
        );

        java.util.List<Long> createdIds = new java.util.ArrayList<>();
        for (InjectionRule rule : rules) {
            long id = RuleManager.getInstance().addRule(rule);
            createdIds.add(id);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("templateName", request.getTemplateName());
        data.put("targetClass", request.getTargetClass());
        data.put("targetMethod", request.getTargetMethod());
        data.put("createdRuleIds", createdIds);
        data.put("ruleCount", createdIds.size());
        return ApiResult.ok(data);
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
