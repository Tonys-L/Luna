package fun.efto.luna.core.injection.target;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/03/29 02:30
 */
public abstract class InjectionLocation {

    private final String name;
    private final String description;
    private final String category;

    protected InjectionLocation(String name, String description, String category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected InjectionLocation() {
        this.name = null;
        this.description = null;
        this.category = "other";
    }

    public static InjectionLocation of(String name, String description, String category, String... aliases) {
        final List<String> aliasList = aliases.length > 0
            ? Arrays.asList(aliases)
            : Collections.emptyList();
        return new InjectionLocation(name, description, category) {
            @Override
            public List<String> getAliases() { return aliasList; }
        };
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /** 分类标识，如 "method"、"line"、"other" */
    public String getCategory() {
        return category;
    }

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public InjectionTarget createTarget(String className, String methodName, String methodDescriptor, Integer lineNumber) {
        throw new UnsupportedOperationException(
            "InjectionLocation '" + getName() + "' must override createTarget() to specify how to create its InjectionTarget");
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InjectionLocation that = (InjectionLocation) o;
        return getName() != null && getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}
