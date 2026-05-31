package fun.efto.luna.core.injection.code;

import fun.efto.luna.core.bytecode.asm.assembler.ExpressionSegment;

import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 10:00
 */
public class CompiledCode {
    private final String condition;
    private final String content;
    private final List<ExpressionSegment> segments;

    public CompiledCode(String condition, String content) {
        this(condition, content, null);
    }

    public CompiledCode(String condition, String content, List<ExpressionSegment> segments) {
        this.condition = condition;
        this.content = content;
        this.segments = segments;
    }

    public boolean hasCondition() {
        return condition != null && !condition.trim().isEmpty();
    }

    public String getCondition() {
        return condition;
    }

    public String getContent() {
        return content;
    }

    public List<ExpressionSegment> getSegments() {
        return segments;
    }
}
