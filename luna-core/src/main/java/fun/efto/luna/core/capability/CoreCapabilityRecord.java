package fun.efto.luna.core.capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since  : 2026/05/29 00:00
 */
public class CoreCapabilityRecord {

    private final String capabilityId;
    private final String displayName;
    private final CapabilityKind kind;
    private final List<String> providedEntries;
    private ReadinessState readinessState;
    private final List<String> dependencies;
    private final LifecyclePolicy lifecyclePolicy;

    public CoreCapabilityRecord(String capabilityId,
                                String displayName,
                                CapabilityKind kind,
                                List<String> providedEntries,
                                ReadinessState readinessState,
                                List<String> dependencies,
                                LifecyclePolicy lifecyclePolicy) {
        this.capabilityId = capabilityId;
        this.displayName = displayName;
        this.kind = kind;
        this.providedEntries = Collections.unmodifiableList(new ArrayList<>(providedEntries));
        this.readinessState = readinessState;
        this.dependencies = Collections.unmodifiableList(new ArrayList<>(dependencies));
        this.lifecyclePolicy = lifecyclePolicy;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CapabilityKind getKind() {
        return kind;
    }

    public List<String> getProvidedEntries() {
        return providedEntries;
    }

    public ReadinessState getReadinessState() {
        return readinessState;
    }

    public void setReadinessState(ReadinessState readinessState) {
        this.readinessState = readinessState;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public LifecyclePolicy getLifecyclePolicy() {
        return lifecyclePolicy;
    }

    public void markReady() {
        this.readinessState = ReadinessState.READY;
    }

    public void markDegraded() {
        this.readinessState = ReadinessState.DEGRADED;
    }

    public void markFailed() {
        this.readinessState = ReadinessState.FAILED;
    }
}
