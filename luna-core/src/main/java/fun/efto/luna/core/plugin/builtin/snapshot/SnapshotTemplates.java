package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.injection.rule.template.RuleTemplate;

import java.util.Arrays;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class SnapshotTemplates {

    public static RuleTemplate lineSnapshot() {
        RuleTemplate t = new RuleTemplate();
        t.setName("line-snapshot");
        t.setDisplayName("行号快照");
        t.setDescription("在指定行号处采集局部变量快照，用于虚拟断点调试");
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

        t.setParameters(Arrays.asList(lineParam));

        RuleTemplate.TemplateRule rule = new RuleTemplate.TemplateRule();
        rule.setInjectionLocation("LINE_BEFORE");
        rule.setCodeType("SNAPSHOT");
        rule.setCode("snapshot:true");
        rule.setLineNumber(0);

        t.setRules(Arrays.asList(rule));
        return t;
    }
}
