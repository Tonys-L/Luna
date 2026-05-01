package fun.efto.luna.agent.web.vo;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2026/01/25
 */
public class InjectionCommand {
    private String clazz;
    private String method;
    private String injectionType;
    private String desc;
    private String codeType;
    private String code;

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getInjectionType() {
        return injectionType;
    }

    public void setInjectionType(String injectionType) {
        this.injectionType = injectionType;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getCodeType() {
        return codeType;
    }

    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isValid() {
        return clazz != null && method != null && injectionType != null && codeType != null && code != null;
    }
}
