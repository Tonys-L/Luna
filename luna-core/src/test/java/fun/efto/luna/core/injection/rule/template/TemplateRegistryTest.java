package fun.efto.luna.core.injection.rule.template;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 14:00
 */
public class TemplateRegistryTest {

    @BeforeAll
    static void registerBuiltinTemplates() {
        TemplateRegistry registry = TemplateRegistry.getInstance();
        registry.register(BuiltinTemplates.methodTiming());
        registry.register(BuiltinTemplates.methodTimingWithThreshold());
        registry.register(BuiltinTemplates.methodAccessLog());
        registry.register(BuiltinTemplates.slowMethodAlert());
        registry.register(BuiltinTemplates.lineSnapshot());
        registry.register(BuiltinTemplates.conditionalBreakpoint());
    }

    @Test
    void testBuiltinTemplatesRegistered() {
        TemplateRegistry registry = TemplateRegistry.getInstance();

        assertNotNull(registry.getTemplate("method-timing"));
        assertNotNull(registry.getTemplate("method-timing-threshold"));
        assertNotNull(registry.getTemplate("method-access-log"));
        assertNotNull(registry.getTemplate("slow-method-alert"));
        assertNotNull(registry.getTemplate("line-snapshot"));
        assertNotNull(registry.getTemplate("conditional-breakpoint"));
    }

    @Test
    void testGetAllTemplates() {
        TemplateRegistry registry = TemplateRegistry.getInstance();
        List<RuleTemplate> templates = registry.getAllTemplates();
        assertTrue(templates.size() >= 6);
    }

    @Test
    void testGetTemplatesByCategory() {
        TemplateRegistry registry = TemplateRegistry.getInstance();

        List<RuleTemplate> perfTemplates = registry.getTemplatesByCategory("performance");
        assertTrue(perfTemplates.size() >= 3);

        List<RuleTemplate> debugTemplates = registry.getTemplatesByCategory("debug");
        assertTrue(debugTemplates.size() >= 2);

        List<RuleTemplate> obsTemplates = registry.getTemplatesByCategory("observability");
        assertTrue(obsTemplates.size() >= 1);
    }

    @Test
    void testRegisterCustomTemplate() {
        TemplateRegistry registry = TemplateRegistry.getInstance();

        RuleTemplate custom = new RuleTemplate();
        custom.setName("custom-test-template");
        custom.setDisplayName("自定义测试模板");
        custom.setCategory("custom");
        custom.setVersion("1.0.0");
        custom.setAuthor("Tester");

        registry.register(custom);

        RuleTemplate retrieved = registry.getTemplate("custom-test-template");
        assertNotNull(retrieved);
        assertEquals("自定义测试模板", retrieved.getDisplayName());

        registry.unregister("custom-test-template");
        assertNull(registry.getTemplate("custom-test-template"));
    }

    @Test
    void testTemplateMetadata() {
        TemplateRegistry registry = TemplateRegistry.getInstance();

        RuleTemplate timing = registry.getTemplate("method-timing");
        assertEquals("方法耗时统计", timing.getDisplayName());
        assertEquals("performance", timing.getCategory());
        assertEquals("1.0.0", timing.getVersion());
        assertEquals("Luna", timing.getAuthor());
        assertNotNull(timing.getDescription());
    }
}
