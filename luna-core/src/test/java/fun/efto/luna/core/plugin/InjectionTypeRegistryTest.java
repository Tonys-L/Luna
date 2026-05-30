package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class InjectionTypeRegistryTest {

    @AfterEach
    void tearDown() {
        InjectionTypeRegistry.getInstance().clear();
    }

    @Test
    void testRegisterAndResolve() {
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
        assertSame(type, InjectionTypeRegistry.getInstance().resolve("method_enter"));
    }

    @Test
    void testResolveUnknown() {
        assertThrows(IllegalArgumentException.class, () -> InjectionTypeRegistry.getInstance().resolve("unknown"));
    }

    @Test
    void testAliasSupport() {
        InjectionType type = new InjectionType() {
            @Override
            public String getName() {
                return "method_enter";
            }

            @Override
            public String getDescription() {
                return "test";
            }

            @Override
            public List<String> getAliases() {
                return Arrays.asList("enter", "ENTER");
            }
        };

        InjectionTypeRegistry.getInstance().register(type);
        assertSame(type, InjectionTypeRegistry.getInstance().resolve("enter"));
        assertSame(type, InjectionTypeRegistry.getInstance().resolve("ENTER"));
    }

    @Test
    void testUnregisterAll() {
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
        InjectionTypeRegistry.getInstance().unregisterAll(Collections.singletonList(type));
        assertThrows(IllegalArgumentException.class, () -> InjectionTypeRegistry.getInstance().resolve("method_enter"));
    }
}
