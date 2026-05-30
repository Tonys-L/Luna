package fun.efto.luna.core.plugin.builtin.line;

import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionType;
import fun.efto.luna.core.injection.target.LineNumberTarget;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/16 10:00
 */
public class LineNumberInjectionType extends InjectionType {

    public static final LineNumberInjectionType BEFORE = new LineNumberInjectionType("line_before", "行号前注入", 0, "LINE_BEFORE");
    public static final LineNumberInjectionType AFTER = new LineNumberInjectionType("line_after", "行号后注入", 0, "LINE_AFTER");

    private final int lineNumber;
    private final List<String> aliases;

    public LineNumberInjectionType(String name, String description, int lineNumber, String... aliases) {
        super(name, description);
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

    public LineNumberInjectionType withLineNumber(int lineNumber) {
        return new LineNumberInjectionType(getName(), getDescription(), lineNumber, aliases.toArray(new String[0]));
    }

    @Override
    public InjectionTarget createTarget(String className, String methodName, String methodDescriptor, Integer lineNumber) {
        int line = lineNumber != null ? lineNumber : 0;
        LineNumberInjectionType typeWithLine = this.withLineNumber(line);
        return new LineNumberTarget(typeWithLine, className, line, 0, methodName, methodDescriptor);
    }

    @Override
    public String toString() {
        return getName() + "(" + lineNumber + ")";
    }
}
