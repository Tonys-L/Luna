package fun.efto.luna.agent.web;

import fun.efto.luna.agent.clazz.ClassScanner;
import fun.efto.luna.core.InjectionExecutor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Jetty Web服务器实现
 * 洋葱架构基础设施层 - 提供HTTP和WebSocket服务
 *
 * @author Tony.L
 * @since 1.0.0
 */
public class JettyWebServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JettyWebServer.class);

    private final Server server;
    private final int port;
    private final JettyConfiguration configuration;
    private volatile boolean running = false;
    private InjectionExecutor injectionExecutor;
    private ClassScanner classScanner;

    public JettyWebServer(int port, JettyConfiguration configuration,
                          InjectionExecutor injectionExecutor,
                          ClassScanner classScanner) {
        this.port = port;
        this.configuration = configuration;
        this.injectionExecutor = injectionExecutor;
        this.classScanner = classScanner;
        this.server = createServer();
    }

    /**
     * 创建和配置Jetty服务器
     */
    private Server createServer() {
        Server jettyServer = new Server();

        // 配置连接器
        ServerConnector connector = new ServerConnector(jettyServer);
        connector.setPort(port);
        connector.setHost(configuration.getHost());

        // 连接器优化配置
        connector.setIdleTimeout(configuration.getIdleTimeoutMs());
        connector.setAcceptQueueSize(configuration.getAcceptQueueSize());

        jettyServer.addConnector(connector);

        // 配置静态资源处理器
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"class_tree_viewer.html"});

        // 设置静态资源基础路径为JAR包内的资源
        URL staticResourceUrl = this.getClass().getClassLoader().getResource("static");
        if (staticResourceUrl != null) {
            resourceHandler.setResourceBase(staticResourceUrl.toExternalForm());
        } else {
            // 如果在开发环境中找不到资源，使用相对路径
            resourceHandler.setResourceBase("src/main/resources/static");
        }

        // 配置Servlet上下文
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // 添加REST API Servlet
        context.addServlet(new ServletHolder(new LunaApiServlet(injectionExecutor, classScanner)), "/api/*");

        // 创建处理器列表，将静态资源处理器和上下文处理器组合
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new org.eclipse.jetty.server.Handler[]{resourceHandler, context});
        jettyServer.setHandler(handlers);

        // 服务器优化配置
        jettyServer.setStopAtShutdown(true);
        jettyServer.setStopTimeout(configuration.getStopTimeoutMs());

        return jettyServer;
    }

    /**
     * 启动服务器
     */
    public void start() throws Exception {
        if (running) {
            throw new IllegalStateException("服务器已经在运行");
        }

        try {
            server.start();
            running = true;

            LOGGER.info("Luna Web服务器启动成功:");
            LOGGER.info("  - HTTP服务: http://{}:{}", configuration.getHost(), port);
            LOGGER.info("  - WebSocket: ws://{}:{}/ws", configuration.getHost(), port);
            LOGGER.info("  - 管理界面: http://{}:{}", configuration.getHost(), port);

        } catch (Exception e) {
            running = false;
            throw new RuntimeException("启动Web服务器失败", e);
        }
    }

    /**
     * 停止服务器
     */
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

    /**
     * 等待服务器启动完成
     */
    public void join() throws InterruptedException {
        if (server != null) {
            server.join();
        }
    }

    /**
     * 检查服务器是否运行中
     */
    public boolean isRunning() {
        return running && server.isRunning();
    }

    /**
     * 获取服务器统计信息
     */
    public JettyServerStats getStats() {
        if (!isRunning()) {
            return new JettyServerStats(false, 0, 0, 0, 0);
        }

        ServerConnector connector = (ServerConnector) server.getConnectors()[0];

        return new JettyServerStats(
                true,
                connector.getConnectedEndPoints().size(),
                System.currentTimeMillis() - System.currentTimeMillis(), // 简化实现
                server.getBeans().size(),
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        );
    }

    /**
     * 服务器统计信息
     */
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

        public boolean isRunning() {
            return running;
        }

        public int getActiveConnections() {
            return activeConnections;
        }

        public long getUptimeMs() {
            return uptimeMs;
        }

        public int getManagedBeans() {
            return managedBeans;
        }

        public long getMemoryUsage() {
            return memoryUsage;
        }

        @Override
        public String toString() {
            return String.format("JettyServerStats{running=%s, connections=%d, uptime=%dms, memory=%d}",
                    running, activeConnections, uptimeMs, memoryUsage);
        }
    }
}

