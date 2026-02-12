package fun.efto.luna.core.type;

/**
 * @author ：Tony.L(286269159@qq.com)
 * @since ：2025/10/3 18:03
 */
public class BaseType {
    private final String name;
    private final String description;

    public BaseType(String name, String description) {
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
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BaseType that = (BaseType) obj;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
