package fun.efto.luna.core.plugin.builtin.line;

import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionLocation;
import fun.efto.luna.core.injection.target.LineNumberTarget;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/16 10:00
 */
public class LineNumberInjectionLocation extends InjectionLocation {

    public static final LineNumberInjectionLocation BEFORE = new LineNumberInjectionLocation("line_before", "行号前注入", "line", 0, "LINE_BEFORE");
    public static final LineNumberInjectionLocation AFTER = new LineNumberInjectionLocation("line_after", "行号后注入", "line", 0, "LINE_AFTER");

    private final int lineNumber;
    private final List<String> aliases;

    public LineNumberInjectionLocation(String name, String description, String category, int lineNumber, String... aliases) {
        super(name, description, category);
        this.lineNumber = lineNumber;
        this.aliases = Arrays.asList(aliases);
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public LineNumberInjectionLocation withLineNumber(int lineNumber) {
        return new LineNumberInjectionLocation(getName(), getDescription(), getCategory(), lineNumber, aliases.toArray(new String[0]));
    }

    @Override
    public InjectionTarget createTarget(String className, String methodName, String methodDescriptor, Integer lineNumber) {
        int line = lineNumber != null ? lineNumber : 0;
        LineNumberInjectionLocation typeWithLine = this.withLineNumber(line);
        return new LineNumberTarget(typeWithLine, className, line, 0, methodName, methodDescriptor);
    }

    @Override
    public String toString() {
        return getName() + "(" + lineNumber + ")";
    }
}
