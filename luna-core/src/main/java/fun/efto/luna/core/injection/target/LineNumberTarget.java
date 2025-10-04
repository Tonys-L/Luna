package fun.efto.luna.core.injection.target;

import fun.efto.luna.core.injection.target.type.LineNumberInjectionType;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/4 4:36
 */
public class LineNumberTarget extends BaseTarget {
    private final int lineNumber;
    private final int lineNumberOffset;

    public LineNumberTarget(LineNumberInjectionType type, String targetClass, int lineNumber, int lineNumberOffset) {
        super(type, targetClass);
        this.lineNumber = lineNumber;
        this.lineNumberOffset = lineNumberOffset;
    }

}
