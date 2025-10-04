package fun.efto.luna.agent.web;

/**
 * Jetty服务器配置
 * 洋葱架构基础设施层 - Jetty服务器优化配置
 * 
 * @author Tony.L
 * @since 1.0.0
 */
public class JettyConfiguration {
    
    private String host = "localhost";
    private long idleTimeoutMs = 30000; // 30秒空闲超时
    private int acceptQueueSize = 128; // 接受队列大小
    private int acceptorThreads = 1; // 接受器线程数
    private int selectorThreads = 2; // 选择器线程数
    private long stopTimeoutMs = 5000; // 停止超时
    private int maxWebSocketMessageSize = 64 * 1024; // WebSocket消息最大64KB
    private long webSocketIdleTimeoutMs = 300000; // WebSocket空闲超时5分钟
    
    // 性能优化配置
    private int minThreads = 10;
    private int maxThreads = 200;
    private int threadIdleTimeoutMs = 60000;
    private int outputBufferSize = 32768;
    private int requestHeaderSize = 8192;
    private int responseHeaderSize = 8192;
    
    public static JettyConfiguration createDefault() {
        return new JettyConfiguration();
    }
    
    public static JettyConfiguration createDevelopment() {
        JettyConfiguration config = new JettyConfiguration();
        config.setHost("0.0.0.0");
        config.setAcceptorThreads(1);
        config.setSelectorThreads(1);
        config.setMinThreads(5);
        config.setMaxThreads(50);
        return config;
    }
    
    public static JettyConfiguration createProduction() {
        JettyConfiguration config = new JettyConfiguration();
        config.setHost("0.0.0.0");
        config.setAcceptorThreads(2);
        config.setSelectorThreads(4);
        config.setMinThreads(20);
        config.setMaxThreads(500);
        config.setAcceptQueueSize(256);
        config.setIdleTimeoutMs(60000);
        return config;
    }
    
    // Getters and Setters
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public long getIdleTimeoutMs() { return idleTimeoutMs; }
    public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }
    
    public int getAcceptQueueSize() { return acceptQueueSize; }
    public void setAcceptQueueSize(int acceptQueueSize) { this.acceptQueueSize = acceptQueueSize; }
    
    public int getAcceptorThreads() { return acceptorThreads; }
    public void setAcceptorThreads(int acceptorThreads) { this.acceptorThreads = acceptorThreads; }
    
    public int getSelectorThreads() { return selectorThreads; }
    public void setSelectorThreads(int selectorThreads) { this.selectorThreads = selectorThreads; }
    
    public long getStopTimeoutMs() { return stopTimeoutMs; }
    public void setStopTimeoutMs(long stopTimeoutMs) { this.stopTimeoutMs = stopTimeoutMs; }
    
    public int getMaxWebSocketMessageSize() { return maxWebSocketMessageSize; }
    public void setMaxWebSocketMessageSize(int maxWebSocketMessageSize) { 
        this.maxWebSocketMessageSize = maxWebSocketMessageSize; 
    }
    
    public long getWebSocketIdleTimeoutMs() { return webSocketIdleTimeoutMs; }
    public void setWebSocketIdleTimeoutMs(long webSocketIdleTimeoutMs) { 
        this.webSocketIdleTimeoutMs = webSocketIdleTimeoutMs; 
    }
    
    public int getMinThreads() { return minThreads; }
    public void setMinThreads(int minThreads) { this.minThreads = minThreads; }
    
    public int getMaxThreads() { return maxThreads; }
    public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }
    
    public int getThreadIdleTimeoutMs() { return threadIdleTimeoutMs; }
    public void setThreadIdleTimeoutMs(int threadIdleTimeoutMs) { 
        this.threadIdleTimeoutMs = threadIdleTimeoutMs; 
    }
    
    public int getOutputBufferSize() { return outputBufferSize; }
    public void setOutputBufferSize(int outputBufferSize) { this.outputBufferSize = outputBufferSize; }
    
    public int getRequestHeaderSize() { return requestHeaderSize; }
    public void setRequestHeaderSize(int requestHeaderSize) { this.requestHeaderSize = requestHeaderSize; }
    
    public int getResponseHeaderSize() { return responseHeaderSize; }
    public void setResponseHeaderSize(int responseHeaderSize) { this.responseHeaderSize = responseHeaderSize; }
    
    @Override
    public String toString() {
        return String.format("JettyConfiguration{host='%s', idleTimeout=%dms, acceptQueue=%d, threads=%d-%d}",
                           host, idleTimeoutMs, acceptQueueSize, minThreads, maxThreads);
    }
}