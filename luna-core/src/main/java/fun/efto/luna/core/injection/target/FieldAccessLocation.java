package fun.efto.luna.core.injection.target;

import java.util.Arrays;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/05/26 16:00
 */
public class FieldAccessLocation extends InjectionLocation {

    private final List<String> aliases;
    private final FieldAccessTarget.AccessType accessType;

    public FieldAccessLocation(String name, String description, FieldAccessTarget.AccessType accessType, String... aliases) {
        super(name, description);
        this.accessType = accessType;
        this.aliases = Arrays.asList(aliases);
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public InjectionTarget createTarget(String className, String methodName, String methodDescriptor, Integer lineNumber) {
        String fieldName = methodName != null ? methodName : "";
        String fieldDescriptor = methodDescriptor != null ? methodDescriptor : "";
        return new FieldAccessTarget(this, className, fieldName, fieldDescriptor, accessType);
    }
}
