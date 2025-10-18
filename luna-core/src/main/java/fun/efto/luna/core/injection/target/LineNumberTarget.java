package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:36
 */
public class LineNumberTarget extends BaseTarget {
    private int lineNumber;
    private int lineNumberOffset;

    public LineNumberTarget() {

    }

    public LineNumberTarget(LineNumberInjectionType type, String targetClass, int lineNumber, int lineNumberOffset) {
        super(type, targetClass);
        this.lineNumber = lineNumber;
        this.lineNumberOffset = lineNumberOffset;
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
}
