package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.NumberSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.utils.time.SyncedArtifact;
import org.hyperagents.yggdrasil.utils.time.VectorClock;

import java.util.Map;

public class SyncedHypermediaTDArtifact extends HypermediaTDArtifact{
  protected VectorClock vectorClock;
  private HypermediaArtifactRegistry registry;

  public void init() {
    vectorClock = new VectorClock(getArtifactId().toString());
  }

  public void init(Object initializationParameters) {
    vectorClock = new VectorClock(getArtifactId().toString());

    String artifactName;
    String artifactUpdateUrl;

    if (initializationParameters instanceof Map<?, ?>) {
      Map<?, ?> paramMap = (Map<?, ?>) initializationParameters;
      Object nameObject = paramMap.get("name");
      Object endpointObject = paramMap.get("endpoint");

      if (nameObject != null && endpointObject != null) {
        artifactName = nameObject.toString();
        artifactUpdateUrl = endpointObject.toString();
        linkArtifact(artifactName, artifactUpdateUrl);
      }
    } else {
      log("Could not read init parameters");
    }
  }

  @OPERATION
  public void linkArtifact(String artifactId, String artifactEndpoint) {
    SyncedArtifact newArtifact = new SyncedArtifact(artifactId, artifactEndpoint);
    vectorClock.addArtifact(newArtifact);
    log("Linked artifact " + artifactId + " to " + artifactEndpoint);
  }


  @OPERATION
  private void updateExternalTimestamp(String artifactId, String externalTimestamp) {

    int ts = Integer.parseInt(externalTimestamp);

    vectorClock.updateExternalTimestamp(artifactId, ts);

  }

  public void updateInternalTimestamp() {
    vectorClock.updateInternalTimestamp();
    registry.updateTimestamps(getArtifactId().toString(),vectorClock.getTimestamps());
  }

  public void injectRegistry(HypermediaArtifactRegistry registry) {
    this.registry = registry;
  }



  public final int getInternalTimestamp() {
    return vectorClock.getInternalTimestamp();
  }
  @Override
  protected void registerInteractionAffordances() {
    registerActionAffordance("http://example.org/linkArtifact", "linkArtifact", "linkArtifact",
      new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .addItem(new StringSchema.Builder().build())
        .build());
    registerActionAffordance("http://example.org/updateExternalTimestamp", "updateExternalTimestamp",
      "updateExternalTimestamp",
      new ArraySchema.Builder()
        .addItem(new StringSchema.Builder().build())
        .addItem(new NumberSchema.Builder().build())
        .build());
  }
}
