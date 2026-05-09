package fun.efto.luna.agent.web.ws;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * 日志 WebSocket Servlet 适配器
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class LogWebSocketServlet extends WebSocketServlet {

    @Override
    public void configure(WebSocketServletFactory factory) {
        // 设置空闲超时时间（例如：1小时）
        factory.getPolicy().setIdleTimeout(3600000);
        // 配置最大文本消息大小（如果需要）
        factory.getPolicy().setMaxTextMessageSize(65535);

        // 注册 Endpoint 创建器
        factory.setCreator((req, resp) -> new LogWebSocketEndpoint());
    }
}
