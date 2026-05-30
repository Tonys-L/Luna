package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.InjectionType;

public class FieldAccessTarget extends BaseTarget {

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since  : 2026/05/25 23:00
 */
    public enum AccessType { READ, WRITE }

    private final String fieldName;
    private final String fieldDescriptor;
    private final AccessType accessType;

    public FieldAccessTarget(InjectionType type, String className,
                             String fieldName, String fieldDescriptor, AccessType accessType) {
        super(type, className);
        this.fieldName = fieldName;
        this.fieldDescriptor = fieldDescriptor;
        this.accessType = accessType;
    }

    public String getFieldName() { return fieldName; }
    public String getFieldDescriptor() { return fieldDescriptor; }
    public AccessType getAccessType() { return accessType; }

    @Override
    public String getMethodName() { return fieldName; }
    @Override
    public String getMethodDescriptor() { return fieldDescriptor; }

    public int getLineNumber() { return -1; }
}
