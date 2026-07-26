package com.campuslink.eventing;

import com.campuslink.entity.OutboxEventEntity;
import com.campuslink.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("eventing")
public class OutboxPublisher {

  private static final int BATCH_SIZE = 100;

  private final OutboxEventRepository events;
  private final OutboxEventTransport transport;
  private final ObjectMapper objectMapper;
  private final int retryDelaySeconds;

  public OutboxPublisher(
      OutboxEventRepository events,
      OutboxEventTransport transport,
      ObjectMapper objectMapper,
      EventingProperties properties) {
    this.events = events;
    this.transport = transport;
    this.objectMapper = objectMapper;
    this.retryDelaySeconds = properties.outboxRetryDelaySeconds();
  }

  @Scheduled(fixedDelayString = "${campuslink.eventing.outbox-publish-interval-ms:1000}")
  public void publishPending() {
    publish(events.findReady(BATCH_SIZE));
  }

  void publish(List<OutboxEventEntity> pending) {
    for (OutboxEventEntity event : pending) {
      try {
        transport.publish(readMessage(event));
        events.markPublished(event.id());
      } catch (Exception error) {
        events.markRetry(event.id(), message(error), retryDelaySeconds);
      }
    }
  }

  private ActivityRegistrationMessage readMessage(OutboxEventEntity event)
      throws JsonProcessingException {
    return objectMapper.readValue(event.payload(), ActivityRegistrationMessage.class);
  }

  private String message(Exception error) {
    String text = error.getMessage();
    return text == null || text.isBlank() ? error.getClass().getSimpleName() : text;
  }
}
