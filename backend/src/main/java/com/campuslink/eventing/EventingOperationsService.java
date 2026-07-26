package com.campuslink.eventing;

import com.campuslink.dto.EventingDtos.DeadLetterView;
import com.campuslink.dto.EventingDtos.EventingMetricsView;
import com.campuslink.dto.EventingDtos.EventingOperationsView;
import com.campuslink.entity.OutboxEventEntity;
import com.campuslink.repository.OutboxEventRepository;
import com.campuslink.service.AuditService;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventingOperationsService {

  private static final int MAX_VISIBLE_DEAD_LETTERS = 100;

  private final OutboxEventRepository outboxEvents;
  private final EventDeadLetterService consumerDeadLetters;
  private final AuditService auditService;

  public EventingOperationsService(
      OutboxEventRepository outboxEvents,
      EventDeadLetterService consumerDeadLetters,
      AuditService auditService) {
    this.outboxEvents = outboxEvents;
    this.consumerDeadLetters = consumerDeadLetters;
    this.auditService = auditService;
  }

  public EventingOperationsView operations() {
    return new EventingOperationsView(metrics(), deadLetters());
  }

  public EventingMetricsView metrics() {
    return new EventingMetricsView(
        outboxEvents.countByStatus("pending"),
        outboxEvents.countByStatus("retry"),
        outboxEvents.countByStatus("dead_letter"),
        consumerDeadLetters.countDeadLetters());
  }

  public List<DeadLetterView> deadLetters() {
    List<DeadLetterView> outbox = outboxEvents.findDeadLetters(MAX_VISIBLE_DEAD_LETTERS).stream()
        .map(this::outboxView)
        .toList();
    List<DeadLetterView> consumer = consumerDeadLetters.deadLetters(MAX_VISIBLE_DEAD_LETTERS).stream()
        .map(event -> new DeadLetterView(event.id(), "consumer", event.eventType(), event.status(),
            event.deliveryCount(), event.failureMessage(), event.deadLetteredAt(), event.replayedAt()))
        .toList();
    return java.util.stream.Stream.concat(outbox.stream(), consumer.stream())
        .sorted(Comparator.comparing(DeadLetterView::occurredAt).reversed())
        .limit(MAX_VISIBLE_DEAD_LETTERS)
        .toList();
  }

  @Transactional
  public void replay(String source, String id, String operatorName) {
    if ("outbox".equals(source)) {
      if (!outboxEvents.requeueDeadLetter(id)) {
        throw new IllegalArgumentException("Outbox 死信事件不存在或已重放");
      }
      auditService.addAudit("事件运维", operatorName + "重放 Outbox 死信事件 " + id);
      return;
    }
    if ("consumer".equals(source)) {
      consumerDeadLetters.replay(id);
      auditService.addAudit("事件运维", operatorName + "重放消费者死信事件 " + id);
      return;
    }
    throw new IllegalArgumentException("死信来源不支持");
  }

  private DeadLetterView outboxView(OutboxEventEntity event) {
    return new DeadLetterView(event.id(), "outbox", event.eventType(), event.status(), event.attempts(),
        event.lastError(), event.createdAt(), null);
  }
}
