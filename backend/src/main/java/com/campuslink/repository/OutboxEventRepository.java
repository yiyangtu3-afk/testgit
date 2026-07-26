package com.campuslink.repository;

import com.campuslink.entity.OutboxEventEntity;
import java.util.List;

public interface OutboxEventRepository {

  void create(String aggregateType, String aggregateId, String eventType, String payload);

  List<OutboxEventEntity> findReady(int limit);

  void markPublished(String eventId);

  void markRetry(String eventId, String message, int retryDelaySeconds);

  OutboxEventEntity findById(String eventId);
}
