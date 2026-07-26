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
  public void markPublished(String eventId) {
    mapper.markPublished(eventId);
  }

  @Override
  public void markRetry(String eventId, String message, int retryDelaySeconds) {
    mapper.markRetry(eventId, message, retryDelaySeconds);
  }

  @Override
  public OutboxEventEntity findById(String eventId) {
    return mapper.findById(eventId);
  }
}
