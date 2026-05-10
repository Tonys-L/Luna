package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.InjectableCode;
import fun.efto.luna.core.injection.code.type.CodeType;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.type.InjectionType;

import java.util.UUID;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2025/10/2 20:41
 */
public class InjectionPoint {
    private final String id;
    private final InjectionTarget target;
    private final InjectableCode code;

    public InjectionPoint(InjectionTarget target, InjectableCode code) {
        this.id = UUID.randomUUID().toString();
        this.target = target;
        this.code = code;
    }

    public String getId() {
        return id;
    }

    public InjectionTarget getTarget() {
        return target;
    }

    public InjectableCode getCode() {
        return code;
    }

    public InjectionType getInjectionType() {
        return target.getType();
    }

    public CodeType getCodeType() {
        return code.getCodeType();
    }
}
