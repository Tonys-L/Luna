package fun.efto.luna.core.injection;

import fun.efto.luna.core.injection.code.CompiledCode;
import fun.efto.luna.core.injection.target.InjectionTarget;
import fun.efto.luna.core.injection.target.InjectionLocation;

import java.util.UUID;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2025/10/2 20:41
 */
public class InjectionPoint {
    private final String id;
    private final InjectionTarget target;
    private final CompiledCode code;
    private final String codeType;
    private final String probeType;
    private final PersistentInjection source;

    public InjectionPoint(InjectionTarget target, CompiledCode code, String codeType, String probeType, PersistentInjection source) {
        this(UUID.randomUUID().toString(), target, code, codeType, probeType, source);
    }

    public InjectionPoint(String id, InjectionTarget target, CompiledCode code, String codeType, String probeType, PersistentInjection source) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.target = target;
        this.code = code;
        this.codeType = codeType;
        this.probeType = probeType;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public InjectionTarget getTarget() {
        return target;
    }

    public CompiledCode getCode() {
        return code;
    }

    public InjectionLocation getInjectionLocation() {
        return target.getLocation();
    }

    public String getCodeType() {
        return codeType;
    }

    public String getProbeType() {
        return probeType;
    }

    public PersistentInjection toPersistentInjection() {
        return source;
    }
}
