package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.target.type.InjectionType;
import fun.efto.luna.core.rule.InjectionRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public class RuleConverterRegistryTest {

    @AfterEach
    void tearDown() {
        RuleConverterRegistry.clear();
        InjectionTypeRegistry.clear();
    }

    @Test
    void testRegisterAndConvert() {
        InjectionType type = new InjectionType() {
            @Override
            public String getName() {
                return "method_enter";
            }

            @Override
            public String getDescription() {
                return "test";
            }
        };

        InjectionPoint expectedPoint = new InjectionPoint(null, null);

        InjectionRuleConverter converter = rule -> expectedPoint;

        InjectionTypeRegistry.register(type);
        RuleConverterRegistry.register(type, converter);

        InjectionRule rule = new InjectionRule();
        rule.setInjectionType("method_enter");

        InjectionPoint result = RuleConverterRegistry.convert(rule);
        assertSame(expectedPoint, result);
    }

    @Test
    void testConvertNoConverter() {
        InjectionType type = new InjectionType() {
            @Override
            public String getName() {
                return "method_enter";
            }

            @Override
            public String getDescription() {
                return "test";
            }
        };

        InjectionTypeRegistry.register(type);

        InjectionRule rule = new InjectionRule();
        rule.setInjectionType("method_enter");

        assertThrows(IllegalStateException.class, () -> RuleConverterRegistry.convert(rule));
    }
}
