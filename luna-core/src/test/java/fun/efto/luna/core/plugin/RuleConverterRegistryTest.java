package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectionPoint;
import fun.efto.luna.core.injection.PersistentInjection;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import fun.efto.luna.core.plugin.registry.RuleConverterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
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
        InjectionLocation location = InjectionLocation.of("method_enter", "test");

        InjectionPoint expectedPoint = new InjectionPoint(null, null);

        InjectionRuleConverter converter = rule -> expectedPoint;

        InjectionTypeRegistry.getInstance().register(location);
        RuleConverterRegistry.getInstance().register(location, converter);

        PersistentInjection injection = new PersistentInjection();
        injection.setInjectionLocation("method_enter");

        InjectionPoint result = RuleConverterRegistry.getInstance().convert(injection);
        assertSame(expectedPoint, result);
    }

    @Test
    void testConvertNoConverter() {
        InjectionLocation location = InjectionLocation.of("method_enter", "test");

        InjectionTypeRegistry.getInstance().register(location);

        PersistentInjection injection = new PersistentInjection();
        injection.setInjectionLocation("method_enter");

        assertThrows(IllegalStateException.class, () -> RuleConverterRegistry.getInstance().convert(injection));
    }
}
