package fun.efto.luna.agent.web.vo;

/**
 * 注入结果 VO
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/26 22:00
 */
public class InjectionResultVO {
    private boolean success;
    private String type;
    private String method;
    private String message;
    private String injectionPointId;

    public InjectionResultVO() {}

    public InjectionResultVO(boolean success, String type, String method, String message, String injectionPointId) {
        this.success = success;
        this.type = type;
        this.method = method;
        this.message = message;
        this.injectionPointId = injectionPointId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getInjectionPointId() { return injectionPointId; }
    public void setInjectionPointId(String injectionPointId) { this.injectionPointId = injectionPointId; }
}
