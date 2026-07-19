package fun.efto.luna.core.injection;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/17 18:12
 */
public class PersistentInjection {
    private String id;
    private String clazz;
    private String methodName;
    private String methodDescriptor;
    private String probeType;
    private String injectionLocation;
    private String codeType;
    private String code;
    private Integer lineNumber;
    private String expression;
    private String fieldName;
    private String fieldDescriptor;
    private boolean enabled = true;
    private boolean ephemeral = false;
    private InjectionStatus status = InjectionStatus.ACTIVE;
    private String groupId;
    private String suspendReason;

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClazz() { return clazz; }
    public void setClazz(String clazz) { this.clazz = clazz; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getMethodDescriptor() { return methodDescriptor; }
    public void setMethodDescriptor(String methodDescriptor) { this.methodDescriptor = methodDescriptor; }

    public String getProbeType() { return probeType; }
    public void setProbeType(String probeType) { this.probeType = probeType; }

    public String getInjectionLocation() { return injectionLocation; }
    public void setInjectionLocation(String injectionLocation) { this.injectionLocation = injectionLocation; }

    public String getCodeType() { return codeType; }
    public void setCodeType(String codeType) { this.codeType = codeType; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getLineNumber() { return lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isEphemeral() { return ephemeral; }
    public void setEphemeral(boolean ephemeral) { this.ephemeral = ephemeral; }

    public InjectionStatus getStatus() { return status; }
    public void setStatus(InjectionStatus status) { this.status = status; }

    public String getSuspendReason() { return suspendReason; }
    public void setSuspendReason(String suspendReason) { this.suspendReason = suspendReason; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldDescriptor() { return fieldDescriptor; }
    public void setFieldDescriptor(String fieldDescriptor) { this.fieldDescriptor = fieldDescriptor; }
}
