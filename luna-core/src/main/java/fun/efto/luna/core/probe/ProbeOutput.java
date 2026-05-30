package fun.efto.luna.core.probe;

import fun.efto.luna.core.buffer.RingBuffer;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/16 20:00
 */
public final class ProbeOutput {

    public static final RingBuffer<ProbeMessage> BUFFER = new RingBuffer<>(4096);

    private ProbeOutput() {
    }

    public static boolean offer(ProbeMessage message) {
        return BUFFER.offer(message);
    }

    /**
     * Convenience method for backward compatibility — wraps plain strings as LOG type messages.
     */
    public static boolean offer(String message) {
        return BUFFER.offer(new ProbeMessage("LOG", message));
    }
}
