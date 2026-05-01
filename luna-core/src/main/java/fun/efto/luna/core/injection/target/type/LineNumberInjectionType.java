package fun.efto.luna.core.injection.target.type;

/**
 * 行号级注入类型
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/03/29 02:30
 */
public class LineNumberInjectionType extends InjectionType {

    public static final LineNumberInjectionType BEFORE = new LineNumberInjectionType("line_before", "行号前注入", 0);
    public static final LineNumberInjectionType AFTER = new LineNumberInjectionType("line_after", "行号后注入", 0);

    private final String name;
    private final String description;
    private final int lineNumber;

    public LineNumberInjectionType(String name, String description, int lineNumber) {
        this.name = name;
        this.description = description;
        this.lineNumber = lineNumber;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public LineNumberInjectionType withLineNumber(int lineNumber) {
        return new LineNumberInjectionType(this.name, this.description, lineNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LineNumberInjectionType that = (LineNumberInjectionType) o;
        return name.equals(that.name) && lineNumber == that.lineNumber;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + lineNumber;
        return result;
    }

    @Override
    public String toString() {
        return name + "(" + lineNumber + ")";
    }
}
