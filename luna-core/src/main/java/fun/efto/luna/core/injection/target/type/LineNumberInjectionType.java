package fun.efto.luna.core.injection.target.type;

/**
 * 行号级注入类型
 * @author ：Tony.L(<286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public class LineNumberInjectionType extends InjectionType {

    public static final LineNumberInjectionType BEFORE = new LineNumberInjectionType(0);
    public static final LineNumberInjectionType AFTER = new LineNumberInjectionType(0);

    private final int lineNumber;

    public LineNumberInjectionType(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public String getName() {
        return "lineNumber";
    }

    @Override
    public String getDescription() {
        return "行号级注入";
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LineNumberInjectionType that = (LineNumberInjectionType) o;
        return lineNumber == that.lineNumber;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + lineNumber;
        return result;
    }

    @Override
    public String toString() {
        return "lineNumber(" + lineNumber + ")";
    }
}
