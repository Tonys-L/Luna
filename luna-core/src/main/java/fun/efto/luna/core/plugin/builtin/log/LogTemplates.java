package fun.efto.luna.core.plugin.builtin.log;

import fun.efto.luna.core.rule.template.RuleTemplate;

import java.util.Arrays;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/16 10:00
 */
public class LogTemplates {

    public static RuleTemplate methodAccessLog() {
        RuleTemplate t = new RuleTemplate();
        t.setName("method-access-log");
        t.setDisplayName("方法访问日志");
        t.setDescription("记录方法入口参数和出口返回值，用于调试和审计");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("observability");
        t.setParameters(Arrays.asList());

        RuleTemplate.TemplateRule enterRule = new RuleTemplate.TemplateRule();
        enterRule.setInjectionType("METHOD_ENTER");
        enterRule.setCodeType("EXPRESSION");
        enterRule.setCode("log:→ call: $0");

        RuleTemplate.TemplateRule exitRule = new RuleTemplate.TemplateRule();
        exitRule.setInjectionType("METHOD_EXIT");
        exitRule.setCodeType("EXPRESSION");
        exitRule.setCode("log:← return");

        t.setRules(Arrays.asList(enterRule, exitRule));
        return t;
    }
}
