package fun.efto.luna.core.spy;

import fun.efto.luna.core.buffer.RingBuffer;

/**
 * 间谍类：作为注入字节码与 Agent 核心通信的桥梁。
 * 目标应用程序的方法被注入后，会直接调用此类中的静态方法。
 * 因此该类必须保持极度的轻量级和无阻塞。
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class LunaSpy {

    /**
     * 全局日志 RingBuffer，大小配置为 4096。
     * 如果满了，offer() 将返回 false 并丢弃日志，严格保证业务线程不阻塞。
     */
    public static final RingBuffer<String> LOG_BUFFER = new RingBuffer<>(4096);

    /**
     * 被注入到目标方法中的日志打印回调
     *
     * @param message 格式化后的日志信息
     */
    public static void onLog(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        // 非阻塞投递
        LOG_BUFFER.offer(message);
    }
}
