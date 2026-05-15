package fun.efto.luna.core.web;

import com.sun.net.httpserver.HttpServer;
import fun.efto.luna.core.plugin.LunaController;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/11 22:00
 */
public final class LunaWebServer {

    private final HttpServer httpServer;
    private final DispatcherServlet dispatcher;
    private final int port;
    private volatile boolean running;

    public LunaWebServer(int port) throws IOException {
        this.port = port;
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.dispatcher = new DispatcherServlet();
        this.httpServer.createContext("/api", dispatcher);
        this.httpServer.setExecutor(null);
    }

    public void start() {
        if (running) {
            throw new IllegalStateException("LunaWebServer is already running");
        }
        httpServer.start();
        running = true;
    }

    public void stop() {
        if (!running) {
            return;
        }
        httpServer.stop(0);
        running = false;
    }

    public void registerControllers(List<LunaController> controllers) {
        for (LunaController controller : controllers) {
            dispatcher.register(controller);
        }
    }

    public void unregisterControllers(List<LunaController> controllers) {
        for (LunaController controller : controllers) {
            dispatcher.unregister(controller);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }
}
