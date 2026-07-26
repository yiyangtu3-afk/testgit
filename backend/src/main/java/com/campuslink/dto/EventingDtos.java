package com.campuslink.dto;

import java.time.LocalDateTime;
import java.util.Map;

public final class EventingDtos {

  private EventingDtos() {
  }

  public record EventingMetricsView(
      int pendingOutboxEvents,
      int retryingOutboxEvents,
      int deadLetterOutboxEvents,
      int deadLetterConsumerEvents) {
  }

  public record DeadLetterView(
      String id,
      String source,
      String eventType,
      String status,
      int attempts,
      String failureMessage,
      LocalDateTime occurredAt,
      LocalDateTime replayedAt) {
  }

  public record EventingOperationsView(
      EventingMetricsView metrics,
      java.util.List<DeadLetterView> deadLetters) {
  }
}
