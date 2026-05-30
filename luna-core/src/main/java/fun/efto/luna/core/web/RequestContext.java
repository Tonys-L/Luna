package fun.efto.luna.core.web;

/**
 * 容器无关的请求上下文接口。
 * 隔离不同 HTTP 服务器实现（Servlet、JDK HttpServer 等），
 * 使路由引擎不依赖任何具体容器 API。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public interface RequestContext {

    /**
     * 获取 HTTP 方法（GET、POST、PUT、DELETE 等）
     */
    String getMethod();

    /**
     * 获取请求路径（不含查询参数）
     */
    String getPath();

    /**
     * 获取查询参数
     */
    String getQueryParam(String name);

    /**
     * 获取请求体内容
     */
    String getRequestBody();

    /**
     * 获取请求头
     */
    String getHeader(String name);

    /**
     * 设置响应状态码
     */
    void setStatus(int status);

    /**
     * 设置响应头
     */
    void setHeader(String name, String value);

    /**
     * 写入响应体
     */
    void writeResponse(String body);
}
