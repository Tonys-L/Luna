package fun.efto.luna.agent.web.mvc;

import fun.efto.luna.core.web.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Servlet 容器的 RequestContext 适配器。
 * 将 HttpServletRequest/HttpServletResponse 适配为容器无关的 RequestContext 接口。
 *
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/27 12:00
 */
public class ServletRequestContext implements RequestContext {

    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    public ServletRequestContext(HttpServletRequest req, HttpServletResponse resp) {
        this.req = req;
        this.resp = resp;
    }

    @Override
    public String getMethod() {
        return req.getMethod();
    }

    @Override
    public String getPath() {
        String pathInfo = req.getPathInfo();
        return pathInfo != null ? pathInfo : "/";
    }

    @Override
    public String getQueryParam(String name) {
        return req.getParameter(name);
    }

    @Override
    public String getRequestBody() {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            java.io.InputStream is = req.getInputStream();
            while ((len = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, len);
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getHeader(String name) {
        return req.getHeader(name);
    }

    @Override
    public void setStatus(int status) {
        resp.setStatus(status);
    }

    @Override
    public void setHeader(String name, String value) {
        resp.setHeader(name, value);
    }

    @Override
    public void writeResponse(String body) {
        try {
            resp.getWriter().write(body);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write response", e);
        }
    }

    /**
     * 获取原始 HttpServletRequest（用于控制器需要直接访问 Servlet API 的场景）
     */
    public HttpServletRequest getServletRequest() {
        return req;
    }

    /**
     * 获取原始 HttpServletResponse
     */
    public HttpServletResponse getServletResponse() {
        return resp;
    }
}
