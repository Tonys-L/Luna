package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.rule.InjectionRule;
import fun.efto.luna.core.injection.rule.template.RuleTemplate;
import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.plugin.builtin.conditional.ConditionalBreakpointTemplates;
import fun.efto.luna.core.plugin.builtin.log.LogTemplates;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotTemplates;
import fun.efto.luna.core.plugin.builtin.trace.TraceTemplates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 14:00
 */
class TemplateNameCleanupTest {

    @Nested
    @DisplayName("迭代6：模板 probeType 与 code 前缀清理")
    class TemplateProbeTypeTest {

        @Test
        @DisplayName("LogTemplates 输出的 probeType 为 LOG，code 不含 log: 前缀")
        void logTemplatesProbeTypeAndCodePrefix() {
            RuleTemplate template = LogTemplates.methodAccessLog();

            for (RuleTemplate.TemplateRule rule : template.getRules()) {
                assertEquals("LOG", rule.getProbeType());
                assertFalse(rule.getCode().startsWith("log:"),
                        "code should not start with 'log:' prefix, but was: " + rule.getCode());
            }
        }

        @Test
        @DisplayName("SnapshotTemplates 输出的 probeType 为 SNAPSHOT，codeType 为 null，code 为 null")
        void snapshotTemplatesProbeTypeAndCodeType() {
            RuleTemplate template = SnapshotTemplates.lineSnapshot();

            for (RuleTemplate.TemplateRule rule : template.getRules()) {
                assertEquals("SNAPSHOT", rule.getProbeType());
                assertNull(rule.getCodeType());
                assertNull(rule.getCode());
            }
        }

        @Test
        @DisplayName("TraceTemplates 输出的 probeType 为 TRACE，codeType 为 null，code 不含 trace: 前缀")
        void traceTemplatesProbeTypeAndCodePrefix() {
            for (RuleTemplate template : TraceTemplates.all()) {
                for (RuleTemplate.TemplateRule rule : template.getRules()) {
                    assertEquals("TRACE", rule.getProbeType());
                    assertNull(rule.getCodeType());
                    assertFalse(rule.getCode().startsWith("trace:"),
                            "code should not start with 'trace:' prefix, but was: " + rule.getCode());
                }
            }
        }

        @Test
        @DisplayName("ConditionalBreakpointTemplates 输出的 probeType 为 SNAPSHOT，codeType 为 null，code 为 null")
        void conditionalBreakpointTemplatesProbeTypeAndCodeType() {
            RuleTemplate template = ConditionalBreakpointTemplates.conditionalBreakpoint();

            for (RuleTemplate.TemplateRule rule : template.getRules()) {
                assertEquals("SNAPSHOT", rule.getProbeType());
                assertNull(rule.getCodeType());
                assertNull(rule.getCode());
            }
        }
    }

    @Nested
    @DisplayName("迭代7：InjectionRule 使用 code 字段（不再有 logContent）")
    class InjectionRuleCodeFieldTest {

        @Test
        @DisplayName("InjectionRule 使用 code 字段而非 logContent")
        void injectionRuleUsesCodeField() throws NoSuchMethodException {
            InjectionRule rule = new InjectionRule();
            rule.setCode("→ call: $0");
            assertEquals("→ call: $0", rule.getCode());

            assertThrows(NoSuchMethodException.class, () ->
                InjectionRule.class.getMethod("getLogContent"));
            assertThrows(NoSuchMethodException.class, () ->
                InjectionRule.class.getMethod("setLogContent", String.class));
        }
    }

    @Nested
    @DisplayName("迭代7：InjectRequest 类存在（InjectionCommand 已重命名）")
    class InjectRequestRenameTest {

        @Test
        @DisplayName("InjectRequest 类存在且可实例化")
        void injectRequestClassExists() {
            InjectRequest request = new InjectRequest();
            assertNotNull(request);
        }

        @Test
        @DisplayName("InjectRequest 具有 probeType 字段")
        void injectRequestHasProbeType() {
            InjectRequest request = new InjectRequest();
            request.setProbeType("LOG");
            assertEquals("LOG", request.getProbeType());
        }

        @Test
        @DisplayName("InjectionCommand 类不再存在")
        void injectionCommandClassRemoved() {
            assertThrows(ClassNotFoundException.class, () ->
                Class.forName("fun.efto.luna.core.injection.InjectionCommand"));
        }
    }

    @Nested
    @DisplayName("迭代6：ExpressionCodeEngine 不再拼接 log: 前缀")
    class ExpressionCodeEngineNoPrefixTest {

        @Test
        @DisplayName("CompiledCode.content 不含 log: 前缀")
        void compiledCodeContentNoLogPrefix() {
            CompiledCode code = new CompiledCode(null, "→ call: $0");
            assertFalse(code.getContent().startsWith("log:"));
            assertEquals("→ call: $0", code.getContent());
        }
    }
}
