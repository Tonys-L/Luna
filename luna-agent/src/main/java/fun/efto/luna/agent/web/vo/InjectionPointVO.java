package fun.efto.luna.agent.web.vo;

/**
 * 注入点列表项 VO
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/26 22:00
 */
public class InjectionPointVO {
    private String id;
    private String type;
    private String method;
    private String code;
    private String codeType;
    private Integer lineNumber;

    public InjectionPointVO() {}

    public InjectionPointVO(String id, String type, String method, String code, String codeType, Integer lineNumber) {
        this.id = id;
        this.type = type;
        this.method = method;
        this.code = code;
        this.codeType = codeType;
        this.lineNumber = lineNumber;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCodeType() { return codeType; }
    public void setCodeType(String codeType) { this.codeType = codeType; }
    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
}
