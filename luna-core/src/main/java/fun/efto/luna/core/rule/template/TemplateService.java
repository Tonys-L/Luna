package fun.efto.luna.core.rule.template;

import fun.efto.luna.core.rule.InjectionRule;
import fun.efto.luna.core.rule.RuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/29 20:00
 */
public class TemplateService {

    private final TemplateRegistry templateRegistry;
    private final RuleManager ruleManager;

    public TemplateService(TemplateRegistry templateRegistry, RuleManager ruleManager) {
        this.templateRegistry = templateRegistry;
        this.ruleManager = ruleManager;
    }

    public List<RuleTemplate> listTemplates() {
        return templateRegistry.getAllTemplates();
    }

    public RuleTemplate getTemplate(String name) {
        return templateRegistry.getTemplate(name);
    }

    public ApplyResult applyTemplate(String templateName, String targetClass,
                                      String targetMethod, String methodDesc,
                                      Map<String, String> parameters) {
        RuleTemplate template = templateRegistry.getTemplate(templateName);
        if (template == null) {
            return ApplyResult.failure("模板不存在: " + templateName);
        }

        List<InjectionRule> rules = TemplateEngine.apply(
                template, targetClass, targetMethod, methodDesc, parameters);

        List<Long> createdIds = new ArrayList<>();
        for (InjectionRule rule : rules) {
            long id = ruleManager.addRule(rule);
            createdIds.add(id);
        }

        return ApplyResult.success(templateName, targetClass, targetMethod, createdIds);
    }

    public static class ApplyResult {
        private final boolean success;
        private final String errorMessage;
        private final String templateName;
        private final String targetClass;
        private final String targetMethod;
        private final List<Long> createdIds;

        private ApplyResult(boolean success, String errorMessage, String templateName,
                            String targetClass, String targetMethod, List<Long> createdIds) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.templateName = templateName;
            this.targetClass = targetClass;
            this.targetMethod = targetMethod;
            this.createdIds = createdIds;
        }

        public static ApplyResult failure(String errorMessage) {
            return new ApplyResult(false, errorMessage, null, null, null, null);
        }

        public static ApplyResult success(String templateName, String targetClass,
                                           String targetMethod, List<Long> createdIds) {
            return new ApplyResult(true, null, templateName, targetClass, targetMethod, createdIds);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public String getTemplateName() { return templateName; }
        public String getTargetClass() { return targetClass; }
        public String getTargetMethod() { return targetMethod; }
        public List<Long> getCreatedIds() { return createdIds; }
        public int getCreatedCount() { return createdIds != null ? createdIds.size() : 0; }
    }
}
