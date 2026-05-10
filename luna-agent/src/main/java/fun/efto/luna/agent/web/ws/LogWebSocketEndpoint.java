package fun.efto.luna.agent.web.ws;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志 WebSocket 端点
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
@WebSocket
public class LogWebSocketEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogWebSocketEndpoint.class);
    private Session session;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        LOGGER.info("WebSocket 连接建立: {}", session.getRemoteAddress());
        LogDispatcher.getInstance().addSession(session);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        LOGGER.info("WebSocket 连接关闭: {}, status: {}, reason: {}", session.getRemoteAddress(), statusCode, reason);
        LogDispatcher.getInstance().removeSession(session);
        this.session = null;
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        LOGGER.error("WebSocket 发生错误: {}", session != null ? session.getRemoteAddress() : "unknown", t);
        if (session != null) {
            LogDispatcher.getInstance().removeSession(session);
        }
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        // Agent 主要是推送日志，接收消息可能是 ping 或者是客户端指令（暂未实现）
        if ("ping".equalsIgnoreCase(message)) {
            try {
                session.getRemote().sendString("pong");
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
