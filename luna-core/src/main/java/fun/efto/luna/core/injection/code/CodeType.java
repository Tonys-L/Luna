package fun.efto.luna.core.injection.code;

/**
 * 代码类型
 * @author : Tony.L(286269159@qq.com>)
 * @since ：2026/03/29 02:30
 */
public enum CodeType {

    /**
     * Java 代码
     */
    JAVA("java", "Java 代码"),

    /**
     * 表达式代码
     */
    EXPRESSION("expression", "表达式代码"),

    /**
     * 快照代码 (虚拟断点)
     */
    SNAPSHOT("snapshot", "快照代码");

    private final String name;
    private final String description;

    CodeType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }

    public static CodeType fromName(String name) {
        for (CodeType ct : values()) {
            if (ct.name.equalsIgnoreCase(name)) {
                return ct;
            }
        }
        throw new IllegalArgumentException("Unknown CodeType: " + name);
    }
}
