package fun.efto.luna.agent.web.ws;

import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 实时日志分发器
 * 负责在后台不断从 RingBuffer 拉取数据并推送给所有连入的 WebSocket 客户端
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/10 00:00
 */
public class LogDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogDispatcher.class);
    /**
     * 独立的探针日志 logger，输出到 luna-probe.log。
     * 与 Luna Agent 运行日志（luna-agent.log）分离，便于长期监控与偶发问题排查。
     * attacher 断开后，只要目标 JVM 存活，此 logger 仍会持续写入文件。
     */
    private static final Logger PROBE_LOGGER = LoggerFactory.getLogger("luna.probe");

    private static final LogDispatcher INSTANCE = new LogDispatcher();

    // 线程安全的会话集合
    private final Set<Session> sessions = new CopyOnWriteArraySet<>();

    // 消费线程的运行状态
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread consumerThread;

    private LogDispatcher() {
        // 单例
    }

    public static LogDispatcher getInstance() {
        return INSTANCE;
    }

    public void addSession(Session session) {
        sessions.add(session);
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }

    /**
     * 启动消费者后台线程
     */
    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        consumerThread = new Thread(this::consumeLoop, "Luna-Log-Dispatcher");
        consumerThread.setDaemon(true); // 设置为守护线程，防止阻止 JVM 退出
        consumerThread.start();
        LOGGER.info("Luna 日志分发器已启动");
    }

    /**
     * 停止消费者后台线程
     */
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            if (consumerThread != null) {
                consumerThread.interrupt();
            }
            // 清理 Session
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.close(1000, "Server stopping");
                }
            }
            sessions.clear();
            LOGGER.info("Luna 日志分发器已停止");
        }
    }

    /**
     * 消费者核心循环
     */
    private void consumeLoop() {
        while (running.get()) {
            try {
                // 如果没有连接，暂停消费但不要把数据吃掉，所以可以先 peek 吗？
                // RingBuffer 是单向消费，如果没有 session，我们可以选择丢弃或者自旋等待。
                // 为了避免日志堆积导致 OOM，如果没有 session，我们需要把缓冲区清空（丢弃）。
                
                ProbeMessage message = ProbeOutput.BUFFER.poll();
                
                if (message != null) {
                    // 注入日志写入独立的 luna-probe.log，与 Luna Agent 运行日志分离
                    PROBE_LOGGER.info("[{}] {}", message.getType(), message.getPayload());

                    if (!sessions.isEmpty()) {
                        broadcast(message.toJson());
                    }
                } else {
                    // 没有数据，进行短暂休眠避免 CPU 空转
                    // 使用 Thread.sleep(1) 可以避免自旋 100% CPU，由于是推送面板，1ms的延迟是完全可接受的
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.error("日志消费循环发生异常", e);
            }
        }
    }

    private void broadcast(String message) {
        for (Session session : sessions) {
            if (session.isOpen()) {
                try {
                    // 使用异步发送提高吞吐量，避免某个慢客户端阻塞整个分发流程
                    session.getRemote().sendStringByFuture(message);
                } catch (Exception e) {
                    // 若发送失败可能连接已失效，移除
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
    }
}
