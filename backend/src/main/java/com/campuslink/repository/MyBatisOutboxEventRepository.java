package com.campuslink.repository;

import com.campuslink.entity.OutboxEventEntity;
import com.campuslink.mapper.OutboxEventMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisOutboxEventRepository implements OutboxEventRepository {

  private final OutboxEventMapper mapper;

  public MyBatisOutboxEventRepository(OutboxEventMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void create(String aggregateType, String aggregateId, String eventType, String payload) {
    mapper.insert(UUID.randomUUID().toString().replace("-", ""), aggregateType, aggregateId,
        eventType, payload);
  }

  @Override
  public List<OutboxEventEntity> findReady(int limit) {
    return mapper.findReady(limit);
  }

  @Override
  public List<OutboxEventEntity> findDeadLetters(int limit) {
    return mapper.findDeadLetters(limit);
  }

  @Override
  public int countByStatus(String status) {
    return mapper.countByStatus(status);
  }

  @Override
  public void markPublished(String eventId) {
    mapper.markPublished(eventId);
  }

  @Override
  public void markRetryOrDeadLetter(
      String eventId, String message, int retryDelaySeconds, int maxAttempts) {
    mapper.markRetryOrDeadLetter(eventId, message, retryDelaySeconds, maxAttempts);
  }

  @Override
  public boolean requeueDeadLetter(String eventId) {
    return mapper.requeueDeadLetter(eventId) == 1;
  }

  @Override
  public OutboxEventEntity findById(String eventId) {
    return mapper.findById(eventId);
  }
}
