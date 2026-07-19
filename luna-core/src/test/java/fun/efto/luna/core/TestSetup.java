package fun.efto.luna.core;

import fun.efto.luna.core.plugin.builtin.CoreModuleInitializer;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 19:00
 */
public final class TestSetup {

    public static synchronized void init() {
        CoreModuleInitializer.initialize();
    }
}
