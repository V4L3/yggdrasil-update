package org.hyperagents.yggdrasil.cartago.artifacts;

import cartago.OPERATION;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.NumberSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.utils.time.SyncedArtifact;
import org.hyperagents.yggdrasil.utils.time.DynamicVectorClock;

import java.util.Map;

public class SyncedHypermediaTDArtifact extends HypermediaTDArtifact{
  protected DynamicVectorClock dynamicVectorClock;
  private HypermediaArtifactRegistry registry;

  public void init() {
    dynamicVectorClock = new DynamicVectorClock(getArtifactId().toString());
  }

  public void init(Object initializationParameters) {
    dynamicVectorClock = new DynamicVectorClock(getArtifactId().toString());

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
    dynamicVectorClock.addArtifact(newArtifact);
    log("Linked artifact " + artifactId + " to " + artifactEndpoint);
  }


  @OPERATION
  private void updateExternalTimestamp(String artifactId, String externalTimestamp) {

    int ts = Integer.parseInt(externalTimestamp);

    dynamicVectorClock.updateExternalTimestamp(artifactId, ts);

  }

  public void updateInternalTimestamp() {
    dynamicVectorClock.updateInternalTimestamp();
    registry.updateTimestamps(getArtifactId().toString(), dynamicVectorClock.getTimestamps());
  }

  public void injectRegistry(HypermediaArtifactRegistry registry) {
    this.registry = registry;
  }



  public final int getInternalTimestamp() {
    return dynamicVectorClock.getInternalTimestamp();
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
