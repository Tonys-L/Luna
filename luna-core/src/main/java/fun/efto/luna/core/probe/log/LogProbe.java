package fun.efto.luna.core.probe.log;

import fun.efto.luna.core.probe.BootstrapClassRegistry;
import fun.efto.luna.core.probe.ProbeMessage;
import fun.efto.luna.core.probe.ProbeOutput;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/16 20:00
 */
public class LogProbe {

    public static final String INTERNAL_NAME = LogProbe.class.getName().replace('.', '/');

    static {
        BootstrapClassRegistry.register(LogProbe.class.getName());
    }

    private LogProbe() {
    }

    public static void onLog(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        ProbeOutput.offer(new ProbeMessage("LOG", message, null, "text"));
    }
}
