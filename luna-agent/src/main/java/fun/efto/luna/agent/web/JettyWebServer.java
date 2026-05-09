package fun.efto.luna.agent.web;

import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.web.controller.ClassController;
import fun.efto.luna.agent.web.controller.InjectionController;
import fun.efto.luna.agent.web.controller.RuleController;
import fun.efto.luna.agent.web.controller.StatusController;
import fun.efto.luna.agent.web.controller.TestController;
import fun.efto.luna.agent.web.mvc.DispatcherServlet;
import fun.efto.luna.core.InjectionExecutor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.net.URL;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/09 10:00
 */
public class JettyWebServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyWebServer.class);

    private final Server server;
    private final int port;
    private final JettyConfiguration configuration;
    private volatile boolean running = false;

    public JettyWebServer(int port, JettyConfiguration configuration,
                          InjectionExecutor injectionExecutor,
                          ClassScanner classScanner,
                          Instrumentation instrumentation) {
        this.port = port;
        this.configuration = configuration;
        this.server = createServer(injectionExecutor, classScanner, instrumentation);
    }

    private Server createServer(InjectionExecutor injectionExecutor,
                                 ClassScanner classScanner,
                                 Instrumentation instrumentation) {
        Server jettyServer = new Server();

        ServerConnector connector = new ServerConnector(jettyServer);
        connector.setPort(port);
        connector.setHost(configuration.getHost());
        connector.setIdleTimeout(configuration.getIdleTimeoutMs());
        connector.setAcceptQueueSize(configuration.getAcceptQueueSize());
        jettyServer.addConnector(connector);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});

        URL staticResourceUrl = this.getClass().getClassLoader().getResource("static");
        if (staticResourceUrl != null) {
            resourceHandler.setResourceBase(staticResourceUrl.toExternalForm());
        } else {
            resourceHandler.setResourceBase("src/main/resources/static");
        }

        ClassResourceHelper classResourceHelper = new ClassResourceHelper(instrumentation);

        DispatcherServlet dispatcher = new DispatcherServlet();
        dispatcher.registerController(new StatusController());
        dispatcher.registerController(new ClassController(classScanner, classResourceHelper));
        dispatcher.registerController(new InjectionController(injectionExecutor, classResourceHelper));
        dispatcher.registerController(new RuleController());
        dispatcher.registerController(new TestController(classScanner, classResourceHelper));

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(dispatcher), "/api/*");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new org.eclipse.jetty.server.Handler[]{resourceHandler, context});
        jettyServer.setHandler(handlers);

        jettyServer.setStopAtShutdown(true);
        jettyServer.setStopTimeout(configuration.getStopTimeoutMs());

        return jettyServer;
    }

    public void start() throws Exception {
        if (running) {
            throw new IllegalStateException("服务器已经在运行");
        }

        try {
            server.start();
            running = true;

            LOGGER.info("Luna Web服务器启动成功:");
            LOGGER.info("  - HTTP服务: http://{}:{}", configuration.getHost(), port);
            LOGGER.info("  - 管理界面: http://{}:{}", configuration.getHost(), port);

        } catch (Exception e) {
            running = false;
            throw new RuntimeException("启动Web服务器失败", e);
        }
    }

    public void stop() throws Exception {
        if (!running) {
            return;
        }

        try {
            server.stop();
            server.destroy();
            running = false;
            LOGGER.info("Luna Web服务器已停止");
        } catch (Exception e) {
            throw new RuntimeException("停止Web服务器失败", e);
        }
    }

    public void join() throws InterruptedException {
        if (server != null) {
            server.join();
        }
    }

    public boolean isRunning() {
        return running && server.isRunning();
    }

    public JettyServerStats getStats() {
        if (!isRunning()) {
            return new JettyServerStats(false, 0, 0, 0, 0);
        }

        ServerConnector connector = (ServerConnector) server.getConnectors()[0];

        return new JettyServerStats(
                true,
                connector.getConnectedEndPoints().size(),
                System.currentTimeMillis() - System.currentTimeMillis(),
                server.getBeans().size(),
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        );
    }

    public static class JettyServerStats {
        private final boolean running;
        private final int activeConnections;
        private final long uptimeMs;
        private final int managedBeans;
        private final long memoryUsage;

        public JettyServerStats(boolean running, int activeConnections, long uptimeMs,
                                int managedBeans, long memoryUsage) {
            this.running = running;
            this.activeConnections = activeConnections;
            this.uptimeMs = uptimeMs;
            this.managedBeans = managedBeans;
            this.memoryUsage = memoryUsage;
        }

        public boolean isRunning() { return running; }
        public int getActiveConnections() { return activeConnections; }
        public long getUptimeMs() { return uptimeMs; }
        public int getManagedBeans() { return managedBeans; }
        public long getMemoryUsage() { return memoryUsage; }

        @Override
        public String toString() {
            return String.format("JettyServerStats{running=%s, connections=%d, uptime=%dms, memory=%d}",
                    running, activeConnections, uptimeMs, memoryUsage);
        }
    }
}
