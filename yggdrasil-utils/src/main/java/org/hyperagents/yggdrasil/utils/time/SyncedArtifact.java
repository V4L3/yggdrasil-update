package org.hyperagents.yggdrasil.utils.time;

public class SyncedArtifact {
    private String artifactId;
    private int timestamp;
    private String endpoint;

    public SyncedArtifact(String artifactId, int timestamp, String endpoint) {
        this.artifactId = artifactId;
        this.timestamp = timestamp;
        this.endpoint = endpoint;
    }

    public SyncedArtifact(String artifactId, String endpoint) {
        this.artifactId = artifactId;
        this.timestamp = 0;
        this.endpoint = endpoint;
    }

    public void incrementTimestamp() {
        this.timestamp++;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
