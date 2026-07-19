package fun.efto.luna.agent.web;

import fun.efto.luna.agent.clazz.ClassResourceHelper;
import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.agent.web.controller.ClassController;
import fun.efto.luna.agent.web.controller.InjectionController;
import fun.efto.luna.agent.web.controller.StatusController;
import fun.efto.luna.agent.web.controller.TestController;
import fun.efto.luna.agent.web.controller.MetricsController;
import fun.efto.luna.agent.web.controller.CapabilityController;
import fun.efto.luna.agent.web.mvc.DispatcherServlet;
import fun.efto.luna.agent.web.ws.LogDispatcher;
import fun.efto.luna.agent.web.ws.LogWebSocketServlet;
import fun.efto.luna.core.injection.InjectionService;
import fun.efto.luna.core.plugin.LunaController;
import fun.efto.luna.core.infra.web.WebServer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.FilterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.List;
import java.util.EnumSet;
import java.io.IOException;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/09 10:00
 */
public class JettyWebServer implements WebServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyWebServer.class);

    private final Server server;
    private final int port;
    private final JettyConfiguration configuration;
    private final DispatcherServlet dispatcher;
    private volatile boolean running = false;

    public JettyWebServer(int port, JettyConfiguration configuration,
                          ClassScanner classScanner,
                          ClassResourceHelper classResourceHelper,
                          InjectionService injectionService) {
        this.port = port;
        this.configuration = configuration;
        this.dispatcher = new DispatcherServlet();
        this.server = createServer(classScanner, classResourceHelper, injectionService);
    }

    private Server createServer(ClassScanner classScanner,
                                 ClassResourceHelper classResourceHelper,
                                 InjectionService injectionService) {
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

        dispatcher.registerController(new StatusController());
        dispatcher.registerController(new ClassController(classScanner, classResourceHelper, injectionService));
        dispatcher.registerController(new InjectionController(injectionService));
        dispatcher.registerController(new TestController(classScanner, classResourceHelper));
        dispatcher.registerController(new MetricsController(injectionService));
        dispatcher.registerController(new CapabilityController());

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setDisplayName("Luna Web Server");

        context.addFilter(new FilterHolder(new Filter() {
            @Override
            public void init(FilterConfig filterConfig) {}
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                request.setCharacterEncoding("UTF-8");
                response.setCharacterEncoding("UTF-8");
                chain.doFilter(request, response);
            }
            @Override
            public void destroy() {}
        }), "/*", EnumSet.of(DispatcherType.REQUEST));

        context.addServlet(new ServletHolder(dispatcher), "/api/*");
        context.addServlet(new ServletHolder(new LogWebSocketServlet()), "/ws/log");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new org.eclipse.jetty.server.Handler[]{resourceHandler, context});
        jettyServer.setHandler(handlers);

        jettyServer.setStopAtShutdown(true);
        jettyServer.setStopTimeout(configuration.getStopTimeoutMs());

        return jettyServer;
    }

    @Override
    public void registerControllers(List<LunaController> controllers) {
        for (LunaController controller : controllers) {
            dispatcher.registerController(controller);
        }
    }

    @Override
    public void unregisterControllers(List<LunaController> controllers) {
        for (LunaController controller : controllers) {
            dispatcher.unregisterController(controller);
        }
    }

    public void start() throws Exception {
        if (running) {
            throw new IllegalStateException("服务器已经在运行");
        }

        try {
            server.start();
            running = true;

            LogDispatcher.getInstance().start();

            LOGGER.info("Luna Web服务器启动成功:");
            LOGGER.info("  - HTTP服务: http://{}:{}", configuration.getHost(), port);
            LOGGER.info("  - WebSocket服务: ws://{}:{}/ws/log", configuration.getHost(), port);

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
            LogDispatcher.getInstance().stop();

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
