package com.campuslink.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.entity.OutboxEventEntity;
import com.campuslink.repository.AuditRepository;
import com.campuslink.repository.OutboxEventRepository;
import com.campuslink.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventingOperationsServiceTest {

  @Test void exposesSeparateOutboxAndConsumerDeadLettersAndRequeuesOutbox() {
    var outbox = new RecordingOutboxRepository();
    outbox.deadLetters.add(outboxEvent());
    var consumerEvents = new EventDeadLetterServiceTest.RecordingDeadLetterRepository();
    var consumer = new EventDeadLetterService(consumerEvents, new ObjectMapper().findAndRegisterModules(),
        (topic, key, event) -> { });
    consumer.record(new ActivityRegistrationMessage("event-2", "activity.registration.waitlisted.v1",
        "registration-2", "activity-2", "student-2", "student-2", null, "waitlisted",
        LocalDateTime.of(2026, 7, 25, 12, 0), "校园摄影展", 1), "activity-2",
        "campuslink.activity.events.v1", "notification database unavailable");
    var audits = new RecordingAuditRepository();
    var service = new EventingOperationsService(outbox, consumer, new AuditService(audits));

    var operations = service.operations();
    service.replay("outbox", "outbox-1", "教务管理员");

    assertThat(operations.metrics().pendingOutboxEvents()).isEqualTo(2);
    assertThat(operations.metrics().retryingOutboxEvents()).isEqualTo(1);
    assertThat(operations.metrics().deadLetterOutboxEvents()).isEqualTo(1);
    assertThat(operations.metrics().deadLetterConsumerEvents()).isEqualTo(1);
    assertThat(operations.deadLetters()).extracting("source").containsExactlyInAnyOrder("outbox", "consumer");
    assertThat(outbox.requeued).containsExactly("outbox-1");
    assertThat(audits.events).containsExactly("教务管理员重放 Outbox 死信事件 outbox-1");
  }

  private OutboxEventEntity outboxEvent() {
    return new OutboxEventEntity("outbox-1", "activity-registration", "registration-1",
        "activity.registration.registered.v1", "{}", "dead_letter", 3,
        LocalDateTime.of(2026, 7, 25, 12, 0), null, "Kafka unavailable",
        LocalDateTime.of(2026, 7, 25, 11, 0));
  }

  private static final class RecordingOutboxRepository implements OutboxEventRepository {
    private final List<OutboxEventEntity> deadLetters = new ArrayList<>();
    private final List<String> requeued = new ArrayList<>();
    @Override public void create(String aggregateType, String aggregateId, String eventType, String payload) { }
    @Override public List<OutboxEventEntity> findReady(int limit) { return List.of(); }
    @Override public List<OutboxEventEntity> findDeadLetters(int limit) { return deadLetters; }
    @Override public int countByStatus(String status) { return switch (status) {
      case "pending" -> 2; case "retry" -> 1; case "dead_letter" -> 1; default -> 0; }; }
    @Override public void markPublished(String eventId) { }
    @Override public void markRetryOrDeadLetter(String eventId, String message, int retryDelaySeconds, int maxAttempts) { }
    @Override public boolean requeueDeadLetter(String eventId) { requeued.add(eventId); return true; }
    @Override public OutboxEventEntity findById(String eventId) { return null; }
  }

  private static final class RecordingAuditRepository implements AuditRepository {
    private final List<String> events = new ArrayList<>();
    @Override public void add(String module, String event) { events.add(event); }
    @Override public List<com.campuslink.entity.DemoEntities.AuditEventEntity> findRecent(int limit) { return List.of(); }
    @Override public int count() { return 0; }
    @Override public int deleteByIds(List<String> eventIds) { return 0; }
  }
}
