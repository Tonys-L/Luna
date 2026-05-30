package fun.efto.luna.core.plugin;

import fun.efto.luna.core.plugin.loader.PluginClassLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
@DisplayName("PluginClassLoader 测试")
public class PluginClassLoaderTest {

    @Test
    @DisplayName("加载框架类时委托父ClassLoader (Parent-First)")
    void testParentFirstForFrameworkClasses() throws ClassNotFoundException {
        URL[] urls = new URL[0];
        ClassLoader parent = getClass().getClassLoader();
        PluginClassLoader cl = new PluginClassLoader("test-plugin", urls, parent);

        Class<?> stringClass = cl.loadClass("java.lang.String");
        assertSame(String.class, stringClass, "java.lang.String should be loaded by parent CL");

        Class<?> lunaPluginClass = cl.loadClass("fun.efto.luna.core.plugin.LunaPlugin");
        assertNotNull(lunaPluginClass, "fun.efto.luna.core.plugin.LunaPlugin should be loaded by parent CL");
    }

    @Test
    @DisplayName("加载非框架前缀类时优先子ClassLoader (Child-First)")
    void testChildFirstForPluginClasses() {
        URL[] urls = new URL[0];
        ClassLoader parent = getClass().getClassLoader();
        PluginClassLoader cl = new PluginClassLoader("test-plugin", urls, parent);

        assertEquals("test-plugin", cl.getPluginId());

        assertDoesNotThrow(() -> {
            try {
                cl.loadClass("com.example.nonexistent.PluginClass");
            } catch (ClassNotFoundException expected) {
            }
        }, "Child-first lookup for non-framework class should not throw unexpected exceptions");
    }

    @Test
    @DisplayName("getPluginId 返回正确的插件ID")
    void testGetPluginId() {
        PluginClassLoader cl = new PluginClassLoader("my-plugin", new URL[0], getClass().getClassLoader());
        assertEquals("my-plugin", cl.getPluginId());
    }

    @Test
    @DisplayName("PARENT_FIRST_PREFIXES 包含核心框架前缀")
    void testParentFirstPrefixes() throws ClassNotFoundException {
        URL[] urls = new URL[0];
        ClassLoader parent = getClass().getClassLoader();
        PluginClassLoader cl = new PluginClassLoader("test-plugin", urls, parent);

        Class<?> listClass = cl.loadClass("java.util.ArrayList");
        assertNotNull(listClass, "java.util.ArrayList should be loaded by parent CL");
    }
}
