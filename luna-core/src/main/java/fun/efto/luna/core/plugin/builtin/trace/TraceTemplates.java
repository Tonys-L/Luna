package fun.efto.luna.core.plugin.builtin.trace;

import fun.efto.luna.core.injection.rule.template.RuleTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 10:00
 */
public class TraceTemplates {

    public static RuleTemplate methodTiming() {
        RuleTemplate t = new RuleTemplate();
        t.setName("method-timing");
        t.setDisplayName("方法耗时统计");
        t.setDescription("统计方法执行耗时，记录入口和出口时间戳，计算并输出耗时（毫秒）");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("performance");
        t.setParameters(Arrays.asList());

        RuleTemplate.TemplateRule enterRule = new RuleTemplate.TemplateRule();
        enterRule.setInjectionLocation("METHOD_ENTER");
        enterRule.setCodeType("EXPRESSION");
        enterRule.setCode("trace:start");

        RuleTemplate.TemplateRule exitRule = new RuleTemplate.TemplateRule();
        exitRule.setInjectionLocation("METHOD_EXIT");
        exitRule.setCodeType("EXPRESSION");
        exitRule.setCode("trace:end:0");

        t.setRules(Arrays.asList(enterRule, exitRule));
        return t;
    }

    public static RuleTemplate methodTimingWithThreshold() {
        RuleTemplate t = new RuleTemplate();
        t.setName("method-timing-threshold");
        t.setDisplayName("方法耗时统计（阈值过滤）");
        t.setDescription("统计方法执行耗时，仅当耗时超过指定阈值时才记录，减少噪音");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("performance");

        RuleTemplate.TemplateParameter thresholdParam = new RuleTemplate.TemplateParameter();
        thresholdParam.setName("threshold");
        thresholdParam.setDisplayName("耗时阈值(ms)");
        thresholdParam.setType("long");
        thresholdParam.setDefaultValue("100");
        thresholdParam.setDescription("超过此阈值才记录耗时");
        thresholdParam.setRequired(false);

        t.setParameters(Arrays.asList(thresholdParam));

        RuleTemplate.TemplateRule enterRule = new RuleTemplate.TemplateRule();
        enterRule.setInjectionLocation("METHOD_ENTER");
        enterRule.setCodeType("EXPRESSION");
        enterRule.setCode("trace:start");

        RuleTemplate.TemplateRule exitRule = new RuleTemplate.TemplateRule();
        exitRule.setInjectionLocation("METHOD_EXIT");
        exitRule.setCodeType("EXPRESSION");
        exitRule.setCode("trace:end:${threshold}");

        t.setRules(Arrays.asList(enterRule, exitRule));
        return t;
    }

    public static RuleTemplate slowMethodAlert() {
        RuleTemplate t = new RuleTemplate();
        t.setName("slow-method-alert");
        t.setDisplayName("慢方法告警");
        t.setDescription("当方法耗时超过阈值时输出告警日志，含方法名和耗时");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("performance");

        RuleTemplate.TemplateParameter thresholdParam = new RuleTemplate.TemplateParameter();
        thresholdParam.setName("threshold");
        thresholdParam.setDisplayName("耗时阈值(ms)");
        thresholdParam.setType("long");
        thresholdParam.setDefaultValue("500");
        thresholdParam.setDescription("超过此阈值输出告警");
        thresholdParam.setRequired(false);

        t.setParameters(Arrays.asList(thresholdParam));

        RuleTemplate.TemplateRule enterRule = new RuleTemplate.TemplateRule();
        enterRule.setInjectionLocation("METHOD_ENTER");
        enterRule.setCodeType("EXPRESSION");
        enterRule.setCode("trace:start");

        RuleTemplate.TemplateRule exitRule = new RuleTemplate.TemplateRule();
        exitRule.setInjectionLocation("METHOD_EXIT");
        exitRule.setCodeType("EXPRESSION");
        exitRule.setCode("trace:alert:${threshold}");

        t.setRules(Arrays.asList(enterRule, exitRule));
        return t;
    }

    public static List<RuleTemplate> all() {
        return Arrays.asList(methodTiming(), methodTimingWithThreshold(), slowMethodAlert());
    }
}
