package fun.efto.luna.core.probe;


/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 20:00
 */
public final class ProbeOutput {

    public static final RingBuffer<ProbeMessage> BUFFER = new RingBuffer<>(4096);

    private ProbeOutput() {
    }

    public static boolean offer(ProbeMessage message) {
        return BUFFER.offer(message);
    }
}
