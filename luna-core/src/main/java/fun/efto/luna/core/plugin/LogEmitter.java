package fun.efto.luna.core.plugin;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/11 22:00
 */

public interface LogEmitter {

    void emitLog(String message);

    void emitSnapshot(String pointId, Object[] localVars, String[] varNames);

    void emitTraceStart();

    void emitTraceEnd(String className, String methodName, long thresholdMs);

    void emitTraceAlert(String className, String methodName, long thresholdMs);
}
