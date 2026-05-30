package fun.efto.luna.core.plugin.builtin.conditional;

import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Arrays;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class ConditionalBreakpointTemplates {

    public static RuleTemplate conditionalBreakpoint() {
        RuleTemplate t = new RuleTemplate();
        t.setName("conditional-breakpoint");
        t.setDisplayName("条件断点");
        t.setDescription("当条件满足时在指定行号采集快照，实现虚拟条件断点");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("debug");

        RuleTemplate.TemplateParameter lineParam = new RuleTemplate.TemplateParameter();
        lineParam.setName("lineNumber");
        lineParam.setDisplayName("行号");
        lineParam.setType("int");
        lineParam.setDefaultValue("0");
        lineParam.setDescription("目标行号");
        lineParam.setRequired(true);

        RuleTemplate.TemplateParameter condParam = new RuleTemplate.TemplateParameter();
        condParam.setName("condition");
        condParam.setDisplayName("条件表达式");
        condParam.setType("string");
        condParam.setDefaultValue("true");
        condParam.setDescription("如 param[2] >= 18");
        condParam.setRequired(false);

        t.setParameters(Arrays.asList(lineParam, condParam));

        RuleTemplate.TemplateRule rule = new RuleTemplate.TemplateRule();
        rule.setInjectionType("LINE_BEFORE");
        rule.setCodeType("SNAPSHOT");
        rule.setCode("snapshot:true");
        rule.setCondition("${condition}");
        rule.setLineNumber(0);

        t.setRules(Arrays.asList(rule));
        return t;
    }
}
