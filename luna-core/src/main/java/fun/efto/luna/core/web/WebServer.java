package fun.efto.luna.core.web;

import fun.efto.luna.core.plugin.LunaController;

import java.util.List;

/**
 * Web 服务器抽象接口，定义控制器注册/注销契约。
 * luna-core 定义此接口，luna-agent 的 JettyWebServer 提供实现。
 * PluginManagerImpl 通过此接口注册插件控制器，无需依赖具体服务器实现。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 10:00
 */
public interface WebServer {

    /**
     * 注册插件控制器到 Web 服务器
     */
    void registerControllers(List<LunaController> controllers);

    /**
     * 注销插件控制器
     */
    void unregisterControllers(List<LunaController> controllers);
}
