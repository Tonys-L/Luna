package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
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
        RuleConverterRegistry.getInstance().clear();
        InjectionTypeRegistry.getInstance().clear();
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

        InjectionTypeRegistry.getInstance().register(type);
        RuleConverterRegistry.getInstance().register(type, converter);

        PersistentInjection injection = new PersistentInjection();
        injection.setInjectionType("method_enter");

        InjectionPoint result = RuleConverterRegistry.getInstance().convert(injection);
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

        InjectionTypeRegistry.getInstance().register(type);

        PersistentInjection injection = new PersistentInjection();
        injection.setInjectionType("method_enter");

        assertThrows(IllegalStateException.class, () -> RuleConverterRegistry.getInstance().convert(injection));
    }
}
