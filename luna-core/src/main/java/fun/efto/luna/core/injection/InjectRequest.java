package fun.efto.luna.core.injection;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 14:00
 */
public class InjectRequest {
    private String clazz;
    private String method;
    private String probeType;
    private String injectionLocation;
    private String desc;
    private String codeType;
    private String code;
    private Integer lineNumber;
    private boolean ephemeral = false;
    private String groupId;

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

    public String getProbeType() {
        return probeType;
    }

    public void setProbeType(String probeType) {
        this.probeType = probeType;
    }

    public String getInjectionLocation() {
        return injectionLocation;
    }

    public void setInjectionLocation(String injectionLocation) {
        this.injectionLocation = injectionLocation;
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
        return clazz != null && method != null && injectionLocation != null;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
