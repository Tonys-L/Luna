package fun.efto.luna.core.injection.rule.template;

import fun.efto.luna.core.injection.rule.InjectionRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 14:00
 */
public class TemplateEngineTest {

    private RuleTemplate methodTimingTemplate;
    private RuleTemplate thresholdTemplate;
    private RuleTemplate conditionalBreakpointTemplate;

    @BeforeEach
    void setUp() {
        methodTimingTemplate = BuiltinTemplates.methodTiming();
        thresholdTemplate = BuiltinTemplates.methodTimingWithThreshold();
        conditionalBreakpointTemplate = BuiltinTemplates.conditionalBreakpoint();
    }

    @Test
    void testMethodTimingGeneratesTwoRules() {
        List<InjectionRule> rules = TemplateEngine.apply(
                methodTimingTemplate, "com.example.UserService", "login", "()V", null);

        assertEquals(2, rules.size());

        InjectionRule enterRule = rules.get(0);
        assertEquals("METHOD_ENTER", enterRule.getInjectionLocation());
        assertEquals("EXPRESSION", enterRule.getCodeType());
        assertEquals("trace:start", enterRule.getLogContent());
        assertNull(enterRule.getExpression());

        InjectionRule exitRule = rules.get(1);
        assertEquals("METHOD_EXIT", exitRule.getInjectionLocation());
        assertEquals("EXPRESSION", exitRule.getCodeType());
        assertEquals("trace:end:0", exitRule.getLogContent());
        assertNull(exitRule.getExpression());
    }

    @Test
    void testMethodTimingWithThresholdSubstitutesParam() {
        Map<String, String> params = new HashMap<>();
        params.put("threshold", "200");

        List<InjectionRule> rules = TemplateEngine.apply(
                thresholdTemplate, "com.example.OrderService", "process", "()V", params);

        assertEquals(2, rules.size());

        InjectionRule enterRule = rules.get(0);
        assertEquals("trace:start", enterRule.getLogContent());

        InjectionRule exitRule = rules.get(1);
        assertEquals("trace:end:200", exitRule.getLogContent());
    }

    @Test
    void testMethodTimingWithThresholdUsesDefault() {
        List<InjectionRule> rules = TemplateEngine.apply(
                thresholdTemplate, "com.example.OrderService", "process", "()V", null);

        InjectionRule exitRule = rules.get(1);
        assertEquals("trace:end:100", exitRule.getLogContent());
    }

    @Test
    void testConditionalBreakpointSetsExpression() {
        Map<String, String> params = new HashMap<>();
        params.put("condition", "param[2] >= 18");

        List<InjectionRule> rules = TemplateEngine.apply(
                conditionalBreakpointTemplate, "com.example.UserService", "checkAge", "(I)V", params);

        assertEquals(1, rules.size());

        InjectionRule rule = rules.get(0);
        assertEquals("LINE_BEFORE", rule.getInjectionLocation());
        assertEquals("SNAPSHOT", rule.getCodeType());
        assertEquals("snapshot:true", rule.getLogContent());
        assertEquals("param[2] >= 18", rule.getExpression());
    }

    @Test
    void testConditionalBreakpointDefaultCondition() {
        List<InjectionRule> rules = TemplateEngine.apply(
                conditionalBreakpointTemplate, "com.example.UserService", "checkAge", "(I)V", null);

        InjectionRule rule = rules.get(0);
        assertEquals("true", rule.getExpression());
    }

    @Test
    void testTargetClassAndMethodSet() {
        List<InjectionRule> rules = TemplateEngine.apply(
                methodTimingTemplate, "com.example.MyService", "doWork", "()V", null);

        for (InjectionRule rule : rules) {
            assertEquals("com.example.MyService", rule.getTargetClass());
            assertEquals("doWork", rule.getTargetMethod());
            assertTrue(rule.isEnabled());
        }
    }

    @Test
    void testSlowMethodAlertTemplate() {
        RuleTemplate template = BuiltinTemplates.slowMethodAlert();
        Map<String, String> params = new HashMap<>();
        params.put("threshold", "1000");

        List<InjectionRule> rules = TemplateEngine.apply(
                template, "com.example.SlowService", "process", "()V", params);

        assertEquals(2, rules.size());
        assertEquals("trace:start", rules.get(0).getLogContent());
        assertEquals("trace:alert:1000", rules.get(1).getLogContent());
    }

    @Test
    void testMethodAccessLogTemplate() {
        RuleTemplate template = BuiltinTemplates.methodAccessLog();

        List<InjectionRule> rules = TemplateEngine.apply(
                template, "com.example.ApiController", "handleRequest", "()V", null);

        assertEquals(2, rules.size());
        assertEquals("log:→ call: $0", rules.get(0).getLogContent());
        assertEquals("log:← return", rules.get(1).getLogContent());
    }
}
