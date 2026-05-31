package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectionCommand;

import java.util.Set;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public interface ProbeHandler {

    String getProbeType();

    boolean usesCode();

    Set<String> supportedInjectionLocations();

    ValidationResult validate(InjectionCommand request);

    void handle(Object code, GenerateContext ctx);
}
