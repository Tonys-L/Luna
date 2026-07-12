package fun.efto.luna.core.plugin.builtin.snapshot;

import fun.efto.luna.core.probe.BootstrapClassRegistry;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 20:00
 */
public class SnapshotProbe {

    public static final String INTERNAL_NAME = SnapshotProbe.class.getName().replace('.', '/');

    static {
        BootstrapClassRegistry.register(SnapshotProbe.class.getName());
        BootstrapClassRegistry.register(StackFrameCapture.class.getName());
    }

    private SnapshotProbe() {
    }

    public static void onSnapshot(String pointId, Object[] localVars, String[] varNames) {
        try {
            String json = StackFrameCapture.capture(pointId, localVars, varNames);
            ProbeOutput.offer(new ProbeMessage("SNAPSHOT", json));
        } catch (Throwable ignored) {
        }
    }
}
