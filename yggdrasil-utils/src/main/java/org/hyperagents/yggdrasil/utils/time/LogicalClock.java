package org.hyperagents.yggdrasil.utils.time;

public interface LogicalClock {
    int getInternalTimestamp();

    void updateInternalTimestamp();

    void updateExternalTimestamp(String externalArtifactId, int timestamp);

    void updateExternalTimestamp(String externalArtifactId);

    int getExternalTimestamp(String externalArtifactId);

    void propagateTimestampUpdate();

    <T> T getTimestamps();
}
