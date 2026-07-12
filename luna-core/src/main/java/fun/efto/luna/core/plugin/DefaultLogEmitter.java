package fun.efto.luna.core.plugin;

import fun.efto.luna.core.plugin.builtin.log.LogProbe;
import fun.efto.luna.core.plugin.builtin.snapshot.SnapshotProbe;
import fun.efto.luna.core.plugin.builtin.trace.TraceProbe;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class DefaultLogEmitter implements LogEmitter {

    @Override
    public void emitLog(String message) {
        LogProbe.onLog(message);
    }

    @Override
    public void emitSnapshot(String pointId, Object[] localVars, String[] varNames) {
        SnapshotProbe.onSnapshot(pointId, localVars, varNames);
    }

    @Override
    public void emitTraceStart() {
        TraceProbe.onTraceStart();
    }

    @Override
    public void emitTraceEnd(String className, String methodName, long thresholdMs) {
        TraceProbe.onTraceEnd(className, methodName, thresholdMs);
    }

    @Override
    public void emitTraceAlert(String className, String methodName, long thresholdMs) {
        TraceProbe.onTraceAlert(className, methodName, thresholdMs);
    }
}
