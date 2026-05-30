package fun.efto.luna.core.probe;

import fun.efto.luna.core.common.RingBuffer;
import fun.efto.luna.core.plugin.builtin.log.LogProbe;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotProbe;
import fun.efto.luna.core.plugin.builtin.trace.TraceProbe;
import fun.efto.luna.core.probe.BootstrapClassRegistry;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;

/**
 * 间谍类：作为注入字节码与 Agent 核心通信的桥梁。
 * 目标应用程序的方法被注入后，会直接调用此类中的静态方法。
 * 因此该类必须保持极度的轻量级和无阻塞。
 *
 * <p>所有方法均委托给对应的 Probe 类，确保单一数据路径，
 * 避免 ThreadLocal 不共享等数据流断裂问题。</p>
 *
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/10 00:00
 */
public class LunaSpy {

    static {
        BootstrapClassRegistry.register(LunaSpy.class.getName());
    }

    public static final RingBuffer<ProbeMessage> LOG_BUFFER = ProbeOutput.BUFFER;

    public static void onLog(String message) {
        LogProbe.onLog(message);
    }

    public static void onSnapshot(String pointId, Object[] localVars, String[] varNames) {
        SnapshotProbe.onSnapshot(pointId, localVars, varNames);
    }

    public static void onTraceStart() {
        TraceProbe.onTraceStart();
    }

    public static void onTraceEnd(String className, String methodName, long thresholdMs) {
        TraceProbe.onTraceEnd(className, methodName, thresholdMs);
    }

    public static void onTraceAlert(String className, String methodName, long thresholdMs) {
        TraceProbe.onTraceAlert(className, methodName, thresholdMs);
    }
}
