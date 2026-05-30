package fun.efto.luna.core.plugin.builtin.trace;

import fun.efto.luna.core.probe.BootstrapClassRegistry;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 20:00
 */
public class TraceProbe {

    public static final String INTERNAL_NAME = TraceProbe.class.getName().replace('.', '/');

    static {
        BootstrapClassRegistry.register(TraceProbe.class.getName());
    }

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    private TraceProbe() {
    }

    public static void onTraceStart() {
        START_TIME.set(System.nanoTime());
    }

    public static void onTraceEnd(String className, String methodName, long thresholdMs) {
        Long start = START_TIME.get();
        if (start == null) return;
        START_TIME.remove();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        if (elapsedMs >= thresholdMs) {
            ProbeOutput.offer(new ProbeMessage("TRACE", "[TRACE] " + className + "." + methodName + " took " + elapsedMs + " ms"));
        }
    }

    public static void onTraceAlert(String className, String methodName, long thresholdMs) {
        Long start = START_TIME.get();
        if (start == null) return;
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        if (elapsedMs >= thresholdMs) {
            ProbeOutput.offer(new ProbeMessage("TRACE", "[SLOW] " + className + "." + methodName + " took " + elapsedMs + " ms (threshold=" + thresholdMs + " ms)"));
        }
    }
}
