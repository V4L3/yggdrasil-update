package org.hyperagents.yggdrasil.utils.time;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

// TODO: Needs refactoring
public class DynamicVectorClock implements LogicalClock {

    private final String artifactId;

    private int timestamp;

    private final HashMap<String, SyncedArtifact> linkedArtifacts;

    public DynamicVectorClock(String artifactId) {
        this.timestamp = 0;
        this.artifactId = artifactId;
        linkedArtifacts = new HashMap<>();
    }

    @Override
    public void updateInternalTimestamp() {
        System.out.println("[" + artifactId + "]Updating clock for artifact: " + artifactId);
        this.timestamp++;
        propagateTimestampUpdate();
    }

    @Override
    public void updateExternalTimestamp(String externalArtifactId, int timestamp) {

        if (!linkedArtifacts.containsKey(externalArtifactId)) {
            System.out.println("[" + artifactId + "]Artifact " + externalArtifactId + " is not linked!");
            return;
        }
        if (linkedArtifacts.get(externalArtifactId).getTimestamp() + 1 != timestamp) {
            System.out.println("[" + artifactId + "]Out of sync Timestamp received from " + artifactId);
            System.out.println("[" + artifactId + "]Received TS was: " + timestamp + " but expected "
                    + (linkedArtifacts.get(externalArtifactId).getTimestamp() + 1));
        }
        linkedArtifacts.get(externalArtifactId).setTimestamp(timestamp);
        System.out.println("[" + artifactId + "]Artifact " + externalArtifactId + " has been updated to "
                + linkedArtifacts.get(externalArtifactId).getTimestamp());

    }

    @Override
    public void updateExternalTimestamp(String externalArtifactId) {
        if (!linkedArtifacts.containsKey(externalArtifactId)) {
            System.out.println("[" + artifactId + "]Artifact " + externalArtifactId + " is not linked!");
            return;
        }
        linkedArtifacts.get(externalArtifactId).incrementTimestamp();
    }

    @Override
    public int getExternalTimestamp(String externalArtifactId) {
        return linkedArtifacts.get(externalArtifactId).getTimestamp();
    }

    @Override
    public void propagateTimestampUpdate() {
        HttpClient client = HttpClient.newHttpClient();

        String payload = String.format("[\"%s\",\"%s\"]", artifactId, getInternalTimestamp());
        System.out.println("[" + artifactId + "]payload: " + payload);

        for (String artifact : linkedArtifacts.keySet()) {
            String endpoint = linkedArtifacts.get(artifact).getEndpoint();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("X-Agent-WebID", "http://localhost:8080/agents/" + artifactId) // Add additional header here
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("[" + artifactId + "]Response code from " + artifact + ": " + response.statusCode());
            } catch (Exception e) {
                System.out.println("[" + artifactId + "]Error updating artifact " + artifact + ": " + e.getMessage());
            }
        }
    }

    public void addArtifact(SyncedArtifact artifact) {

        if (linkedArtifacts.containsKey(artifact.getArtifactId())) {
            System.out.println("[" + artifactId + "]Artifact " + artifact.getArtifactId() + " is already linked!");
            return;
        }
        linkedArtifacts.put(artifact.getArtifactId(), artifact);
        System.out.println("[" + artifactId + "]Artifact " + artifactId + " has been linked" + " with Endpoint "
                + artifact.getEndpoint());
    }

    public void removeArtifact(String artifactId) {
        this.linkedArtifacts.remove(artifactId);
    }

    public int getInternalTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public HashMap<String, Integer> getTimestamps() {
        HashMap<String, Integer> vectorClock = new HashMap<>();

        for (Map.Entry<String, SyncedArtifact> entry : linkedArtifacts.entrySet()) {
            vectorClock.put(entry.getKey(), entry.getValue().getTimestamp());
        }

        vectorClock.put(this.artifactId, this.timestamp);

        return vectorClock;
    }

}
