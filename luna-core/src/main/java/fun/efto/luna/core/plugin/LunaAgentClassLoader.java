package fun.efto.luna.core.plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:35
 */
public class LunaAgentClassLoader extends URLClassLoader {

    public LunaAgentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void appendUrl(URL url) {
        addURL(url);
    }
}
