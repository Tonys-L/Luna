package fun.efto.luna.core.plugin;

import fun.efto.luna.core.probe.LunaSpy;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */
public class DefaultLogEmitter implements LogEmitter {

    @Override
    public void emitLog(String message) {
        LunaSpy.onLog(message);
    }

    @Override
    public void emitSnapshot(String pointId, Object[] localVars, String[] varNames) {
        LunaSpy.onSnapshot(pointId, localVars, varNames);
    }

    @Override
    public void emitTraceStart() {
        LunaSpy.onTraceStart();
    }

    @Override
    public void emitTraceEnd(String className, String methodName, long thresholdMs) {
        LunaSpy.onTraceEnd(className, methodName, thresholdMs);
    }

    @Override
    public void emitTraceAlert(String className, String methodName, long thresholdMs) {
        LunaSpy.onTraceAlert(className, methodName, thresholdMs);
    }
}
