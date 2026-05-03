package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:36
 */
public class LineNumberTarget extends BaseTarget {
    private int lineNumber;
    private int lineNumberOffset;
    private String methodName;
    private String methodDescriptor;

    public LineNumberTarget() {

    }

    public LineNumberTarget(LineNumberInjectionType type, String targetClass, int lineNumber, int lineNumberOffset) {
        super(type, targetClass);
        this.lineNumber = lineNumber;
        this.lineNumberOffset = lineNumberOffset;
    }

    public LineNumberTarget(LineNumberInjectionType type, String targetClass, int lineNumber, int lineNumberOffset, String methodName, String methodDescriptor) {
        super(type, targetClass);
        this.lineNumber = lineNumber;
        this.lineNumberOffset = lineNumberOffset;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumberOffset() {
        return lineNumberOffset;
    }

    public void setLineNumberOffset(int lineNumberOffset) {
        this.lineNumberOffset = lineNumberOffset;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public void setMethodDescriptor(String methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }
}
