package fun.efto.luna.agent.web.vo;

import java.util.List;

/**
 * 模板应用结果 VO
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/27 12:00
 */
public class TemplateApplyResultVO {
    private final String templateName;
    private final String targetClass;
    private final String targetMethod;
    private final List<Long> createdRuleIds;
    private final int ruleCount;

    public TemplateApplyResultVO(String templateName, String targetClass, String targetMethod,
                                 List<Long> createdRuleIds, int ruleCount) {
        this.templateName = templateName;
        this.targetClass = targetClass;
        this.targetMethod = targetMethod;
        this.createdRuleIds = createdRuleIds;
        this.ruleCount = ruleCount;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getTargetMethod() {
        return targetMethod;
    }

    public List<Long> getCreatedRuleIds() {
        return createdRuleIds;
    }

    public int getRuleCount() {
        return ruleCount;
    }
}
