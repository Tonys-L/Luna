package fun.efto.luna.core.injection.target;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/26 16:00
 */
public class ConstructorLocation extends InjectionLocation {

    private final List<String> aliases;

    public ConstructorLocation(String name, String description, String... aliases) {
        super(name, description);
        this.aliases = Arrays.asList(aliases);
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public InjectionTarget createTarget(String className, String methodName, String methodDescriptor, Integer lineNumber) {
        String descriptor = methodDescriptor != null ? methodDescriptor : "";
        return new ConstructorTarget(this, className, descriptor);
    }
}
