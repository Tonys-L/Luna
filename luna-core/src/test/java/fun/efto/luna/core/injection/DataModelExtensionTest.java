package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.rule.InjectionRule;
import fun.efto.luna.core.injection.rule.template.RuleTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 10:00
 */
class DataModelExtensionTest {

    @Nested
    @DisplayName("InjectionRule 新字段测试")
    class InjectionRuleTest {

        @Test
        @DisplayName("probeType getter/setter")
        void probeTypeGetterSetter() {
            InjectionRule rule = new InjectionRule();
            assertNull(rule.getProbeType());
            rule.setProbeType("LOG");
            assertEquals("LOG", rule.getProbeType());
        }

        @Test
        @DisplayName("injectionLocation getter/setter")
        void injectionLocationGetterSetter() {
            InjectionRule rule = new InjectionRule();
            assertNull(rule.getInjectionLocation());
            rule.setInjectionLocation("method_enter");
            assertEquals("method_enter", rule.getInjectionLocation());
        }
    }

    @Nested
    @DisplayName("PersistentInjection 新字段测试")
    class PersistentInjectionTest {

        @Test
        @DisplayName("probeType getter/setter")
        void probeTypeGetterSetter() {
            PersistentInjection injection = new PersistentInjection();
            assertNull(injection.getProbeType());
            injection.setProbeType("TRACE");
            assertEquals("TRACE", injection.getProbeType());
        }

        @Test
        @DisplayName("injectionLocation getter/setter")
        void injectionLocationGetterSetter() {
            PersistentInjection injection = new PersistentInjection();
            assertNull(injection.getInjectionLocation());
            injection.setInjectionLocation("method_exit");
            assertEquals("method_exit", injection.getInjectionLocation());
        }
    }

    @Nested
    @DisplayName("InjectionCommand 新字段测试")
    class InjectionCommandTest {

        @Test
        @DisplayName("probeType getter/setter")
        void probeTypeGetterSetter() {
            InjectionCommand command = new InjectionCommand();
            assertNull(command.getProbeType());
            command.setProbeType("SNAPSHOT");
            assertEquals("SNAPSHOT", command.getProbeType());
        }

        @Test
        @DisplayName("injectionLocation getter/setter")
        void injectionLocationGetterSetter() {
            InjectionCommand command = new InjectionCommand();
            assertNull(command.getInjectionLocation());
            command.setInjectionLocation("line_before");
            assertEquals("line_before", command.getInjectionLocation());
        }
    }

    @Nested
    @DisplayName("RuleTemplate.TemplateRule 新字段测试")
    class TemplateRuleTest {

        @Test
        @DisplayName("probeType getter/setter")
        void probeTypeGetterSetter() {
            RuleTemplate.TemplateRule rule = new RuleTemplate.TemplateRule();
            assertNull(rule.getProbeType());
            rule.setProbeType("LOG");
            assertEquals("LOG", rule.getProbeType());
        }

        @Test
        @DisplayName("injectionLocation getter/setter")
        void injectionLocationGetterSetter() {
            RuleTemplate.TemplateRule rule = new RuleTemplate.TemplateRule();
            assertNull(rule.getInjectionLocation());
            rule.setInjectionLocation("method_enter");
            assertEquals("method_enter", rule.getInjectionLocation());
        }
    }
}
