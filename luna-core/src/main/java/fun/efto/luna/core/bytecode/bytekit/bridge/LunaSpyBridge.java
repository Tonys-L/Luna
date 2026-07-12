package fun.efto.luna.core.bytecode.bytekit.bridge;

import fun.efto.luna.core.plugin.builtin.log.LogProbe;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/31 22:30
 */
public class LunaSpyBridge {

    private LunaSpyBridge() {
    }

    public static void onMethodEnter(Object target, Object[] args, String methodName) {
        String message = "[ENTER] " + methodName;
        LogProbe.onLog(message);
    }

    public static void onMethodExit(Object target, Object[] args, String methodName, Object returnValue) {
        String message = "[EXIT] " + methodName + " => " + returnValue;
        LogProbe.onLog(message);
    }
}
