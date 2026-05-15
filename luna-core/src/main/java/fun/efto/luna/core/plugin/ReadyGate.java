package fun.efto.luna.core.plugin;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */

public class ReadyGate {

    private volatile boolean ready = false;

    public void markReady() {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }
}
