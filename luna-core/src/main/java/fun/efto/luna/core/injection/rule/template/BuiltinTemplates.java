package fun.efto.luna.core.injection.rule.template;

import java.util.Arrays;
import java.util.List;

/**
 * 内置模板库 - 提供开箱即用的常用注入模式
 *
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 10:00
 */
public class BuiltinTemplates {

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
        enterRule.setInjectionType("METHOD_ENTER");
        enterRule.setCodeType("EXPRESSION");
        enterRule.setCode("trace:start");

        RuleTemplate.TemplateRule exitRule = new RuleTemplate.TemplateRule();
        exitRule.setInjectionType("METHOD_EXIT");
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
        enterRule.setInjectionType("METHOD_ENTER");
        enterRule.setCodeType("EXPRESSION");
        enterRule.setCode("trace:start");

        RuleTemplate.TemplateRule exitRule = new RuleTemplate.TemplateRule();
        exitRule.setInjectionType("METHOD_EXIT");
        exitRule.setCodeType("EXPRESSION");
        exitRule.setCode("trace:end:${threshold}");

        t.setRules(Arrays.asList(enterRule, exitRule));
        return t;
    }

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
        enterRule.setInjectionType("METHOD_ENTER");
        enterRule.setCodeType("EXPRESSION");
        enterRule.setCode("trace:start");

        RuleTemplate.TemplateRule exitRule = new RuleTemplate.TemplateRule();
        exitRule.setInjectionType("METHOD_EXIT");
        exitRule.setCodeType("EXPRESSION");
        exitRule.setCode("trace:alert:${threshold}");

        t.setRules(Arrays.asList(enterRule, exitRule));
        return t;
    }

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
        rule.setInjectionType("LINE_BEFORE");
        rule.setCodeType("SNAPSHOT");
        rule.setCode("snapshot:true");
        rule.setLineNumber(0);

        t.setRules(Arrays.asList(rule));
        return t;
    }

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

    public static List<RuleTemplate> allTraceTemplates() {
        return Arrays.asList(methodTiming(), methodTimingWithThreshold(), slowMethodAlert());
    }
}
