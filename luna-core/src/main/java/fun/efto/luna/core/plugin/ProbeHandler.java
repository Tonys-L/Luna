package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.injection.code.CompiledCode;

import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public interface ProbeHandler {

    String getProbeType();

    boolean usesCode();

    Set<String> supportedInjectionLocations();

    ValidationResult validate(InjectRequest request);

    void handle(CompiledCode code, GenerateContext ctx);
}
