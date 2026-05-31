package fun.efto.luna.core.plugin;

import fun.efto.luna.core.injection.InjectRequest;
import fun.efto.luna.core.plugin.registry.InjectionTypeRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since  : 2026/06/01 01:30
 */
public abstract class AbstractProbeHandler implements ProbeHandler {

    @Override
    public ValidationResult validate(InjectRequest request) {
        String locationName = request.getInjectionLocation();
        if (locationName == null || locationName.isEmpty()) {
            return ValidationResult.fail("injectionLocation is required");
        }

        try {
            InjectionTypeRegistry.getInstance().resolve(locationName);
        } catch (IllegalArgumentException e) {
            return ValidationResult.fail("Unsupported injection location: " + locationName);
        }

        if (!supportedInjectionLocations().contains(locationName)) {
            return ValidationResult.fail("Probe type '" + getProbeType() + "' does not support location: " + locationName);
        }

        if (usesCode() && request.getCodeType() == null) {
            return ValidationResult.fail("codeType is required for probe type '" + getProbeType() + "'");
        }

        return doValidate(request);
    }

    protected ValidationResult doValidate(InjectRequest request) {
        return ValidationResult.ok();
    }
}
