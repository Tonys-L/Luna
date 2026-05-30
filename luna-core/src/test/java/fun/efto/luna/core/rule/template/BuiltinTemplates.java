package fun.efto.luna.core.rule.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * 测试专用的内置规则模板定义工具类
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/17 18:00
 */
public final class BuiltinTemplates {

    public static RuleTemplate methodTiming() {
        RuleTemplate t = new RuleTemplate();
        t.setName("method-timing");
        t.setDisplayName("方法耗时统计");
        t.setDescription("Measure method execution time");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("performance");
        t.setParameters(Collections.emptyList());

        RuleTemplate.TemplateRule rule1 = new RuleTemplate.TemplateRule();
        rule1.setInjectionType("METHOD_ENTER");
        rule1.setCodeType("EXPRESSION");
        rule1.setCode("trace:start");

        RuleTemplate.TemplateRule rule2 = new RuleTemplate.TemplateRule();
        rule2.setInjectionType("METHOD_EXIT");
        rule2.setCodeType("EXPRESSION");
        rule2.setCode("trace:end:0");

        t.setRules(Arrays.asList(rule1, rule2));
        return t;
    }

    public static RuleTemplate methodTimingWithThreshold() {
        RuleTemplate t = new RuleTemplate();
        t.setName("method-timing-threshold");
        t.setDisplayName("阈值耗时监控");
        t.setDescription("Measure method execution time with alert threshold");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("performance");

        RuleTemplate.TemplateParameter param = new RuleTemplate.TemplateParameter();
        param.setName("threshold");
        param.setDefaultValue("100");
        t.setParameters(Collections.singletonList(param));

        RuleTemplate.TemplateRule rule1 = new RuleTemplate.TemplateRule();
        rule1.setInjectionType("METHOD_ENTER");
        rule1.setCodeType("EXPRESSION");
        rule1.setCode("trace:start");

        RuleTemplate.TemplateRule rule2 = new RuleTemplate.TemplateRule();
        rule2.setInjectionType("METHOD_EXIT");
        rule2.setCodeType("EXPRESSION");
        rule2.setCode("trace:end:${threshold}");

        t.setRules(Arrays.asList(rule1, rule2));
        return t;
    }

    public static RuleTemplate methodAccessLog() {
        RuleTemplate t = new RuleTemplate();
        t.setName("method-access-log");
        t.setDisplayName("方法调用日志");
        t.setDescription("Log method calls");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("observability");
        t.setParameters(Collections.emptyList());

        RuleTemplate.TemplateRule rule1 = new RuleTemplate.TemplateRule();
        rule1.setInjectionType("METHOD_ENTER");
        rule1.setCodeType("EXPRESSION");
        rule1.setCode("log:→ call: $0");

        RuleTemplate.TemplateRule rule2 = new RuleTemplate.TemplateRule();
        rule2.setInjectionType("METHOD_EXIT");
        rule2.setCodeType("EXPRESSION");
        rule2.setCode("log:← return");

        t.setRules(Arrays.asList(rule1, rule2));
        return t;
    }

    public static RuleTemplate slowMethodAlert() {
        RuleTemplate t = new RuleTemplate();
        t.setName("slow-method-alert");
        t.setDisplayName("慢方法告警");
        t.setDescription("Alert on slow methods");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("performance");

        RuleTemplate.TemplateParameter param = new RuleTemplate.TemplateParameter();
        param.setName("threshold");
        param.setDefaultValue("500");
        t.setParameters(Collections.singletonList(param));

        RuleTemplate.TemplateRule rule1 = new RuleTemplate.TemplateRule();
        rule1.setInjectionType("METHOD_ENTER");
        rule1.setCodeType("EXPRESSION");
        rule1.setCode("trace:start");

        RuleTemplate.TemplateRule rule2 = new RuleTemplate.TemplateRule();
        rule2.setInjectionType("METHOD_EXIT");
        rule2.setCodeType("EXPRESSION");
        rule2.setCode("trace:alert:${threshold}");

        t.setRules(Arrays.asList(rule1, rule2));
        return t;
    }

    public static RuleTemplate lineSnapshot() {
        RuleTemplate t = new RuleTemplate();
        t.setName("line-snapshot");
        t.setDisplayName("行变量快照");
        t.setDescription("Capture a snapshot on line execution");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("debug");
        t.setParameters(Collections.emptyList());

        RuleTemplate.TemplateRule rule = new RuleTemplate.TemplateRule();
        rule.setInjectionType("LINE_BEFORE");
        rule.setCodeType("SNAPSHOT");
        rule.setCode("snapshot:true");

        t.setRules(Collections.singletonList(rule));
        return t;
    }

    public static RuleTemplate conditionalBreakpoint() {
        RuleTemplate t = new RuleTemplate();
        t.setName("conditional-breakpoint");
        t.setDisplayName("条件虚拟断点");
        t.setDescription("Virtual conditional breakpoint");
        t.setVersion("1.0.0");
        t.setAuthor("Luna");
        t.setCategory("debug");

        RuleTemplate.TemplateParameter param = new RuleTemplate.TemplateParameter();
        param.setName("condition");
        param.setDefaultValue("true");
        t.setParameters(Collections.singletonList(param));

        RuleTemplate.TemplateRule rule = new RuleTemplate.TemplateRule();
        rule.setInjectionType("LINE_BEFORE");
        rule.setCodeType("SNAPSHOT");
        rule.setCode("snapshot:true");
        rule.setCondition("${condition}");

        t.setRules(Collections.singletonList(rule));
        return t;
    }
}
