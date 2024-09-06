package org.hyperagents.yggdrasil.cartago.entities;

import cartago.CartagoEvent;
import cartago.ICartagoCallback;
import cartago.events.ArtifactObsEvent;
import cartago.util.agent.Percept;

import java.util.*;
import java.util.stream.Stream;

import org.hyperagents.yggdrasil.cartago.HypermediaArtifactRegistry;
import org.hyperagents.yggdrasil.eventbus.messageboxes.HttpNotificationDispatcherMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.HttpNotificationDispatcherMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

public class NotificationCallback implements ICartagoCallback {
  private final HttpInterfaceConfig httpConfig;
  private final HttpNotificationDispatcherMessagebox messagebox;
  private final String workspaceName;
  private final String artifactName;
  private final HypermediaArtifactRegistry registry;

  public NotificationCallback(
      final HttpInterfaceConfig httpConfig,
      final HttpNotificationDispatcherMessagebox messagebox,
      final String workspaceName,
      final String artifactName,
      final HypermediaArtifactRegistry registry) {
    this.httpConfig = httpConfig;
    this.messagebox = messagebox;
    this.workspaceName = workspaceName;
    this.artifactName = artifactName;
    this.registry = registry;
  }

  @Override
  public void notifyCartagoEvent(final CartagoEvent event) {
    if (event instanceof ArtifactObsEvent e) {
      final var percept = new Percept(e);

      if (percept.hasSignal()) {
        Stream
            .of(
                Optional.ofNullable(percept.getSignal()))
            .flatMap(Optional::stream)
            .forEach(p -> this.messagebox.sendMessage(
                new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
                    this.httpConfig.getArtifactUri(
                        workspaceName,
                        artifactName),
                    p.toString())));
        return;
      }

      assert percept.getArtifactSource().getName().equals(artifactName);
      assert percept.getArtifactSource().getWorkspaceId().getName().equals(workspaceName);

      Stream
          .of(
              Optional.ofNullable(percept.getPropChanged()),
              Optional.ofNullable(percept.getAddedProperties()),
              Optional.ofNullable(percept.getRemovedProperties()))
          .flatMap(Optional::stream)
          .flatMap(Arrays::stream)
          .forEach(p -> this.messagebox.sendMessage(
              new HttpNotificationDispatcherMessage.ArtifactObsPropertyUpdated(
                  this.httpConfig.getArtifactUri(
                      workspaceName,
                      artifactName),
                  formatMessage(p.toString(), e.getArtifactId().toString(), percept))));
    }
  }

  private String formatMessage(String message, String id, Percept percept) {
    HashMap<String, Integer> timestampsMap = registry.getTimestamps(id);
    List<Object> annots = percept.getPropChanged()[0].getAnnots();
    // Extract name and value of the obsProperty from the message
    String propertyName = "";
    String propertyValue = "";
    if (message.contains("(")) {
      int startIndex = message.indexOf("(");
      propertyName = message.substring(0, startIndex);
      propertyValue = message.substring(startIndex + 1, message.length() - 1);
    }

    // Start constructing the JSON-LD inspired string
    StringBuilder jsonLd = new StringBuilder();
    jsonLd.append("{\n");
    jsonLd.append("  \"@context\": \"https://www.w3.org/ns/activitystreams\",\n");
    jsonLd.append("  \"type\": \"Notification\",\n");
    jsonLd.append("  \"message\": {\n");
    jsonLd.append("    \"propertyName\": \"").append(propertyName).append("\",\n");
    jsonLd.append("    \"value\": \"").append(propertyValue).append("\"\n");
    jsonLd.append("  }");

    // Add timestamps if available
    if (!timestampsMap.isEmpty()) {
      jsonLd.append(",\n  \"timestamps\": {\n");
      int count = 0;
      for (Map.Entry<String, Integer> entry : timestampsMap.entrySet()) {
        jsonLd.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
        if (count < timestampsMap.size() - 1) {
          jsonLd.append(",");
        }
        jsonLd.append("\n");
        count++;
      }
      jsonLd.append("  }\n");
    } else {
      jsonLd.append("\n");
    }

    // Add annotations if available
    if (annots != null && !annots.isEmpty()) {
      jsonLd.append(",\n  \"annotations\": {\n");
      for (int i = 0; i < annots.size(); i++) {
        String annot = annots.get(i).toString();
        String annotName = annot.substring(0, annot.indexOf("("));
        String annotValue = annot.substring(annot.indexOf("(") + 1, annot.length() - 1);

        jsonLd.append("    \"").append(annotName).append("\": \"").append(annotValue).append("\"");
        if (i < annots.size() - 1) {
          jsonLd.append(",");
        }
        jsonLd.append("\n");
      }
      jsonLd.append("  }");
    }

    jsonLd.append("}");

    return jsonLd.toString();
  }
}
